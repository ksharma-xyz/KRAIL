package xyz.ksharma.krail.trip.planner.ui.savedtrips

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.testing.coroutines.KrailTestScope
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeInfoTileManager
import xyz.ksharma.krail.core.testing.fakes.FakeNearbyStopsRepository
import xyz.ksharma.krail.core.testing.fakes.FakeNswParkRideSandook
import xyz.ksharma.krail.core.testing.fakes.FakeParkRideFacilityManager
import xyz.ksharma.krail.core.testing.fakes.FakeParkRideService
import xyz.ksharma.krail.core.testing.fakes.FakePlatformOps
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.feature.track.TrackingManager
import xyz.ksharma.krail.trip.planner.ui.searchstop.RealSearchSessionStore
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.fromStopDisplay
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.toStopDisplay
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeAppReviewManager
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeInviteFriendsTileManager
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeStopResultsManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A saved trip card shows a label whenever its stop is the labelled stop (`docs/MAP_HOME_UX.md` rule 37),
 * and it must never draw first without it. The labels are read with the trips, so the first state that
 * holds a trip to draw already holds the labels, and a card cannot grow when its label lands (rule 22).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SavedTripsLabelsWithTripsTest {

    private val sandook = FakeSandook()

    @Test
    fun `the first list a card can draw from already holds the labels of its stops`() = krailRunTest {
        labelStop("Home", HOME_EMOJI, CENTRAL_ID, CENTRAL_NAME, sortOrder = 0)
        saveTrip(CENTRAL_ID, CENTRAL_NAME, TOWN_HALL_ID, TOWN_HALL_NAME)
        val viewModel = buildViewModel()
        val seen = mutableListOf<SavedTripsState>()

        val screen = launch { viewModel.uiState.collect { seen += it } }
        pumpOnce(SCREEN_SETTLE_MS)

        val drawable = seen.filter { !it.isSavedTripsLoading && it.savedTrips.isNotEmpty() }
        assertTrue(drawable.isNotEmpty(), "The trip was never offered to the screen.")
        drawable.forEach { state ->
            val trip = state.savedTrips.single()
            assertEquals("Home", trip.fromStopDisplay(state.stopLabels).label, "The card drew without its label.")
            assertNull(trip.toStopDisplay(state.stopLabels).label)
        }
        leave(screen)
    }

    @Test
    fun `a custom label reaches a card at either end`() = krailRunTest {
        labelStop("Gym", GYM_EMOJI, TOWN_HALL_ID, TOWN_HALL_NAME, sortOrder = 0)
        saveTrip(CENTRAL_ID, CENTRAL_NAME, TOWN_HALL_ID, TOWN_HALL_NAME)
        val viewModel = buildViewModel()

        val screen = watch(viewModel)

        val state = viewModel.uiState.value
        assertNull(state.savedTrips.single().fromStopDisplay(state.stopLabels).label)
        assertEquals("Gym", state.savedTrips.single().toStopDisplay(state.stopLabels).label)
        leave(screen)
    }

    @Test
    fun `a trip with no labelled stop shows no label`() = krailRunTest {
        labelStop("Home", HOME_EMOJI, "200099", "Somewhere Else", sortOrder = 0)
        saveTrip(CENTRAL_ID, CENTRAL_NAME, TOWN_HALL_ID, TOWN_HALL_NAME)
        val viewModel = buildViewModel()

        val screen = watch(viewModel)

        val state = viewModel.uiState.value
        assertNull(state.savedTrips.single().fromStopDisplay(state.stopLabels).label)
        assertNull(state.savedTrips.single().toStopDisplay(state.stopLabels).label)
        leave(screen)
    }

    @Test
    fun `a label set while the screen is open reaches the card at once`() = krailRunTest {
        saveTrip(CENTRAL_ID, CENTRAL_NAME, TOWN_HALL_ID, TOWN_HALL_NAME)
        val viewModel = buildViewModel()
        val screen = watch(viewModel)

        labelStop("Work", WORK_EMOJI, TOWN_HALL_ID, TOWN_HALL_NAME, sortOrder = 1)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals("Work", state.savedTrips.single().toStopDisplay(state.stopLabels).label)
        leave(screen)
    }

    private fun labelStop(label: String, emoji: String, stopId: String, stopName: String, sortOrder: Long) =
        sandook.upsertStopLabel(label, emoji, stopId, stopName, sortOrder)

    private fun saveTrip(fromId: String, fromName: String, toId: String, toName: String) =
        sandook.insertOrReplaceTrip(
            tripId = "$fromId->$toId",
            fromStopId = fromId,
            fromStopName = fromName,
            toStopId = toId,
            toStopName = toName,
        )

    /** Stands in for the screen collecting `uiState` with `collectAsStateWithLifecycle`. */
    private fun KrailTestScope.watch(viewModel: SavedTripsViewModel): Job {
        val job = launch { viewModel.uiState.collect {} }
        pumpOnce(SCREEN_SETTLE_MS)
        return job
    }

    private fun KrailTestScope.leave(screen: Job) {
        screen.cancel()
        pumpOnce(SUBSCRIPTION_GRACE_MS + BEAT_MS)
    }

    private fun KrailTestScope.buildViewModel() = SavedTripsViewModel(
        sandook = sandook,
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
        appReviewManager = FakeAppReviewManager(),
        nearbyStopsRepository = FakeNearbyStopsRepository(),
    )

    private companion object {
        const val SCREEN_SETTLE_MS = 1_000L
        const val BEAT_MS = 500L

        /** Mirrors the `WhileSubscribed` grace `uiState` is shared with. */
        const val SUBSCRIPTION_GRACE_MS = 5_000L

        const val HOME_EMOJI = "🏠"
        const val WORK_EMOJI = "💼"
        const val GYM_EMOJI = "🏋"
        const val CENTRAL_ID = "200060"
        const val CENTRAL_NAME = "Central Station"
        const val TOWN_HALL_ID = "200070"
        const val TOWN_HALL_NAME = "Town Hall Station"
    }
}
