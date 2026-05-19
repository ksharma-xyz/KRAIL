package xyz.ksharma.krail.departures.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingCommand
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.DepartureBoardSource
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toDepartureRelativeString
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.coroutines.ext.launchWithExceptionHandler
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.departures.ui.state.DeparturesUiEvent
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("TooManyFunctions", "")
class DeparturesViewModel(
    private val repository: DepartureBoardRepository,
    private val analytics: Analytics,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    // Tracks which stop this ViewModel instance is responsible for polling.
    private val activeStopId = MutableStateFlow<String?>(null)

    // Surface that triggered the most recent LoadDepartures — used for analytics on retry/error.
    private var activeSource: DepartureBoardSource = DepartureBoardSource.MAP_SHEET

    private val _uiState: MutableStateFlow<DeparturesState> = MutableStateFlow(DeparturesState())
    val uiState: StateFlow<DeparturesState> = _uiState.asStateFlow()

    /**
     * Ticks every [REFRESH_TIME_TEXT_DURATION] ms while subscribed, recomputing the
     * "in X mins" text for each departure — same pattern as TimeTableViewModel.
     */
    val isActive: StateFlow<Boolean> = MutableStateFlow(false).onStart {
        while (true) {
            // Throw if the collecting coroutine was cancelled — avoids launching unnecessary
            // work between the end of delay() (which already throws on cancel) and the guard
            // check, and makes the intent explicit.
            currentCoroutineContext().ensureActive()
            if (_uiState.value.departures.isNotEmpty()) {
                updateRelativeTimeText()
            }
            delay(REFRESH_TIME_TEXT_DURATION)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true,
    )

    private var relativeTimeTickCount = 0

    init {
        log("[$LOG_TAG] t=${nowMs()} ViewModel CREATED")
        // Collect repository flow into _uiState so updateRelativeTimeText() can mutate it.
        //
        // Gated on uiState having subscribers via SharingStarted.WhileSubscribed(5_000) —
        // the same lifecycle the sibling DepartureBoardViewModel uses. Without this gate the
        // repository's infinite pollStop loop runs for the ViewModel's entire lifetime
        // (network hit every refresh interval) even when the sheet is closed/backgrounded,
        // and never lets a TestCoroutineScheduler reach idle. The 5_000 ms grace keeps
        // polling alive across config changes / brief detaches, matching the sibling.
        viewModelScope.launchWithExceptionHandler<DeparturesViewModel>(ioDispatcher) {
            SharingStarted.WhileSubscribed(POLL_SUBSCRIPTION_GRACE_MS)
                .command(_uiState.subscriptionCount)
                .distinctUntilChanged()
                .flatMapLatest { command ->
                    if (command == SharingCommand.START) {
                        activeStopId.flatMapLatest { stopId ->
                            log("[$LOG_TAG] t=${nowMs()} activeStopId changed → $stopId, switching repo flow")
                            if (stopId != null) {
                                repository.pollStop(stopId)
                            } else {
                                flowOf(DeparturesState(isLoading = false))
                            }
                        }
                    } else {
                        emptyFlow()
                    }
                }
                .collect { state ->
                    log(
                        "[$LOG_TAG] t=${nowMs()} repo state received: isLoading=${state.isLoading} " +
                            "silentLoading=${state.silentLoading} isError=${state.isError} " +
                            "departures=${state.departures.size} " +
                            "prevDepartures=${state.previousDepartures.size}",
                    )
                    // Fire error event on the first transition into error state per polling session.
                    if (state.isError && !_uiState.value.isError) {
                        activeStopId.value?.let { stopId ->
                            analytics.trackDepartureBoardStatus(
                                stopId = stopId,
                                action = AnalyticsEvent.DepartureBoardStatusEvent.Action.ERROR,
                                source = activeSource,
                            )
                        }
                    }
                    _uiState.value = state
                }
        }
    }

    // Compute relative time inside the atomic update lambda so it always operates on the
    // current state — never on a stale snapshot. The previous pattern (snapshot → withContext
    // → update) had a window where the repository could push a new departure between the
    // snapshot and the update call, causing that departure to be silently dropped.
    //
    // withContext(ioDispatcher) is also removed — we are already on ioDispatcher (launched
    // via launchWithExceptionHandler(ioDispatcher)), so it was a no-op context switch.
    @OptIn(ExperimentalTime::class)
    private fun updateRelativeTimeText() =
        viewModelScope.launchWithExceptionHandler<DeparturesViewModel>(ioDispatcher) {
            val tick = ++relativeTimeTickCount
            val t = nowMs()
            _uiState.update { current ->
                val updated = current.copy(
                    departures = current.departures.map { departure ->
                        departure.copy(
                            relativeTimeText = runCatching {
                                departure.departureUtcDateTime.toDepartureRelativeString()
                            }.getOrDefault(""),
                        )
                    }.toImmutableList(),
                    previousDepartures = current.previousDepartures.map { departure ->
                        departure.copy(
                            relativeTimeText = runCatching {
                                departure.departureUtcDateTime.toDepartureRelativeString()
                            }.getOrDefault(""),
                        )
                    }.toImmutableList(),
                )
                log(
                    "[$LOG_TAG] t=$t relativeTime tick=#$tick updated departures=${updated.departures.size} " +
                        "prev=${updated.previousDepartures.size}",
                )
                updated
            }
        }

    fun onEvent(event: DeparturesUiEvent) {
        when (event) {
            is DeparturesUiEvent.LoadDepartures -> onLoadDepartures(event)
            DeparturesUiEvent.Refresh -> onRefresh()
            is DeparturesUiEvent.LoadPreviousDepartures -> onLoadPreviousDepartures(event.stopId)
            DeparturesUiEvent.StopPolling -> onStopPolling()
            is DeparturesUiEvent.LineFilterChanged -> onLineFilterChanged(event)
            is DeparturesUiEvent.TogglePreviousDepartures -> onTogglePreviousDepartures(event)
            is DeparturesUiEvent.DepartureBoardToggle -> onDepartureBoardToggle(event)
        }
    }

    private fun onLoadDepartures(event: DeparturesUiEvent.LoadDepartures) {
        val t = nowMs()
        val current = activeStopId.value
        if (event.stopId != current) {
            log(
                "[$LOG_TAG] t=$t onEvent LoadDepartures → stopId=${event.stopId} " +
                    "source=${event.source} (was $current)",
            )
            activeSource = event.source
            analytics.trackDepartureBoardScreenView(
                stopId = event.stopId,
                stopName = event.stopName,
                source = event.source,
            )
            activeStopId.value = event.stopId
        } else {
            log(
                "[$LOG_TAG] t=$t onEvent LoadDepartures SAME STOP — already polling " +
                    event.stopId,
            )
        }
    }

    private fun onRefresh() {
        val t = nowMs()
        val stopId = activeStopId.value
        if (stopId != null) {
            log("[$LOG_TAG] t=$t onEvent Refresh → stopId=$stopId")
            analytics.trackDepartureBoardStatus(
                stopId = stopId,
                action = AnalyticsEvent.DepartureBoardStatusEvent.Action.RETRY,
                source = activeSource,
            )
            viewModelScope.launch { repository.refresh(stopId) }
        } else {
            log("[$LOG_TAG] t=$t onEvent Refresh NOOP — no active stop")
        }
    }

    private fun onLoadPreviousDepartures(stopId: String) {
        log("[$LOG_TAG] t=${nowMs()} onEvent LoadPreviousDepartures → stopId=$stopId")
        viewModelScope.launch { repository.loadPreviousDepartures(stopId) }
    }

    private fun onStopPolling() {
        val t = nowMs()
        val stopId = activeStopId.value
        if (stopId != null) {
            log("[$LOG_TAG] t=$t onEvent StopPolling → stopId=$stopId")
            activeStopId.value = null
        } else {
            log("[$LOG_TAG] t=$t onEvent StopPolling NOOP — no active stop")
        }
    }

    private fun onLineFilterChanged(event: DeparturesUiEvent.LineFilterChanged) {
        log(
            "[$LOG_TAG] t=${nowMs()} onEvent LineFilterChanged → stopId=${event.stopId} " +
                "line=${event.lineNumber} selected=${event.selected}",
        )
        analytics.trackDepartureBoardLineFilterClick(
            stopId = event.stopId,
            selected = event.selected,
            lineNumber = event.lineNumber,
            transportMode = event.transportMode,
            source = activeSource,
        )
    }

    private fun onTogglePreviousDepartures(event: DeparturesUiEvent.TogglePreviousDepartures) {
        log(
            "[$LOG_TAG] t=${nowMs()} onEvent TogglePreviousDepartures → stopId=${event.stopId} " +
                "show=${event.show}",
        )
        analytics.trackDepartureBoardShowPrevious(
            stopId = event.stopId,
            show = event.show,
            source = activeSource,
        )
    }

    private fun onDepartureBoardToggle(event: DeparturesUiEvent.DepartureBoardToggle) {
        log(
            "[$LOG_TAG] t=${nowMs()} onEvent DepartureBoardToggle → " +
                "stopId=${event.stopId} expand=${event.expand} source=${event.source}",
        )
        analytics.trackDepartureBoardToggle(
            stopId = event.stopId,
            stopName = event.stopName,
            expand = event.expand,
            source = event.source,
        )
    }

    override fun onCleared() {
        log("[$LOG_TAG] t=${nowMs()} ViewModel CLEARED activeStopId=${activeStopId.value}")
        // viewModelScope cancellation propagates to the flatMapLatest → pollStop chain,
        // which clears loading state via its finally block. No manual cleanup needed.
        super.onCleared()
    }

    @OptIn(ExperimentalTime::class)
    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    companion object {
        const val LOG_TAG = "DEPARTURES"
        private const val REFRESH_TIME_TEXT_DURATION = 10_000L

        // Keep the repository poll alive for this long after the last uiState subscriber
        // leaves, so config changes / brief detaches don't restart polling. Matches the
        // SharingStarted.WhileSubscribed(5_000) used by the sibling DepartureBoardViewModel.
        private const val POLL_SUBSCRIPTION_GRACE_MS = 5_000L
    }
}
