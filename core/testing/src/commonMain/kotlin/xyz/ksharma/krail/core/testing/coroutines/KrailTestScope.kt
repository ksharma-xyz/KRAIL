package xyz.ksharma.krail.core.testing.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlin.time.Duration

/**
 * The single shared coroutine/scheduler harness for the entire KRAIL test suite.
 *
 * Solves three pre-existing pain points that produced flakes and a 98 GB CI log:
 *
 *  1. **"Detected use of different schedulers"** — a field-level
 *     `StandardTestDispatcher()` creates its own [TestCoroutineScheduler] while bare
 *     `runTest { }` creates another, so the first dispatch onto the production
 *     `ioDispatcher` blows up. [KrailTestScope] owns ONE scheduler and exposes it as
 *     [ioDispatcher] / [mainDispatcher] so every collaborator runs on the same virtual
 *     clock. `krailRunTest` (see `KrailRunTest.kt`) passes the same scheduler into
 *     `runTest`.
 *  2. **Unbounded virtual time against infinite pollers** — `channelFlow` /
 *     `while (true) { delay(...); fetch() }` loops never settle, so `advanceUntilIdle()`
 *     spins forever. Tests of pollers must use [pumpOnce] (bounded
 *     `advanceTimeBy + runCurrent`) plus Turbine with an explicit cancellation.
 *  3. **Per-test ceremony** — every test currently re-declares the dispatcher fields
 *     and `Dispatchers.setMain` / `resetMain` plumbing. `krailRunTest` centralises that.
 *
 * Idiomatic usage:
 * ```
 * @Test fun `loads data` () = krailRunTest {
 *     val repo = MyRepository(service = FakeService(), ioDispatcher = ioDispatcher)
 *     repo.observe().test {                         // Turbine
 *         runCurrent()                              // emit current frame, no time jump
 *         assertEquals(Initial, awaitItem())
 *         pumpOnce(refreshIntervalMs)               // exactly ONE poll cycle for an
 *         assertEquals(Refreshed, awaitItem())      // infinite-poller flow
 *         cancelAndIgnoreRemainingEvents()          // never let an infinite flow spin
 *     }
 * }
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KrailTestScope internal constructor(
    private val testScope: TestScope,
) : CoroutineScope by testScope {

    val scheduler: TestCoroutineScheduler = testScope.testScheduler

    /** Inject into production `ioDispatcher` slots so I/O runs on the same virtual clock. */
    val ioDispatcher: CoroutineDispatcher = StandardTestDispatcher(scheduler, name = "krail-io")

    /**
     * Same scheduler, separate name for trace clarity. `krailRunTest` installs this as
     * `Dispatchers.Main` so Molecule / ViewModel code that captures `Dispatchers.Main`
     * at construction time sees this dispatcher.
     */
    val mainDispatcher: CoroutineDispatcher = StandardTestDispatcher(scheduler, name = "krail-main")

    /**
     * Drain all coroutines whose dispatch time is the *current* virtual instant.
     * Does NOT advance virtual time, so it's safe against infinite pollers (unlike
     * `advanceUntilIdle()`, which is deliberately NOT exposed here — see [pumpOnce]).
     *
     * Use this after a one-shot launch (e.g. `repo.refresh(...)`, the first fetch on a
     * fresh `pollStop` collection) where you want the work to complete without time
     * passing.
     */
    fun runCurrent() {
        scheduler.runCurrent()
    }

    /**
     * Bounded advance — the ONLY safe pattern against `channelFlow` / `while(true){…}`
     * pollers. Advances [interval] of virtual time, then drains the work scheduled at
     * exactly that instant. Never use `advanceUntilIdle()` against an infinite poller
     * (it never returns; see commit history of `DepartureBoardRepositoryTest`).
     */
    fun pumpOnce(interval: Duration) {
        scheduler.advanceTimeBy(interval)
        scheduler.runCurrent()
    }

    /** Convenience overload for poll intervals already expressed as milliseconds. */
    fun pumpOnce(intervalMs: Long) {
        scheduler.advanceTimeBy(intervalMs)
        scheduler.runCurrent()
    }
}
