package xyz.ksharma.krail.departures.ui

import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toApiDateString
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toApiTimeString
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.coroutines.ext.launchWithExceptionHandler
import xyz.ksharma.krail.coroutines.ext.suspendSafeResult
import xyz.ksharma.krail.departures.network.api.service.DeparturesService
import xyz.ksharma.krail.departures.ui.business.toStopDepartures
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.departures.ui.state.model.DepartureTiming
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Singleton repository that owns the departure-polling loop.
 *
 * At most **one stop** is polled at a time. Callers switch the active stop via
 * [setActiveStop] and observe a stop's state via [observeStop].
 *
 * The in-memory cache survives for the app lifetime, so previously loaded data
 * remains available while offline or when a card is re-expanded.
 *
 * Thread safety: all mutable state ([cache], [lastFetchTime], [activeStopId], job
 * references) is protected by [mutex]. Callers (ViewModels) may invoke public
 * functions from any thread — the mutex serialises access.
 */
class DepartureBoardRepository(
    private val departuresService: DeparturesService,
    private val ioDispatcher: CoroutineDispatcher,
    private val config: DepartureBoardConfig = DepartureBoardConfig(),
) {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val mutex = Mutex()

    private val cache = mutableMapOf<String, MutableStateFlow<DeparturesState>>()
    private val lastFetchTime = mutableMapOf<String, Long>()
    private val lastPreviousFetchTime = mutableMapOf<String, Long>()

    private var activeJob: Job? = null
    private var activeStopId: String? = null

    /** Monotonically increasing counter — each `startPolling` call gets its own session ID. */
    private var pollSessionCounter = 0

    @OptIn(ExperimentalTime::class)
    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    /**
     * Returns a hot [Flow] of [DeparturesState] for [stopId].
     * Backed by a [MutableStateFlow] kept in the in-memory cache.
     */
    fun observeStop(stopId: String): Flow<DeparturesState> = flow { emitAll(stateFor(stopId)) }

    /**
     * Switches the active polling stop to [stopId].
     *
     * - [stopId] == current active stop → no-op.
     * - [stopId] is `null` → cancels the current poll, nothing new starts.
     * - Otherwise → cancels the current poll, starts a 30-second refresh loop for [stopId].
     */
    fun setActiveStop(stopId: String?) {
        scope.launchWithExceptionHandler<DepartureBoardRepository>(ioDispatcher) {
            val t = nowMs()
            val prev = mutex.withLock {
                if (activeStopId == stopId) {
                    log("[$LOG_TAG] t=$t setActiveStop NOOP — already active: $stopId")
                    return@launchWithExceptionHandler
                }
                val old = activeStopId
                activeJob?.cancel()
                activeJob = null
                activeStopId = stopId
                old
            }
            if (stopId == null) {
                log("[$LOG_TAG] t=$t setActiveStop → null (was $prev), polling stopped")
                return@launchWithExceptionHandler
            }
            log("[$LOG_TAG] t=$t setActiveStop → $stopId (was $prev), cancelled prior job, starting polling")
            startPolling(stopId)
        }
    }

    /**
     * Cancels the active poll only if [stopId] is currently the active stop.
     *
     * Safe to call from `ViewModel.onCleared()` — prevents a stale ViewModel from
     * accidentally stopping a loop started by another consumer.
     */
    fun stopIfActive(stopId: String) {
        scope.launchWithExceptionHandler<DepartureBoardRepository>(ioDispatcher) {
            val t = nowMs()
            mutex.withLock {
                if (activeStopId == stopId) {
                    activeJob?.cancel()
                    activeJob = null
                    activeStopId = null
                    log("[$LOG_TAG] t=$t stopIfActive MATCHED — stopped polling for $stopId")
                } else {
                    log("[$LOG_TAG] t=$t stopIfActive NOOP — active=$activeStopId, requested=$stopId")
                }
            }
        }
    }

    /** Triggers an immediate silent refresh for [stopId], independent of the poll loop. */
    fun refresh(stopId: String) {
        scope.launchWithExceptionHandler<DepartureBoardRepository>(ioDispatcher) {
            log("[$LOG_TAG] t=${nowMs()} refresh triggered for stopId=$stopId")
            fetchDepartures(stopId = stopId, showFullLoading = false)
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun startPolling(stopId: String) {
        val sessionId = ++pollSessionCounter
        val t0 = nowMs()
        val hasData = stateFor(stopId).value.departures.isNotEmpty()
        if (!hasData) stateFor(stopId).update { DeparturesState(isLoading = true) }
        log("[$LOG_TAG] t=$t0 startPolling session=#$sessionId stopId=$stopId hasData=$hasData")

        val nowMs = Clock.System.now().toEpochMilliseconds()
        val lastFetch = mutex.withLock { lastFetchTime[stopId] ?: 0L }
        val elapsedMs = nowMs - lastFetch
        log("[$LOG_TAG] t=$t0 session=#$sessionId lastFetchTime=$lastFetch elapsedMs=$elapsedMs refreshIntervalMs=${config.refreshIntervalMs}")

        mutex.withLock {
            if (activeStopId != stopId) {
                log("[$LOG_TAG] t=${this@DepartureBoardRepository.nowMs()} session=#$sessionId ABORTED — active stop changed to $activeStopId before job launched")
                return  // guard: stop was switched while we prepared
            }
            activeJob = scope.launchWithExceptionHandler<DepartureBoardRepository>(ioDispatcher) {
                // Check cancellation before any work — covers the gap between launch() and the
                // first suspension point.
                ensureActive()
                if (elapsedMs >= config.refreshIntervalMs) {
                    // Either never fetched, or the 30-second window has expired — fetch now.
                    // If we have cached data, do a silent refresh (dots in header, not a spinner).
                    log("[$LOG_TAG] t=${nowMs()} session=#$sessionId window expired ($elapsedMs >= ${config.refreshIntervalMs}ms), fetching now (showFullLoading=${!hasData})")
                    fetchDepartures(stopId = stopId, showFullLoading = !hasData, sessionId = sessionId)
                } else {
                    // Still within the 30-second window — show cached data, wait for the remainder.
                    val waitMs = config.refreshIntervalMs - elapsedMs
                    log("[$LOG_TAG] t=${nowMs()} session=#$sessionId within refresh window, waiting ${waitMs}ms before first fetch")
                    delay(waitMs)
                    fetchDepartures(stopId = stopId, showFullLoading = false, sessionId = sessionId)
                }
                var iteration = 1
                while (true) {
                    delay(config.refreshIntervalMs)
                    // delay() throws CancellationException when the job is cancelled. The explicit
                    // ensureActive() here is a belt-and-suspenders guard in case a future refactor
                    // moves delay() away from being the first statement.
                    ensureActive()
                    log("[$LOG_TAG] t=${nowMs()} session=#$sessionId auto-refresh #$iteration for stopId=$stopId")
                    fetchDepartures(stopId = stopId, showFullLoading = false, sessionId = sessionId)
                    iteration++
                }
            }
            log("[$LOG_TAG] t=${nowMs()} session=#$sessionId job launched: $activeJob")
        }
    }

    private suspend fun stateFor(stopId: String): MutableStateFlow<DeparturesState> =
        mutex.withLock {
            cache.getOrPut(stopId) { MutableStateFlow(DeparturesState(isLoading = false)) }
        }

    @OptIn(ExperimentalTime::class)
    private suspend fun fetchDepartures(stopId: String, showFullLoading: Boolean, sessionId: Int = -1) {
        // Throw immediately if this coroutine was cancelled before we touch any state.
        currentCoroutineContext().ensureActive()
        val fetchStart = nowMs()
        log("[$LOG_TAG] t=$fetchStart session=#$sessionId fetchDepartures START stopId=$stopId showFullLoading=$showFullLoading")
        val flow = stateFor(stopId)
        if (showFullLoading) {
            flow.update { DeparturesState(isLoading = true) }
        } else {
            flow.update { it.copy(silentLoading = true) }
        }
        // suspendSafeResult re-throws CancellationException so it is never silently swallowed.
        departuresService.suspendSafeResult(ioDispatcher) {
            departures(stopId = stopId, date = null, time = null)
        }.onSuccess { response ->
            val fetchEnd = nowMs()
            // Record fetch time only on success — a failed request should not block the
            // refresh window, so the next setActiveStop can retry immediately.
            mutex.withLock { lastFetchTime[stopId] = fetchEnd }
            // Check again after the network call — the job may have been cancelled while
            // the request was in flight.
            currentCoroutineContext().ensureActive()
            val departures = response.toStopDepartures()
            log("[$LOG_TAG] t=$fetchEnd session=#$sessionId fetchDepartures SUCCESS stopId=$stopId count=${departures.size} durationMs=${fetchEnd - fetchStart}")
            flow.update { current ->
                DeparturesState(
                    isLoading = false,
                    silentLoading = false,
                    isError = false,
                    departures = departures,
                    // Preserve previously fetched past departures across regular refreshes.
                    previousDepartures = current.previousDepartures,
                    isPreviousLoading = current.isPreviousLoading,
                    previousWindowMinutes = current.previousWindowMinutes,
                )
            }
        }.onFailure { throwable ->
            val fetchEnd = nowMs()
            logError(
                message = "[$LOG_TAG] t=$fetchEnd session=#$sessionId fetchDepartures FAILURE stopId=$stopId durationMs=${fetchEnd - fetchStart}",
                throwable = throwable,
            )
            flow.update {
                it.copy(
                    isLoading = false,
                    silentLoading = false,
                    isError = it.departures.isEmpty(),
                )
            }
        }
    }

    /**
     * Fetches departures from the past [DepartureBoardConfig.previousDeparturesWindowMinutes]
     * minutes for [stopId] and stores only the ones with a departure time before now.
     *
     * Called when the user taps "Show previous" in the departure board UI.
     * Results are stored in [DeparturesState.previousDepartures] and survive regular refreshes.
     */
    @OptIn(ExperimentalTime::class)
    fun loadPreviousDepartures(stopId: String) {
        scope.launchWithExceptionHandler<DepartureBoardRepository>(ioDispatcher) {
            val t0 = nowMs()
            val lastPrevFetch = mutex.withLock { lastPreviousFetchTime[stopId] ?: 0L }
            val elapsedMs = t0 - lastPrevFetch
            val flow = stateFor(stopId)

            // If we already have data and it's still within the refresh window, skip the fetch.
            val isCacheHit = elapsedMs < config.refreshIntervalMs && flow.value.previousDepartures.isNotEmpty()
            if (isCacheHit) {
                log("[$LOG_TAG] t=$t0 loadPreviousDepartures CACHE HIT stopId=$stopId elapsedMs=$elapsedMs count=${flow.value.previousDepartures.size}")
                return@launchWithExceptionHandler
            }

            log("[$LOG_TAG] t=$t0 loadPreviousDepartures START stopId=$stopId elapsedMs=$elapsedMs windowMinutes=${config.previousDeparturesWindowMinutes}")
            flow.update { it.copy(isPreviousLoading = true) }
            val fromTime = Clock.System.now() - config.previousDeparturesWindowMinutes.minutes
            // suspendSafeResult re-throws CancellationException so it is never silently swallowed.
            departuresService.suspendSafeResult(ioDispatcher) {
                departures(
                    stopId = stopId,
                    date = fromTime.toApiDateString(),
                    time = fromTime.toApiTimeString(),
                )
            }.onSuccess { response ->
                currentCoroutineContext().ensureActive()
                val now = Clock.System.now()
                val allFromResponse = response.toStopDepartures()
                val previous = allFromResponse
                    .filter { departure ->
                        runCatching {
                            Instant.parse(departure.departureUtcDateTime) < now
                        }.getOrDefault(false)
                    }
                    .map { it.copy(timing = DepartureTiming.Previous) }
                    .toImmutableList()
                val t1 = nowMs()
                log("[$LOG_TAG] t=$t1 loadPreviousDepartures SUCCESS stopId=$stopId total=${allFromResponse.size} filtered=${previous.size} durationMs=${t1 - t0}")
                mutex.withLock { lastPreviousFetchTime[stopId] = t1 }
                flow.update {
                    it.copy(
                        previousDepartures = previous,
                        isPreviousLoading = false,
                        previousWindowMinutes = config.previousDeparturesWindowMinutes,
                    )
                }
            }.onFailure { throwable ->
                val t1 = nowMs()
                logError(
                    message = "[$LOG_TAG] t=$t1 loadPreviousDepartures FAILURE stopId=$stopId durationMs=${t1 - t0}",
                    throwable = throwable,
                )
                flow.update { it.copy(isPreviousLoading = false) }
            }
        }
    }

    companion object {
        private const val LOG_TAG = "DEPARTURES_REPO"
    }
}
