package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.testing.coroutines.KrailTestScope
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideLookup
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * What `intro_lets_krail` says about the search for a Park & Ride station: which distance found the station on the
 * card, that none was near, that the search could not be made, or that there was none. It never changes what the
 * rider sees.
 */
class IntroParkRideLookupTest {

    private fun KrailTestScope.answeredYes(
        result: LocationResult,
        location: LatLng?,
        readiness: StopsReadiness = StopsReadiness.Ready(stopCount = 100),
    ): IntroRig {
        val rig = IntroRig(this, initialReadiness = readiness)
        rig.send(IntroUiEvent.AnswerParkRide(yes = true), IntroUiEvent.LocationResultReceived(result, location))
        return rig
    }

    @Test
    fun `a station inside the closer distance is found near`() = krailRunTest {
        val rig = answeredYes(LocationResult.GRANTED, FOUR_KM_FROM_BELLA_VISTA)

        assertEquals(ParkRideLookup.FOUND_NEAR, rig.state.parkRideLookup)
    }

    @Test
    fun `a station only the wider distance reaches is found wider`() = krailRunTest {
        val rig = answeredYes(LocationResult.GRANTED, SEVEN_KM_FROM_BELLA_VISTA)

        assertEquals(ParkRideLookup.FOUND_WIDER, rig.state.parkRideLookup)
    }

    @Test
    fun `the message that none is near is none near`() = krailRunTest {
        val rig = answeredYes(LocationResult.GRANTED, TWELVE_KM_FROM_BELLA_VISTA)

        assertEquals(ParkRideLookup.NONE_NEAR, rig.state.parkRideLookup)
    }

    @Test
    fun `stops that failed leave a usable location with no answer, which is failed`() = krailRunTest {
        val rig = answeredYes(
            LocationResult.GRANTED,
            FOUR_KM_FROM_BELLA_VISTA,
            readiness = StopsReadiness.Failed(attempt = 1, reason = "disk"),
        )

        assertEquals(ParkRideLookup.FAILED, rig.state.parkRideLookup)
    }

    @Test
    fun `stops not ready inside the wait are failed`() = krailRunTest {
        val rig = answeredYes(LocationResult.GRANTED, FOUR_KM_FROM_BELLA_VISTA, readiness = StopsReadiness.Loading)

        pumpOnce(PARK_RIDE_STOPS_WAIT_MS)

        assertEquals(ParkRideLookup.FAILED, rig.state.parkRideLookup)
    }

    @Test
    fun `no usable location is not looked`() = krailRunTest {
        listOf(LocationResult.DENIED, LocationResult.UNAVAILABLE, LocationResult.GRANTED_NO_FIX).forEach { result ->
            val rig = answeredYes(result, location = null)

            assertEquals(ParkRideLookup.NOT_LOOKED, rig.state.parkRideLookup, "$result")
        }
    }

    @Test
    fun `answering no is not looked`() = krailRunTest {
        val rig = IntroRig(this)

        rig.send(IntroUiEvent.AnswerParkRide(yes = false))

        assertEquals(ParkRideLookup.NOT_LOOKED, rig.state.parkRideLookup)
    }

    @Test
    fun `a result the rider was shown is kept when the answer later changes`() = krailRunTest {
        val rig = answeredYes(LocationResult.GRANTED, SEVEN_KM_FROM_BELLA_VISTA)

        rig.send(IntroUiEvent.AnswerParkRide(yes = false))

        assertEquals(ParkRideLookup.FOUND_WIDER, rig.state.parkRideLookup)
    }

    @Test
    fun `the finish sends what the search came to`() = krailRunTest {
        val rig = answeredYes(LocationResult.GRANTED, TWELVE_KM_FROM_BELLA_VISTA)
        rig.send(IntroUiEvent.AcknowledgeNoStation)
        rig.send(
            IntroUiEvent.SelectStop(CENTRAL.toIntroStop()),
            IntroUiEvent.ConfirmStop,
            IntroUiEvent.SelectStop(TALLAWONG.toIntroStop()),
            IntroUiEvent.ConfirmStop,
        )

        rig.answerAndFinish(yes = true)

        assertEquals("none_near", rig.outcomeEvent().properties?.get("parkRideLookup"))
    }
}
