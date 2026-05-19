package xyz.ksharma.krail.core.testing.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
) = runTest {
    val scope = KrailTestScope(this)
    Dispatchers.setMain(scope.mainDispatcher)
    try {
        scope.body()
    } finally {
        Dispatchers.resetMain()
    }
}
