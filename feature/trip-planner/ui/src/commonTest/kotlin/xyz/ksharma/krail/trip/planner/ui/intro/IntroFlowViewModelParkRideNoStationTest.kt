package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.sandook.utils.GeoUtils
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val SUGGESTION_FLAG_WRITE = "pref:KEY_PARK_RIDE_SUGGESTION_SHOWN=true"

/**
 * The Park & Ride look goes out in two passes over the same list of stations, 5 km and then 10 km, and when neither finds one
 * the card's room says so and the one button, OK, moves on. Nothing moves on by itself.
 */
class IntroFlowViewModelParkRideNoStationTest {

    private fun IntroRig.answerYesAt(fix: LatLng) = send(
        IntroUiEvent.AnswerParkRide(yes = true),
        IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, fix),
    )

    private fun kmFromBellaVista(fix: LatLng): Double = assertNotNull(
        GeoUtils.haversineDistance(
            lat1 = fix.latitude,
            lon1 = fix.longitude,
            lat2 = BELLA_VISTA_STATION.position!!.latitude,
            lon2 = BELLA_VISTA_STATION.position!!.longitude,
        ),
    )

    @Test
    fun `the fixtures are the distances they are named for, and Bella Vista is the nearest station to each`() {
        mapOf(
            FOUR_KM_FROM_BELLA_VISTA to 4.0,
            SEVEN_KM_FROM_BELLA_VISTA to 7.0,
            NINE_POINT_NINE_KM_FROM_BELLA_VISTA to 9.9,
            TEN_POINT_ONE_KM_FROM_BELLA_VISTA to 10.1,
            TWELVE_KM_FROM_BELLA_VISTA to 12.0,
        ).forEach { (fix, km) ->
            assertEquals(km, kmFromBellaVista(fix), 0.01, "$fix")
            INTRO_STATIONS.filter { it != BELLA_VISTA_STATION }.forEach { other ->
                val position = other.position!!
                val away = GeoUtils.haversineDistance(fix.latitude, fix.longitude, position.latitude, position.longitude)!!
                assertTrue(away > kmFromBellaVista(fix), "${other.stationName} is nearer to $fix")
            }
        }
    }

    @Test
    fun `4 km finds the station on the first pass, and the list is read once`() = krailRunTest {
        val rig = IntroRig(this)

        rig.answerYesAt(FOUR_KM_FROM_BELLA_VISTA)

        assertEquals("Bella Vista", rig.card().stationName)
        assertEquals(IntroStep.PARK_RIDE, rig.state.step)
        assertEquals(1, rig.catalogue.stationReads, "the catalogue is read once")
    }

    @Test
    fun `7 km finds the station on the second pass, shown as the card is shown, from the same single read of the list`() = krailRunTest {
        val rig = IntroRig(this)
        rig.refreshWritesBellaVista()

        rig.answerYesAt(SEVEN_KM_FROM_BELLA_VISTA)

        val card = rig.card()
        assertEquals("Bella Vista", card.stationName)
        assertEquals(listOf("Bella Vista"), card.carParks.map { it.name })
        assertEquals(introBar(rig.state), introBar(rig.state), "the card has its own bar, Not now and Add to home")
        assertEquals("Add to home", introBar(rig.state)?.right?.label)
        assertEquals(1, rig.catalogue.stationReads, "the second pass walks the list already read, so it is no second read")
        assertEquals(1, rig.loader.refreshedMappings.size, "the one availability call, the same as at 4 km")
    }

    @Test
    fun `the edge is ten kilometres, 9_9 finds a station and 10_1 shows the message`() = krailRunTest {
        val near = IntroRig(this)
        near.answerYesAt(NINE_POINT_NINE_KM_FROM_BELLA_VISTA)
        assertEquals("Bella Vista", near.card().stationName)

        val far = IntroRig(this)
        far.answerYesAt(TEN_POINT_ONE_KM_FROM_BELLA_VISTA)
        assertEquals(ParkRideCardPhase.NoStationNear, far.state.parkRideCard)
    }

    @Test
    fun `12 km shows the message and one live OK, adds nothing, and makes no availability call or second read`() = krailRunTest {
        val rig = IntroRig(this)

        rig.answerYesAt(TWELVE_KM_FROM_BELLA_VISTA)

        assertEquals(ParkRideCardPhase.NoStationNear, rig.state.parkRideCard)
        assertEquals(IntroStep.PARK_RIDE, rig.state.step, "the screen stays until the rider answers")
        assertEquals(ParkRideAnswer.YES, rig.state.parkRideAnswer)
        assertEquals(LocationResult.GRANTED, rig.state.locationResult)
        assertTrue(!rig.state.addedParkRide)
        assertEquals(IntroCardSlot.NoStation, introCardSlot(rig.state))
        val bar = assertNotNull(introBar(rig.state))
        assertNull(bar.left)
        assertEquals("OK", bar.right.label)
        assertTrue(bar.shown && bar.rightShown)
        assertEquals(1, rig.catalogue.stationReads, "both passes ran over one read")
        assertTrue(rig.loader.refreshedMappings.isEmpty(), "no station, so no availability call")
        assertEquals(0, rig.log.count(SUGGESTION_FLAG_WRITE), "the question is not left until the rider answers")
    }

    @Test
    fun `nothing moves on by itself, however long the message is left`() = krailRunTest {
        val rig = IntroRig(this)
        rig.answerYesAt(TWELVE_KM_FROM_BELLA_VISTA)

        pumpOnce(PARK_RIDE_STOPS_WAIT_MS * 3)
        pumpOnce(FINISH_WAIT_TIMEOUT_MS)

        assertEquals(IntroStep.PARK_RIDE, rig.state.step)
        assertEquals(ParkRideCardPhase.NoStationNear, rig.state.parkRideCard)
    }

    @Test
    fun `OK moves on once, and a double tap lands once`() = krailRunTest {
        val rig = IntroRig(this)
        rig.answerYesAt(TWELVE_KM_FROM_BELLA_VISTA)

        rig.viewModel.onEvent(IntroUiEvent.AcknowledgeNoStation, pressedOn = IntroStep.PARK_RIDE)
        rig.viewModel.onEvent(IntroUiEvent.AcknowledgeNoStation, pressedOn = IntroStep.PARK_RIDE)
        rig.settle()

        assertEquals(IntroStep.DESTINATION, rig.state.step, "one tap, one screen, never past the destination")
        assertEquals(1, rig.log.count(SUGGESTION_FLAG_WRITE), "the question is left once")
        assertTrue(!rig.state.addedParkRide)
        assertEquals(ParkRideAnswer.YES, rig.state.parkRideAnswer)
        assertEquals(LocationResult.GRANTED, rig.state.locationResult)
    }

    @Test
    fun `OK does nothing anywhere else, so it cannot move a rider who is not on the message`() = krailRunTest {
        val onCard = IntroRig(this)
        onCard.showBellaVista()
        val card = onCard.state
        onCard.send(IntroUiEvent.AcknowledgeNoStation)
        assertEquals(card, onCard.state, "a card is not the message")

        val asking = IntroRig(this)
        asking.send(IntroUiEvent.AcknowledgeNoStation)
        assertEquals(ON_PARK_RIDE, asking.state)

        val past = IntroRig(this)
        past.passNoStationNear()
        val onDestination = past.state
        past.send(IntroUiEvent.AcknowledgeNoStation)
        assertEquals(onDestination, past.state, "a press from a screen the Intro has left is dropped")
    }

    @Test
    fun `a second yes on the message starts no new look`() = krailRunTest {
        val rig = IntroRig(this)
        rig.answerYesAt(TWELVE_KM_FROM_BELLA_VISTA)
        val message = rig.state

        rig.send(IntroUiEvent.AnswerParkRide(yes = true))

        assertEquals(message, rig.state)
        assertEquals(1, rig.catalogue.stationReads)
    }

    @Test
    fun `after the process is killed the message is still there with its OK, and nothing is looked up again`() = krailRunTest {
        val before = IntroRig(this)
        before.answerYesAt(TWELVE_KM_FROM_BELLA_VISTA)

        val after = IntroRig(this, handle = before.handle)
        after.settle()

        assertEquals(before.state, after.state)
        assertEquals(ParkRideCardPhase.NoStationNear, after.state.parkRideCard)
        assertEquals(IntroStep.PARK_RIDE, after.state.step)
        assertEquals("OK", introBar(after.state)?.right?.label)
        assertEquals(0, after.catalogue.stationReads, "a restored message needs no new look")
        assertTrue(after.loader.refreshedMappings.isEmpty())

        after.send(IntroUiEvent.AcknowledgeNoStation)

        assertEquals(IntroStep.DESTINATION, after.state.step)
    }

    @Test
    fun `a location that cannot be used still passes the screen over, with no message`() = krailRunTest {
        listOf(
            LocationResult.DENIED to null,
            LocationResult.UNAVAILABLE to null,
            LocationResult.GRANTED to null,
        ).forEach { (result, fix) ->
            val rig = IntroRig(this)

            rig.send(IntroUiEvent.AnswerParkRide(yes = true), IntroUiEvent.LocationResultReceived(result, fix))

            assertEquals(IntroStep.DESTINATION, rig.state.step, "$result")
            assertEquals(ParkRideCardPhase.None, rig.state.parkRideCard, "$result")
            assertEquals(0, rig.catalogue.stationReads, "$result")
        }
    }

    @Test
    fun `the outcome after OK is what it was when the screen passed itself over, and adds no event`() = krailRunTest {
        val rig = IntroRig(this)
        rig.answerYesAt(TWELVE_KM_FROM_BELLA_VISTA)
        rig.send(IntroUiEvent.AcknowledgeNoStation)
        rig.send(
            IntroUiEvent.SelectStop(CENTRAL.toIntroStop()), IntroUiEvent.ConfirmStop,
            IntroUiEvent.SelectStop(TALLAWONG.toIntroStop()), IntroUiEvent.ConfirmStop,
        )
        rig.answerAndFinish(yes = true)

        val params = rig.outcomeEvent().properties.orEmpty()
        assertEquals("yes", params["parkRideAnswer"])
        assertEquals("granted", params["locationResult"])
        assertEquals(false, params["addedParkRide"])
        assertEquals(1, rig.analytics.getTrackedEvents("intro_lets_krail").size)
    }
}
