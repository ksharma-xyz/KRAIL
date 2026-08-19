package xyz.ksharma.krail.core.testing.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * KMP-safe drop-in replacement for `runTest { }` + manual `Dispatchers.setMain/resetMain`.
 * Owns the single shared `TestCoroutineScheduler` for both `TestScope` and any production
 * dispatcher exposed via [KrailTestScope]. No JUnit `@Rule` required — works in plain
 * `kotlin.test` on every KMP target.
 *
 * See [KrailTestScope] for the full rationale (shared scheduler, infinite-poller rule,
 * idiomatic Turbine pattern).
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun krailRunTest(
    body: suspend KrailTestScope.() -> Unit,
): TestResult = try {
    runTest {
        val scope = KrailTestScope(this)
        Dispatchers.setMain(scope.mainDispatcher)
        scope.body()
    }
} finally {
    // Reset AFTER runTest returns, not inside it. `runTest` drains the scheduler once
    // more on the way out, and anything still parked on a scope that is not a child of
    // the test scope — a `viewModelScope`, most often — dispatches onto Dispatchers.Main
    // during that drain. Resetting inside the test body leaves those dispatches with no
    // Main delegate, and the test dies with a DispatchException that has nothing to do
    // with what it was asserting.
    Dispatchers.resetMain()
}
