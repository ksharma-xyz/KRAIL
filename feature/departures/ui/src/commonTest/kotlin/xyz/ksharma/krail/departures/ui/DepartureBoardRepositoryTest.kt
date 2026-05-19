package xyz.ksharma.krail.departures.ui

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.core.testing.fakes.FakeDeparturesService
import xyz.ksharma.krail.departures.network.api.model.DepartureMonitorResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Re-enabled #1601. Two structural fixes since the original suite shipped @Ignore'd:
 *
 *  1. ONE shared scheduler. `krailRunTest` (KrailTestKit, :core:testing) owns the single
 *     `TestCoroutineScheduler` exposed via `ioDispatcher`. Production
 *     `DepartureBoardRepository` accepts `ioDispatcher` in its constructor, so passing
 *     `ioDispatcher` makes the repository's internal launches run on the same virtual
 *     clock as `runTest`. No more "Detected use of different schedulers" trap.
 *
 *  2. Bounded virtual time. `pollStop` is a `channelFlow { while (true) { delay(...);
 *     fetch() } }` infinite poller. `advanceUntilIdle()` against this never returns —
 *     it spun forever and produced a 98 GB Gradle log. The fix is to use only:
 *       - `runCurrent()` to drain work already scheduled at the *current* virtual instant
 *         (the first fetch after a fresh collection)
 *       - `pumpOnce(intervalMs + δ)` to advance time by exactly one refresh window and
 *         then drain — fires precisely ONE auto-refresh cycle, no more.
 *     `cancelAndIgnoreRemainingEvents()` on the Turbine block is mandatory so the
 *     channelFlow is structurally cancelled before the next test inherits a live job.
 *
 * The local `FakeDeparturesService` was removed in favour of the canonical one from
 * `:core:testing`, which carries the same `response` / `shouldThrow` / `callCount` API.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DepartureBoardRepositoryTest {

    // Minimal config with a short refresh interval so tests don't wait 30 s of virtual time.
    private val testConfig = DepartureBoardConfig(
        refreshIntervalMs = REFRESH_INTERVAL_MS,
        previousDeparturesWindowMinutes = PREV_WINDOW_MINUTES,
    )

    // ── pollStop — initial fetch ──────────────────────────────────────────────

    @Test
    fun `Given no cached data When pollStop collected Then emits loading then success`() = krailRunTest {
        val service = FakeDeparturesService(response = buildResponse(count = 3))
        val repo = DepartureBoardRepository(service, ioDispatcher, testConfig)

        repo.pollStop(STOP_A).test {
            val loading = awaitItem()
            assertTrue(loading.isLoading, "Should show full loading when no cached data")

            runCurrent()

            val success = awaitItem()
            assertFalse(success.isLoading)
            assertFalse(success.isError)
            assertEquals(3, success.departures.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given no cached data When API fails Then emits error state`() = krailRunTest {
        val service = FakeDeparturesService(shouldThrow = true)
        val repo = DepartureBoardRepository(service, ioDispatcher, testConfig)

        repo.pollStop(STOP_A).test {
            awaitItem() // loading
            runCurrent()

            val error = awaitItem()
            assertFalse(error.isLoading)
            assertTrue(error.isError, "Should show error when API fails with no cached data")

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── pollStop — cancellation clears loading state ──────────────────────────

    @Test
    fun `Given pollStop collected When collection cancelled Then loading flags cleared`() = krailRunTest {
        val service = FakeDeparturesService(response = buildResponse(count = 1))
        val repo = DepartureBoardRepository(service, ioDispatcher, testConfig)

        repo.pollStop(STOP_A).test {
            awaitItem() // loading — cancelled mid-flight
            cancelAndIgnoreRemainingEvents()
        }
        runCurrent()

        // After cancellation, cache should have isLoading=false.
        repo.observeStop(STOP_A).test {
            val state = awaitItem()
            assertFalse(state.isLoading, "isLoading must be cleared after pollStop cancelled")
            assertFalse(state.silentLoading, "silentLoading must be cleared after pollStop cancelled")
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── pollStop — auto-refresh loop ──────────────────────────────────────────

    @Test
    fun `Given active pollStop When refresh interval elapses Then API is called again`() = krailRunTest {
        val service = FakeDeparturesService(response = buildResponse(count = 1))
        val repo = DepartureBoardRepository(service, ioDispatcher, testConfig)

        repo.pollStop(STOP_A).test {
            awaitItem() // loading
            runCurrent()
            awaitItem() // first success
            val callsAfterFirst = service.callCount

            pumpOnce(REFRESH_INTERVAL_MS + EXTRA_DELTA_MS)

            assertTrue(
                service.callCount > callsAfterFirst,
                "Auto-refresh should fire after interval elapses",
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── pollStop — refresh window (cache hit) ─────────────────────────────────

    @Test
    fun `Given recent successful fetch When pollStop re-collected quickly Then waits for window before fetch`() =
        krailRunTest {
            val service = FakeDeparturesService(response = buildResponse(count = 1))
            val repo = DepartureBoardRepository(service, ioDispatcher, testConfig)

            // First collection — fetches immediately.
            repo.pollStop(STOP_A).test {
                awaitItem() // loading
                runCurrent()
                awaitItem() // success
                cancelAndIgnoreRemainingEvents()
            }
            val callsAfterFirst = service.callCount

            // Re-collect immediately — still within the refresh window. Must NOT call
            // `pumpOnce` here: that would advance virtual time and fire the auto-refresh.
            repo.pollStop(STOP_A).test {
                runCurrent()
                assertEquals(
                    callsAfterFirst, service.callCount,
                    "No immediate re-fetch within the refresh window",
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── observeStop — reads cache without polling ─────────────────────────────

    @Test
    fun `Given no active polling When observeStop collected Then emits idle state — no API call`() = krailRunTest {
        val service = FakeDeparturesService(response = buildResponse(count = 2))
        val repo = DepartureBoardRepository(service, ioDispatcher, testConfig)

        repo.observeStop(STOP_A).test {
            val initial = awaitItem()
            assertFalse(initial.isLoading, "observeStop should not trigger loading")
            assertEquals(0, service.callCount, "observeStop must not call the API")
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    fun `Given cached data When refresh called Then API is called immediately`() = krailRunTest {
        val service = FakeDeparturesService(response = buildResponse(count = 1))
        val repo = DepartureBoardRepository(service, ioDispatcher, testConfig)

        repo.pollStop(STOP_A).test {
            awaitItem() // loading
            runCurrent()
            awaitItem() // success
            val callsBefore = service.callCount

            repo.refresh(STOP_A)
            runCurrent()

            assertEquals(callsBefore + 1, service.callCount, "refresh should trigger one extra API call")
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── loadPreviousDepartures ────────────────────────────────────────────────

    @Test
    fun `Given loadPreviousDepartures called When API succeeds Then previousDepartures populated`() = krailRunTest {
        val pastTime = "2020-01-01T00:00:00Z"
        val service = FakeDeparturesService(response = buildResponse(count = 2, plannedTime = pastTime))
        val repo = DepartureBoardRepository(service, ioDispatcher, testConfig)

        repo.pollStop(STOP_A).test {
            awaitItem() // loading
            runCurrent()
            awaitItem() // success

            repo.loadPreviousDepartures(STOP_A)
            runCurrent()

            val loading = awaitItem()
            assertTrue(loading.isPreviousLoading)

            val done = awaitItem()
            assertFalse(done.isPreviousLoading)
            assertTrue(done.previousDepartures.isNotEmpty(), "Previous departures should be populated")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given loadPreviousDepartures called When API fails Then isPreviousLoading is reset to false`() = krailRunTest {
        val service = FakeDeparturesService(response = buildResponse(count = 1))
        val repo = DepartureBoardRepository(service, ioDispatcher, testConfig)

        repo.pollStop(STOP_A).test {
            awaitItem() // loading
            runCurrent()
            awaitItem() // success

            service.shouldThrow = true

            repo.loadPreviousDepartures(STOP_A)
            runCurrent()

            val loading = awaitItem()
            assertTrue(loading.isPreviousLoading)

            val done = awaitItem()
            assertFalse(done.isPreviousLoading, "isPreviousLoading must be cleared even on failure")

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── silent-loading cleared on cancellation ────────────────────────────────

    @Test
    fun `Given silent refresh in flight When collection cancelled Then silentLoading cleared`() = krailRunTest {
        val service = FakeDeparturesService(response = buildResponse(count = 1))
        val repo = DepartureBoardRepository(service, ioDispatcher, testConfig)

        // First collect — populates cache so next collect does a silent refresh.
        repo.pollStop(STOP_A).test {
            awaitItem() // loading
            runCurrent()
            awaitItem() // success
            cancelAndIgnoreRemainingEvents()
        }

        // Second collect — cancels while silentLoading may be in flight.
        repo.pollStop(STOP_A).test {
            cancelAndIgnoreRemainingEvents()
        }
        runCurrent()

        repo.observeStop(STOP_A).test {
            val state = awaitItem()
            assertFalse(state.silentLoading, "silentLoading must be cleared after cancellation")
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── cache isolation ───────────────────────────────────────────────────────

    @Test
    fun `Given two stops polled separately When each succeeds Then their states are independent`() = krailRunTest {
        val service = FakeDeparturesService(response = buildResponse(count = 2))
        val repo = DepartureBoardRepository(service, ioDispatcher, testConfig)

        // Poll STOP_A — 2 departures.
        repo.pollStop(STOP_A).test {
            awaitItem() // loading
            runCurrent()
            awaitItem() // success
            cancelAndIgnoreRemainingEvents()
        }

        // Poll STOP_B — 4 departures (change response).
        service.response = buildResponse(count = 4)
        repo.pollStop(STOP_B).test {
            awaitItem() // loading
            runCurrent()
            awaitItem() // success
            cancelAndIgnoreRemainingEvents()
        }

        // STOP_A should still have its original 2 departures.
        repo.observeStop(STOP_A).test {
            val stateA = awaitItem()
            assertEquals(2, stateA.departures.size, "STOP_A state must not be affected by STOP_B poll")
            cancelAndIgnoreRemainingEvents()
        }

        repo.observeStop(STOP_B).test {
            val stateB = awaitItem()
            assertEquals(4, stateB.departures.size, "STOP_B state should reflect its own poll")
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── loadPreviousDepartures — cache hit ────────────────────────────────────

    @Test
    fun `Given loadPreviousDepartures succeeded When called again quickly Then no extra API call`() = krailRunTest {
        val pastTime = "2020-01-01T00:00:00Z"
        val service = FakeDeparturesService(response = buildResponse(count = 2, plannedTime = pastTime))
        val repo = DepartureBoardRepository(service, ioDispatcher, testConfig)

        repo.pollStop(STOP_A).test {
            awaitItem() // loading
            runCurrent()
            awaitItem() // success

            repo.loadPreviousDepartures(STOP_A)
            runCurrent()

            awaitItem() // isPreviousLoading = true
            awaitItem() // done — previousDepartures populated

            val callsAfterFirst = service.callCount

            // Second call within the refresh window — cache hit: returns immediately
            // without suspending. Do NOT `pumpOnce` here — that would advance virtual
            // time, fire the auto-refresh loop, and inflate callCount.
            repo.loadPreviousDepartures(STOP_A)

            assertEquals(
                callsAfterFirst, service.callCount,
                "Second loadPreviousDepartures within the refresh window must not call the API",
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── fetchDepartures — error with existing data ────────────────────────────

    @Test
    fun `Given cached departures When refresh fails Then isError stays false`() = krailRunTest {
        val service = FakeDeparturesService(response = buildResponse(count = 2))
        val repo = DepartureBoardRepository(service, ioDispatcher, testConfig)

        repo.pollStop(STOP_A).test {
            awaitItem() // loading
            runCurrent()
            awaitItem() // success (2 departures cached)

            service.shouldThrow = true
            repo.refresh(STOP_A)
            runCurrent()

            // silentLoading cleared; departures preserved; isError stays false.
            val afterFailure = awaitItem()
            assertFalse(afterFailure.isError, "isError must stay false when cached data exists")
            assertEquals(2, afterFailure.departures.size, "Cached departures must survive a failed refresh")

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── observeStop — reflects pollStop results ───────────────────────────────

    @Test
    fun `Given pollStop completed When observeStop collected Then returns cached data`() = krailRunTest {
        val service = FakeDeparturesService(response = buildResponse(count = 3))
        val repo = DepartureBoardRepository(service, ioDispatcher, testConfig)

        repo.pollStop(STOP_A).test {
            awaitItem() // loading
            runCurrent()
            awaitItem() // success
            cancelAndIgnoreRemainingEvents()
        }

        // observeStop should see the data that pollStop fetched.
        repo.observeStop(STOP_A).test {
            val state = awaitItem()
            assertEquals(3, state.departures.size, "observeStop should reflect data fetched by pollStop")
            assertFalse(state.isLoading)
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
        const val REFRESH_INTERVAL_MS = 1_000L
        const val EXTRA_DELTA_MS = 100L
        const val PREV_WINDOW_MINUTES = 30L
    }
}
