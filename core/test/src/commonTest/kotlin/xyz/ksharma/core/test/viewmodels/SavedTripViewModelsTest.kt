package xyz.ksharma.core.test.viewmodels

import app.cash.turbine.test
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.core.test.fakes.FakeAnalytics
import xyz.ksharma.core.test.fakes.FakeAppVersionManager
import xyz.ksharma.core.test.fakes.FakeFlag
import xyz.ksharma.core.test.fakes.FakeInfoTileManager
import xyz.ksharma.core.test.fakes.FakeNswParkRideSandook
import xyz.ksharma.core.test.fakes.FakeParkRideFacilityManager
import xyz.ksharma.core.test.fakes.FakeParkRideService
import xyz.ksharma.core.test.fakes.FakePlatformOps
import xyz.ksharma.core.test.fakes.FakeSandook
import xyz.ksharma.core.test.fakes.FakeSandookPreferences
import xyz.ksharma.core.test.fakes.FakeStopResultsManager
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.info.tile.network.api.db.isInfoTileDismissed
import xyz.ksharma.krail.info.tile.network.api.db.markInfoTileAsDismissed
import xyz.ksharma.krail.info.tile.state.InfoTileCta
import xyz.ksharma.krail.info.tile.state.InfoTileData
import xyz.ksharma.krail.park.ride.network.NswParkRideFacilityManager
import xyz.ksharma.krail.park.ride.network.service.ParkRideService
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SandookPreferences.Companion.KEY_DISMISSED_INFO_TILES
import xyz.ksharma.krail.trip.planner.ui.savedtrips.SavedTripsViewModel
import xyz.ksharma.krail.trip.planner.ui.searchstop.StopResultsManager
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
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

    private val fakeFlag = FakeFlag()

    private val fakeSandookPreferences = FakeSandookPreferences()

    private val fakeAppVersionManager = FakeAppVersionManager()

    private val fakePlatformOps = FakePlatformOps()
    private val fakeInfoTileManager = FakeInfoTileManager()

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
            platformOps = fakePlatformOps,
            infoTileManager = fakeInfoTileManager,
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
            viewModel.onEvent(SavedTripUiEvent.ReverseStopClick)

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

    // region Info Tile Tests
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
            fakeAppVersionManager.setUpdateCopy(
                title = infoTile.title,
                description = infoTile.description,
                ctaText = infoTile.primaryCta?.text ?: "",
                key = infoTile.key
            )
            // mimic info tile present in state
            fakeSandookPreferences.setString(KEY_DISMISSED_INFO_TILES, "update_key")

            viewModel.uiState.test {
                var item = awaitItem() // Consume initial state to ensure analytics event is tracked
                println("Received initial item with infoTiles: ${item.infoTiles}")
                viewModel.onEvent(SavedTripUiEvent.DismissInfoTile(infoTile))
                item = awaitItem()
                println("Received item after dismissal with infoTiles: ${item.infoTiles}")
                assertTrue(item.infoTiles?.isEmpty() == true)
                assertTrue(fakeSandookPreferences.isInfoTileDismissed(infoTile.key))
                cancelAndIgnoreRemainingEvents()

            }
        }

    @Test
    fun `GIVEN info tiles from infoTileManager WHEN updateInfoTilesUiState is called THEN infoTiles are updated in UI state`() =
        runTest {
            val infoTile = InfoTileData(
                key = "tile_key",
                title = "Promo",
                description = "Promo Desc",
                type = InfoTileData.InfoTileType.INFO,
                primaryCta = InfoTileCta(
                    text = "Learn More",
                    url = "https://promo.com"
                )
            )
            fakeInfoTileManager.setTiles(listOf(infoTile))
            viewModel.uiState.test {
                skipItems(1)
                val item = awaitItem()
                assertTrue(item.infoTiles?.contains(infoTile) == true)
                cancelAndIgnoreRemainingEvents()

            }
        }

    @Test
    fun `GIVEN an info tile already dismissed WHEN DismissInfoTile event is triggered THEN infoTiles remains unchanged`() =
        runTest {
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

            }
        }

    @Test
    fun `GIVEN multiple info tiles WHEN one is dismissed THEN only that tile is removed and marked dismissed`() =
        runTest {
            val updateTile = InfoTileData(
                key = "update_key",
                title = "Update Available",
                description = "Update your app",
                type = InfoTileData.InfoTileType.APP_UPDATE,
                primaryCta = InfoTileCta(
                    text = "Update",
                    url = "https://store.com/app"
                )
            )
            val criticalAlertTile = InfoTileData(
                key = "alert_key",
                title = "Critical Alert",
                description = "Warning, disruptions expected",
                type = InfoTileData.InfoTileType.CRITICAL_ALERT,
                primaryCta = InfoTileCta(
                    text = "Read more",
                    url = "https://example.com/read"
                )
            )
            fakeAppVersionManager.mockCurrentVersion = updateTile.key
            fakeAppVersionManager.setUpdateCopy(
                title = updateTile.title,
                description = updateTile.description,
                ctaText = updateTile.primaryCta?.text ?: "",
                key = updateTile.key
            )
            fakeInfoTileManager.setTiles(listOf(updateTile, criticalAlertTile))

            viewModel.uiState.test {
                skipItems(1)
                awaitItem().run {
                    println("Received initial item with infoTiles: ${this.infoTiles}")
                    assertEquals(2, this.infoTiles?.size)
                }
                viewModel.onEvent(SavedTripUiEvent.DismissInfoTile(criticalAlertTile))
                awaitItem().run {
                    println("Received item after dismissal with infoTiles: ${this.infoTiles}")
                    assertEquals(1, this.infoTiles?.size)
                    assertTrue(this.infoTiles?.contains(updateTile) == true)
                }
                cancelAndIgnoreRemainingEvents()

            }
        }

    @Test
    fun `GIVEN no info tile WHEN DismissInfoTile event is triggered THEN state remains unchanged`() =
        runTest {
            val infoTile = InfoTileData(
                key = "non_existent",
                title = "Not present",
                description = "Should not exist",
                type = InfoTileData.InfoTileType.APP_UPDATE,
                primaryCta = null
            )
            viewModel.uiState.test {
                val initial = awaitItem()
                assertNull(initial.infoTiles)
                viewModel.onEvent(SavedTripUiEvent.DismissInfoTile(infoTile))
                // Await next state, or verify no new state is emitted
                expectNoEvents() // No new state should be emitted
                cancelAndIgnoreRemainingEvents()

            }
        }

    // endregion Info Tile Tests

    // region Discover Tests

    @Test
    fun `GIVEN Discover button WHEN clicked THEN analytics tracked, badge hidden, and preference updated`() =
        runTest {
            // Simulate discover available and badge shown
            fakeFlag.setDiscoverAvailable(true)
            fakeSandookPreferences.setDiscoverClicked(false)

            viewModel.uiState.test {
                skipItems(1)
                viewModel.onEvent(SavedTripUiEvent.AnalyticsDiscoverButtonClick)
                val item = awaitItem()
                assertFalse(item.displayDiscoverBadge)
                assertTrue(fakeSandookPreferences.hasDiscoverBeenClicked())
                val fakeAnalytics: FakeAnalytics = fakeAnalytics as FakeAnalytics
                assertTrue(fakeAnalytics.isEventTracked(AnalyticsEvent.DiscoverButtonClick.name))
                cancelAndIgnoreRemainingEvents()

            }
        }

    // endregion Discover Tests

    // region Park and Ride Tests

    @Test
    fun `GIVEN ParkRideCardClick event WHEN expanded THEN stopId is added to observed set`() =
        runTest {
            val facility = createParkRideFacilityDetail()
            val parkRideState = createParkRideUiState(
                stopId = "STOP_1",
                stopName = "Test Stop",
                facilities = persistentSetOf(facility),
                isLoading = false
            )

            viewModel.uiState.test {
                skipItems(1)
                viewModel.onEvent(
                    SavedTripUiEvent.ParkRideCardClick(
                        parkRideState,
                        isExpanded = true
                    )
                )
                val item = awaitItem()
                assertTrue(item.observeParkRideStopIdSet.contains("STOP_1"))
                cancelAndIgnoreRemainingEvents()

            }
        }

    @Test
    fun `GIVEN ParkRideCardClick event WHEN collapsed THEN stopId is removed from observed set`() =
        runTest {
            val facility = createParkRideFacilityDetail()
            val parkRideState = createParkRideUiState(
                stopId = "STOP_1",
                stopName = "Test Stop",
                facilities = persistentSetOf(facility),
                isLoading = false
            )
            // First expand
            viewModel.onEvent(
                SavedTripUiEvent.ParkRideCardClick(
                    parkRideState,
                    isExpanded = true
                )
            )
            viewModel.uiState.test {
                skipItems(1)
                // Then collapse
                viewModel.onEvent(
                    SavedTripUiEvent.ParkRideCardClick(
                        parkRideState,
                        isExpanded = false
                    )
                )
                val item = awaitItem()
                assertFalse(item.observeParkRideStopIdSet.contains("STOP_1"))
                cancelAndIgnoreRemainingEvents()

            }
        }

    fun createParkRideUiState(
        stopId: String = "STOP_1",
        stopName: String = "Test Stop",
        facilities: Set<ParkRideUiState.ParkRideFacilityDetail> = persistentSetOf(),
        isLoading: Boolean = false,
        error: String? = null,
    ): ParkRideUiState {
        return ParkRideUiState(
            stopId = stopId,
            stopName = stopName,
            facilities = facilities.toPersistentSet(),
            isLoading = isLoading,
            error = error,
        )
    }

    fun createParkRideFacilityDetail(
        facilityId: String = "FAC_1",
        stopId: String = "STOP_1",
        facilityName: String = "Test Facility",
        spotsAvailable: Int = 10,
        totalSpots: Int = 20,
        percentageFull: Int = 50,
        timeText: String = "10:00 AM"
    ): ParkRideUiState.ParkRideFacilityDetail {
        return ParkRideUiState.ParkRideFacilityDetail(
            spotsAvailable = spotsAvailable,
            totalSpots = totalSpots,
            facilityName = facilityName,
            percentageFull = percentageFull,
            stopId = stopId,
            timeText = timeText,
            facilityId = facilityId,
        )
    }

    // endregion Park and Ride Tests

    // region Selected Stop Events Tests

    @Test
    fun `GIVEN FromStopChanged event with valid JSON WHEN triggered THEN fromStop is updated in UI state`() =
        runTest {
            // GIVEN a valid StopItem JSON
            val stopItem = xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem(
                stopName = "Central Station",
                stopId = "10101"
            )
            val stopJson = stopItem.toJsonString()

            viewModel.uiState.test {
                skipItems(1) // Skip initial state

                // WHEN FromStopChanged event is triggered
                viewModel.onEvent(SavedTripUiEvent.FromStopChanged(stopJson))

                // THEN fromStop should be updated in UI state
                val item = awaitItem()
                assertEquals(stopItem.stopId, item.fromStop?.stopId)
                assertEquals(stopItem.stopName, item.fromStop?.stopName)

                // AND stopResultsManager should have the selected stop
                assertEquals(stopItem, fakeStopResultsManager.selectedFromStop)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN ToStopChanged event with valid JSON WHEN triggered THEN toStop is updated in UI state`() =
        runTest {
            // GIVEN a valid StopItem JSON
            val stopItem = xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem(
                stopName = "Town Hall",
                stopId = "10102"
            )
            val stopJson = stopItem.toJsonString()

            viewModel.uiState.test {
                skipItems(1) // Skip initial state

                // WHEN ToStopChanged event is triggered
                viewModel.onEvent(SavedTripUiEvent.ToStopChanged(stopJson))

                // THEN toStop should be updated in UI state
                val item = awaitItem()
                assertEquals(stopItem.stopId, item.toStop?.stopId)
                assertEquals(stopItem.stopName, item.toStop?.stopName)

                // AND stopResultsManager should have the selected stop
                assertEquals(stopItem, fakeStopResultsManager.selectedToStop)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN FromStopChanged event with invalid JSON WHEN triggered THEN fromStop remains unchanged`() =
        runTest {
            // GIVEN invalid JSON
            val invalidJson = "invalid json string"

            viewModel.uiState.test {
                val initialState = awaitItem()

                // WHEN FromStopChanged event is triggered with invalid JSON
                viewModel.onEvent(SavedTripUiEvent.FromStopChanged(invalidJson))

                // THEN fromStop should remain unchanged (null in this case)
                expectNoEvents() // No state update should occur
                assertEquals(null, fakeStopResultsManager.selectedFromStop)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN ToStopChanged event with invalid JSON WHEN triggered THEN toStop remains unchanged`() =
        runTest {
            // GIVEN invalid JSON
            val invalidJson = "invalid json string"

            viewModel.uiState.test {
                val initialState = awaitItem()

                // WHEN ToStopChanged event is triggered with invalid JSON
                viewModel.onEvent(SavedTripUiEvent.ToStopChanged(invalidJson))

                // THEN toStop should remain unchanged (null in this case)
                expectNoEvents() // No state update should occur
                assertEquals(null, fakeStopResultsManager.selectedToStop)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN both from and to stops selected WHEN ReverseStopClick event is triggered THEN stops are swapped`() =
        runTest {
            // GIVEN both from and to stops are selected
            val fromStop = xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem(
                stopName = "Central Station",
                stopId = "10101"
            )
            val toStop = xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem(
                stopName = "Town Hall",
                stopId = "10102"
            )

            // Set initial stops
            fakeStopResultsManager.setSelectedFromStop(fromStop)
            fakeStopResultsManager.setSelectedToStop(toStop)

            viewModel.uiState.test {
                skipItems(1) // Skip initial state that loads the selected stops

                // WHEN ReverseStopClick event is triggered
                viewModel.onEvent(SavedTripUiEvent.ReverseStopClick)

                // THEN stops should be swapped in UI state
                val item = awaitItem()
                assertEquals(toStop.stopId, item.fromStop?.stopId)
                assertEquals(toStop.stopName, item.fromStop?.stopName)
                assertEquals(fromStop.stopId, item.toStop?.stopId)
                assertEquals(fromStop.stopName, item.toStop?.stopName)

                // AND in the stopResultsManager
                assertEquals(toStop, fakeStopResultsManager.selectedFromStop)
                assertEquals(fromStop, fakeStopResultsManager.selectedToStop)

                // AND analytics event should be tracked
                val fakeAnalytics: FakeAnalytics = fakeAnalytics as FakeAnalytics
                assertTrue(fakeAnalytics.isEventTracked(AnalyticsEvent.ReverseStopClickEvent.name))

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN only from stop selected WHEN ReverseStopClick event is triggered THEN from becomes to and to becomes null`() =
        runTest {
            // GIVEN only from stop is selected
            val fromStop = xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem(
                stopName = "Central Station",
                stopId = "10101"
            )

            fakeStopResultsManager.setSelectedFromStop(fromStop)
            fakeStopResultsManager.setSelectedToStop(null)

            viewModel.uiState.test {
                skipItems(1) // Skip initial state

                // WHEN ReverseStopClick event is triggered
                viewModel.onEvent(SavedTripUiEvent.ReverseStopClick)

                // THEN from should become null and to should become the original from
                val item = awaitItem()
                assertEquals(null, item.fromStop)
                assertEquals(fromStop.stopId, item.toStop?.stopId)
                assertEquals(fromStop.stopName, item.toStop?.stopName)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN no stops selected WHEN ReverseStopClick event is triggered THEN both remain null`() =
        runTest {
            // GIVEN no stops are selected
            fakeStopResultsManager.setSelectedFromStop(null)
            fakeStopResultsManager.setSelectedToStop(null)

            viewModel.uiState.test {
                skipItems(1) // Skip initial state

                // WHEN ReverseStopClick event is triggered
                viewModel.onEvent(SavedTripUiEvent.ReverseStopClick)

                // THEN both should remain null
                val item = awaitItem()
                assertEquals(null, item.fromStop)
                assertEquals(null, item.toStop)

                // AND analytics event should still be tracked
                val fakeAnalytics: FakeAnalytics = fakeAnalytics as FakeAnalytics
                assertTrue(fakeAnalytics.isEventTracked(AnalyticsEvent.ReverseStopClickEvent.name))

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN stops are selected WHEN multiple events are triggered in sequence THEN UI state updates correctly`() =
        runTest {
            // GIVEN initial stops
            val centralStation = xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem(
                stopName = "Central Station",
                stopId = "10101"
            )
            val townHall = xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem(
                stopName = "Town Hall",
                stopId = "10102"
            )
            val airport = xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem(
                stopName = "Sydney Airport",
                stopId = "10104"
            )

            viewModel.uiState.test {
                skipItems(1) // Skip initial state

                // WHEN setting from stop
                viewModel.onEvent(SavedTripUiEvent.FromStopChanged(centralStation.toJsonString()))
                awaitItem().run {
                    assertEquals(centralStation, fromStop)
                    assertEquals(null, toStop)
                }

                // WHEN setting to stop
                viewModel.onEvent(SavedTripUiEvent.ToStopChanged(townHall.toJsonString()))
                awaitItem().run {
                    assertEquals(centralStation, fromStop)
                    assertEquals(townHall, toStop)
                }

                // WHEN reversing stops
                viewModel.onEvent(SavedTripUiEvent.ReverseStopClick)
                awaitItem().run {
                    assertEquals(townHall, fromStop)
                    assertEquals(centralStation, toStop)
                }

                // WHEN changing from stop again
                viewModel.onEvent(SavedTripUiEvent.FromStopChanged(airport.toJsonString()))
                awaitItem().run {
                    assertEquals(airport, fromStop)
                    assertEquals(centralStation, toStop)
                }

                cancelAndIgnoreRemainingEvents()
            }
        }

    // endregion Selected Stop Events Tests
}
