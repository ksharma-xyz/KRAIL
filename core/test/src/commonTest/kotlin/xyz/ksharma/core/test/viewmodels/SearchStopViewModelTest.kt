package xyz.ksharma.core.test.viewmodels

import app.cash.turbine.test
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.core.test.fakes.FakeAnalytics
import xyz.ksharma.core.test.fakes.FakeSandook
import xyz.ksharma.core.test.fakes.FakeStopResultsManager
import xyz.ksharma.core.test.fakes.FakeTripPlanningService
import xyz.ksharma.core.test.helpers.AnalyticsTestHelper.assertScreenViewEventTracked
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.trip.planner.ui.searchstop.SearchStopViewModel
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
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

            // This line is not affecting the FakeStopResultsManager
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
                SearchStopUiEvent.StopSelected(
                    StopItem(
                        stopName = "name",
                        persistentSetOf(TransportMode.Train()),
                        stopId = "stopID",
                    )
                )
            )

            // THEN
            assertTrue(fakeAnalytics is FakeAnalytics)
            assertTrue(fakeAnalytics.isEventTracked("stop_selected"))
            val event = fakeAnalytics.getTrackedEvent("stop_selected")
            assertIs<AnalyticsEvent.StopSelectedEvent>(event)
            assertEquals("stopID", event.stopId)
        }
}
