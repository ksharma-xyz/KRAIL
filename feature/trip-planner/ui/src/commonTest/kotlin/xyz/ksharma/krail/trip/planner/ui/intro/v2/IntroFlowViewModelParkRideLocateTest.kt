package xyz.ksharma.krail.trip.planner.ui.intro.v2

import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideCardPhase
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
    fun `a fix outside five kilometres shows no station near`() = krailRunTest {
        // 5.4 km from the nearest station, and a rider in the city who is far from all three.
        listOf(FIVE_POINT_FOUR_KM_FROM_BELLA_VISTA, AT_CENTRAL).forEach { fix ->
            val rig = IntroRig(this)

            rig.send(
                IntroUiEvent.AnswerParkRide(yes = true),
                IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, fix),
            )

            assertEquals(ParkRideCardPhase.NoStationNear, rig.state.parkRideCard, "$fix")
            assertEquals(IntroStep.PARK_RIDE, rig.state.step, "the screen shows one plain line and Next")
            assertEquals(LocationResult.GRANTED, rig.state.locationResult)
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

            assertEquals(IntroStep.FIRST_TRIP, rig.state.step, "$result $location")
            assertEquals(recorded, rig.state.locationResult, "$result $location")
            assertEquals(ParkRideCardPhase.None, rig.state.parkRideCard, "$result $location")
            assertEquals(ParkRideAnswer.YES, rig.state.parkRideAnswer, "$result $location")
            assertEquals(1, rig.log.count(SUGGESTION_FLAG_WRITE), "$result $location")

            rig.send(IntroUiEvent.Back, IntroUiEvent.Next)
            assertEquals(1, rig.log.count(SUGGESTION_FLAG_WRITE), "written once however often the step is left")
        }
    }

    @Test
    fun `a location result nobody asked for is ignored`() = krailRunTest {
        val rig = IntroRig(this)

        rig.send(IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_TALLAWONG))

        assertEquals(IntroState(), rig.state)

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
    fun `answering yes again after no station near looks again`() = krailRunTest {
        val rig = IntroRig(this)
        rig.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_CENTRAL),
        )
        assertEquals(ParkRideCardPhase.NoStationNear, rig.state.parkRideCard)

        rig.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, FOUR_POINT_SIX_KM_FROM_BELLA_VISTA),
        )

        assertEquals("Bella Vista", rig.card().stationName)
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
        assertEquals(IntroStep.FIRST_TRIP, rig.state.step)
        assertEquals(ParkRideCardPhase.None, rig.state.parkRideCard, "the look that was waiting is cancelled")
        assertEquals(1, rig.log.count(SUGGESTION_FLAG_WRITE))
    }

    @Test
    fun `stops that never get ready give no station near after ten seconds and nothing blocks meanwhile`() =
        krailRunTest {
            val rig = IntroRig(this, initialReadiness = StopsReadiness.Loading)
            rig.send(
                IntroUiEvent.AnswerParkRide(yes = true),
                IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, FOUR_POINT_SIX_KM_FROM_BELLA_VISTA),
            )
            assertEquals(ParkRideCardPhase.Locating(), rig.state.parkRideCard, "waiting for the stops")

            rig.send(IntroUiEvent.Next)
            assertEquals(IntroStep.FIRST_TRIP, rig.state.step, "the rider is not held on the wait")
            assertEquals(1, rig.log.count(SUGGESTION_FLAG_WRITE))

            pumpOnce(PARK_RIDE_STOPS_WAIT_MS - 1)
            assertEquals(ParkRideCardPhase.Locating(), rig.state.parkRideCard, "still inside the wait")
            pumpOnce(1)

            assertEquals(ParkRideCardPhase.NoStationNear, rig.state.parkRideCard)
            assertEquals(10_000L, PARK_RIDE_STOPS_WAIT_MS)
        }

    @Test
    fun `stops that fail give no station near at once, with no error`() = krailRunTest {
        val rig = IntroRig(this, initialReadiness = StopsReadiness.Failed(attempt = 1, reason = "disk"))

        rig.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, FOUR_POINT_SIX_KM_FROM_BELLA_VISTA),
        )

        assertEquals(ParkRideCardPhase.NoStationNear, rig.state.parkRideCard)
        assertEquals(IntroStep.PARK_RIDE, rig.state.step)
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
