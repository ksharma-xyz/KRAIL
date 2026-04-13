package xyz.ksharma.krail.departures.ui

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.krail.departures.network.api.model.DepartureMonitorResponse
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.departures.ui.state.DeparturesUiEvent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

                viewModel.onEvent(DeparturesUiEvent.LoadDepartures(STOP_A))
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

            viewModel.onEvent(DeparturesUiEvent.LoadDepartures(STOP_A))
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

            viewModel.onEvent(DeparturesUiEvent.LoadDepartures(STOP_A))
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
        viewModel.onEvent(DeparturesUiEvent.LoadDepartures(STOP_A))
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

            viewModel.onEvent(DeparturesUiEvent.LoadDepartures(STOP_A))
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
    }
}
