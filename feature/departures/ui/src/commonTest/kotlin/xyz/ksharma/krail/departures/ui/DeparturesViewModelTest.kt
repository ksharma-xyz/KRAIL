package xyz.ksharma.krail.departures.ui

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.DepartureBoardSource
import xyz.ksharma.krail.departures.network.api.model.DepartureMonitorResponse
import xyz.ksharma.krail.departures.ui.state.DeparturesUiEvent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [DeparturesViewModel].
 *
 * Covers the **single-stop departure screen** — the sheet that opens when a user taps a stop
 * (e.g. from a search result or a journey leg). The ViewModel drives that screen via a sealed
 * [DeparturesUiEvent] interface: `LoadDepartures`, `Refresh`, `StopPolling`, and
 * `LoadPreviousDepartures`.
 *
 * For the saved-trips screen that shows an accordion of multiple stops, see
 * [DepartureBoardViewModelTest] in the `feature:trip-planner:ui` module.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeparturesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testConfig = DepartureBoardConfig(
        refreshIntervalMs = 1_000L,
        previousDeparturesWindowMinutes = 30L,
    )

    private lateinit var fakeService: FakeDeparturesService
    private lateinit var repository: DepartureBoardRepository
    private lateinit var viewModel: DeparturesViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeService = FakeDeparturesService(response = buildResponse(2))
        repository = DepartureBoardRepository(
            departuresService = fakeService,
            ioDispatcher = testDispatcher,
            config = testConfig,
        )
        viewModel = DeparturesViewModel(
            repository = repository,
            analytics = NoOpAnalytics,
            ioDispatcher = testDispatcher,
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── LoadDepartures ────────────────────────────────────────────────────────

    @Test
    fun `Given initial state When no event sent Then uiState is default`() = runTest {
        viewModel.uiState.test {
            val initial = awaitItem()
            // DeparturesState() defaults: isLoading=true, not error, no departures
            assertTrue(initial.isLoading || !initial.isError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given LoadDepartures event sent When API succeeds Then uiState has departures`() =
        runTest {
            viewModel.uiState.test {
                awaitItem() // initial

                viewModel.onEvent(DeparturesUiEvent.LoadDepartures(stopId = STOP_A, source = DEFAULT_SOURCE))
                advanceUntilIdle()

                // loading
                awaitItem()

                val success = awaitItem()
                assertFalse(success.isLoading)
                assertFalse(success.isError)
                assertEquals(2, success.departures.size)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given LoadDepartures event sent When API fails Then uiState shows error`() = runTest {
        fakeService.shouldThrow = true

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.onEvent(DeparturesUiEvent.LoadDepartures(stopId = STOP_A, source = DEFAULT_SOURCE))
            advanceUntilIdle()

            awaitItem() // loading

            val error = awaitItem()
            assertFalse(error.isLoading)
            assertTrue(error.isError)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── StopPolling ───────────────────────────────────────────────────────────

    @Test
    fun `Given active stop When StopPolling sent Then uiState resets to idle`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.onEvent(DeparturesUiEvent.LoadDepartures(stopId = STOP_A, source = DEFAULT_SOURCE))
            advanceUntilIdle()

            awaitItem() // loading
            awaitItem() // success

            viewModel.onEvent(DeparturesUiEvent.StopPolling)
            advanceUntilIdle()

            // After StopPolling the VM switches activeStopId to null → repo emits idle state
            val idle = awaitItem()
            assertFalse(idle.isLoading, "Should not be loading after polling stopped")
            assertFalse(idle.isError)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Test
    fun `Given active stop When Refresh sent Then API is called again`() = runTest {
        viewModel.onEvent(DeparturesUiEvent.LoadDepartures(stopId = STOP_A, source = DEFAULT_SOURCE))
        advanceUntilIdle()
        val callsBefore = fakeService.callCount

        viewModel.onEvent(DeparturesUiEvent.Refresh)
        advanceUntilIdle()

        assertEquals(callsBefore + 1, fakeService.callCount)
    }

    @Test
    fun `Given no active stop When Refresh sent Then NOOP - no API call`() = runTest {
        // No LoadDepartures sent → no active stop
        val callsBefore = fakeService.callCount

        viewModel.onEvent(DeparturesUiEvent.Refresh)
        advanceUntilIdle()

        assertEquals(callsBefore, fakeService.callCount, "Refresh with no active stop should be a NOOP")
    }

    // ── LoadPreviousDepartures ────────────────────────────────────────────────

    @Test
    fun `Given active stop When LoadPreviousDepartures sent Then isPreviousLoading transitions true then false`() =
        runTest {
            // Use a past departure time so the previous departures filter keeps the rows
            fakeService.response = buildResponse(count = 1, plannedTime = "2020-01-01T00:00:00Z")

            viewModel.onEvent(DeparturesUiEvent.LoadDepartures(stopId = STOP_A, source = DEFAULT_SOURCE))
            advanceUntilIdle()

            viewModel.uiState.test {
                awaitItem() // current success state

                viewModel.onEvent(DeparturesUiEvent.LoadPreviousDepartures(STOP_A))
                advanceUntilIdle()

                val loading = awaitItem()
                assertTrue(loading.isPreviousLoading)

                val done = awaitItem()
                assertFalse(done.isPreviousLoading)

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── LoadDepartures — stop switching / NOOP ────────────────────────────────

    @Test
    fun `Given polling stop A When LoadDepartures for different stop B Then switches and polls B`() =
        runTest {
            viewModel.onEvent(DeparturesUiEvent.LoadDepartures(stopId = STOP_A, source = DEFAULT_SOURCE))
            advanceUntilIdle()
            val callsForA = fakeService.callCount
            assertTrue(callsForA >= 1, "Polling STOP_A should trigger at least one API call")

            viewModel.onEvent(DeparturesUiEvent.LoadDepartures(stopId = STOP_B, source = DEFAULT_SOURCE))
            advanceUntilIdle()

            assertTrue(
                fakeService.callCount > callsForA,
                "Switching to STOP_B should trigger an additional API call",
            )
        }

    @Test
    fun `Given active stop When LoadDepartures sent for same stop Then no extra API call`() =
        runTest {
            viewModel.onEvent(DeparturesUiEvent.LoadDepartures(stopId = STOP_A, source = DEFAULT_SOURCE))
            advanceUntilIdle()
            val callsAfterFirst = fakeService.callCount

            // Same stop — ViewModel guards against re-setting the same activeStopId
            viewModel.onEvent(DeparturesUiEvent.LoadDepartures(stopId = STOP_A, source = DEFAULT_SOURCE))
            advanceUntilIdle()

            assertEquals(
                callsAfterFirst,
                fakeService.callCount,
                "Sending LoadDepartures for the same stop must be a NOOP",
            )
        }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun buildResponse(
        count: Int,
        plannedTime: String = "2099-01-01T09:00:00Z",
    ) = DepartureMonitorResponse(
        stopEvents = List(count) {
            DepartureMonitorResponse.StopEvent(
                departureTimePlanned = plannedTime,
                departureTimeEstimated = null,
                transportation = DepartureMonitorResponse.Transportation(
                    id = "T1",
                    disassembledName = "T1",
                    destination = DepartureMonitorResponse.Destination(
                        id = "dest",
                        name = "Destination $it",
                    ),
                    product = DepartureMonitorResponse.Product(cls = 1, iconId = 1, name = "Train"),
                ),
                location = null,
            )
        },
    )

    private companion object {
        const val STOP_A = "10111010"
        const val STOP_B = "10111020"
        val DEFAULT_SOURCE = DepartureBoardSource.MAP_SHEET
    }
}

// ── Analytics tests ───────────────────────────────────────────────────────────

/**
 * Tests that verify [DeparturesViewModel] fires the correct analytics events with the
 * correct [DepartureBoardSource] attribution.
 *
 * Uses [CapturingAnalytics] instead of [NoOpAnalytics] so each tracked event can be asserted.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeparturesViewModelAnalyticsTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testConfig = DepartureBoardConfig(
        refreshIntervalMs = 1_000L,
        previousDeparturesWindowMinutes = 30L,
    )

    private lateinit var fakeService: FakeDeparturesService
    private lateinit var repository: DepartureBoardRepository
    private lateinit var analytics: CapturingAnalytics
    private lateinit var viewModel: DeparturesViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeService = FakeDeparturesService(response = buildResponse(2))
        analytics = CapturingAnalytics()
        repository = DepartureBoardRepository(
            departuresService = fakeService,
            ioDispatcher = testDispatcher,
            config = testConfig,
        )
        viewModel = DeparturesViewModel(
            repository = repository,
            analytics = analytics,
            ioDispatcher = testDispatcher,
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── LoadDepartures — screen-view source ───────────────────────────────────

    @Test
    fun `Given LoadDepartures with TIMETABLE_SHEET source Then screen view event tracks TIMETABLE_SHEET`() =
        runTest {
            viewModel.onEvent(
                DeparturesUiEvent.LoadDepartures(stopId = STOP_A, source = DepartureBoardSource.TIMETABLE_SHEET),
            )
            advanceUntilIdle()

            val screenViews = analytics.events.filterIsInstance<AnalyticsEvent.DepartureBoardScreenViewEvent>()
            assertEquals(1, screenViews.size)
            assertEquals(DepartureBoardSource.TIMETABLE_SHEET, screenViews.first().source)
        }

    @Test
    fun `Given LoadDepartures with MAP_SHEET source Then screen view event tracks MAP_SHEET`() =
        runTest {
            viewModel.onEvent(
                DeparturesUiEvent.LoadDepartures(stopId = STOP_A, source = DepartureBoardSource.MAP_SHEET),
            )
            advanceUntilIdle()

            val screenViews = analytics.events.filterIsInstance<AnalyticsEvent.DepartureBoardScreenViewEvent>()
            assertEquals(1, screenViews.size)
            assertEquals(DepartureBoardSource.MAP_SHEET, screenViews.first().source)
        }

    @Test
    fun `Given same stop loaded twice Then screen view event fires only once`() = runTest {
        viewModel.onEvent(DeparturesUiEvent.LoadDepartures(stopId = STOP_A, source = DepartureBoardSource.MAP_SHEET))
        advanceUntilIdle()
        viewModel.onEvent(DeparturesUiEvent.LoadDepartures(stopId = STOP_A, source = DepartureBoardSource.MAP_SHEET))
        advanceUntilIdle()

        val screenViews = analytics.events.filterIsInstance<AnalyticsEvent.DepartureBoardScreenViewEvent>()
        assertEquals(1, screenViews.size, "Screen view must not fire again for the same stop")
    }

    // ── Refresh — retry event ─────────────────────────────────────────────────

    @Test
    fun `Given active stop loaded with TIMETABLE_SHEET When Refresh sent Then retry event tracks TIMETABLE_SHEET`() =
        runTest {
            viewModel.onEvent(
                DeparturesUiEvent.LoadDepartures(stopId = STOP_A, source = DepartureBoardSource.TIMETABLE_SHEET),
            )
            advanceUntilIdle()

            viewModel.onEvent(DeparturesUiEvent.Refresh)
            advanceUntilIdle()

            val retryEvents = analytics.events
                .filterIsInstance<AnalyticsEvent.DepartureBoardStatusEvent>()
                .filter { it.action == AnalyticsEvent.DepartureBoardStatusEvent.Action.RETRY }
            assertEquals(1, retryEvents.size)
            assertEquals(DepartureBoardSource.TIMETABLE_SHEET, retryEvents.first().source)
            assertEquals(STOP_A, retryEvents.first().stopId)
        }

    @Test
    fun `Given no active stop When Refresh sent Then no retry event fired`() = runTest {
        viewModel.onEvent(DeparturesUiEvent.Refresh)
        advanceUntilIdle()

        val retryEvents = analytics.events
            .filterIsInstance<AnalyticsEvent.DepartureBoardStatusEvent>()
            .filter { it.action == AnalyticsEvent.DepartureBoardStatusEvent.Action.RETRY }
        assertEquals(0, retryEvents.size, "Refresh with no active stop must not fire a retry event")
    }

    // ── Error state — error event ─────────────────────────────────────────────

    @Test
    fun `Given API fails When error state reached Then error event fires once with correct source`() =
        runTest {
            fakeService.shouldThrow = true

            viewModel.uiState.test {
                awaitItem() // initial

                viewModel.onEvent(
                    DeparturesUiEvent.LoadDepartures(stopId = STOP_A, source = DepartureBoardSource.MAP_SHEET),
                )
                advanceUntilIdle()

                awaitItem() // loading
                awaitItem() // error state

                val errorEvents = analytics.events
                    .filterIsInstance<AnalyticsEvent.DepartureBoardStatusEvent>()
                    .filter { it.action == AnalyticsEvent.DepartureBoardStatusEvent.Action.ERROR }
                assertEquals(1, errorEvents.size, "Error event must fire exactly once per error transition")
                assertEquals(DepartureBoardSource.MAP_SHEET, errorEvents.first().source)
                assertEquals(STOP_A, errorEvents.first().stopId)

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── DepartureBoardToggle — toggle event ───────────────────────────────────

    @Test
    fun `Given DepartureBoardToggle event Then toggle event fires with the event source`() = runTest {
        viewModel.onEvent(
            DeparturesUiEvent.DepartureBoardToggle(
                stopId = STOP_A,
                stopName = "Central Station",
                expand = true,
                source = DepartureBoardSource.TIMETABLE_SHEET,
            ),
        )
        advanceUntilIdle()

        val toggleEvents = analytics.events.filterIsInstance<AnalyticsEvent.DepartureBoardToggleEvent>()
        assertEquals(1, toggleEvents.size)
        val event = toggleEvents.first()
        assertEquals(STOP_A, event.stopId)
        assertEquals(true, event.expand)
        assertEquals(DepartureBoardSource.TIMETABLE_SHEET, event.source)
    }

    @Test
    fun `Given active TIMETABLE_SHEET session When DepartureBoardToggle with MAP_SHEET Then toggle uses MAP_SHEET`() =
        runTest {
            viewModel.onEvent(
                DeparturesUiEvent.LoadDepartures(stopId = STOP_A, source = DepartureBoardSource.TIMETABLE_SHEET),
            )
            advanceUntilIdle()

            viewModel.onEvent(
                DeparturesUiEvent.DepartureBoardToggle(
                    stopId = STOP_A,
                    stopName = "Central Station",
                    expand = false,
                    source = DepartureBoardSource.MAP_SHEET,
                ),
            )
            advanceUntilIdle()

            val toggleEvents = analytics.events.filterIsInstance<AnalyticsEvent.DepartureBoardToggleEvent>()
            assertEquals(DepartureBoardSource.MAP_SHEET, toggleEvents.first().source)
        }

    // ── activeSource switches with stop ───────────────────────────────────────

    @Test
    fun `Given source changes with stop switch When Refresh sent Then retry uses new source`() =
        runTest {
            viewModel.onEvent(
                DeparturesUiEvent.LoadDepartures(stopId = STOP_A, source = DepartureBoardSource.TIMETABLE_SHEET),
            )
            advanceUntilIdle()

            viewModel.onEvent(
                DeparturesUiEvent.LoadDepartures(stopId = STOP_B, source = DepartureBoardSource.MAP_SHEET),
            )
            advanceUntilIdle()

            viewModel.onEvent(DeparturesUiEvent.Refresh)
            advanceUntilIdle()

            val retryEvents = analytics.events
                .filterIsInstance<AnalyticsEvent.DepartureBoardStatusEvent>()
                .filter { it.action == AnalyticsEvent.DepartureBoardStatusEvent.Action.RETRY }
            assertEquals(DepartureBoardSource.MAP_SHEET, retryEvents.last().source)
        }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun buildResponse(
        count: Int,
        plannedTime: String = "2099-01-01T09:00:00Z",
    ) = DepartureMonitorResponse(
        stopEvents = List(count) {
            DepartureMonitorResponse.StopEvent(
                departureTimePlanned = plannedTime,
                departureTimeEstimated = null,
                transportation = DepartureMonitorResponse.Transportation(
                    id = "T1",
                    disassembledName = "T1",
                    destination = DepartureMonitorResponse.Destination(
                        id = "dest",
                        name = "Destination $it",
                    ),
                    product = DepartureMonitorResponse.Product(cls = 1, iconId = 1, name = "Train"),
                ),
                location = null,
            )
        },
    )

    private companion object {
        const val STOP_A = "10111010"
        const val STOP_B = "10111020"
    }
}

/** No-op [Analytics] for tests — discards all events. */
private object NoOpAnalytics : Analytics {
    override fun track(event: AnalyticsEvent) = Unit
    override fun setUserId(userId: String) = Unit
    override fun setUserProperty(name: String, value: String) = Unit
}

/** Spy [Analytics] that records every tracked event for assertion in tests. */
private class CapturingAnalytics : Analytics {
    val events = mutableListOf<AnalyticsEvent>()
    override fun track(event: AnalyticsEvent) { events.add(event) }
    override fun setUserId(userId: String) = Unit
    override fun setUserProperty(name: String, value: String) = Unit
}
