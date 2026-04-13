package xyz.ksharma.krail.departures.ui

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toApiDateString
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toApiTimeString
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
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
 * Repository that owns departure data and the polling lifecycle.
 *
 * **Polling is collection-driven**: callers obtain a polling flow via [pollStop] and collect it;
 * the network loop runs exactly as long as the flow is collected. When the collector cancels
 * (e.g. [SharingStarted.WhileSubscribed] stops the chain because the UI went to background, or
 * [flatMapLatest] switches to a different stop), the loop stops automatically and loading flags
 * are cleared in a `finally` block — no separate `stopIfActive` call required.
 *
 * The in-memory [cache] survives the app lifetime, so previously loaded data is always
 * available while offline or when a card is re-expanded.
 */
class DepartureBoardRepository(
    private val departuresService: DeparturesService,
    private val ioDispatcher: CoroutineDispatcher,
    private val config: DepartureBoardConfig = DepartureBoardConfig(),
) {

    private val mutex = Mutex()
    private val cache = mutableMapOf<String, MutableStateFlow<DeparturesState>>()
    private val lastFetchTime = mutableMapOf<String, Long>()
    private val lastPreviousFetchTime = mutableMapOf<String, Long>()

    /** Monotonically increasing counter for log correlation — not shared across threads safely,
     *  but only used for debugging so a rare tear is acceptable. */
    private var pollSessionCounter = 0

    @OptIn(ExperimentalTime::class)
    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    /**
     * Returns a cold [Flow] of [DeparturesState] for [stopId] backed by the in-memory cache.
     * Does **not** trigger any network calls. Use [pollStop] when active polling is needed.
     */
    fun observeStop(stopId: String): Flow<DeparturesState> = flow { emitAll(stateFor(stopId)) }

    /**
     * Returns a cold [Flow] that polls [stopId] for departures **while collected**.
     *
     * - Collection starts → polling loop starts, loading state set if no cached data.
     * - Collection cancelled → loop stops, loading flags cleared via `finally`.
     * - Refresh-window guard: if data was fetched recently (within [DepartureBoardConfig.refreshIntervalMs]),
     *   the flow waits for the remainder of the window before the first fetch.
     *
     * Designed to be consumed via `flatMapLatest` + `SharingStarted.WhileSubscribed` so that:
     * - Switching the expanded card cancels the old polling and starts the new one automatically.
     * - Backgrounding the app stops all polling without any lifecycle-hook wiring.
     */
    fun pollStop(stopId: String): Flow<DeparturesState> = channelFlow {
        val sessionId = ++pollSessionCounter
        val stateFlow = stateFor(stopId)

        // Forward all cache updates to this channel while the flow is collected.
        launch { stateFlow.collect { send(it) } }

        try {
            val hasData = stateFlow.value.departures.isNotEmpty()
            if (!hasData) stateFlow.update { DeparturesState(isLoading = true) }

            val lastFetch = mutex.withLock { lastFetchTime[stopId] ?: 0L }
            val t0 = nowMs()
            val elapsedMs = t0 - lastFetch
            val elapsedLabel = if (lastFetch == 0L) "never fetched" else "${elapsedMs}ms ago"
            log("[$LOG_TAG] t=$t0 session=#$sessionId pollStop START stopId=$stopId hasData=$hasData lastFetch=$elapsedLabel refreshIntervalMs=${config.refreshIntervalMs}")

            if (lastFetch == 0L || elapsedMs >= config.refreshIntervalMs) {
                fetchDepartures(stopId, showFullLoading = !hasData, sessionId = sessionId)
            } else {
                val waitMs = config.refreshIntervalMs - elapsedMs
                log("[$LOG_TAG] t=$t0 session=#$sessionId within refresh window, waiting ${waitMs}ms before first fetch")
                delay(waitMs)
                fetchDepartures(stopId, showFullLoading = false, sessionId = sessionId)
            }

            var iteration = 1
            while (true) {
                delay(config.refreshIntervalMs)
                ensureActive()
                log("[$LOG_TAG] t=${nowMs()} session=#$sessionId auto-refresh #$iteration for stopId=$stopId")
                fetchDepartures(stopId, showFullLoading = false, sessionId = sessionId)
                iteration++
            }
        } finally {
            // Runs on cancellation (WhileSubscribed, flatMapLatest switch) or completion.
            // Clears stale loading flags so cached state stays consistent for the next collect.
            stateFlow.update { it.copy(isLoading = false, silentLoading = false) }
            log("[$LOG_TAG] t=${nowMs()} session=#$sessionId pollStop ENDED for stopId=$stopId")
        }
    }

    /** Triggers an immediate silent refresh for [stopId], independent of any polling flow. */
    suspend fun refresh(stopId: String) {
        log("[$LOG_TAG] t=${nowMs()} refresh triggered for stopId=$stopId")
        fetchDepartures(stopId = stopId, showFullLoading = false)
    }

    private suspend fun stateFor(stopId: String): MutableStateFlow<DeparturesState> =
        mutex.withLock {
            cache.getOrPut(stopId) { MutableStateFlow(DeparturesState(isLoading = false)) }
        }

    @OptIn(ExperimentalTime::class)
    private suspend fun fetchDepartures(stopId: String, showFullLoading: Boolean, sessionId: Int = -1) {
        currentCoroutineContext().ensureActive()
        val fetchStart = nowMs()
        log("[$LOG_TAG] t=$fetchStart session=#$sessionId fetchDepartures START [API] stopId=$stopId showFullLoading=$showFullLoading")
        val flow = stateFor(stopId)
        if (showFullLoading) {
            flow.update { DeparturesState(isLoading = true) }
        } else {
            flow.update { it.copy(silentLoading = true) }
        }
        departuresService.suspendSafeResult(ioDispatcher) {
            departures(stopId = stopId, date = null, time = null)
        }.onSuccess { response ->
            val fetchEnd = nowMs()
            mutex.withLock { lastFetchTime[stopId] = fetchEnd }
            currentCoroutineContext().ensureActive()
            val departures = response.toStopDepartures()
            val prevFetchTime = mutex.withLock { lastPreviousFetchTime[stopId] ?: 0L }
            val prevWindowMs = config.previousDeparturesWindowMinutes * 60_000L
            val isPrevStale = prevFetchTime > 0L && (fetchEnd - prevFetchTime) > prevWindowMs
            if (isPrevStale) {
                log("[$LOG_TAG] t=$fetchEnd session=#$sessionId previousDepartures STALE — clearing (age=${fetchEnd - prevFetchTime}ms > window=${prevWindowMs}ms)")
            }
            log("[$LOG_TAG] t=$fetchEnd session=#$sessionId fetchDepartures SUCCESS stopId=$stopId count=${departures.size} durationMs=${fetchEnd - fetchStart}")
            flow.update { current ->
                DeparturesState(
                    isLoading = false,
                    silentLoading = false,
                    isError = false,
                    departures = departures,
                    previousDepartures = if (isPrevStale) persistentListOf() else current.previousDepartures,
                    isPreviousLoading = if (isPrevStale) false else current.isPreviousLoading,
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
    suspend fun loadPreviousDepartures(stopId: String) {
        val t0 = nowMs()
        val lastPrevFetch = mutex.withLock { lastPreviousFetchTime[stopId] ?: 0L }
        val elapsedMs = t0 - lastPrevFetch
        val elapsedLabel = if (lastPrevFetch == 0L) "never fetched" else "${elapsedMs}ms ago"
        val flow = stateFor(stopId)

        val isCacheHit = elapsedMs < config.refreshIntervalMs && flow.value.previousDepartures.isNotEmpty()
        if (isCacheHit) {
            log("[$LOG_TAG] t=$t0 loadPreviousDepartures CACHE HIT stopId=$stopId lastFetch=$elapsedLabel count=${flow.value.previousDepartures.size}")
            return
        }

        log("[$LOG_TAG] t=$t0 loadPreviousDepartures START [API] stopId=$stopId lastFetch=$elapsedLabel windowMinutes=${config.previousDeparturesWindowMinutes}")
        flow.update { it.copy(isPreviousLoading = true) }
        val fromTime = Clock.System.now() - config.previousDeparturesWindowMinutes.minutes
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

    companion object {
        private const val LOG_TAG = "DEPARTURES_REPO"
    }
}
