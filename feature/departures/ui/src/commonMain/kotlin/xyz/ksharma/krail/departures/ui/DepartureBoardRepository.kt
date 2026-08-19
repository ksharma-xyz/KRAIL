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
    /**
     * Wall-clock seam for the refresh window. It has to be injectable: the window is
     * compared against `delay`-driven poll ticks, so a test that advances virtual time
     * must move this clock too or the window never opens. See `dateTimeModule`.
     */
    private val clock: Clock = Clock.System,
) {

    private val mutex = Mutex()
    private val cache = mutableMapOf<String, MutableStateFlow<DeparturesState>>()
    private val lastFetchTime = mutableMapOf<String, Long>()
    private val lastPreviousFetchTime = mutableMapOf<String, Long>()

    private var pollSessionCounter = 0

    @OptIn(ExperimentalTime::class)
    private fun nowMs(): Long = clock.now().toEpochMilliseconds()

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
            log(
                "[$LOG_TAG] t=$t0 session=#$sessionId pollStop START stopId=$stopId " +
                    "hasData=$hasData lastFetch=$elapsedLabel " +
                    "refreshIntervalMs=${config.refreshIntervalMs}",
            )

            // Land on the window boundary before the first attempt, so a session that
            // starts mid-window does not sit idle for a whole extra interval.
            if (lastFetch != 0L && elapsedMs < config.refreshIntervalMs) {
                val waitMs = config.refreshIntervalMs - elapsedMs
                log(
                    "[$LOG_TAG] t=$t0 session=#$sessionId within refresh window, " +
                        "waiting ${waitMs}ms before first fetch",
                )
                delay(waitMs)
            }

            var iteration = 0
            while (true) {
                ensureActive()
                if (iteration > 0) {
                    log(
                        "[$LOG_TAG] t=${nowMs()} session=#$sessionId auto-refresh #$iteration " +
                            "for stopId=$stopId",
                    )
                }
                fetchIfWindowOpen(
                    stopId = stopId,
                    showFullLoading = !hasData && iteration == 0,
                    sessionId = sessionId,
                )
                iteration++
                delay(config.refreshIntervalMs)
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

    /**
     * Single-flight gate on [DepartureBoardConfig.refreshIntervalMs].
     *
     * The same stop can be polled by more than one collector at a time — the map sheet and
     * the saved trips card both expand it, and each collects its own [pollStop]. They share
     * one cache entry, so they must also share one network call per window: whichever
     * session gets the lock first claims the window and fetches; the others skip and keep
     * receiving the result through the shared state flow.
     *
     * The claim is written **before** the call rather than after it, because two sessions
     * waking in the same instant would otherwise both see a stale timestamp and both fetch.
     * [fetchDepartures] overwrites it with the completion time on success, so the next
     * window is measured from when fresh data actually landed.
     */
    private suspend fun fetchIfWindowOpen(stopId: String, showFullLoading: Boolean, sessionId: Int) {
        val claimed = mutex.withLock {
            val lastFetch = lastFetchTime[stopId] ?: 0L
            val now = nowMs()
            val isWindowOpen = lastFetch == 0L || now - lastFetch >= config.refreshIntervalMs
            if (isWindowOpen) lastFetchTime[stopId] = now
            isWindowOpen
        }
        if (claimed) {
            fetchDepartures(stopId, showFullLoading = showFullLoading, sessionId = sessionId)
        } else {
            log(
                "[$LOG_TAG] t=${nowMs()} session=#$sessionId skipping fetch for stopId=$stopId — " +
                    "another session already claimed this refresh window",
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    @Suppress("LongMethod", "MagicNumber")
    private suspend fun fetchDepartures(stopId: String, showFullLoading: Boolean, sessionId: Int = -1) {
        currentCoroutineContext().ensureActive()
        val fetchStart = nowMs()
        log(
            "[$LOG_TAG] t=$fetchStart session=#$sessionId fetchDepartures START [API] " +
                "stopId=$stopId showFullLoading=$showFullLoading",
        )
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
                log(
                    "[$LOG_TAG] t=$fetchEnd session=#$sessionId previousDepartures STALE —" +
                        " clearing (age=${fetchEnd - prevFetchTime}ms > window=${prevWindowMs}ms)",
                )
            }
            log(
                "[$LOG_TAG] t=$fetchEnd session=#$sessionId fetchDepartures SUCCESS stopId=$stopId " +
                    "count=${departures.size} durationMs=${fetchEnd - fetchStart}",
            )
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
                message = "[$LOG_TAG] t=$fetchEnd session=#$sessionId fetchDepartures FAILURE " +
                    "stopId=$stopId durationMs=${fetchEnd - fetchStart}",
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
    @Suppress("LongMethod")
    @OptIn(ExperimentalTime::class)
    suspend fun loadPreviousDepartures(stopId: String) {
        val t0 = nowMs()
        val lastPrevFetch = mutex.withLock { lastPreviousFetchTime[stopId] ?: 0L }
        val elapsedMs = t0 - lastPrevFetch
        val elapsedLabel = if (lastPrevFetch == 0L) "never fetched" else "${elapsedMs}ms ago"
        val flow = stateFor(stopId)

        val isCacheHit = elapsedMs < config.refreshIntervalMs && flow.value.previousDepartures.isNotEmpty()
        if (isCacheHit) {
            log(
                "[$LOG_TAG] t=$t0 loadPreviousDepartures CACHE HIT stopId=$stopId " +
                    "lastFetch=$elapsedLabel count=${flow.value.previousDepartures.size}",
            )
            return
        }

        log(
            "[$LOG_TAG] t=$t0 loadPreviousDepartures START [API] stopId=$stopId " +
                "lastFetch=$elapsedLabel windowMinutes=${config.previousDeparturesWindowMinutes}",
        )
        flow.update { it.copy(isPreviousLoading = true) }
        val fromTime = clock.now() - config.previousDeparturesWindowMinutes.minutes
        departuresService.suspendSafeResult(ioDispatcher) {
            departures(
                stopId = stopId,
                date = fromTime.toApiDateString(),
                time = fromTime.toApiTimeString(),
            )
        }.onSuccess { response ->
            currentCoroutineContext().ensureActive()
            val now = clock.now()
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
            log(
                "[$LOG_TAG] t=$t1 loadPreviousDepartures SUCCESS stopId=$stopId " +
                    "total=${allFromResponse.size} filtered=${previous.size} durationMs=${t1 - t0}",
            )
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
                message = "[$LOG_TAG] t=$t1 loadPreviousDepartures FAILURE " +
                    "stopId=$stopId durationMs=${t1 - t0}",
                throwable = throwable,
            )
            flow.update { it.copy(isPreviousLoading = false) }
        }
    }

    companion object {
        private const val LOG_TAG = "DEPARTURES_REPO"
    }
}
