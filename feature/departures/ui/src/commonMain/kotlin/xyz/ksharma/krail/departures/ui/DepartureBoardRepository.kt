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
            mutex.withLock {
                if (activeStopId == stopId) return@launchWithExceptionHandler
                activeJob?.cancel()
                activeJob = null
                activeStopId = stopId
            }
            if (stopId == null) {
                log("[$LOG_TAG] setActiveStop → null, polling stopped")
                return@launchWithExceptionHandler
            }
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
            mutex.withLock {
                if (activeStopId == stopId) {
                    activeJob?.cancel()
                    activeJob = null
                    activeStopId = null
                    log("[$LOG_TAG] stopIfActive → stopped polling for $stopId")
                }
            }
        }
    }

    /** Triggers an immediate silent refresh for [stopId], independent of the poll loop. */
    fun refresh(stopId: String) {
        scope.launchWithExceptionHandler<DepartureBoardRepository>(ioDispatcher) {
            fetchDepartures(stopId = stopId, showFullLoading = false)
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun startPolling(stopId: String) {
        val hasData = stateFor(stopId).value.departures.isNotEmpty()
        if (!hasData) stateFor(stopId).update { DeparturesState(isLoading = true) }
        log("[$LOG_TAG] setActiveStop → stopId=$stopId, hasData=$hasData")

        val nowMs = Clock.System.now().toEpochMilliseconds()
        val elapsedMs = nowMs - mutex.withLock { lastFetchTime[stopId] ?: 0L }

        mutex.withLock {
            if (activeStopId != stopId) return  // guard: stop was switched while we prepared
            activeJob = scope.launchWithExceptionHandler<DepartureBoardRepository>(ioDispatcher) {
                // Check cancellation before any work — covers the gap between launch() and the
                // first suspension point.
                ensureActive()
                if (elapsedMs >= config.refreshIntervalMs) {
                    // Either never fetched, or the 30-second window has expired — fetch now.
                    // If we have cached data, do a silent refresh (dots in header, not a spinner).
                    fetchDepartures(stopId = stopId, showFullLoading = !hasData)
                } else {
                    // Still within the 30-second window — show cached data, wait for the remainder.
                    log("[$LOG_TAG] within refresh window for $stopId, waiting ${config.refreshIntervalMs - elapsedMs}ms")
                    delay(config.refreshIntervalMs - elapsedMs)
                    fetchDepartures(stopId = stopId, showFullLoading = false)
                }
                while (true) {
                    delay(config.refreshIntervalMs)
                    // delay() throws CancellationException when the job is cancelled. The explicit
                    // ensureActive() here is a belt-and-suspenders guard in case a future refactor
                    // moves delay() away from being the first statement.
                    ensureActive()
                    log("[$LOG_TAG] auto-refresh for stopId=$stopId")
                    fetchDepartures(stopId = stopId, showFullLoading = false)
                }
            }
        }
    }

    private suspend fun stateFor(stopId: String): MutableStateFlow<DeparturesState> =
        mutex.withLock {
            cache.getOrPut(stopId) { MutableStateFlow(DeparturesState(isLoading = false)) }
        }

    @OptIn(ExperimentalTime::class)
    private suspend fun fetchDepartures(stopId: String, showFullLoading: Boolean) {
        // Throw immediately if this coroutine was cancelled before we touch any state.
        currentCoroutineContext().ensureActive()
        val flow = stateFor(stopId)
        if (showFullLoading) {
            flow.update { DeparturesState(isLoading = true) }
        } else {
            flow.update { it.copy(silentLoading = true) }
        }
        mutex.withLock { lastFetchTime[stopId] = Clock.System.now().toEpochMilliseconds() }
        // suspendSafeResult re-throws CancellationException so it is never silently swallowed.
        departuresService.suspendSafeResult(ioDispatcher) {
            departures(stopId = stopId, date = null, time = null)
        }.onSuccess { response ->
            // Check again after the network call — the job may have been cancelled while
            // the request was in flight.
            currentCoroutineContext().ensureActive()
            val departures = response.toStopDepartures()
            log("[$LOG_TAG] success — ${departures.size} departures for stopId=$stopId")
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
            logError(
                message = "[$LOG_TAG] failed to fetch departures for stopId=$stopId",
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
            val nowMs = Clock.System.now().toEpochMilliseconds()
            val elapsedMs = nowMs - mutex.withLock { lastPreviousFetchTime[stopId] ?: 0L }
            val flow = stateFor(stopId)

            // If we already have data and it's still within the refresh window, skip the fetch.
            val isCacheHit = elapsedMs < config.refreshIntervalMs && flow.value.previousDepartures.isNotEmpty()
            if (isCacheHit) {
                log("[$LOG_TAG] previous departures cache hit for stopId=$stopId (${elapsedMs}ms ago)")
                return@launchWithExceptionHandler
            }

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
                val previous = response.toStopDepartures()
                    .filter { departure ->
                        runCatching {
                            Instant.parse(departure.departureUtcDateTime) < now
                        }.getOrDefault(false)
                    }
                    .map { it.copy(timing = DepartureTiming.Previous) }
                    .toImmutableList()
                log("[$LOG_TAG] previous departures — ${previous.size} found for stopId=$stopId")
                mutex.withLock { lastPreviousFetchTime[stopId] = Clock.System.now().toEpochMilliseconds() }
                flow.update {
                    it.copy(
                        previousDepartures = previous,
                        isPreviousLoading = false,
                        previousWindowMinutes = config.previousDeparturesWindowMinutes,
                    )
                }
            }.onFailure { throwable ->
                logError(
                    message = "[$LOG_TAG] failed to fetch previous departures for stopId=$stopId",
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
