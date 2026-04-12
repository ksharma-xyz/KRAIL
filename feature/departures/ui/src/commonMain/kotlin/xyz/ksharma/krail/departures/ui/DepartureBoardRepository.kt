package xyz.ksharma.krail.departures.ui

import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.datetime.DateTimeHelper.calculateTimeDifferenceFromNow
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toApiDateString
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toApiTimeString
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toGenericFormattedTimeString
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
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
 */
class DepartureBoardRepository(
    private val departuresService: DeparturesService,
    ioDispatcher: CoroutineDispatcher,
    private val config: DepartureBoardConfig = DepartureBoardConfig(),
) {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    // Accessed only from the Main thread (ViewModel calls), so a plain map is safe.
    private val cache = mutableMapOf<String, MutableStateFlow<DeparturesState>>()

    // Tracks the epoch-ms of the last successful fetch per stop, to avoid redundant
    // API calls when a stop is collapsed then re-expanded within the 30-second window.
    private val lastFetchTime = mutableMapOf<String, Long>()

    // Same guard for previous-departures fetches — avoids a new API call if the user
    // collapses and re-expands "Show previous" within the same refresh window.
    private val lastPreviousFetchTime = mutableMapOf<String, Long>()

    private var activeJob: Job? = null
    private var relativeTimeJob: Job? = null
    private var activeStopId: String? = null

    /**
     * Returns a hot [Flow] of [DeparturesState] for [stopId].
     * Backed by a [MutableStateFlow] kept in the in-memory cache.
     */
    fun observeStop(stopId: String): Flow<DeparturesState> = stateFor(stopId)

    /**
     * Switches the active polling stop to [stopId].
     *
     * - [stopId] == current active stop → no-op.
     * - [stopId] is `null` → cancels the current poll, nothing new starts.
     * - Otherwise → cancels the current poll, starts a 30-second refresh loop for [stopId].
     */
    @OptIn(ExperimentalTime::class)
    fun setActiveStop(stopId: String?) {
        if (activeStopId == stopId) return
        activeJob?.cancel()
        activeJob = null
        relativeTimeJob?.cancel()
        relativeTimeJob = null
        activeStopId = stopId
        if (stopId == null) {
            log("[$LOG_TAG] setActiveStop → null, polling stopped")
            return
        }

        val hasData = stateFor(stopId).value.departures.isNotEmpty()
        // Only show the full loading spinner when there is nothing cached to display.
        if (!hasData) stateFor(stopId).update { DeparturesState(isLoading = true) }
        log("[$LOG_TAG] setActiveStop → stopId=$stopId, hasData=$hasData")

        activeJob = scope.launch {
            val nowMs = Clock.System.now().toEpochMilliseconds()
            val elapsedMs = nowMs - (lastFetchTime[stopId] ?: 0L)

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
                log("[$LOG_TAG] auto-refresh for stopId=$stopId")
                fetchDepartures(stopId = stopId, showFullLoading = false)
            }
        }

        relativeTimeJob = scope.launch {
            while (true) {
                delay(config.relativeTimeRefreshMs)
                updateRelativeTime(stopId)
            }
        }
    }

    /**
     * Cancels the active poll only if [stopId] is currently the active stop.
     *
     * Safe to call from `ViewModel.onCleared()` — prevents a stale ViewModel from
     * accidentally stopping a loop started by another consumer.
     */
    fun stopIfActive(stopId: String) {
        if (activeStopId == stopId) setActiveStop(null)
    }

    @OptIn(ExperimentalTime::class)
    private fun updateRelativeTime(stopId: String) {
        val flow = stateFor(stopId)
        if (flow.value.departures.isEmpty() && flow.value.previousDepartures.isEmpty()) return
        flow.update { state ->
            state.copy(
                departures = state.departures.map { d ->
                    d.copy(
                        relativeTimeText = runCatching {
                            calculateTimeDifferenceFromNow(d.departureUtcDateTime)
                                .toGenericFormattedTimeString()
                        }.getOrDefault(""),
                    )
                }.toImmutableList(),
                previousDepartures = state.previousDepartures.map { d ->
                    d.copy(
                        relativeTimeText = runCatching {
                            calculateTimeDifferenceFromNow(d.departureUtcDateTime)
                                .toGenericFormattedTimeString()
                        }.getOrDefault(""),
                    )
                }.toImmutableList(),
            )
        }
    }

    /** Triggers an immediate silent refresh for [stopId], independent of the poll loop. */
    fun refresh(stopId: String) {
        scope.launch { fetchDepartures(stopId = stopId, showFullLoading = false) }
    }

    private fun stateFor(stopId: String): MutableStateFlow<DeparturesState> =
        cache.getOrPut(stopId) { MutableStateFlow(DeparturesState(isLoading = false)) }

    @OptIn(ExperimentalTime::class)
    private suspend fun fetchDepartures(stopId: String, showFullLoading: Boolean) {
        val flow = stateFor(stopId)
        if (showFullLoading) {
            flow.update { DeparturesState(isLoading = true) }
        } else {
            flow.update { it.copy(silentLoading = true) }
        }
        lastFetchTime[stopId] = Clock.System.now().toEpochMilliseconds()
        runCatching {
            departuresService.departures(stopId = stopId, date = null, time = null)
        }.onSuccess { response ->
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
     * Fetches departures from the past [PREVIOUS_WINDOW_MINUTES] minutes for [stopId]
     * and stores only the ones with a departure time before now.
     *
     * Called when the user taps "Show previous" in the departure board UI.
     * Results are stored in [DeparturesState.previousDepartures] and survive regular refreshes.
     */
    @OptIn(ExperimentalTime::class)
    fun loadPreviousDepartures(stopId: String) {
        scope.launch {
            val nowMs = Clock.System.now().toEpochMilliseconds()
            val elapsedMs = nowMs - (lastPreviousFetchTime[stopId] ?: 0L)
            val flow = stateFor(stopId)

            // If we already have data and it's still within the refresh window, skip the fetch.
            if (elapsedMs < config.refreshIntervalMs && flow.value.previousDepartures.isNotEmpty()) {
                log("[$LOG_TAG] previous departures cache hit for stopId=$stopId (${elapsedMs}ms ago)")
                return@launch
            }

            flow.update { it.copy(isPreviousLoading = true) }
            val fromTime = Clock.System.now() - config.previousDeparturesWindowMinutes.minutes
            runCatching {
                departuresService.departures(
                    stopId = stopId,
                    date = fromTime.toApiDateString(),
                    time = fromTime.toApiTimeString(),
                )
            }.onSuccess { response ->
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
                lastPreviousFetchTime[stopId] = Clock.System.now().toEpochMilliseconds()
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
