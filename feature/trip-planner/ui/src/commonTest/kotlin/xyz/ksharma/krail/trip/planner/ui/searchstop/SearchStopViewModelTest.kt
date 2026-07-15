package xyz.ksharma.krail.trip.planner.ui.searchstop

import app.cash.turbine.test
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeNearbyStopsManagerForMap
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeRemoteAddressResultsManager
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeStopResultsManager
import xyz.ksharma.krail.core.testing.fakes.FakeTripPlanningService
import xyz.ksharma.krail.core.testing.helpers.AnalyticsTestHelper.assertScreenViewEventTracked
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.trip.planner.ui.searchstop.SearchStopViewModel
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.core.transport.nsw.NswTransportMode
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.ListState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SearchStopViewModelTest {

    private val fakeAnalytics: Analytics = FakeAnalytics()
    private val tripPlanningService = FakeTripPlanningService()
    private lateinit var viewModel: SearchStopViewModel
    private val fakeStopResultsManager = FakeStopResultsManager()
    private val fakeRemoteAddressResultsManager = FakeRemoteAddressResultsManager()
    private val fakeFlag = FakeFlag()
    private val fakeNearbyStopsManager = FakeNearbyStopsManagerForMap()
    private val fakePreferences = FakeSandookPreferences()
    private val fakeSandook = FakeSandook()

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SearchStopViewModel(
            analytics = fakeAnalytics,
            stopResultsManager = fakeStopResultsManager,
            remoteAddressResultsManager = fakeRemoteAddressResultsManager,
            flag = fakeFlag,
            nearbyStopsManager = fakeNearbyStopsManager,
            ioDispatcher = testDispatcher,
            preferences = fakePreferences,
            sandook = fakeSandook,
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN SearchStopViewModel WHEN uiState is collected THEN analytics event is tracked`() =
        runTest {
            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.searchResults.isEmpty())
                assertTrue(state.recentStops.isEmpty())
                assertIs<ListState.Recent>(state.listState)

                // checkMapsAvailability() sets isMapsAvailable=false (same as default) so no
                // second emission; just advance to let onStart run trackScreenViewEvent.
                advanceUntilIdle()
                assertScreenViewEventTracked(
                    fakeAnalytics,
                    expectedScreenName = AnalyticsScreen.SearchStop.name,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    /* This test is not valid anymore, as we're not calling API.
        @Test
        fun `GIVEN search query WHEN SearchTextChanged is triggered and api is success THEN uiState is updated with results`() =
            runTest {
                tripPlanningService.isSuccess = true

                viewModel.uiState.test {
                    skipItems(1) // initial state

                    viewModel.onEvent(SearchStopUiEvent.SearchTextChanged("abcd"))

                    awaitItem().run {
                        assertTrue(isLoading)
                        assertFalse(isError)
                        assertTrue(stops.isEmpty())
                    }


                    viewModel.onEvent(SearchStopUiEvent.SearchTextChanged("stop"))
                    awaitItem().run {
                        assertFalse(isLoading)
                        assertFalse(isError)
                        assertEquals(2, stops.size)
                    }

                    cancelAndIgnoreRemainingEvents()
                }
            }
    */

    @Test
    fun `GIVEN search query WHEN SearchTextChanged and api fails THEN uiState is updated with error`() =
        runTest {
            val query = "test"

            // Set the flag to throw an error in FakeStopResultsManager
            fakeStopResultsManager.shouldThrowError = true
            tripPlanningService.isSuccess = false

            viewModel.uiState.test {
                skipItems(1) // initial state

                viewModel.onEvent(SearchStopUiEvent.SearchTextChanged(query))

                // Loading state - should be Results with isLoading=true
                val loadingState = awaitItem()
                val loadingListState = loadingState.listState
                assertIs<ListState.Results>(loadingListState)
                assertTrue(loadingListState.isLoading)
                assertFalse(loadingListState.isError)

                // Error state - should be Error listState
                val errorState = awaitItem()
                assertIs<ListState.Error>(errorState.listState)
                assertTrue(errorState.searchResults.isEmpty())

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN stop item WHEN StopSelected is triggered THEN analytics event is tracked`() =
        runTest {

            // WHEN
            viewModel.onEvent(
                SearchStopUiEvent.TrackStopSelected(
                    StopItem(
                        stopName = "name",
                        stopId = "stopID",
                    ),
                    isRecentSearch = false
                )
            )

            // THEN
            assertTrue(fakeAnalytics is FakeAnalytics)
            assertTrue(fakeAnalytics.isEventTracked("stop_selected"))
            val event = fakeAnalytics.getTrackedEvent("stop_selected")
            assertIs<AnalyticsEvent.StopSelectedEvent>(event)
            assertEquals("stopID", event.stopId)
            assertEquals(false, event.isRecentSearch)
            assertEquals(AnalyticsEvent.StopSelectedEvent.LocationKind.TRANSIT_STOP, event.locationKind)
            assertEquals(null, event.addressType)
        }

    @Test
    fun `GIVEN an address stop item WHEN StopSelected is triggered THEN analytics event carries location kind and address type`() =
        runTest {
            viewModel.onEvent(
                SearchStopUiEvent.TrackStopSelected(
                    StopItem(
                        stopName = "123 Example St",
                        stopId = "streetID:123",
                        locationKind = xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.LocationKind.ADDRESS,
                        addressType = "street",
                    ),
                    isRecentSearch = false,
                ),
            )

            assertTrue(fakeAnalytics is FakeAnalytics)
            val event = fakeAnalytics.getTrackedEvent("stop_selected")
            assertIs<AnalyticsEvent.StopSelectedEvent>(event)
            assertEquals(AnalyticsEvent.StopSelectedEvent.LocationKind.ADDRESS, event.locationKind)
            assertEquals(AnalyticsEvent.StopSelectedEvent.AddressType.STREET, event.addressType)
        }

    @Test
    fun `GIVEN an address stop item with an unrecognised raw type WHEN StopSelected is triggered THEN address type folds to UNKNOWN`() =
        runTest {
            viewModel.onEvent(
                SearchStopUiEvent.TrackStopSelected(
                    StopItem(
                        stopName = "Some POI",
                        stopId = "poiID:456",
                        locationKind = xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.LocationKind.ADDRESS,
                        addressType = "some-new-nsw-type-we-dont-know-about",
                    ),
                    isRecentSearch = false,
                ),
            )

            assertTrue(fakeAnalytics is FakeAnalytics)
            val event = fakeAnalytics.getTrackedEvent("stop_selected")
            assertIs<AnalyticsEvent.StopSelectedEvent>(event)
            assertEquals(AnalyticsEvent.StopSelectedEvent.AddressType.UNKNOWN, event.addressType)
        }

    @Test
    fun `GIVEN recent stop item WHEN StopSelected with isRecentSearch true is triggered THEN analytics event is tracked with correct flag`() =
        runTest {

            // WHEN
            viewModel.onEvent(
                SearchStopUiEvent.TrackStopSelected(
                    StopItem(
                        stopName = "Recent Stop",
                        stopId = "recentStopID",
                    ),
                    isRecentSearch = true
                )
            )

            // THEN
            assertTrue(fakeAnalytics is FakeAnalytics)
            assertTrue(fakeAnalytics.isEventTracked("stop_selected"))
            val event = fakeAnalytics.getTrackedEvent("stop_selected")
            assertIs<AnalyticsEvent.StopSelectedEvent>(event)
            assertEquals("recentStopID", event.stopId)
            assertEquals(true, event.isRecentSearch)
        }

    // region RecentSearchStops

    // kotlin
    @Test
    fun `GIVEN recent stops exist WHEN RefreshRecentStopsList is triggered THEN recent stops are loaded in state`() =
        runTest {
            // GIVEN - Add some recent stops to the fake manager
            val recentStop1 = SearchStopState.StopResult(
                stopId = "recent1",
                stopName = "Recent Stop 1",
                transportModeType = persistentListOf(NswTransportMode.Train)
            )
            val recentStop2 = SearchStopState.StopResult(
                stopId = "recent2",
                stopName = "Recent Stop 2",
                transportModeType = persistentListOf(NswTransportMode.Bus)
            )

            fakeStopResultsManager.addRecentSearchStop(recentStop1)
            fakeStopResultsManager.addRecentSearchStop(recentStop2)

            // WHEN - Trigger refresh on the existing ViewModel
            viewModel.uiState.test {
                skipItems(1) // initial state
                viewModel.onEvent(SearchStopUiEvent.RefreshRecentStopsList)
                advanceUntilIdle()

                awaitItem().run {
                    assertEquals(2, recentStops.size)
                    assertEquals("recent2", recentStops[0].stopId)
                    assertEquals("recent1", recentStops[1].stopId)
                }

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN recent stops exist WHEN ClearRecentStops is triggered THEN recent stops are cleared from state`() =
        runTest {
            // GIVEN - Add some recent stops
            val recentStop1 = SearchStopState.StopResult(
                stopId = "recent1",
                stopName = "Recent Stop 1",
                transportModeType = persistentListOf(NswTransportMode.Train)
            )
            val recentStop2 = SearchStopState.StopResult(
                stopId = "recent2",
                stopName = "Recent Stop 2",
                transportModeType = persistentListOf(NswTransportMode.Bus)
            )

            fakeStopResultsManager.addRecentSearchStop(recentStop1)
            fakeStopResultsManager.addRecentSearchStop(recentStop2)

            viewModel.uiState.test {
                skipItems(1) // initial state
                // Load recents into the ViewModel first
                viewModel.onEvent(SearchStopUiEvent.RefreshRecentStopsList)
                advanceUntilIdle()

                awaitItem().run {
                    assertEquals(2, recentStops.size)
                }

                // WHEN - Trigger clear recent stops
                viewModel.onEvent(SearchStopUiEvent.ClearRecentSearchStops(recentSearchCount = 2))
                advanceUntilIdle()

                // THEN - Verify recent stops are cleared
                awaitItem().run {
                    assertTrue(recentStops.isEmpty())
                }

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN recent stops exist WHEN ViewModel is initialized THEN recent stops are not auto-loaded`() =
        runTest {
            // GIVEN - Add a recent stop to the manager
            val recentStop = SearchStopState.StopResult(
                stopId = "recent1",
                stopName = "Recent Stop 1",
                transportModeType = persistentListOf(NswTransportMode.Train)
            )
            fakeStopResultsManager.addRecentSearchStop(recentStop)

            // WHEN - Create a fresh ViewModel
            val vm = SearchStopViewModel(
                analytics = fakeAnalytics,
                stopResultsManager = fakeStopResultsManager,
                remoteAddressResultsManager = fakeRemoteAddressResultsManager,
                flag = fakeFlag,
                nearbyStopsManager = fakeNearbyStopsManager,
                ioDispatcher = testDispatcher,
                preferences = fakePreferences,
                sandook = fakeSandook,
            )

            // THEN - Initial state should not include recents (screen triggers refresh)
            vm.uiState.test {
                val initial = awaitItem()
                assertTrue(initial.recentStops.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN stop A selected then stop B then stop A again WHEN recent stops are fetched THEN stop A is most recent and only appears once`() =
        runTest {
            // GIVEN - Create different stops
            val stopA = StopItem(stopName = "Central Station", stopId = "central123")
            val stopB = StopItem(stopName = "Town Hall", stopId = "townhall456")

            // WHEN - Select stops in sequence: A -> B -> A
            fakeStopResultsManager.setSelectedFromStop(stopA)
            fakeStopResultsManager.setSelectedToStop(stopB)
            fakeStopResultsManager.setSelectedFromStop(stopA) // Select A again

            // THEN - Verify A is most recent and appears only once
            val recentStops = fakeStopResultsManager.recentSearchStops()
            assertEquals(2, recentStops.size)
            assertEquals("central123", recentStops[0].stopId) // A should be most recent
            assertEquals("townhall456", recentStops[1].stopId) // B should be second

            // Verify A appears only once (not duplicated)
            val stopACount = recentStops.count { it.stopId == "central123" }
            assertEquals(1, stopACount)
        }

    @Test
    fun `GIVEN no recent stops WHEN ClearRecentStops is triggered THEN state remains unchanged`() =
        runTest {
            // GIVEN - No recent stops (default state)
            viewModel.uiState.test {
                skipItems(1) // Skip initial state

                // WHEN - Trigger clear recent stops on empty state
                viewModel.onEvent(SearchStopUiEvent.ClearRecentSearchStops(recentSearchCount = 0))

                // THEN - State should remain unchanged with empty recent stops
                expectNoEvents()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN recent stops exist WHEN ClearRecentStops is triggered THEN StopResultsManager clearRecentSearchStops is called`() =
        runTest {
            // GIVEN - Add some recent stops
            val recentStop = SearchStopState.StopResult(
                stopId = "recent1",
                stopName = "Recent Stop 1",
                transportModeType = persistentListOf(NswTransportMode.Train)
            )
            fakeStopResultsManager.addRecentSearchStop(recentStop)

            // Verify recent stops exist before clearing
            assertEquals(1, fakeStopResultsManager.recentSearchStops().size)

            // WHEN - Trigger clear recent stops
            viewModel.onEvent(SearchStopUiEvent.ClearRecentSearchStops(recentSearchCount = 1))
            advanceUntilIdle()

            // THEN - Verify the manager's clear method was called
            assertEquals(0, fakeStopResultsManager.recentSearchStops().size)
        }

    @Test
    fun `GIVEN recent stops exist WHEN ClearRecentSearchStops is triggered THEN ClearRecentSearchClickEvent analytics is tracked with correct count`() =
        runTest {
            // GIVEN - Recent stops count
            val recentSearchCount = 3

            // WHEN - Trigger clear recent stops
            viewModel.onEvent(SearchStopUiEvent.ClearRecentSearchStops(recentSearchCount = recentSearchCount))

            // THEN - Verify analytics event is tracked with correct count
            assertTrue(fakeAnalytics is FakeAnalytics)
            assertTrue(fakeAnalytics.isEventTracked("clear_recent_search_stops"))
            val event = fakeAnalytics.getTrackedEvent("clear_recent_search_stops")
            assertIs<AnalyticsEvent.ClearRecentSearchClickEvent>(event)
            assertEquals(recentSearchCount, event.recentSearchCount)
        }

    @Test
    fun `GIVEN zero recent stops WHEN ClearRecentSearchStops is triggered THEN analytics event is tracked with zero count`() =
        runTest {
            // WHEN - Trigger clear recent stops with zero count
            viewModel.onEvent(SearchStopUiEvent.ClearRecentSearchStops(recentSearchCount = 0))

            // THEN - Verify analytics event is tracked with zero count
            assertTrue(fakeAnalytics is FakeAnalytics)
            assertTrue(fakeAnalytics.isEventTracked("clear_recent_search_stops"))
            val event = fakeAnalytics.getTrackedEvent("clear_recent_search_stops")
            assertIs<AnalyticsEvent.ClearRecentSearchClickEvent>(event)
            assertEquals(0, event.recentSearchCount)
        }

    // endregion RecentSearchStops

    // region SelectOnMapButtonClicked

    @Test
    fun `GIVEN SelectOnMapButtonClicked WHEN sent to ViewModel THEN select_on_map_button_click event is tracked`() =
        runTest {
            viewModel.onEvent(SearchStopUiEvent.SelectOnMapButtonClicked)
            advanceUntilIdle()

            assertTrue(fakeAnalytics is FakeAnalytics)
            assertTrue(fakeAnalytics.isEventTracked("select_on_map_button_click"))
            assertIs<AnalyticsEvent.SelectOnMapButtonClickEvent>(
                fakeAnalytics.getTrackedEvent("select_on_map_button_click"),
            )
        }

    // endregion SelectOnMapButtonClicked

    // region Search query analytics redaction

    @Test
    fun `GIVEN a query with results WHEN search completes THEN search_stop_query carries no raw text`() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)
                viewModel.onEvent(SearchStopUiEvent.SearchTextChanged("Central"))
                advanceUntilIdle()

                assertTrue(fakeAnalytics is FakeAnalytics)
                val event = fakeAnalytics.getTrackedEvent("search_stop_query")
                assertIs<AnalyticsEvent.SearchStopQuery>(event)
                assertEquals("Central".length, event.queryLength)
                assertEquals(1, event.resultsCount)
                assertTrue(event.searchSessionId.isNotBlank())
                assertFalse(event.properties.orEmpty().containsKey("query"))

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN zero results and address pipeline off WHEN query has no digits THEN carve-out keeps the query`() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)
                viewModel.onEvent(SearchStopUiEvent.SearchTextChanged("townhall"))
                advanceUntilIdle()

                assertTrue(fakeAnalytics is FakeAnalytics)
                val event = fakeAnalytics.getTrackedEvent("search_stop_query")
                assertIs<AnalyticsEvent.SearchStopQuery>(event)
                assertEquals(0, event.resultsCount)
                assertEquals("townhall", event.zeroResultQuery)
                assertEquals("townhall", event.properties?.get("query"))

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN zero results WHEN query contains digits THEN carve-out never keeps the query`() =
        runTest {
            viewModel.uiState.test {
                skipItems(1)
                viewModel.onEvent(SearchStopUiEvent.SearchTextChanged("4 fulton place"))
                advanceUntilIdle()

                assertTrue(fakeAnalytics is FakeAnalytics)
                val event = fakeAnalytics.getTrackedEvent("search_stop_query")
                assertIs<AnalyticsEvent.SearchStopQuery>(event)
                assertEquals(0, event.resultsCount)
                assertEquals(null, event.zeroResultQuery)
                assertFalse(event.properties.orEmpty().containsKey("query"))

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN address pipeline eligible WHEN local results are zero THEN local event defers the carve-out`() =
        runTest {
            val addressViewModel = addressAwareViewModel(minQueryLength = 6)
            addressViewModel.uiState.test {
                skipItems(1)
                addressViewModel.onEvent(SearchStopUiEvent.SearchTextChanged("townhall"))
                advanceUntilIdle()

                assertTrue(fakeAnalytics is FakeAnalytics)
                val event = fakeAnalytics.getTrackedEvent("search_stop_query")
                assertIs<AnalyticsEvent.SearchStopQuery>(event)
                assertEquals(null, event.zeroResultQuery)
                assertFalse(event.properties.orEmpty().containsKey("query"))

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN search fails WHEN error event fires THEN it carries no raw text`() =
        runTest {
            fakeStopResultsManager.shouldThrowError = true
            viewModel.uiState.test {
                skipItems(1)
                viewModel.onEvent(SearchStopUiEvent.SearchTextChanged("townhall"))
                advanceUntilIdle()

                assertTrue(fakeAnalytics is FakeAnalytics)
                val event = fakeAnalytics.getTrackedEvent("search_stop_query")
                assertIs<AnalyticsEvent.SearchStopQuery>(event)
                assertTrue(event.isError)
                assertFalse(event.properties.orEmpty().containsKey("query"))

                cancelAndIgnoreRemainingEvents()
            }
        }

    // endregion Search query analytics redaction

    // region Address search eligibility

    private fun addressAwareViewModel(minQueryLength: Int = 6) = SearchStopViewModel(
        analytics = fakeAnalytics,
        stopResultsManager = fakeStopResultsManager,
        remoteAddressResultsManager = fakeRemoteAddressResultsManager,
        flag = fakeFlag,
        nearbyStopsManager = fakeNearbyStopsManager,
        ioDispatcher = testDispatcher,
        preferences = fakePreferences,
        sandook = fakeSandook,
        isAddressSearchEnabled = { true },
        addressSearchMinQueryLength = { minQueryLength },
    )

    @Test
    fun `GIVEN address search enabled WHEN query is below threshold THEN no address request is made`() =
        runTest {
            fakeRemoteAddressResultsManager.results = listOf(
                SearchStopState.SearchResult.Address(
                    addressId = "addr-1",
                    displayName = "Sydney Opera House",
                    addressType = "poi",
                ),
            )
            val addressViewModel = addressAwareViewModel(minQueryLength = 6)

            addressViewModel.uiState.test {
                skipItems(1)
                addressViewModel.onEvent(SearchStopUiEvent.SearchTextChanged("Syd"))
                advanceUntilIdle()

                assertEquals(0, fakeRemoteAddressResultsManager.callCount)
                assertTrue(addressViewModel.uiState.value.addressResults.isEmpty())

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN address search enabled WHEN query meets threshold THEN address request is made and results shown`() =
        runTest {
            fakeRemoteAddressResultsManager.results = listOf(
                SearchStopState.SearchResult.Address(
                    addressId = "addr-1",
                    displayName = "Sydney Opera House",
                    addressType = "poi",
                ),
            )
            val addressViewModel = addressAwareViewModel(minQueryLength = 6)

            addressViewModel.uiState.test {
                skipItems(1)
                addressViewModel.onEvent(SearchStopUiEvent.SearchTextChanged("Sydney"))
                advanceUntilIdle()

                assertEquals(1, fakeRemoteAddressResultsManager.callCount)
                assertEquals("Sydney", fakeRemoteAddressResultsManager.lastQuery)
                assertEquals(1, addressViewModel.uiState.value.addressResults.size)
                assertFalse(addressViewModel.uiState.value.isAddressSearchLoading)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN a cached query WHEN the exact same query is searched again THEN no second network call is made`() =
        runTest {
            fakeRemoteAddressResultsManager.results = listOf(
                SearchStopState.SearchResult.Address(
                    addressId = "addr-1",
                    displayName = "Sydney Opera House",
                    addressType = "poi",
                ),
            )
            val addressViewModel = addressAwareViewModel(minQueryLength = 6)

            addressViewModel.uiState.test {
                skipItems(1)

                addressViewModel.onEvent(SearchStopUiEvent.SearchTextChanged("Sydney"))
                advanceUntilIdle()
                assertEquals(1, fakeRemoteAddressResultsManager.callCount)

                // Clear then retype the identical query - the normalized/case-folded
                // cache key is the same, so this must be served from cache, not the
                // network.
                addressViewModel.onEvent(SearchStopUiEvent.SearchTextChanged(""))
                advanceUntilIdle()
                addressViewModel.onEvent(SearchStopUiEvent.SearchTextChanged("Sydney"))
                advanceUntilIdle()

                assertEquals(1, fakeRemoteAddressResultsManager.callCount)
                assertEquals(1, addressViewModel.uiState.value.addressResults.size)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN a failed address request WHEN the exact same query is searched again THEN a second network call is made`() =
        runTest {
            fakeRemoteAddressResultsManager.shouldThrowError = true
            val addressViewModel = addressAwareViewModel(minQueryLength = 6)

            addressViewModel.uiState.test {
                skipItems(1)

                addressViewModel.onEvent(SearchStopUiEvent.SearchTextChanged("Sydney"))
                advanceUntilIdle()
                assertEquals(1, fakeRemoteAddressResultsManager.callCount)
                assertTrue(addressViewModel.uiState.value.addressResults.isEmpty())

                // The first call failed - a failure must never be cached as "no
                // results", or the retry below would be wrongly served from cache
                // instead of hitting the network again.
                fakeRemoteAddressResultsManager.shouldThrowError = false
                fakeRemoteAddressResultsManager.results = listOf(
                    SearchStopState.SearchResult.Address(
                        addressId = "addr-1",
                        displayName = "Sydney Opera House",
                        addressType = "poi",
                    ),
                )

                addressViewModel.onEvent(SearchStopUiEvent.SearchTextChanged(""))
                advanceUntilIdle()
                addressViewModel.onEvent(SearchStopUiEvent.SearchTextChanged("Sydney"))
                advanceUntilIdle()

                assertEquals(2, fakeRemoteAddressResultsManager.callCount)
                assertEquals(1, addressViewModel.uiState.value.addressResults.size)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN address search disabled by default constructor WHEN query is long enough THEN no address request is made`() =
        runTest {
            fakeRemoteAddressResultsManager.results = listOf(
                SearchStopState.SearchResult.Address(
                    addressId = "addr-1",
                    displayName = "Sydney Opera House",
                    addressType = "poi",
                ),
            )

            viewModel.uiState.test {
                skipItems(1)
                viewModel.onEvent(SearchStopUiEvent.SearchTextChanged("Sydney Opera House"))
                advanceUntilIdle()

                assertEquals(0, fakeRemoteAddressResultsManager.callCount)
                assertTrue(viewModel.uiState.value.addressResults.isEmpty())

                cancelAndIgnoreRemainingEvents()
            }
        }

    // endregion Address search eligibility
}
