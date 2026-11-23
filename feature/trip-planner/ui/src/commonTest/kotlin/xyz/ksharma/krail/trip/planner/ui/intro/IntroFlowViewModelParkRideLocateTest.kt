package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val SUGGESTION_FLAG_WRITE = "pref:KEY_PARK_RIDE_SUGGESTION_SHOWN=true"

private val TALLAWONG_CAR_PARKS = listOf("Tallawong P1", "Tallawong P2", "Tallawong P3")

class IntroFlowViewModelParkRideLocateTest {

    @Test
    fun `a fix inside five kilometres shows the nearest station and its car park names`() = krailRunTest {
        val rig = IntroRig(this)

        rig.showBellaVista()

        val card = rig.card()
        assertEquals("2153478", card.stationId)
        assertEquals("Bella Vista", card.stationName)
        assertEquals(listOf("Bella Vista"), card.carParks.map { it.name })
        assertTrue(card.carParks.none { it.hasReading })
        assertEquals(LocationResult.GRANTED, rig.state.locationResult)
        assertEquals(IntroStep.PARK_RIDE, rig.state.step, "the card is on the question step")
        assertFalse(rig.state.addedParkRide)
    }

    @Test
    fun `stops that become ready inside the wait still give the card`() = krailRunTest {
        val rig = IntroRig(this, initialReadiness = StopsReadiness.Loading)
        rig.showBellaVista()

        pumpOnce(PARK_RIDE_STOPS_WAIT_MS - 1_000)
        rig.readiness.emit(StopsReadiness.Ready(stopCount = 100))
        runCurrent()

        assertEquals("Bella Vista", rig.card().stationName)
    }

    @Test
    fun `answering yes records yes and starts locating`() = krailRunTest {
        val rig = IntroRig(this)

        rig.send(IntroUiEvent.AnswerParkRide(yes = true))

        assertEquals(ParkRideAnswer.YES, rig.state.parkRideAnswer)
        assertEquals(ParkRideCardPhase.Locating(), rig.state.parkRideCard)
        assertEquals(IntroStep.PARK_RIDE, rig.state.step)
        assertEquals(LocationResult.NOT_ASKED, rig.state.locationResult)
        assertEquals(0, rig.log.count(SUGGESTION_FLAG_WRITE))
    }

    @Test
    fun `a fix outside ten kilometres shows no station near and waits for OK, so the screen is not passed over`() = krailRunTest {
        // 12 km from the nearest station, and a rider in the city who is far from all three.
        listOf(TWELVE_KM_FROM_BELLA_VISTA, AT_CENTRAL).forEach { fix ->
            val rig = IntroRig(this)

            rig.send(
                IntroUiEvent.AnswerParkRide(yes = true),
                IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, fix),
            )

            assertEquals(ParkRideCardPhase.NoStationNear, rig.state.parkRideCard, "$fix")
            assertEquals(IntroStep.PARK_RIDE, rig.state.step, "nothing moves on without the rider's OK")
            assertEquals(LocationResult.GRANTED, rig.state.locationResult)
            assertEquals(0, rig.log.count(SUGGESTION_FLAG_WRITE), "the question is not left yet: $fix")
        }
    }

    @Test
    fun `the nearest of several stations in range is the one shown`() = krailRunTest {
        val rig = IntroRig(this)

        rig.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_TALLAWONG),
        )

        // Kellyville is also within five kilometres and sorts first in the list.
        assertEquals("Tallawong", rig.card().stationName)
        assertEquals(TALLAWONG_CAR_PARKS, rig.card().carParks.map { it.name })
    }

    @Test
    fun `no usable location moves to the trip step, marks the suggestion once and shows no card`() = krailRunTest {
        val cases = listOf(
            Triple(LocationResult.DENIED, null, LocationResult.DENIED),
            Triple(LocationResult.UNAVAILABLE, null, LocationResult.UNAVAILABLE),
            // A fix that never arrived behind a granted result is nothing to find a station from.
            Triple(LocationResult.GRANTED, null, LocationResult.UNAVAILABLE),
            // A fix behind a result that is not granted is dropped, never used.
            Triple(LocationResult.DENIED, AT_TALLAWONG, LocationResult.DENIED),
        )

        cases.forEach { (result, location, recorded) ->
            val rig = IntroRig(this)

            rig.send(IntroUiEvent.AnswerParkRide(yes = true), IntroUiEvent.LocationResultReceived(result, location))

            assertEquals(IntroStep.DESTINATION, rig.state.step, "$result $location")
            assertEquals(recorded, rig.state.locationResult, "$result $location")
            assertEquals(ParkRideCardPhase.None, rig.state.parkRideCard, "$result $location")
            assertEquals(ParkRideAnswer.YES, rig.state.parkRideAnswer, "$result $location")
            assertEquals(1, rig.log.count(SUGGESTION_FLAG_WRITE), "$result $location")
        }
    }

    @Test
    fun `a location result nobody asked for is ignored`() = krailRunTest {
        val rig = IntroRig(this)

        rig.send(IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_TALLAWONG))

        assertEquals(ON_PARK_RIDE, rig.state)

        rig.send(
            IntroUiEvent.AnswerParkRide(yes = false),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_TALLAWONG),
        )

        assertEquals(ParkRideCardPhase.None, rig.state.parkRideCard)
        assertEquals(LocationResult.NOT_ASKED, rig.state.locationResult)
    }

    @Test
    fun `a second yes while locating or while the card shows starts nothing new`() = krailRunTest {
        val rig = IntroRig(this)

        rig.send(IntroUiEvent.AnswerParkRide(yes = true), IntroUiEvent.AnswerParkRide(yes = true))
        assertEquals(ParkRideCardPhase.Locating(), rig.state.parkRideCard)

        rig.send(IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, FOUR_POINT_SIX_KM_FROM_BELLA_VISTA))
        val shown = rig.state
        rig.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_CENTRAL),
        )

        assertEquals(shown, rig.state)
    }

    @Test
    fun `answering yes again after no station near is dropped, because the rider has already moved on`() = krailRunTest {
        val rig = IntroRig(this)
        rig.passNoStationNear()
        assertEquals(ParkRideCardPhase.NoStationNear, rig.state.parkRideCard)
        assertEquals(IntroStep.DESTINATION, rig.state.step)
        val before = rig.state

        rig.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, FOUR_POINT_SIX_KM_FROM_BELLA_VISTA),
        )

        assertEquals(before, rig.state, "a Park & Ride answer from another screen changed the Intro")
    }

    @Test
    fun `answering no ends the look, drops the card and moves to the trip step`() = krailRunTest {
        val rig = IntroRig(this, initialReadiness = StopsReadiness.Loading)
        rig.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, FOUR_POINT_SIX_KM_FROM_BELLA_VISTA),
        )

        rig.send(IntroUiEvent.AnswerParkRide(yes = false))
        rig.readiness.emit(StopsReadiness.Ready(stopCount = 100))
        rig.send()

        assertEquals(ParkRideAnswer.NO, rig.state.parkRideAnswer)
        assertEquals(IntroStep.DESTINATION, rig.state.step)
        assertEquals(ParkRideCardPhase.None, rig.state.parkRideCard, "the look that was waiting is cancelled")
        assertEquals(1, rig.log.count(SUGGESTION_FLAG_WRITE))
    }

    @Test
    fun `stops that never get ready pass the screen over after ten seconds, with no message, because nothing is known about what is near`() =
        krailRunTest {
            val rig = IntroRig(this, initialReadiness = StopsReadiness.Loading)
            rig.send(
                IntroUiEvent.AnswerParkRide(yes = true),
                IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, FOUR_POINT_SIX_KM_FROM_BELLA_VISTA),
            )
            assertEquals(ParkRideCardPhase.Locating(), rig.state.parkRideCard, "waiting for the stops")

            pumpOnce(PARK_RIDE_STOPS_WAIT_MS - 1)
            assertEquals(ParkRideCardPhase.Locating(), rig.state.parkRideCard, "still inside the wait")
            assertEquals(IntroStep.PARK_RIDE, rig.state.step)
            pumpOnce(1)

            assertEquals(
                ParkRideCardPhase.None,
                rig.state.parkRideCard,
                "no message, because the rider was not told anything",
            )
            assertEquals(IntroStep.DESTINATION, rig.state.step)
            assertEquals(1, rig.log.count(SUGGESTION_FLAG_WRITE))
            assertEquals(10_000L, PARK_RIDE_STOPS_WAIT_MS)
        }

    @Test
    fun `not now while the stops are awaited gives up the look and the rider is not held on the wait`() =
        krailRunTest {
            val rig = IntroRig(this, initialReadiness = StopsReadiness.Loading)
            rig.send(
                IntroUiEvent.AnswerParkRide(yes = true),
                IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, FOUR_POINT_SIX_KM_FROM_BELLA_VISTA),
            )

            rig.send(IntroUiEvent.DeclineParkRide)
            rig.readiness.emit(StopsReadiness.Ready(stopCount = 100))
            pumpOnce(PARK_RIDE_STOPS_WAIT_MS * 2)

            assertEquals(IntroStep.DESTINATION, rig.state.step)
            assertEquals(ParkRideCardPhase.None, rig.state.parkRideCard, "the cancelled look never settles")
            assertEquals(1, rig.log.count(SUGGESTION_FLAG_WRITE))
        }

    @Test
    fun `a Park and Ride answer sent from a later screen is dropped, so it starts no look and never pulls anyone forward`() =
        krailRunTest {
            val rig = IntroRig(this, handle = savedAt(IntroState(step = IntroStep.DESTINATION)))
            val before = rig.state

            rig.send(
                IntroUiEvent.AnswerParkRide(yes = true),
                IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_CENTRAL),
            )

            assertEquals(before, rig.state)
            assertEquals(
                IntroStep.DESTINATION,
                rig.state.step,
                "the rider is not on the Park & Ride screen, so they stay put",
            )
        }

    @Test
    fun `stops that fail pass the screen over at once, with no error and no message`() = krailRunTest {
        val rig = IntroRig(this, initialReadiness = StopsReadiness.Failed(attempt = 1, reason = "disk"))

        rig.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, FOUR_POINT_SIX_KM_FROM_BELLA_VISTA),
        )

        assertEquals(ParkRideCardPhase.None, rig.state.parkRideCard)
        assertEquals(IntroStep.DESTINATION, rig.state.step)
    }

    @Test
    fun `clearing the view model cancels a look that is still waiting`() = krailRunTest {
        val rig = IntroRig(this, initialReadiness = StopsReadiness.Loading)
        rig.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, FOUR_POINT_SIX_KM_FROM_BELLA_VISTA),
        )
        assertEquals(ParkRideCardPhase.Locating(), rig.state.parkRideCard)

        rig.clearViewModel()
        pumpOnce(PARK_RIDE_STOPS_WAIT_MS * 2)
        rig.readiness.emit(StopsReadiness.Ready(stopCount = 100))
        runCurrent()

        assertEquals(ParkRideCardPhase.Locating(), rig.state.parkRideCard, "a leaked look would have settled by now")
    }
}
