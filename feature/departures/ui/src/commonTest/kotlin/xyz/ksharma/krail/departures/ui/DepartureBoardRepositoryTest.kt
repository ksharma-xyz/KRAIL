package xyz.ksharma.krail.departures.ui

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import xyz.ksharma.krail.departures.network.api.model.DepartureMonitorResponse
import xyz.ksharma.krail.departures.network.api.service.DeparturesService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DepartureBoardRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    // Minimal config with a short refresh interval so tests don't wait 30 s.
    private val testConfig = DepartureBoardConfig(
        refreshIntervalMs = 1_000L,
        previousDeparturesWindowMinutes = 30L,
    )

    private fun makeRepo(service: DeparturesService) = DepartureBoardRepository(
        departuresService = service,
        ioDispatcher = testDispatcher,
        config = testConfig,
    )

    // ── pollStop — initial fetch ──────────────────────────────────────────────

    @Test
    fun `Given no cached data When pollStop collected Then emits loading then success`() =
        runTest {
            val service = FakeDeparturesService(response = buildResponse(count = 3))
            val repo = makeRepo(service)

            repo.pollStop(STOP_A).test {
                // loading
                val loading = awaitItem()
                assertTrue(loading.isLoading, "Should show full loading when no cached data")

                advanceUntilIdle()

                val success = awaitItem()
                assertFalse(success.isLoading)
                assertFalse(success.isError)
                assertEquals(3, success.departures.size)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given no cached data When API fails Then emits error state`() = runTest {
        val service = FakeDeparturesService(shouldThrow = true)
        val repo = makeRepo(service)

        repo.pollStop(STOP_A).test {
            awaitItem() // loading

            advanceUntilIdle()

            val error = awaitItem()
            assertFalse(error.isLoading)
            assertTrue(error.isError, "Should show error when API fails with no cached data")

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── pollStop — cancellation clears loading state ──────────────────────────

    @Test
    fun `Given pollStop collected When collection cancelled Then loading flags cleared`() =
        runTest {
            val service = FakeDeparturesService(response = buildResponse(count = 1))
            val repo = makeRepo(service)

            repo.pollStop(STOP_A).test {
                awaitItem() // loading — cancelled mid-flight
                cancelAndIgnoreRemainingEvents()
            }
            advanceUntilIdle()

            // After cancellation, cache should have isLoading=false
            repo.observeStop(STOP_A).test {
                val state = awaitItem()
                assertFalse(state.isLoading, "isLoading must be cleared after pollStop cancelled")
                assertFalse(state.silentLoading, "silentLoading must be cleared after pollStop cancelled")
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── pollStop — auto-refresh loop ──────────────────────────────────────────

    @Test
    fun `Given active pollStop When refresh interval elapses Then API is called again`() =
        runTest {
            val service = FakeDeparturesService(response = buildResponse(count = 1))
            val repo = makeRepo(service)

            repo.pollStop(STOP_A).test {
                awaitItem() // loading
                advanceUntilIdle()
                awaitItem() // first success
                val callsAfterFirst = service.callCount

                advanceTimeBy(testConfig.refreshIntervalMs + 100)
                advanceUntilIdle()

                assertTrue(service.callCount > callsAfterFirst, "Auto-refresh should fire after interval elapses")
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── pollStop — refresh window (cache hit) ─────────────────────────────────

    @Test
    fun `Given recent successful fetch When pollStop re-collected quickly Then waits for window before fetch`() =
        runTest {
            val service = FakeDeparturesService(response = buildResponse(count = 1))
            val repo = makeRepo(service)

            // First collection — fetches immediately
            repo.pollStop(STOP_A).test {
                awaitItem() // loading
                advanceUntilIdle()
                awaitItem() // success
                cancelAndIgnoreRemainingEvents()
            }
            val callsAfterFirst = service.callCount

            // Re-collect immediately — still within the refresh window
            repo.pollStop(STOP_A).test {
                // Should NOT emit a new loading state (has cached data)
                advanceUntilIdle()
                // No new fetch should have fired yet
                assertEquals(callsAfterFirst, service.callCount, "No immediate re-fetch within the refresh window")
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── observeStop — reads cache without polling ─────────────────────────────

    @Test
    fun `Given no active polling When observeStop collected Then emits idle state — no API call`() =
        runTest {
            val service = FakeDeparturesService(response = buildResponse(count = 2))
            val repo = makeRepo(service)

            repo.observeStop(STOP_A).test {
                val initial = awaitItem()
                assertFalse(initial.isLoading, "observeStop should not trigger loading")
                assertEquals(0, service.callCount, "observeStop must not call the API")
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    fun `Given cached data When refresh called Then API is called immediately`() = runTest {
        val service = FakeDeparturesService(response = buildResponse(count = 1))
        val repo = makeRepo(service)

        repo.pollStop(STOP_A).test {
            awaitItem() // loading
            advanceUntilIdle()
            awaitItem() // success
            val callsBefore = service.callCount

            repo.refresh(STOP_A)
            advanceUntilIdle()

            assertEquals(callsBefore + 1, service.callCount, "refresh should trigger one extra API call")
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── loadPreviousDepartures ────────────────────────────────────────────────

    @Test
    fun `Given loadPreviousDepartures called When API succeeds Then previousDepartures populated`() =
        runTest {
            val pastTime = "2020-01-01T00:00:00Z"
            val service = FakeDeparturesService(response = buildResponse(count = 2, plannedTime = pastTime))
            val repo = makeRepo(service)

            repo.pollStop(STOP_A).test {
                awaitItem() // loading
                advanceUntilIdle()
                awaitItem() // success

                repo.loadPreviousDepartures(STOP_A)
                advanceUntilIdle()

                val loading = awaitItem()
                assertTrue(loading.isPreviousLoading)

                val done = awaitItem()
                assertFalse(done.isPreviousLoading)
                assertTrue(done.previousDepartures.isNotEmpty(), "Previous departures should be populated")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given loadPreviousDepartures called When API fails Then isPreviousLoading is reset to false`() =
        runTest {
            val service = FakeDeparturesService(response = buildResponse(count = 1))
            val repo = makeRepo(service)

            repo.pollStop(STOP_A).test {
                awaitItem() // loading
                advanceUntilIdle()
                awaitItem() // success

                service.shouldThrow = true

                repo.loadPreviousDepartures(STOP_A)
                advanceUntilIdle()

                val loading = awaitItem()
                assertTrue(loading.isPreviousLoading)

                val done = awaitItem()
                assertFalse(done.isPreviousLoading, "isPreviousLoading must be cleared even on failure")

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── silent-loading cleared on cancellation ────────────────────────────────

    @Test
    fun `Given silent refresh in flight When collection cancelled Then silentLoading cleared`() =
        runTest {
            val service = FakeDeparturesService(response = buildResponse(count = 1))
            val repo = makeRepo(service)

            // First collect — populates cache so next collect does a silent refresh
            repo.pollStop(STOP_A).test {
                awaitItem() // loading
                advanceUntilIdle()
                awaitItem() // success
                cancelAndIgnoreRemainingEvents()
            }

            // Second collect — cancels while silentLoading may be in flight
            repo.pollStop(STOP_A).test {
                cancelAndIgnoreRemainingEvents()
            }
            advanceUntilIdle()

            repo.observeStop(STOP_A).test {
                val state = awaitItem()
                assertFalse(state.silentLoading, "silentLoading must be cleared after cancellation")
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
        const val STOP_B = "10111020"
    }
}

/**
 * Fake [DeparturesService] that returns a configurable response or throws.
 */
class FakeDeparturesService(
    var response: DepartureMonitorResponse = DepartureMonitorResponse(stopEvents = emptyList()),
    var shouldThrow: Boolean = false,
) : DeparturesService {

    var callCount = 0
        private set

    override suspend fun departures(
        stopId: String,
        date: String?,
        time: String?,
    ): DepartureMonitorResponse {
        callCount++
        if (shouldThrow) throw RuntimeException("Fake network error")
        return response
    }
}
