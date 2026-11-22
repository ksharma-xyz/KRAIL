package xyz.ksharma.krail.trip.planner.ui.savedtrips

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.maps.data.model.NearbyStop
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
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.LabelChipTap
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.LabelTapOutcome
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeAppReviewManager
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeInviteFriendsTileManager
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeStopResultsManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What the Home and Work chips do inside the ViewModel (`docs/MAP_HOME_UX.md` rule 41): a tap
 * starts at most one thing at a time, a set label waits for one fix and then plans or falls back,
 * an unset label goes to the stop search, and a stop chosen there lands on the label. The state is
 * the ViewModel's, so the cases that matter to a rotation are here too.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SavedTripsLabelChipTest {

    private val sandook = FakeSandook()
    private val nearbyStops = FakeNearbyStopsRepository()

    // region a tap

    @Test
    fun `a tap on an unset chip decides to set that label`() = krailRunTest {
        val viewModel = buildViewModel()
        val screen = watchSavedTrips(viewModel)

        viewModel.onEvent(SavedTripUiEvent.LabelChipClick("Home"))

        assertEquals(LabelChipTap.Decided(LabelTapOutcome.Assign("Home")), tapOf(viewModel))
        leaveSavedTrips(screen)
    }

    @Test
    fun `a tap on a set chip waits for the one location fix`() = krailRunTest {
        setHome()
        val viewModel = buildViewModel()
        val screen = watchSavedTrips(viewModel)

        viewModel.onEvent(SavedTripUiEvent.LabelChipClick("Home"))

        assertEquals(LabelChipTap.Locating("Home"), tapOf(viewModel))
        leaveSavedTrips(screen)
    }

    @Test
    fun `a second tap while one is being worked out changes nothing`() = krailRunTest {
        setHome()
        setWork()
        val viewModel = buildViewModel()
        val screen = watchSavedTrips(viewModel)

        viewModel.onEvent(SavedTripUiEvent.LabelChipClick("Home"))
        viewModel.onEvent(SavedTripUiEvent.LabelChipClick("Work"))

        assertEquals(LabelChipTap.Locating("Home"), tapOf(viewModel), "One tap at a time, so one fix.")
        leaveSavedTrips(screen)
    }

    @Test
    fun `a tap on a label with no row still decides, it is one of the built in two`() = krailRunTest {
        val viewModel = buildViewModel()
        val screen = watchSavedTrips(viewModel)
        sandook.deleteStopLabel("Work")
        runCurrent()

        viewModel.onEvent(SavedTripUiEvent.LabelChipClick("Work"))

        assertEquals(LabelChipTap.Decided(LabelTapOutcome.Assign("Work")), tapOf(viewModel))
        leaveSavedTrips(screen)
    }

    @Test
    fun `a tap on a label that is neither Home nor Work does nothing`() = krailRunTest {
        val viewModel = buildViewModel()
        val screen = watchSavedTrips(viewModel)

        viewModel.onEvent(SavedTripUiEvent.LabelChipClick("Gym"))

        assertEquals(LabelChipTap.Idle, tapOf(viewModel))
        leaveSavedTrips(screen)
    }

    @Test
    fun `handling the decision steps the tap back to idle so the next tap starts fresh`() = krailRunTest {
        val viewModel = buildViewModel()
        val screen = watchSavedTrips(viewModel)
        viewModel.onEvent(SavedTripUiEvent.LabelChipClick("Home"))

        viewModel.onEvent(SavedTripUiEvent.LabelChipTapHandled)

        assertEquals(LabelChipTap.Idle, tapOf(viewModel), "A re-launched screen must find nothing to act on.")
        viewModel.onEvent(SavedTripUiEvent.LabelChipClick("Work"))
        assertEquals(LabelChipTap.Decided(LabelTapOutcome.Assign("Work")), tapOf(viewModel))
        leaveSavedTrips(screen)
    }

    // endregion

    // region the fix

    @Test
    fun `a fix and a stop near plans from the nearest stop to the labelled stop`() = krailRunTest {
        setHome()
        nearbyStops.addStop(CENTRAL)
        nearbyStops.addStop(REDFERN)
        val viewModel = buildViewModel()
        val screen = watchSavedTrips(viewModel)
        viewModel.onEvent(SavedTripUiEvent.LabelChipClick("Home"))

        viewModel.onEvent(SavedTripUiEvent.LabelChipLocated("Home", RIDER_LAT, RIDER_LON))
        runCurrent()

        assertEquals(
            LabelChipTap.Decided(
                LabelTapOutcome.Plan(
                    from = StopItem(stopId = "200060", stopName = "Central Station"),
                    to = HOME_STOP,
                ),
            ),
            tapOf(viewModel),
        )
        leaveSavedTrips(screen)
    }

    @Test
    fun `no fix falls back to the search with Home as the destination and no start`() = krailRunTest {
        setHome()
        nearbyStops.addStop(CENTRAL)
        val viewModel = buildViewModel()
        val screen = watchSavedTrips(viewModel)
        viewModel.onEvent(SavedTripUiEvent.LabelChipClick("Home"))

        viewModel.onEvent(SavedTripUiEvent.LabelChipLocated("Home", latitude = null, longitude = null))
        runCurrent()

        assertEquals(LabelChipTap.Decided(LabelTapOutcome.Search(to = HOME_STOP)), tapOf(viewModel))
        leaveSavedTrips(screen)
    }

    @Test
    fun `no stop within the radius falls back to the search`() = krailRunTest {
        setHome()
        nearbyStops.addStop(REDFERN)
        val viewModel = buildViewModel()
        val screen = watchSavedTrips(viewModel)
        viewModel.onEvent(SavedTripUiEvent.LabelChipClick("Home"))

        viewModel.onEvent(SavedTripUiEvent.LabelChipLocated("Home", RIDER_LAT, RIDER_LON))
        runCurrent()

        assertEquals(LabelChipTap.Decided(LabelTapOutcome.Search(to = HOME_STOP)), tapOf(viewModel))
        leaveSavedTrips(screen)
    }

    @Test
    fun `a stops lookup that fails is the same as no stop near, never a tap left pending`() = krailRunTest {
        setHome()
        nearbyStops.shouldThrowError = true
        val viewModel = buildViewModel()
        val screen = watchSavedTrips(viewModel)
        viewModel.onEvent(SavedTripUiEvent.LabelChipClick("Home"))

        viewModel.onEvent(SavedTripUiEvent.LabelChipLocated("Home", RIDER_LAT, RIDER_LON))
        runCurrent()

        assertEquals(LabelChipTap.Decided(LabelTapOutcome.Search(to = HOME_STOP)), tapOf(viewModel))
        leaveSavedTrips(screen)
    }

    @Test
    fun `a rider standing at the labelled stop gets the search, not a trip from it to itself`() = krailRunTest {
        setStop("Home", CENTRAL_ITEM)
        nearbyStops.addStop(CENTRAL)
        val viewModel = buildViewModel()
        val screen = watchSavedTrips(viewModel)
        viewModel.onEvent(SavedTripUiEvent.LabelChipClick("Home"))

        viewModel.onEvent(SavedTripUiEvent.LabelChipLocated("Home", RIDER_LAT, RIDER_LON))
        runCurrent()

        assertEquals(LabelChipTap.Decided(LabelTapOutcome.Search(to = CENTRAL_ITEM)), tapOf(viewModel))
        leaveSavedTrips(screen)
    }

    @Test
    fun `a fix nobody asked for is ignored`() = krailRunTest {
        setHome()
        nearbyStops.addStop(CENTRAL)
        val viewModel = buildViewModel()
        val screen = watchSavedTrips(viewModel)

        viewModel.onEvent(SavedTripUiEvent.LabelChipLocated("Home", RIDER_LAT, RIDER_LON))
        runCurrent()
        assertEquals(LabelChipTap.Idle, tapOf(viewModel), "No tap is waiting, so a fix is not a trip.")

        viewModel.onEvent(SavedTripUiEvent.LabelChipClick("Home"))
        viewModel.onEvent(SavedTripUiEvent.LabelChipLocated("Work", RIDER_LAT, RIDER_LON))
        runCurrent()
        assertEquals(LabelChipTap.Locating("Home"), tapOf(viewModel), "A fix for another label is not ours.")
        leaveSavedTrips(screen)
    }

    @Test
    fun `a label cleared while the fix was being taken ends the tap`() = krailRunTest {
        setHome()
        val viewModel = buildViewModel()
        val screen = watchSavedTrips(viewModel)
        viewModel.onEvent(SavedTripUiEvent.LabelChipClick("Home"))
        sandook.updateStopLabelStop("Home", null, null)
        runCurrent()

        viewModel.onEvent(SavedTripUiEvent.LabelChipLocated("Home", RIDER_LAT, RIDER_LON))
        runCurrent()

        assertEquals(LabelChipTap.Idle, tapOf(viewModel), "There is no stop left to plan to.")
        leaveSavedTrips(screen)
    }

    // endregion

    // region rotation and leaving

    @Test
    fun `a tap in flight survives the screen being recreated`() = krailRunTest {
        setHome()
        val viewModel = buildViewModel()
        var screen = watchSavedTrips(viewModel)
        viewModel.onEvent(SavedTripUiEvent.LabelChipClick("Home"))

        // A rotation: the collector goes and comes back inside the subscription grace.
        screen.cancel()
        pumpOnce(ROTATION_MS)
        screen = watchSavedTrips(viewModel)

        assertEquals(LabelChipTap.Locating("Home"), tapOf(viewModel), "The rotation must not lose the tap.")
        leaveSavedTrips(screen)
    }

    @Test
    fun `a tap left from before the rider was away is dropped, not replayed`() = krailRunTest {
        setHome()
        val viewModel = buildViewModel()
        val screen = watchSavedTrips(viewModel)
        viewModel.onEvent(SavedTripUiEvent.LabelChipClick("Home"))

        screen.cancel()
        pumpOnce(SUBSCRIPTION_GRACE_MS + BEAT_MS)
        val back = watchSavedTrips(viewModel)

        assertEquals(LabelChipTap.Idle, tapOf(viewModel), "Coming back later must not start a trip.")
        leaveSavedTrips(back)
    }

    // endregion

    // region setting a label from the chips

    @Test
    fun `a stop chosen for an unset label is stored and the label becomes set`() = krailRunTest {
        val viewModel = buildViewModel()
        val screen = watchSavedTrips(viewModel)

        viewModel.onEvent(SavedTripUiEvent.LabelStopAssigned("Home", "200070", "Town Hall Station"))
        runCurrent()

        val home = stateOf(viewModel).stopLabels.single { it.label == "Home" }
        assertTrue(home.isSet)
        assertEquals("200070", home.stopId)
        assertEquals("Town Hall Station", home.stopName)
        leaveSavedTrips(screen)
    }

    @Test
    fun `choosing a different stop for a set label replaces its stop`() = krailRunTest {
        setHome()
        val viewModel = buildViewModel()
        val screen = watchSavedTrips(viewModel)

        viewModel.onEvent(SavedTripUiEvent.LabelStopAssigned("Home", "200060", "Central Station"))
        runCurrent()

        assertEquals("200060", stateOf(viewModel).stopLabels.single { it.label == "Home" }.stopId)
        leaveSavedTrips(screen)
    }

    @Test
    fun `a label deleted in Manage is created again by setting it`() = krailRunTest {
        val viewModel = buildViewModel()
        val screen = watchSavedTrips(viewModel)
        sandook.deleteStopLabel("Work")
        runCurrent()

        viewModel.onEvent(SavedTripUiEvent.LabelStopAssigned("Work", "200070", "Town Hall Station"))
        runCurrent()

        val work = stateOf(viewModel).stopLabels.single { it.label == "Work" }
        assertEquals("200070", work.stopId)
        assertEquals(WORK_EMOJI, work.emoji, "It comes back with its own emoji.")
        leaveSavedTrips(screen)
    }

    @Test
    fun `a stop another label already holds is not given to a second label`() = krailRunTest {
        setHome()
        val viewModel = buildViewModel()
        val screen = watchSavedTrips(viewModel)

        viewModel.onEvent(SavedTripUiEvent.LabelStopAssigned("Work", HOME_STOP.stopId, HOME_STOP.stopName))
        runCurrent()

        assertNull(stateOf(viewModel).stopLabels.single { it.label == "Work" }.stopId, "A stop is on one label.")
        assertEquals(HOME_STOP.stopId, stateOf(viewModel).stopLabels.single { it.label == "Home" }.stopId)
        leaveSavedTrips(screen)
    }

    // endregion

    /** Home set to Town Hall, Work present and unset, as a rider who has only set Home has them. */
    private fun setHome() {
        sandook.upsertStopLabel("Work", WORK_EMOJI, null, null, 1)
        setStop("Home", HOME_STOP)
    }

    private fun setWork() = setStop("Work", StopItem(stopId = "200080", stopName = "Wynyard Station"))

    private fun setStop(label: String, stop: StopItem) {
        val emoji = if (label == "Home") HOME_EMOJI else WORK_EMOJI
        sandook.upsertStopLabel(label, emoji, stop.stopId, stop.stopName, if (label == "Home") 0 else 1)
    }

    /** What the screen would read now: the state after every coroutine due at this instant has run. */
    private fun KrailTestScope.stateOf(viewModel: SavedTripsViewModel): SavedTripsState {
        runCurrent()
        return viewModel.uiState.value
    }

    private fun KrailTestScope.tapOf(viewModel: SavedTripsViewModel): LabelChipTap =
        stateOf(viewModel).labelChipTap

    /** Stands in for the screen collecting `uiState` with `collectAsStateWithLifecycle`. */
    private fun KrailTestScope.watchSavedTrips(viewModel: SavedTripsViewModel): Job {
        val job = launch { viewModel.uiState.collect {} }
        pumpOnce(SCREEN_SETTLE_MS)
        return job
    }

    /** Drop the subscription and let its grace expire so the ViewModel's coroutines finish with the test. */
    private fun KrailTestScope.leaveSavedTrips(screen: Job) {
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
        nearbyStopsRepository = nearbyStops,
    )

    private companion object {
        /** Long enough for the `onStart` bootstrap, including its own settle delay. */
        const val SCREEN_SETTLE_MS = 1_000L
        const val BEAT_MS = 500L
        const val ROTATION_MS = 800L

        /** Mirrors the `WhileSubscribed` grace `uiState` is shared with. */
        const val SUBSCRIPTION_GRACE_MS = 5_000L

        const val HOME_EMOJI = "🏠"
        const val WORK_EMOJI = "💼"

        val HOME_STOP = StopItem(stopId = "200070", stopName = "Town Hall Station")
        val CENTRAL_ITEM = StopItem(stopId = "200060", stopName = "Central Station")

        /** A rider about 70 m from Central and about 1.06 km from Redfern. */
        const val RIDER_LAT = -33.8845
        const val RIDER_LON = 151.2065

        val CENTRAL = NearbyStop("200060", "Central Station", -33.8840, 151.2060, emptyList())
        val REDFERN = NearbyStop("201510", "Redfern Station", -33.8914, 151.1985, emptyList())
    }
}
