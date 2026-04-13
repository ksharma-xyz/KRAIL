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

    // ── setActiveStop / observeStop ───────────────────────────────────────────

    @Test
    fun `Given no active stop When setActiveStop is called Then observeStop emits loading state`() =
        runTest {
            val service = FakeDeparturesService()
            val repo = makeRepo(service)

            repo.observeStop(STOP_A).test {
                // Initial state before setActiveStop
                val initial = awaitItem()
                assertFalse(initial.isLoading, "Initial state should not be loading")

                repo.setActiveStop(STOP_A)
                advanceUntilIdle()

                // After setActiveStop with no cached data, repository emits loading=true
                val loading = awaitItem()
                assertTrue(loading.isLoading, "Should show full loading when no cached data")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given active stop set When API succeeds Then observeStop emits departures`() =
        runTest {
            val service = FakeDeparturesService(response = buildResponse(count = 3))
            val repo = makeRepo(service)

            repo.observeStop(STOP_A).test {
                awaitItem() // initial

                repo.setActiveStop(STOP_A)
                advanceUntilIdle()

                // loading state
                awaitItem()

                val result = awaitItem()
                assertFalse(result.isLoading)
                assertFalse(result.isError)
                assertEquals(3, result.departures.size)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given active stop set When API fails Then observeStop emits error state`() =
        runTest {
            val service = FakeDeparturesService(shouldThrow = true)
            val repo = makeRepo(service)

            repo.observeStop(STOP_A).test {
                awaitItem() // initial

                repo.setActiveStop(STOP_A)
                advanceUntilIdle()

                awaitItem() // loading

                val result = awaitItem()
                assertFalse(result.isLoading)
                assertTrue(result.isError, "Should show error when API fails and no cached data")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given setActiveStop called with same stop When already active Then NOOP - no extra loading emitted`() =
        runTest {
            val service = FakeDeparturesService(response = buildResponse(count = 2))
            val repo = makeRepo(service)

            repo.setActiveStop(STOP_A)
            advanceUntilIdle()

            val callCountBefore = service.callCount

            repo.setActiveStop(STOP_A) // same stop — should NOOP
            advanceUntilIdle()

            assertEquals(callCountBefore, service.callCount, "No extra API call for same stop")
        }

    @Test
    fun `Given stop A is active When setActiveStop called with null Then polling stops`() =
        runTest {
            val service = FakeDeparturesService(response = buildResponse(count = 1))
            val repo = makeRepo(service)

            repo.setActiveStop(STOP_A)
            advanceUntilIdle()
            val callsAfterFirstActivation = service.callCount

            repo.setActiveStop(null)
            advanceUntilIdle()

            // Advance past the refresh interval — no new calls should fire
            advanceTimeBy(testConfig.refreshIntervalMs * 3)
            advanceUntilIdle()

            assertEquals(callsAfterFirstActivation, service.callCount, "No further calls after stop set to null")
        }

    // ── stopIfActive ─────────────────────────────────────────────────────────

    @Test
    fun `Given stop A is active When stopIfActive called with A Then loading state is cleared`() =
        runTest {
            val service = FakeDeparturesService(response = buildResponse(count = 2))
            val repo = makeRepo(service)

            repo.observeStop(STOP_A).test {
                awaitItem() // initial

                repo.setActiveStop(STOP_A)
                advanceUntilIdle()

                // Consume loading + success
                awaitItem()
                awaitItem()

                repo.stopIfActive(STOP_A)
                advanceUntilIdle()

                // After stopIfActive, loading flags must be false
                // (the repo clears them even if a job was cancelled mid-flight)
                expectNoEvents()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `Given stop A is active When stopIfActive called with stop B Then polling for A continues`() =
        runTest {
            val service = FakeDeparturesService(response = buildResponse(count = 1))
            val repo = makeRepo(service)

            repo.setActiveStop(STOP_A)
            advanceUntilIdle()
            val callsAfterActivation = service.callCount

            repo.stopIfActive(STOP_B) // wrong stop — should NOOP
            advanceUntilIdle()

            // Advance past one refresh window — polling should still fire
            advanceTimeBy(testConfig.refreshIntervalMs + 100)
            advanceUntilIdle()

            assertTrue(service.callCount > callsAfterActivation, "Polling should continue after NOOP stopIfActive")
        }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    fun `Given active stop When refresh called Then API is called immediately`() =
        runTest {
            val service = FakeDeparturesService(response = buildResponse(count = 1))
            val repo = makeRepo(service)

            repo.setActiveStop(STOP_A)
            advanceUntilIdle()
            val callsBefore = service.callCount

            repo.refresh(STOP_A)
            advanceUntilIdle()

            assertEquals(callsBefore + 1, service.callCount, "refresh should trigger one extra API call")
        }

    // ── refresh window (cache hit) ────────────────────────────────────────────

    @Test
    fun `Given recent fetch When setActiveStop same stop then switch back quickly Then no duplicate fetch`() =
        runTest {
            val service = FakeDeparturesService(response = buildResponse(count = 1))
            val repo = makeRepo(service)

            repo.setActiveStop(STOP_A)
            advanceUntilIdle()
            val callsAfterFirst = service.callCount

            repo.setActiveStop(STOP_B)
            advanceUntilIdle()
            repo.setActiveStop(STOP_A) // return to A quickly — should be within the refresh window
            advanceUntilIdle()

            // Stop A was fetched recently — the window check in startPolling means no immediate re-fetch
            assertEquals(callsAfterFirst, service.callCount - 1, "Re-activating stop A within window should wait, not fetch immediately")
        }

    // ── auto-refresh loop ─────────────────────────────────────────────────────

    @Test
    fun `Given active stop When refresh interval elapses Then API is called again`() =
        runTest {
            val service = FakeDeparturesService(response = buildResponse(count = 1))
            val repo = makeRepo(service)

            repo.setActiveStop(STOP_A)
            advanceUntilIdle()
            val callsAfterFirst = service.callCount

            advanceTimeBy(testConfig.refreshIntervalMs + 100)
            advanceUntilIdle()

            assertTrue(service.callCount > callsAfterFirst, "Auto-refresh should fire after interval elapses")
        }

    // ── stale loading state cleared on job cancel ─────────────────────────────

    @Test
    fun `Given silent refresh in flight When setActiveStop switches stop Then abandoned stop loading is cleared`() =
        runTest {
            val service = FakeDeparturesService(response = buildResponse(count = 1))
            val repo = makeRepo(service)

            // Load stop A once so it has data (enabling silent refresh on re-activation)
            repo.setActiveStop(STOP_A)
            advanceUntilIdle()

            repo.observeStop(STOP_A).test {
                awaitItem() // current state

                // Switch away — cancels any in-flight job for stop A
                repo.setActiveStop(STOP_B)
                advanceUntilIdle()

                // Stop A's loading flags should be cleared after the job is cancelled
                val stateAfterSwitch = expectMostRecentItem()
                assertFalse(stateAfterSwitch.isLoading, "isLoading must be false after stop switched away")
                assertFalse(stateAfterSwitch.silentLoading, "silentLoading must be false after stop switched away")

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── loadPreviousDepartures ────────────────────────────────────────────────

    @Test
    fun `Given loadPreviousDepartures called When API succeeds Then previousDepartures populated`() =
        runTest {
            // Use a departure time in the past so the filter keeps it
            val pastTime = "2020-01-01T00:00:00Z"
            val service = FakeDeparturesService(response = buildResponse(count = 2, plannedTime = pastTime))
            val repo = makeRepo(service)

            repo.setActiveStop(STOP_A)
            advanceUntilIdle()

            repo.observeStop(STOP_A).test {
                awaitItem() // current state after initial fetch

                repo.loadPreviousDepartures(STOP_A)
                advanceUntilIdle()

                // isPreviousLoading = true then false
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

            repo.setActiveStop(STOP_A)
            advanceUntilIdle()

            // Now make service throw for the next call (previous departures fetch)
            service.shouldThrow = true

            repo.observeStop(STOP_A).test {
                awaitItem() // current state

                repo.loadPreviousDepartures(STOP_A)
                advanceUntilIdle()

                val loading = awaitItem()
                assertTrue(loading.isPreviousLoading)

                val done = awaitItem()
                assertFalse(done.isPreviousLoading, "isPreviousLoading must be cleared even on failure")

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
