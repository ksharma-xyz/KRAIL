package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.dhruva.location.Location
import xyz.ksharma.dhruva.location.LocationError
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.PermissionResultEvent
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeUserLocationManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntroLocateTest {

    private val fix = Location(latitude = -33.730592, longitude = 150.944024, timestamp = 0L)
    private val results = mutableListOf<Pair<LocationResult, LatLng?>>()
    private val record: (LocationResult, LatLng?) -> Unit = { result, location -> results += result to location }

    private fun manager(answer: suspend () -> Result<Location>) = FakeUserLocationManager(currentLocation = answer)

    @Test
    fun `a granted fix is sent once, as the fix, straight through`() = krailRunTest {
        val location = manager { Result.success(fix) }

        location.locateOnce(record)

        assertEquals(
            listOf<Pair<LocationResult, LatLng?>>(LocationResult.GRANTED to LatLng(fix.latitude, fix.longitude)),
            results,
        )
        assertEquals(1, location.currentLocationCalls)
    }

    @Test
    fun `a refused permission is sent once as denied, with no fix`() = krailRunTest {
        manager { Result.failure(LocationError.PermissionDenied()) }.locateOnce(record)

        assertEquals(listOf<Pair<LocationResult, LatLng?>>(LocationResult.DENIED to null), results)
    }

    @Test
    fun `an unknown location failure is sent once as unavailable`() = krailRunTest {
        manager { Result.failure(LocationError.Unknown(IllegalStateException("no fix"))) }.locateOnce(record)

        assertEquals(listOf<Pair<LocationResult, LatLng?>>(LocationResult.UNAVAILABLE to null), results)
    }

    @Test
    fun `a manager that throws is sent once as unavailable and the exception does not escape`() = krailRunTest {
        manager { error("the permission controller failed") }.locateOnce(record)

        assertEquals(listOf<Pair<LocationResult, LatLng?>>(LocationResult.UNAVAILABLE to null), results)
    }

    private fun allowed(answer: suspend () -> Result<Location>) =
        FakeUserLocationManager(permissionStatus = PermissionStatus.Granted, currentLocation = answer)

    @Test
    fun `a rider who allowed location and got no fix is sent as granted with no fix`() = krailRunTest {
        allowed { Result.failure(LocationError.Unknown(IllegalStateException("no fix"))) }.locateOnce(record)

        assertEquals(listOf<Pair<LocationResult, LatLng?>>(LocationResult.GRANTED_NO_FIX to null), results)
    }

    @Test
    fun `a fix that outlasts the bound after location was allowed is granted with no fix`() = krailRunTest {
        val never = CompletableDeferred<Result<Location>>()
        val location = allowed { never.await() }

        launch { location.locateOnce(record) }
        pumpOnce(LOCATE_BOUND_MS + 1)

        assertEquals(listOf<Pair<LocationResult, LatLng?>>(LocationResult.GRANTED_NO_FIX to null), results)
    }

    @Test
    fun `the Intro's request is reported as the Intro's`() = krailRunTest {
        val location = manager { Result.success(fix) }

        location.locateOnce(record)

        assertEquals(listOf(PermissionResultEvent.Source.INTRO), location.locationSources)
    }

    @Test
    fun `a wait with no answer sends nothing until the bound and then unavailable`() = krailRunTest {
        val never = CompletableDeferred<Result<Location>>()
        val location = manager { never.await() }

        val job = launch { location.locateOnce(record) }
        pumpOnce(LOCATE_BOUND_MS - 1)
        assertTrue(results.isEmpty(), "nothing is sent before the bound: $results")

        pumpOnce(2)

        assertEquals(listOf<Pair<LocationResult, LatLng?>>(LocationResult.UNAVAILABLE to null), results)
        assertFalse(job.isActive)
    }

    @Test
    fun `a caller that is cancelled is sent nothing`() = krailRunTest {
        val never = CompletableDeferred<Result<Location>>()
        val location = manager { never.await() }
        val job: Job = launch { location.locateOnce(record) }
        runCurrent()

        job.cancel()
        pumpOnce(LOCATE_BOUND_MS + 1)

        assertTrue(results.isEmpty(), "a cancelled ask must not answer: $results")
    }

    @Test
    fun `the effect asks while the card awaits and nothing is sent, and otherwise does not`() {
        assertEquals(LocateStep.Locate, locateStep(awaiting = true, sent = false))
        assertEquals(LocateStep.Wait, locateStep(awaiting = true, sent = true))
        assertEquals(LocateStep.Reset, locateStep(awaiting = false, sent = false))
        assertEquals(LocateStep.Reset, locateStep(awaiting = false, sent = true))
    }

    @Test
    fun `only a Locating card with no station id awaits the location`() {
        fun awaits(card: ParkRideCardPhase) = IntroState(parkRideCard = card).isAwaitingLocation()

        assertTrue(awaits(ParkRideCardPhase.Locating()))
        assertFalse(awaits(ParkRideCardPhase.Locating(stationId = "2153478")), "worked out again, no permission")
        assertFalse(awaits(ParkRideCardPhase.None))
        assertFalse(awaits(ParkRideCardPhase.NoStationNear))
    }

    @Test
    fun `only a refusal is a denial`() {
        assertEquals(LocationResult.DENIED, locationResultOf(LocationError.PermissionDenied()))
        assertEquals(LocationResult.UNAVAILABLE, locationResultOf(LocationError.Unknown(IllegalStateException())))
        assertEquals(LocationResult.UNAVAILABLE, locationResultOf(IllegalStateException()))
    }

    @Test
    fun `each step reports its own screen`() {
        assertEquals(AnalyticsScreen.IntroParkRide, IntroStep.PARK_RIDE.toAnalyticsScreen())
        assertEquals(AnalyticsScreen.IntroDestination, IntroStep.DESTINATION.toAnalyticsScreen())
    }
}
