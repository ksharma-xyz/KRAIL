package xyz.ksharma.krail.trip.planner.ui.savedtrips

import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.appreview.DelightMoment
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
import xyz.ksharma.krail.trip.planner.ui.searchstop.RealSearchSessionStore
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeAppReviewManager
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeInviteFriendsTileManager
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeStopResultsManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Saved Trips is the one calm screen the store review sheet is allowed to land on.
 *
 * Tapping a Park & Ride card is a delight moment that happens *on* that screen, so it arms and
 * fires in one place after a short delay rather than waiting for the next visit. The delay is
 * the whole risk: three seconds is long enough for the rider to open a timetable, the settings
 * screen or the stop search, and the review sheet is a system dialog that will happily open
 * over any of them. A sheet on the wrong screen is exactly what the calm-screen rule exists to
 * prevent, and it is invisible to a test that only checks the moment eventually fires.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SavedTripsReviewMomentTest {

    private val appReviewManager = FakeAppReviewManager()

    private val parkRideCard = ParkRideUiState(
        stopId = "STOP_1",
        stopName = "Bella Vista",
        facilities = persistentSetOf(),
        isLoading = false,
    )

    @Test
    fun `the review moment fires while the rider is still on saved trips`() = krailRunTest {
        val viewModel = buildViewModel()
        val screen = watchSavedTrips(viewModel)

        viewModel.onEvent(SavedTripUiEvent.ParkRideCardClick(parkRideCard, isExpanded = true))
        pumpOnce(REVIEW_DELAY_MS + BEAT_MS)

        assertEquals(
            listOf(DelightMoment.PARK_RIDE_CARD_TAPPED),
            appReviewManager.armedMoments,
            "Expanding a card on the calm screen is a delight moment and should still fire",
        )
        leaveSavedTrips(screen)
    }

    @Test
    fun `the review moment is dropped when the rider leaves before the delay elapses`() =
        krailRunTest {
            val viewModel = buildViewModel()
            val screen = watchSavedTrips(viewModel)

            viewModel.onEvent(SavedTripUiEvent.ParkRideCardClick(parkRideCard, isExpanded = true))

            // The rider taps into a timetable a beat later — well inside the review delay.
            pumpOnce(BEAT_MS)
            screen.cancel()
            pumpOnce(REVIEW_DELAY_MS + SUBSCRIPTION_GRACE_MS + BEAT_MS)

            assertTrue(
                appReviewManager.armedMoments.isEmpty(),
                "The review sheet was requested after the rider had already left Saved Trips, " +
                    "so it would have opened over whatever they opened instead",
            )
        }

    /** Stands in for the screen collecting `uiState` with `collectAsStateWithLifecycle`. */
    private fun KrailTestScope.watchSavedTrips(viewModel: SavedTripsViewModel): Job {
        val job = launch { viewModel.uiState.collect {} }
        pumpOnce(SCREEN_SETTLE_MS)
        return job
    }

    /**
     * Drop the screen's subscription and let `WhileSubscribed`'s grace expire, so the
     * ViewModel's own coroutines are finished before the test scope tears the main dispatcher
     * back down underneath them.
     */
    private fun KrailTestScope.leaveSavedTrips(screen: Job) {
        screen.cancel()
        pumpOnce(SUBSCRIPTION_GRACE_MS + BEAT_MS)
    }

    private fun KrailTestScope.buildViewModel() = SavedTripsViewModel(
        sandook = FakeSandook(),
        analytics = FakeAnalytics(),
        ioDispatcher = ioDispatcher,
        nswParkRideFacilityManager = FakeParkRideFacilityManager(),
        parkRideService = FakeParkRideService(),
        parkRideSandook = FakeNswParkRideSandook(),
        stopResultsManager = FakeStopResultsManager(),
        searchSessionStore = RealSearchSessionStore(),
        flag = FakeFlag(),
        preferences = FakeSandookPreferences(),
        infoTileManager = FakeInfoTileManager(),
        platformOps = FakePlatformOps(),
        inviteFriendsTileManager = FakeInviteFriendsTileManager(),
        trackingManager = TrackingManager(),
        appReviewManager = appReviewManager,
    )

    private companion object {
        /** Mirrors `PARK_RIDE_CARD_REVIEW_DELAY_MS`, which is private to the ViewModel. */
        const val REVIEW_DELAY_MS = 3_000L

        /** Long enough for the `onStart` bootstrap, including its own settle delay. */
        const val SCREEN_SETTLE_MS = 1_000L

        const val BEAT_MS = 500L

        /** Mirrors the `WhileSubscribed` grace `uiState` is shared with. */
        const val SUBSCRIPTION_GRACE_MS = 5_000L
    }
}
