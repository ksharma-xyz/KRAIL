package xyz.ksharma.krail.trip.planner.ui.savedtrips

import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.core.testing.coroutines.KrailTestScope
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeInfoTileManager
import xyz.ksharma.krail.core.testing.fakes.FakeNswParkRideSandook
import xyz.ksharma.krail.core.testing.fakes.FakeParkRideFacilityManager
import xyz.ksharma.krail.core.testing.fakes.FakeParkRideService
import xyz.ksharma.krail.core.testing.fakes.FakePlatformOps
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.feature.track.TrackingManager
import xyz.ksharma.krail.park.ride.network.model.CarParkFacilityDetailResponse
import xyz.ksharma.krail.park.ride.network.model.ParkingStopBatchResponse
import xyz.ksharma.krail.park.ride.network.service.ParkRideService
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.trip.planner.ui.searchstop.RealSearchSessionStore
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeAppReviewManager
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeInviteFriendsTileManager
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeStopResultsManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * An open Park & Ride card should keep its number honest.
 *
 * The card exists to answer "can I still get a space", which is a number that moves while the
 * rider is looking at it. Until now it was fetched on the tap that opened the card and on the
 * next arrival at the screen, and never again: a rider who opened a card and kept watching it
 * saw the same figure indefinitely, while the ViewModel's own documentation described a
 * background refresh that did not exist.
 *
 * The poll is deliberately not a second way to spend the API budget. It only *triggers* the
 * same refresh the tap does; the per-facility cooldown still decides which facilities are
 * actually fetched, so a tick that finds everything fresh costs nothing.
 *
 * ## Why the recorded call time is rewound by hand
 *
 * That cooldown is measured against the wall clock, while the poll's own delay runs on virtual
 * time — so pumping the test clock forward moves the tick without moving the cooldown. Rewinding
 * the stored timestamp stands in for the elapsed minutes. The cooldown's own arithmetic is not
 * what these tests are about; it is covered by `ParkRideAvailabilityLoaderTest`. (An injectable
 * clock is landing separately, at which point the rewind can go.)
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class SavedTripsParkRidePollTest {

    private val parkRideSandook: NswParkRideSandook = FakeNswParkRideSandook()
    private val parkRideService = CountingParkRideService()
    private val flag = FakeFlag()

    private val bellaVista = ParkRideUiState(
        stopId = BELLA_VISTA_STOP,
        stopName = "Bella Vista",
        facilities = persistentSetOf(),
        isLoading = false,
    )

    @Test
    fun `an open card refreshes again once the cooldown has passed`() = krailRunTest {
        val viewModel = buildViewModel()
        val screen = watchSavedTrips(viewModel)
        expand(viewModel)
        val callsAfterTap = parkRideService.callCount

        letTheCooldownElapse()
        pumpOnce(ONE_TICK_MS)

        assertEquals(
            callsAfterTap + 1,
            parkRideService.callCount,
            "The card has been open for a full cooldown and its number has not been " +
                "refetched, so the rider is reading a figure from when they tapped",
        )
        leaveSavedTrips(screen)
    }

    @Test
    fun `a tick while the facility is still fresh spends nothing`() = krailRunTest {
        val viewModel = buildViewModel()
        val screen = watchSavedTrips(viewModel)
        expand(viewModel)
        val callsAfterTap = parkRideService.callCount

        // No rewind: the facility was fetched moments ago and is still on cooldown.
        pumpOnce(ONE_TICK_MS)

        assertEquals(
            callsAfterTap,
            parkRideService.callCount,
            "The poll must go through the shared cooldown, not around it",
        )
        leaveSavedTrips(screen)
    }

    @Test
    fun `a backgrounded screen does not poll`() = krailRunTest {
        val viewModel = buildViewModel()
        val screen = watchSavedTrips(viewModel)
        expand(viewModel)
        val callsAfterTap = parkRideService.callCount

        // The rider leaves; nothing collects uiState any more.
        screen.cancel()
        letTheCooldownElapse()
        pumpOnce(ONE_TICK_MS + SUBSCRIPTION_GRACE_MS)

        assertEquals(
            callsAfterTap,
            parkRideService.callCount,
            "Polling continued with the screen gone, which is the whole reason " +
                "WhileSubscribed gates it",
        )
    }

    @Test
    fun `collapsing the last card stops the poll`() = krailRunTest {
        val viewModel = buildViewModel()
        val screen = watchSavedTrips(viewModel)
        expand(viewModel)
        viewModel.onEvent(SavedTripUiEvent.ParkRideCardClick(bellaVista, isExpanded = false))
        pumpOnce(BEAT_MS)
        val callsAfterCollapse = parkRideService.callCount

        letTheCooldownElapse()
        pumpOnce(ONE_TICK_MS)

        assertEquals(
            callsAfterCollapse,
            parkRideService.callCount,
            "A closed card is not being read, so its number does not need keeping fresh",
        )
        leaveSavedTrips(screen)
    }

    /**
     * Drop the screen's subscription and let the grace expire, so the ViewModel's coroutines
     * are finished before the test scope tears the main dispatcher back down underneath them.
     */
    private fun KrailTestScope.leaveSavedTrips(screen: Job) {
        screen.cancel()
        pumpOnce(SUBSCRIPTION_GRACE_MS + BEAT_MS)
    }

    private fun KrailTestScope.expand(viewModel: SavedTripsViewModel) {
        viewModel.onEvent(SavedTripUiEvent.ParkRideCardClick(bellaVista, isExpanded = true))
        pumpOnce(BEAT_MS)
    }

    /** Stands in for the minutes that pass while the rider watches the card. */
    private suspend fun letTheCooldownElapse() {
        parkRideSandook.updateApiCallTimestamp(
            facilityId = BELLA_VISTA_FACILITY,
            timestamp = Instant.DISTANT_PAST.epochSeconds,
        )
    }

    /** Stands in for the screen collecting `uiState` with `collectAsStateWithLifecycle`. */
    private fun KrailTestScope.watchSavedTrips(viewModel: SavedTripsViewModel): Job {
        val job = launch { viewModel.uiState.collect {} }
        pumpOnce(SCREEN_SETTLE_MS)
        return job
    }

    private fun KrailTestScope.buildViewModel(): SavedTripsViewModel {
        // Same value either side of the peak boundary, so the interval under test does not
        // depend on what time of day CI happens to run at.
        flag.setFlagValue(
            FlagKeys.NSW_PARK_RIDE_PEAK_TIME_COOLDOWN.key,
            FlagValue.NumberValue(POLL_INTERVAL_SECONDS),
        )
        flag.setFlagValue(
            FlagKeys.NSW_PARK_RIDE_NON_PEAK_TIME_COOLDOWN.key,
            FlagValue.NumberValue(POLL_INTERVAL_SECONDS),
        )
        return SavedTripsViewModel(
            sandook = FakeSandook(),
            analytics = FakeAnalytics(),
            ioDispatcher = ioDispatcher,
            nswParkRideFacilityManager = FakeParkRideFacilityManager(),
            parkRideService = parkRideService,
            parkRideSandook = parkRideSandook,
            stopResultsManager = FakeStopResultsManager(),
            searchSessionStore = RealSearchSessionStore(),
            flag = flag,
            preferences = FakeSandookPreferences(),
            infoTileManager = FakeInfoTileManager(),
            platformOps = FakePlatformOps(),
            inviteFriendsTileManager = FakeInviteFriendsTileManager(),
            trackingManager = TrackingManager(),
            appReviewManager = FakeAppReviewManager(),
        )
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
        const val BELLA_VISTA_STOP = "2153478"
        const val BELLA_VISTA_FACILITY = "31"

        /** Both cooldown flags are set to this, so it is also the poll interval. */
        const val POLL_INTERVAL_SECONDS = 600L

        const val MILLIS_PER_SECOND = 1_000L

        /** The interval plus the poll's own margin, plus a beat to land past it. */
        const val ONE_TICK_MS = POLL_INTERVAL_SECONDS * MILLIS_PER_SECOND + 5_000L + 500L
        const val SUBSCRIPTION_GRACE_MS = 5_000L
        const val SCREEN_SETTLE_MS = 1_000L
        const val BEAT_MS = 500L
    }
}
