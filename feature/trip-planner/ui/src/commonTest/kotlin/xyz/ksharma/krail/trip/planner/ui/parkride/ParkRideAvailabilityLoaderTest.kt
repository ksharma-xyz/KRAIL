package xyz.ksharma.krail.trip.planner.ui.parkride

import kotlinx.coroutines.test.runTest
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeNswParkRideSandook
import xyz.ksharma.krail.core.testing.fakes.FakeParkRideService
import xyz.ksharma.krail.park.ride.network.model.CarParkFacilityDetailResponse
import xyz.ksharma.krail.park.ride.network.model.ParkingStopBatchResponse
import xyz.ksharma.krail.park.ride.network.model.StopFacilities
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

    // region Mappings spanning more than one stop
    //
    // The picker's sheet is per-station today, but nothing in the signature says so and the
    // cooldown bookkeeping is per-facility, not per-stop. A list covering two stations is what
    // exposes the difference between "these are the facilities to refresh" and "this is the
    // stop to ask about".

    @Test
    fun `both stops in a two-station list are asked about`() = runTest {
        val batchService = BatchParkRideService()

        loaderFor(batchService).refreshIfNeeded(TWO_STATIONS)

        assertEquals(
            setOf(BELLA_VISTA_STOP, NORWEST_STOP),
            batchService.requestedStopIds,
            "Only the first mapping's stop was fetched, so the second station's facilities " +
                "were counted as refreshable and then never asked for",
        )
    }

    @Test
    fun `a station the batch never asked about keeps its recorded call time`() = runTest {
        val batchService = BatchParkRideService()
        val batchLoader = loaderFor(batchService)
        givenBothStationsHaveGoneStale(batchLoader)
        batchService.requestedStopIds.clear()

        batchLoader.refreshIfNeeded(TWO_STATIONS)

        val norwestTimestamp = parkRideSandook.getLastApiCallTimestamp(NORWEST_FACILITY)
        assertTrue(
            (norwestTimestamp ?: 0) > 0,
            "Norwest was reset to DISTANT_PAST because it was missing from a batch that was " +
                "never asked for it — its cooldown is now permanently defeated, so every " +
                "refresh spends an API call on it",
        )
    }

    @Test
    fun `each facility is stored against its own stop`() = runTest {
        val fanOutLoader = loaderFor(CountingParkRideService())

        fanOutLoader.refreshIfNeeded(TWO_STATIONS)

        val norwestRows = parkRideSandook.getByStopIds(listOf(NORWEST_STOP))
        assertEquals(
            listOf(NORWEST_FACILITY),
            norwestRows.map { it.facilityId },
            "Norwest's car park was filed under Bella Vista's stop, because the fan-out was " +
                "handed one stop id for every facility in the list",
        )
    }

    /**
     * Both stations have been loaded once and have since gone cold, which is the state a
     * refresh actually runs against: a recorded call time that is old enough to be off
     * cooldown, and therefore distinguishable from the `DISTANT_PAST` a failure writes.
     */
    private suspend fun givenBothStationsHaveGoneStale(loader: ParkRideAvailabilityLoader) {
        loader.refreshIfNeeded(listOf(TWO_STATIONS[0]))
        loader.refreshIfNeeded(listOf(TWO_STATIONS[1]))
        val wellOffCooldown = Clock.System.now().epochSeconds - AN_HOUR_SECONDS
        parkRideSandook.updateApiCallTimestamp(BELLA_VISTA_FACILITY, wellOffCooldown)
        parkRideSandook.updateApiCallTimestamp(NORWEST_FACILITY, wellOffCooldown)
    }

    private fun loaderFor(service: ParkRideService) = RealParkRideAvailabilityLoader(
        parkRideSandook = parkRideSandook,
        parkRideService = service,
        flag = FakeFlag(),
    )

    /**
     * Answers the BFF batch call the way the real one does: only the stops that were asked
     * about come back, each carrying only its own facilities.
     */
    private class BatchParkRideService(
        private val delegate: ParkRideService = FakeParkRideService(),
    ) : ParkRideService by delegate {

        val requestedStopIds = mutableSetOf<String>()

        override suspend fun fetchAvailabilityForStops(
            stopIds: List<String>,
        ): ParkingStopBatchResponse {
            requestedStopIds += stopIds
            return ParkingStopBatchResponse(
                stops = stopIds
                    .mapNotNull { stopId -> FACILITIES_BY_STOP[stopId]?.let { stopId to it } }
                    .associate { (stopId, facilityId) ->
                        stopId to StopFacilities(
                            facilities = mapOf(
                                facilityId to FakeParkRideService
                                    .defaultFacilityResponses
                                    .getValue(facilityId),
                            ),
                        )
                    },
            )
        }
    }

    // endregion Mappings spanning more than one stop

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

        const val BELLA_VISTA_STOP = "2153478"
        const val BELLA_VISTA_FACILITY = "31"
        const val NORWEST_STOP = "9999999"
        const val NORWEST_FACILITY = "42"

        /** Comfortably past both the peak and the off-peak cooldown. */
        const val AN_HOUR_SECONDS = 3_600L

        val TWO_STATIONS = listOf(
            ParkRideMapping(
                stopId = BELLA_VISTA_STOP,
                facilityId = BELLA_VISTA_FACILITY,
                facilityName = "Bella Vista",
            ),
            ParkRideMapping(
                stopId = NORWEST_STOP,
                facilityId = NORWEST_FACILITY,
                facilityName = "Norwest",
            ),
        )

        val FACILITIES_BY_STOP = mapOf(
            BELLA_VISTA_STOP to BELLA_VISTA_FACILITY,
            NORWEST_STOP to NORWEST_FACILITY,
        )
    }
}
