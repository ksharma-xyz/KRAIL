package xyz.ksharma.krail.io.gtfs.readiness

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.coroutines.ext.suspendSafeResult
import xyz.ksharma.krail.io.gtfs.nswstops.StopsManager
import xyz.ksharma.krail.io.gtfs.nswstops.StopsManager.Companion.MINIMUM_REQUIRED_NSW_STOPS
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.sandook.SandookPreferences.Companion.KEY_NSW_STOPS_VERSION
import xyz.ksharma.krail.sandook.SandookPreferences.Companion.NSW_STOPS_VERSION
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs the stops insert and publishes whether the stops are usable. The decision lives in
 * [reduceReadiness]; this class only sequences the work, reads the evidence and retries.
 *
 * Bus routes follow the stops in the same run, best effort: once the stops are Ready the bus
 * routes insert runs, a failure there is logged and changes nothing, and it is not retried until
 * the next launch.
 */
internal class RealAppDataReadiness(
    private val stopsManager: StopsManager,
    private val busRoutesManager: StopsManager,
    private val sandook: Sandook,
    private val preferences: SandookPreferences,
    private val ioDispatcher: CoroutineDispatcher,
    private val scope: CoroutineScope,
) : AppDataReadiness {

    private val _state = MutableStateFlow<StopsReadiness>(StopsReadiness.Unknown)
    override val state: StateFlow<StopsReadiness> = _state.asStateFlow()

    // Only touched by the single run that holds the Loading claim, so runs never overlap.
    private var attempts = 0

    override suspend fun awaitReady(timeoutMs: Long): StopsReadiness {
        if (_state.value == StopsReadiness.Unknown) retry()
        return withTimeoutOrNull(timeoutMs) {
            _state.first { it is StopsReadiness.Ready || it is StopsReadiness.Failed }
        } ?: _state.value
    }

    override fun retry() {
        val retriesAllowed = claimRun() ?: return
        scope.launch { runAttempts(retriesAllowed) }
    }

    /**
     * Moves to Loading atomically so two callers cannot both start. Null when a run is in progress,
     * or when the stops are already Ready, so a repeated launch call cannot demote good data.
     */
    private fun claimRun(): Int? {
        while (true) {
            val current = _state.value
            if (current == StopsReadiness.Loading || current is StopsReadiness.Ready) return null
            if (_state.compareAndSet(current, StopsReadiness.Loading)) {
                return if (current == StopsReadiness.Unknown) MAX_AUTOMATIC_RETRIES else 0
            }
        }
    }

    /** Whatever happens, the state leaves Loading: a cancelled or crashed run ends in Failed. */
    private suspend fun runAttempts(retriesAllowed: Int) {
        var published = false
        try {
            val outcome = attemptUntilTerminal(retriesAllowed)
            _state.value = outcome
            published = true
            if (outcome is StopsReadiness.Ready) insertBusRoutesBestEffort()
        } finally {
            if (!published) _state.value = StopsReadiness.Failed(attempts, REASON_RUN_INTERRUPTED)
        }
    }

    private suspend fun attemptUntilTerminal(retriesAllowed: Int): StopsReadiness {
        var retriesUsed = 0
        while (true) {
            attempts++
            val outcome = reduceReadiness(runStopsOnce(), attempts)
            if (outcome !is StopsReadiness.Failed || retriesUsed == retriesAllowed) return outcome
            retriesUsed++
            log("AppDataReadiness attempt $attempts failed, retry $retriesUsed of $retriesAllowed")
            delay(RETRY_BACKOFF_MS * retriesUsed)
        }
    }

    private suspend fun runStopsOnce(): ReadinessInput {
        val failure = runCatching { stopsManager.insertStops().getOrThrow() }
            .onFailure { if (it is CancellationException) throw it }
            .exceptionOrNull()
        if (failure != null) return ReadinessInput.InsertFailed(failure.reasonText())
        return suspendSafeResult(ioDispatcher) { readEvidence() }
            .getOrElse { ReadinessInput.InsertFailed(it.reasonText()) }
    }

    private suspend fun insertBusRoutesBestEffort() {
        runCatching { busRoutesManager.insertStops().getOrThrow() }
            .onFailure { error ->
                if (error is CancellationException) throw error
                logError("AppDataReadiness bus routes insert failed, not retried until next launch: ${error.message}")
            }
    }

    private fun readEvidence() = ReadinessInput.InsertDone(
        storedVersion = preferences.getLong(KEY_NSW_STOPS_VERSION) ?: 0L,
        expectedVersion = NSW_STOPS_VERSION,
        stopCount = sandook.stopsCount(),
        minimumStopCount = MINIMUM_REQUIRED_NSW_STOPS,
        canaryFound = sandook.selectStopsByIds(listOf(CANARY_STOP_ID)).isNotEmpty(),
    )

    private fun Throwable.reasonText(): String = message ?: this::class.simpleName.orEmpty()

    internal companion object {
        const val MAX_AUTOMATIC_RETRIES = 3
        const val RETRY_BACKOFF_MS = 500L

        // Central Station, the parent stop. Present in the bundled stops data; if a data refresh
        // ever drops it, readiness reports Failed and this constant is the first thing to check.
        const val CANARY_STOP_ID = "200060"
    }
}
