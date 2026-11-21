package xyz.ksharma.krail.core.testing.fakes

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import xyz.ksharma.krail.io.gtfs.readiness.AppDataReadiness
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness

/**
 * A readiness the test drives with [emit]. It never runs an insert, [retry] counts and calls
 * [onRetry]. [awaitReady] resolves the way the real one does: on Ready or Failed, or on the timeout.
 */
class FakeAppDataReadiness(
    initial: StopsReadiness = StopsReadiness.Ready(stopCount = 0),
    private val onRetry: () -> Unit = {},
) : AppDataReadiness {

    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<StopsReadiness> = _state.asStateFlow()

    var retryCallCount = 0
        private set

    fun emit(state: StopsReadiness) {
        _state.value = state
    }

    override suspend fun awaitReady(timeoutMs: Long): StopsReadiness =
        withTimeoutOrNull(timeoutMs) {
            _state.first { it is StopsReadiness.Ready || it is StopsReadiness.Failed }
        } ?: _state.value

    override fun retry() {
        retryCallCount++
        onRetry()
    }
}
