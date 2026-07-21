package xyz.ksharma.krail.trip.planner.ui.parkride

import kotlinx.coroutines.test.runTest
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeNswParkRideSandook
import xyz.ksharma.krail.core.testing.fakes.FakeParkRideService
import xyz.ksharma.krail.park.ride.network.model.CarParkFacilityDetailResponse
import xyz.ksharma.krail.park.ride.network.model.ParkingStopBatchResponse
import xyz.ksharma.krail.park.ride.network.service.ParkRideService
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.trip.planner.ui.savedtrips.filterRefreshableFacilities
import xyz.ksharma.krail.trip.planner.ui.savedtrips.parkRideApiCooldown
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideMapping
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * The map sheet and the home Park & Ride card are two entry points onto one API budget.
 *
 * Both write and read the same `lastApiCallTimestamp` per facility and both gate on
 * [filterRefreshableFacilities], so loading a station in the sheet must leave the home card
 * with nothing to fetch. If these ever diverge, a rider opening the sheet and then expanding
 * the card would spend two API calls for one piece of information — silently, since neither
 * screen would look wrong.
 */
@OptIn(ExperimentalTime::class)
class ParkRideAvailabilityLoaderTest {

    private val parkRideSandook: NswParkRideSandook = FakeNswParkRideSandook()
    private val service = CountingParkRideService()
    private val loader = RealParkRideAvailabilityLoader(
        parkRideSandook = parkRideSandook,
        parkRideService = service,
        flag = FakeFlag(),
    )

    private val mappings = listOf(
        ParkRideMapping(stopId = "2153478", facilityId = "31", facilityName = "Bella Vista"),
    )

    @Test
    fun `a first load calls the API once and records the call`() = runTest {
        loader.refreshIfNeeded(mappings)

        assertEquals(1, service.callCount)
        // The timestamp is what every other surface gates on.
        assertTrue((parkRideSandook.getLastApiCallTimestamp("31") ?: 0) > 0)
    }

    @Test
    fun `expanding the home card after the sheet loaded makes no second call`() = runTest {
        loader.refreshIfNeeded(mappings)
        assertEquals(1, service.callCount)

        // Exactly the gate SavedTripsViewModel applies when a card is expanded.
        val refreshable = filterRefreshableFacilities(
            parkRideSandook = parkRideSandook,
            facilityIds = setOf("31"),
            cooldown = parkRideApiCooldown(
                peakTimeCooldownSeconds = PEAK_COOLDOWN_SECONDS,
                nonPeakTimeCooldownSeconds = NON_PEAK_COOLDOWN_SECONDS,
            ),
            nowInstant = Clock.System.now(),
        )

        assertTrue(
            refreshable.isEmpty(),
            "Facility should be on cooldown after the sheet loaded it, so the home card " +
                "serves cached data instead of refetching",
        )

        // And going through the loader again is a no-op rather than a second call.
        loader.refreshIfNeeded(mappings)
        assertEquals(1, service.callCount)
    }

    @Test
    fun `a facility whose cooldown has elapsed is refetched`() = runTest {
        loader.refreshIfNeeded(mappings)
        assertEquals(1, service.callCount)

        // Rewind the recorded call well past the longest cooldown.
        parkRideSandook.updateApiCallTimestamp(
            facilityId = "31",
            timestamp = Instant.DISTANT_PAST.epochSeconds,
        )

        loader.refreshIfNeeded(mappings)
        assertEquals(2, service.callCount)
    }

    @Test
    fun `no mappings means no API call`() = runTest {
        loader.refreshIfNeeded(emptyList())

        assertEquals(0, service.callCount)
    }

    /** Wraps the shared fake purely to count network hits. */
    private class CountingParkRideService(
        private val delegate: ParkRideService = FakeParkRideService(),
    ) : ParkRideService {
        var callCount = 0
            private set

        override suspend fun fetchCarParkFacilities(
            facilityId: String,
        ): Result<CarParkFacilityDetailResponse> {
            callCount++
            return delegate.fetchCarParkFacilities(facilityId)
        }

        override suspend fun fetchCarParkFacilities(): Result<Map<String, String>> =
            delegate.fetchCarParkFacilities()

        override suspend fun fetchAvailabilityForStops(
            stopIds: List<String>,
        ): ParkingStopBatchResponse? = delegate.fetchAvailabilityForStops(stopIds)
    }

    private companion object {
        const val PEAK_COOLDOWN_SECONDS = 120L
        const val NON_PEAK_COOLDOWN_SECONDS = 600L
    }
}
