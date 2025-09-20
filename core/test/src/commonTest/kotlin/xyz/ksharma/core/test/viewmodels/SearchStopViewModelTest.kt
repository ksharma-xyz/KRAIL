package xyz.ksharma.core.test.viewmodels

import app.cash.turbine.test
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.core.test.fakes.FakeAnalytics
import xyz.ksharma.core.test.fakes.FakeStopResultsManager
import xyz.ksharma.core.test.fakes.FakeTripPlanningService
import xyz.ksharma.core.test.helpers.AnalyticsTestHelper.assertScreenViewEventTracked
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.trip.planner.ui.searchstop.SearchStopViewModel
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
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

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SearchStopViewModel(
            analytics = fakeAnalytics,
            stopResultsManager = fakeStopResultsManager,
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
                awaitItem().run {
                    assertFalse(isLoading)
                    assertFalse(isError)
                    assertTrue(stops.isEmpty())
                }

                advanceUntilIdle()
                assertScreenViewEventTracked(
                    fakeAnalytics,
                    expectedScreenName = AnalyticsScreen.SearchStop.name,
                )
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
                awaitItem().run {
                    assertTrue(isLoading)
                    assertFalse(isError)
                    assertTrue(stops.isEmpty())
                }

                awaitItem().run {
                    assertFalse(isLoading)
                    assertTrue(isError)
                    assertTrue(stops.isEmpty())
                }

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

    @Test
    fun `GIVEN recent stops exist WHEN ViewModel is initialized THEN recent stops are loaded in state`() =
        runTest {
            // GIVEN - Add some recent stops to the fake manager
            val recentStop1 = SearchStopState.StopResult(
                stopId = "recent1",
                stopName = "Recent Stop 1",
                transportModeType = persistentListOf(TransportMode.Train())
            )
            val recentStop2 = SearchStopState.StopResult(
                stopId = "recent2",
                stopName = "Recent Stop 2",
                transportModeType = persistentListOf(TransportMode.Bus())
            )

            fakeStopResultsManager.addRecentSearchStop(recentStop1)
            fakeStopResultsManager.addRecentSearchStop(recentStop2)

            // WHEN - Create a new ViewModel instance to trigger initialization
            val newViewModel = SearchStopViewModel(
                analytics = fakeAnalytics,
                stopResultsManager = fakeStopResultsManager,
            )

            // THEN
            newViewModel.uiState.test {
                advanceUntilIdle()
                skipItems(1) // Skip initial state

                awaitItem().run {
                    assertEquals(2, recentStops.size)
                    assertEquals("recent2", recentStops[0].stopId) // Most recent first
                    assertEquals("recent1", recentStops[1].stopId)
                }
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
    fun `GIVEN recent stops exist WHEN ClearRecentStops is triggered THEN recent stops are cleared from state`() =
        runTest {
            // GIVEN - Add some recent stops
            val recentStop1 = SearchStopState.StopResult(
                stopId = "recent1",
                stopName = "Recent Stop 1",
                transportModeType = persistentListOf(TransportMode.Train())
            )
            val recentStop2 = SearchStopState.StopResult(
                stopId = "recent2",
                stopName = "Recent Stop 2",
                transportModeType = persistentListOf(TransportMode.Bus())
            )

            fakeStopResultsManager.addRecentSearchStop(recentStop1)
            fakeStopResultsManager.addRecentSearchStop(recentStop2)

            viewModel.uiState.test {
                // Skip initial state
                advanceUntilIdle()
                skipItems(1)

                awaitItem().run {
                    assertTrue(recentStops.size == 2)
                }

                // WHEN - Trigger clear recent stops
                viewModel.onEvent(SearchStopUiEvent.ClearRecentSearchStops)

                // THEN - Verify recent stops are cleared
                awaitItem().run {
                    assertTrue(recentStops.isEmpty())
                    assertFalse(isLoading)
                    assertFalse(isError)
                }

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN no recent stops WHEN ClearRecentStops is triggered THEN state remains unchanged`() =
        runTest {
            // GIVEN - No recent stops (default state)
            viewModel.uiState.test {
                skipItems(1) // Skip initial state

                // WHEN - Trigger clear recent stops on empty state
                viewModel.onEvent(SearchStopUiEvent.ClearRecentSearchStops)

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
                transportModeType = persistentListOf(TransportMode.Train())
            )
            fakeStopResultsManager.addRecentSearchStop(recentStop)

            // Verify recent stops exist before clearing
            assertEquals(1, fakeStopResultsManager.recentSearchStops().size)

            // WHEN - Trigger clear recent stops
            viewModel.onEvent(SearchStopUiEvent.ClearRecentSearchStops)
            advanceUntilIdle()

            // THEN - Verify the manager's clear method was called
            assertEquals(0, fakeStopResultsManager.recentSearchStops().size)
        }

    // endregion RecentSearchStops
}
