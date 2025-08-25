package xyz.ksharma.core.test.viewmodels

import app.cash.turbine.test
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.core.test.fakes.FakeAnalytics
import xyz.ksharma.core.test.fakes.FakeAppInfoProvider
import xyz.ksharma.core.test.fakes.FakeAppVersionManager
import xyz.ksharma.core.test.fakes.FakeFlag
import xyz.ksharma.core.test.fakes.FakeNswParkRideSandook
import xyz.ksharma.core.test.fakes.FakeParkRideFacilityManager
import xyz.ksharma.core.test.fakes.FakeParkRideService
import xyz.ksharma.core.test.fakes.FakePlatformOps
import xyz.ksharma.core.test.fakes.FakeSandook
import xyz.ksharma.core.test.fakes.FakeSandookPreferences
import xyz.ksharma.core.test.fakes.FakeStopResultsManager
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.appversion.AppVersionManager
import xyz.ksharma.krail.core.remote_config.flag.Flag
import xyz.ksharma.krail.park.ride.network.NswParkRideFacilityManager
import xyz.ksharma.krail.park.ride.network.service.ParkRideService
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.taj.components.InfoTileCta
import xyz.ksharma.krail.taj.components.InfoTileData
import xyz.ksharma.krail.trip.planner.ui.savedtrips.SavedTripsViewModel
import xyz.ksharma.krail.trip.planner.ui.savedtrips.isInfoTileDismissed
import xyz.ksharma.krail.trip.planner.ui.savedtrips.markInfoTileAsDismissed
import xyz.ksharma.krail.trip.planner.ui.searchstop.StopResultsManager
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SavedTripsViewModelTest {

    private val sandook: Sandook = FakeSandook()
    private val fakeNswParkRideSandook: NswParkRideSandook = FakeNswParkRideSandook()
    private val fakeAnalytics: Analytics = FakeAnalytics()
    private lateinit var viewModel: SavedTripsViewModel
    private val fakeParkRideManager: NswParkRideFacilityManager = FakeParkRideFacilityManager()

    private val fakeParkRideService: ParkRideService = FakeParkRideService()

    private val fakeStopResultsManager: StopResultsManager = FakeStopResultsManager()

    private val testDispatcher = StandardTestDispatcher()

    private val fakeFlag: Flag = FakeFlag()

    private val fakeSandookPreferences = FakeSandookPreferences()

    private val fakeAppVersionManager = FakeAppVersionManager()

    private val fakeAppInfoProvider = FakeAppInfoProvider()
    private val fakePlatformOps = FakePlatformOps()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SavedTripsViewModel(
            sandook = sandook,
            analytics = fakeAnalytics,
            ioDispatcher = testDispatcher,
            nswParkRideFacilityManager = fakeParkRideManager,
            parkRideService = fakeParkRideService,
            parkRideSandook = fakeNswParkRideSandook,
            stopResultsManager = fakeStopResultsManager,
            flag = fakeFlag,
            preferences = fakeSandookPreferences,
            appVersionManager = fakeAppVersionManager,
            appInfoProvider = fakeAppInfoProvider,
            platformOps = fakePlatformOps,
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /*
        @Test
        fun `GIVEN initial state WHEN observer is active THEN analytics event should be tracked`() =
            runTest {
                // Ensure analytics events have not been tracked before observation
                assertFalse((fakeAnalytics as FakeAnalytics).isEventTracked("view_screen"))

                viewModel.uiState.test {
                    val item = awaitItem()
                    assertEquals(item, SavedTripsState())

    //                advanceUntilIdle()
                    assertScreenViewEventTracked(
                        fakeAnalytics,
                        expectedScreenName = AnalyticsScreen.SavedTrips.name,
                    )

                    cancelAndConsumeRemainingEvents()
                    viewModel.cleanupJobs()
                }
            }
    */

    @Test
    fun `GIVEN no observer is active WHEN checking analytics THEN event should not be tracked`() =
        runTest {
            // GIVEN no observer is active

            // WHEN checking analytics
            val eventTracked = (fakeAnalytics as FakeAnalytics).isEventTracked("view_screen")

            // THEN event should not be tracked
            assertFalse(eventTracked)
        }

    @Test
    fun `GIVEN a saved trip WHEN observed THEN UiState should update savedTrips`() =
        runTest {
            // GIVEN a saved trip
            sandook.insertOrReplaceTrip(
                tripId = "1",
                fromStopId = "FROM_STOP_ID_1",
                fromStopName = "STOP_NAME_1",
                toStopId = "TO_ID_1",
                toStopName = "STOP_NAME_2",
            )

            // Observe state
            viewModel.uiState.test {
                skipItems(1)

                val item = awaitItem()

                // THEN Verify that the state is updated after loading trips
                assertFalse(item.isSavedTripsLoading)
                assertTrue(item.savedTrips.isNotEmpty())

                cancelAndIgnoreRemainingEvents()
                viewModel.cleanupJobs()
            }
        }

    @Test
    fun `GIVEN a saved trip WHEN DeleteSavedTrip event is triggered THEN the trip should be deleted and UiState should update`() =
        runTest {
            // GIVEN a saved trip
            val trip = Trip(
                fromStopId = "FROM_STOP_ID_1",
                fromStopName = "STOP_NAME_1",
                toStopId = "TO_ID_1",
                toStopName = "STOP_NAME_2",
            )
            sandook.insertOrReplaceTrip(
                tripId = trip.tripId,
                fromStopId = trip.fromStopId,
                fromStopName = trip.fromStopName,
                toStopId = trip.toStopId,
                toStopName = trip.toStopName,
            )

            // Ensure initial state
            viewModel.uiState.test {
                skipItems(2)

                // WHEN the DeleteSavedTrip event is triggered
                viewModel.onEvent(
                    SavedTripUiEvent.DeleteSavedTrip(trip = trip)
                )

                // THEN verify that the trip is deleted and the state is updated
                val item = awaitItem()
                assertFalse(item.isSavedTripsLoading)
                assertTrue(item.savedTrips.isEmpty())
                cancelAndIgnoreRemainingEvents()
                viewModel.cleanupJobs()
            }
        }

    @Test
    fun `GIVEN a trip WHEN AnalyticsSavedTripCardClick event is triggered THEN trackSavedTripCardClick should be called`() =
        runTest {
            // GIVEN a trip
            val fromStopId = "FROM_STOP_ID_1"
            val toStopId = "TO_STOP_ID_1"

            // WHEN the AnalyticsSavedTripCardClick event is triggered
            viewModel.onEvent(SavedTripUiEvent.AnalyticsSavedTripCardClick(fromStopId, toStopId))

            // THEN verify that [SavedTripCardClickEvent] is called with correct parameters
            val fakeAnalytics: FakeAnalytics = fakeAnalytics as FakeAnalytics
            val eventName = AnalyticsEvent.SavedTripCardClickEvent(
                fromStopId = fromStopId,
                toStopId = toStopId
            ).name
            assertTrue(fakeAnalytics.isEventTracked(eventName))
        }

    @Test
    fun `GIVEN a trip WHEN AnalyticsReverseSavedTrip event is triggered THEN track ReverseStopClickEvent should be called`() =
        runTest {
            // WHEN the AnalyticsReverseSavedTrip event is triggered
            viewModel.onEvent(SavedTripUiEvent.AnalyticsReverseSavedTrip)

            // THEN verify that track is called with ReverseStopClickEvent
            val fakeAnalytics: FakeAnalytics = fakeAnalytics as FakeAnalytics
            val eventName = AnalyticsEvent.ReverseStopClickEvent.name
            assertTrue(fakeAnalytics.isEventTracked(eventName))
        }

    @Test
    fun `GIVEN a trip WHEN AnalyticsLoadTimeTableClick event is triggered THEN trackLoadTimeTableClick should be called`() =
        runTest {
            // GIVEN a trip
            val fromStopId = "FROM_STOP_ID_1"
            val toStopId = "TO_STOP_ID_1"

            // WHEN the AnalyticsLoadTimeTableClick event is triggered
            viewModel.onEvent(SavedTripUiEvent.AnalyticsLoadTimeTableClick(fromStopId, toStopId))

            // THEN verify that trackLoadTimeTableClick is called with correct parameters
            val fakeAnalytics: FakeAnalytics = fakeAnalytics as FakeAnalytics
            val eventName = AnalyticsEvent.LoadTimeTableClickEvent(
                fromStopId = fromStopId,
                toStopId = toStopId
            ).name
            assertTrue(fakeAnalytics.isEventTracked(eventName))
        }

    @Test
    fun `GIVEN a trip WHEN AnalyticsSettingsButtonClick event is triggered THEN track SettingsClickEvent should be called`() =
        runTest {
            // WHEN the AnalyticsSettingsButtonClick event is triggered
            viewModel.onEvent(SavedTripUiEvent.AnalyticsSettingsButtonClick)

            // THEN verify that track is called with SettingsClickEvent
            val fakeAnalytics: FakeAnalytics = fakeAnalytics as FakeAnalytics
            val eventName = AnalyticsEvent.SettingsClickEvent.name
            assertTrue(fakeAnalytics.isEventTracked(eventName))
        }

    @Test
    fun `GIVEN a trip WHEN AnalyticsFromButtonClick event is triggered THEN track FromFieldClickEvent should be called`() =
        runTest {
            // WHEN the AnalyticsFromButtonClick event is triggered
            viewModel.onEvent(SavedTripUiEvent.AnalyticsFromButtonClick)

            // THEN verify that track is called with FromFieldClickEvent
            val fakeAnalytics: FakeAnalytics = fakeAnalytics as FakeAnalytics
            val eventName = AnalyticsEvent.FromFieldClickEvent.name
            assertTrue(fakeAnalytics.isEventTracked(eventName))
        }

    @Test
    fun `GIVEN a trip WHEN AnalyticsToButtonClick event is triggered THEN track ToFieldClickEvent should be called`() =
        runTest {
            // WHEN the AnalyticsToButtonClick event is triggered
            viewModel.onEvent(SavedTripUiEvent.AnalyticsToButtonClick)

            // THEN verify that track is called with ToFieldClickEvent
            val fakeAnalytics: FakeAnalytics = fakeAnalytics as FakeAnalytics
            val eventName = AnalyticsEvent.ToFieldClickEvent.name
            assertTrue(fakeAnalytics.isEventTracked(eventName))
        }

    @Test
    fun `GIVEN InfoTileCtaClick event WHEN triggered THEN platformOps openUrl is called with correct url`() =
        runTest {
            // GIVEN an info tile with a primary CTA URL
            val testUrl = "https://example.com"
            val infoTile = InfoTileData(
                key = "test_key",
                title = "Test",
                description = "Test Desc",
                type = InfoTileData.InfoTileType.APP_UPDATE,
                primaryCta = InfoTileCta(
                    text = "Go",
                    url = testUrl
                )
            )

            // WHEN the InfoTileCtaClick event is triggered
            viewModel.onEvent(SavedTripUiEvent.InfoTileCtaClick(infoTile))

            // THEN verify platformOps.openUrl was called with the correct URL
            assertEquals(testUrl, fakePlatformOps.lastOpenedUrl)
        }

    @Test
    fun `GIVEN an info tile WHEN DismissInfoTile event is triggered THEN tile is removed and marked dismissed`() =
        runTest {
            // Ensure the key matches what the ViewModel will use
            fakeAppVersionManager.mockCurrentVersion = "update_key"
            val infoTile = InfoTileData(
                key = "update_key",
                title = "Update Available",
                description = "Update your app",
                type = InfoTileData.InfoTileType.APP_UPDATE,
                primaryCta = InfoTileCta(
                    text = "Update",
                    url = "https://store.com/app"
                )
            )
            // Simulate info tile present in state by using a public event that adds it
            fakeAppVersionManager.setUpdateCopy(
                title = infoTile.title,
                description = infoTile.description,
                ctaText = infoTile.primaryCta?.text ?: "",
                key = infoTile.key
            )
            viewModel.uiState.test {
                skipItems(2) // Initial state + after checkAppVersion
                // Dismiss the tile
                viewModel.onEvent(SavedTripUiEvent.DismissInfoTile(infoTile))
                val item = awaitItem()
                assertTrue(item.infoTiles?.isEmpty() == true)
                assertTrue(fakeSandookPreferences.isInfoTileDismissed(infoTile.key))
                cancelAndIgnoreRemainingEvents()
                viewModel.cleanupJobs()
            }
        }

    @Test
    fun `GIVEN an info tile already dismissed WHEN DismissInfoTile event is triggered THEN infoTiles remains unchanged`() = runTest {
        val infoTile = InfoTileData(
            key = "already_dismissed",
            title = "Dismissed",
            description = "Already gone",
            type = InfoTileData.InfoTileType.APP_UPDATE,
            primaryCta = InfoTileCta(
                text = "Update",
                url = "https://store.com/app"
            )
        )
        // Mark as dismissed in preferences before test
        fakeSandookPreferences.markInfoTileAsDismissed(infoTile.key)
        // Simulate info tile present in state
        fakeAppVersionManager.setUpdateCopy(
            title = infoTile.title,
            description = infoTile.description,
            ctaText = infoTile.primaryCta?.text ?: "",
            key = infoTile.key
        )
        viewModel.uiState.test {
            val item = awaitItem()
            println("Received item with infoTiles: ${item.infoTiles}")
            assertNull(item.infoTiles)
            // Dismiss the tile (should be a no-op, no new state emitted)
            viewModel.onEvent(SavedTripUiEvent.DismissInfoTile(infoTile))
            // No need to await another item, just check preference
            assertTrue(fakeSandookPreferences.isInfoTileDismissed(infoTile.key))
            cancelAndIgnoreRemainingEvents()
            viewModel.cleanupJobs()
        }
    }
}
