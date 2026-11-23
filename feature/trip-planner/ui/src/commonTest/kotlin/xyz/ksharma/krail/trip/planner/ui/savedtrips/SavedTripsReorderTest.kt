package xyz.ksharma.krail.trip.planner.ui.savedtrips

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.testing.coroutines.KrailTestScope
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeInfoTileManager
import xyz.ksharma.krail.core.testing.fakes.FakeNearbyStopsRepository
import xyz.ksharma.krail.core.testing.fakes.FakeNswParkRideSandook
import xyz.ksharma.krail.core.testing.fakes.FakeParkRideFacilityManager
import xyz.ksharma.krail.core.testing.fakes.FakeParkRideService
import xyz.ksharma.krail.core.testing.fakes.FakePlatformOps
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.feature.track.TrackingManager
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource.UserAdded
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.sandook.db.SavedParkRide
import xyz.ksharma.krail.trip.planner.ui.searchstop.RealSearchSessionStore
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.PARK_RIDE_ROW_KEY
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeAppReviewManager
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeInviteFriendsTileManager
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeStopResultsManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Reorder mode on the ViewModel (`docs/MAP_HOME_UX.md` rule 31): a long press begins a session, moves
 * and removals live only in it, Done writes once and cancel writes nothing, and leaving the screen
 * drops the session without saving. The reducer itself is `ReorderSessionTest`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SavedTripsReorderTest {

    private val sandook = FakeSandook()
    private val preferences = FakeSandookPreferences()
    private val analytics = FakeAnalytics()
    private val parkRideSandook: NswParkRideSandook = FakeNswParkRideSandook()
    private lateinit var lastViewModel: SavedTripsViewModel

    // region entering

    @Test
    fun `a long press begins a session with the row after the second trip and the pressed card lifted`() =
        krailRunTest {
            saveTrips(AB, CD, EF)
            saveStation()
            val screen = openHome(viewModel())

            val vm = lastViewModel
            vm.onEvent(SavedTripUiEvent.EnterReorder(liftedKey = CD))
            pumpOnce(BEAT_MS)

            val session = assertNotNull(vm.uiState.value.reorder)
            assertEquals(listOf(AB, CD, PARK_RIDE_ROW_KEY, EF), session.keys.toList())
            assertEquals(CD, session.lifted)
            leave(screen)
        }

    @Test
    fun `the row comes first with no trips and last place is clamped to the trips there are`() = krailRunTest {
        saveStation()
        val screen = openHome(viewModel())

        lastViewModel.onEvent(SavedTripUiEvent.EnterReorder(liftedKey = PARK_RIDE_ROW_KEY))
        pumpOnce(BEAT_MS)

        assertEquals(listOf(PARK_RIDE_ROW_KEY), lastViewModel.uiState.value.reorder?.keys?.toList())
        leave(screen)
    }

    @Test
    fun `with no trips and no station there is nothing to reorder`() = krailRunTest {
        val screen = openHome(viewModel())

        lastViewModel.onEvent(SavedTripUiEvent.EnterReorder(liftedKey = AB))
        pumpOnce(BEAT_MS)

        assertNull(lastViewModel.uiState.value.reorder)
        leave(screen)
    }

    @Test
    fun `a second long press while a session is open does not start it again`() = krailRunTest {
        saveTrips(AB, CD, EF)
        val screen = openHome(viewModel())
        lastViewModel.onEvent(SavedTripUiEvent.EnterReorder(liftedKey = AB))
        lastViewModel.onEvent(SavedTripUiEvent.ReorderMove(fromKey = AB, toKey = EF))
        pumpOnce(BEAT_MS)
        val moved = assertNotNull(lastViewModel.uiState.value.reorder)

        lastViewModel.onEvent(SavedTripUiEvent.EnterReorder(liftedKey = CD))
        pumpOnce(BEAT_MS)

        assertEquals(moved, lastViewModel.uiState.value.reorder)
        leave(screen)
    }

    @Test
    fun `the row's stored place is read with the trips and the session honours it`() = krailRunTest {
        saveTrips(AB, CD, EF)
        preferences.setLong(SandookPreferences.KEY_PARK_RIDE_ROW_INDEX, 1L)
        saveStation()
        val screen = openHome(viewModel())

        assertEquals(1, lastViewModel.uiState.value.parkRideRowIndex)
        lastViewModel.onEvent(SavedTripUiEvent.EnterReorder(liftedKey = AB))
        pumpOnce(BEAT_MS)

        assertEquals(listOf(AB, PARK_RIDE_ROW_KEY, CD, EF), lastViewModel.uiState.value.reorder?.keys?.toList())
        leave(screen)
    }

    // endregion

    // region nothing is written until Done

    @Test
    fun `moving and removing write nothing, and cancel drops the session and writes nothing`() = krailRunTest {
        saveTrips(AB, CD, EF)
        saveStation()
        val screen = openHome(viewModel())
        val vm = lastViewModel

        vm.onEvent(SavedTripUiEvent.EnterReorder(liftedKey = AB))
        vm.onEvent(SavedTripUiEvent.ReorderMove(fromKey = AB, toKey = EF))
        vm.onEvent(SavedTripUiEvent.ReorderMove(fromKey = PARK_RIDE_ROW_KEY, toKey = AB))
        vm.onEvent(SavedTripUiEvent.ReorderRemove(tripId = CD))
        pumpOnce(BEAT_MS)
        assertTrue(assertNotNull(vm.uiState.value.reorder).hasChanges, "Sanity: the session holds the edit")
        assertEquals(listOf(AB, CD, EF), storedOrder(), "Nothing is written while the session is open")

        vm.onEvent(SavedTripUiEvent.ReorderCancel)
        pumpOnce(BEAT_MS)

        assertNull(vm.uiState.value.reorder)
        assertEquals(listOf(AB, CD, EF), storedOrder(), "Cancel leaves the order and the trips as they were")
        assertNull(preferences.getLong(SandookPreferences.KEY_PARK_RIDE_ROW_INDEX))
        assertEquals(listOf(AB, CD, EF), vm.uiState.value.savedTrips.map { it.tripId })
        leave(screen)
    }

    // endregion

    // region Done

    @Test
    fun `done writes the new order and the row's place and shows them at once`() = krailRunTest {
        saveTrips(AB, CD, EF)
        saveStation()
        val screen = openHome(viewModel())
        val vm = lastViewModel
        vm.onEvent(SavedTripUiEvent.EnterReorder(liftedKey = AB))
        vm.onEvent(SavedTripUiEvent.ReorderMove(fromKey = EF, toKey = AB))
        vm.onEvent(SavedTripUiEvent.ReorderMove(fromKey = PARK_RIDE_ROW_KEY, toKey = AB))

        vm.onEvent(SavedTripUiEvent.ReorderDone)
        pumpOnce(BEAT_MS)

        // Start [AB, CD, row, EF]. EF to the front gives [EF, AB, CD, row], then the row to where AB is gives [EF, row, AB, CD].
        assertEquals(listOf(EF, AB, CD), storedOrder(), "The trips are written in the new order")
        assertEquals(1L, preferences.getLong(SandookPreferences.KEY_PARK_RIDE_ROW_INDEX), "The row's place is kept")
        assertNull(vm.uiState.value.reorder)
        assertEquals(listOf(EF, AB, CD), vm.uiState.value.savedTrips.map { it.tripId })
        assertEquals(1, vm.uiState.value.parkRideRowIndex)
        leave(screen)
    }

    @Test
    fun `done with nothing moved writes nothing at all`() = krailRunTest {
        saveTrips(AB, CD, EF)
        saveStation()
        val screen = openHome(viewModel())
        lastViewModel.onEvent(SavedTripUiEvent.EnterReorder(liftedKey = AB))

        lastViewModel.onEvent(SavedTripUiEvent.ReorderDone)
        pumpOnce(BEAT_MS)

        assertEquals(listOf(AB, CD, EF), storedOrder())
        assertNull(preferences.getLong(SandookPreferences.KEY_PARK_RIDE_ROW_INDEX))
        assertFalse(analytics.isEventTracked("saved_trip_card_reordered"))
        leave(screen)
    }

    @Test
    fun `a trip taken off in the session is deleted by done and not before`() = krailRunTest {
        saveTrips(AB, CD, EF)
        val screen = openHome(viewModel())
        lastViewModel.onEvent(SavedTripUiEvent.EnterReorder(liftedKey = CD))
        lastViewModel.onEvent(SavedTripUiEvent.ReorderRemove(tripId = CD))
        pumpOnce(BEAT_MS)
        assertEquals(listOf(AB, CD, EF), storedOrder(), "Staged only")

        lastViewModel.onEvent(SavedTripUiEvent.ReorderDone)
        pumpOnce(BEAT_MS)

        assertEquals(listOf(AB, EF), storedOrder())
        assertTrue(analytics.isEventTracked("delete_saved_trip_card_click"))
        leave(screen)
    }

    @Test
    fun `done reports a reorder for each trip whose place changed and for none that stayed`() = krailRunTest {
        saveTrips(AB, CD, EF)
        val screen = openHome(viewModel())
        lastViewModel.onEvent(SavedTripUiEvent.EnterReorder(liftedKey = AB))
        lastViewModel.onEvent(SavedTripUiEvent.ReorderMove(fromKey = AB, toKey = EF))

        lastViewModel.onEvent(SavedTripUiEvent.ReorderDone)
        pumpOnce(BEAT_MS)

        // Start [AB, CD, EF] and move AB to the end: CD and EF each move up one and AB moves down two.
        val moved = analytics.getTrackedEvents("saved_trip_card_reordered")
            .filterIsInstance<AnalyticsEvent.SavedTripCardReorderedEvent>()
        assertEquals(setOf("A", "C", "E"), moved.map { it.fromStopId }.toSet())
        val ab = moved.single { it.fromStopId == "A" }
        assertEquals(0, ab.previousIndex)
        assertEquals(2, ab.newIndex)
        assertEquals(3, ab.totalCount)
        assertIs<AnalyticsEvent.SavedTripCardReorderedEvent>(moved.first())
        leave(screen)
    }

    // endregion

    // region leaving and rotating

    @Test
    fun `a rotation keeps the session`() = krailRunTest {
        saveTrips(AB, CD, EF)
        val vm = viewModel()
        var screen = openHome(vm)
        vm.onEvent(SavedTripUiEvent.EnterReorder(liftedKey = AB))
        vm.onEvent(SavedTripUiEvent.ReorderMove(fromKey = AB, toKey = EF))
        pumpOnce(BEAT_MS)
        val before = assertNotNull(vm.uiState.value.reorder)

        // The Activity is recreated: the old collector goes and a new one comes, inside the grace.
        screen.cancel()
        pumpOnce(RECREATE_MS)
        screen = openHome(vm)

        assertEquals(before, vm.uiState.value.reorder)
        leave(screen)
    }

    @Test
    fun `being away past the grace drops the session without saving`() = krailRunTest {
        saveTrips(AB, CD, EF)
        val vm = viewModel()
        var screen = openHome(vm)
        vm.onEvent(SavedTripUiEvent.EnterReorder(liftedKey = AB))
        vm.onEvent(SavedTripUiEvent.ReorderMove(fromKey = AB, toKey = EF))

        screen.cancel()
        pumpOnce(AWAY_MS)
        screen = openHome(vm)

        assertNull(vm.uiState.value.reorder, "Backgrounding mid edit is a cancel")
        assertEquals(listOf(AB, CD, EF), storedOrder())
        leave(screen)
    }

    // endregion

    private fun storedOrder(): List<String> = sandook.selectAllTrips().sortedBy { it.sort_order }.map { it.tripId }

    private fun saveTrips(vararg ids: String) {
        ids.forEach { id ->
            val (from, to) = id.split("->")
            sandook.insertOrReplaceTrip(id, from, "Stop $from", to, "Stop $to")
        }
    }

    private suspend fun saveStation() {
        parkRideSandook.insertOrReplaceSavedParkRides(
            parkRideInfoList = setOf(
                SavedParkRide(
                    stopId = STATION,
                    facilityId = FACILITY,
                    stopName = "Bella Vista",
                    facilityName = "Car park",
                    source = UserAdded.value,
                ),
            ),
            source = UserAdded,
        )
    }

    private fun KrailTestScope.viewModel(): SavedTripsViewModel {
        lastViewModel = SavedTripsViewModel(
            sandook = sandook,
            analytics = analytics,
            ioDispatcher = ioDispatcher,
            nswParkRideFacilityManager = FakeParkRideFacilityManager(),
            parkRideService = FakeParkRideService(),
            parkRideSandook = parkRideSandook,
            stopResultsManager = FakeStopResultsManager(),
            searchSessionStore = RealSearchSessionStore(),
            flag = xyz.ksharma.krail.core.testing.fakes.FakeFlag(),
            preferences = preferences,
            infoTileManager = FakeInfoTileManager(),
            platformOps = FakePlatformOps(),
            inviteFriendsTileManager = FakeInviteFriendsTileManager(),
            trackingManager = TrackingManager(),
            appReviewManager = FakeAppReviewManager(),
            nearbyStopsRepository = FakeNearbyStopsRepository(),
        )
        return lastViewModel
    }

    /** Stands in for the screen collecting `uiState` with `collectAsStateWithLifecycle`. */
    private fun KrailTestScope.openHome(viewModel: SavedTripsViewModel): Job {
        val job = launch { viewModel.uiState.collect {} }
        pumpOnce(SCREEN_SETTLE_MS)
        return job
    }

    private fun KrailTestScope.leave(screen: Job) {
        screen.cancel()
        pumpOnce(SUBSCRIPTION_GRACE_MS + BEAT_MS)
    }

    private companion object {
        const val AB = "A->B"
        const val CD = "C->D"
        const val EF = "E->F"
        const val STATION = "2153478"
        const val FACILITY = "31"
        const val SUBSCRIPTION_GRACE_MS = 5_000L
        const val RECREATE_MS = 1_000L
        const val AWAY_MS = 6_000L
        const val SCREEN_SETTLE_MS = 1_000L
        const val BEAT_MS = 500L
    }
}
