package xyz.ksharma.krail.departures.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.datetime.DateTimeHelper.calculateTimeDifferenceFromNow
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toGenericFormattedTimeString
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.coroutines.ext.launchWithExceptionHandler
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.departures.ui.state.DeparturesUiEvent
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class DeparturesViewModel(
    private val repository: DepartureBoardRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    // Tracks which stop this ViewModel instance is responsible for polling.
    private val activeStopId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalTime::class)
    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

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
        viewModelScope.launchWithExceptionHandler<DeparturesViewModel>(ioDispatcher) {
            activeStopId
                .flatMapLatest { stopId ->
                    log("[$LOG_TAG] t=${nowMs()} activeStopId changed → $stopId, switching repo flow")
                    if (stopId != null) {
                        repository.pollStop(stopId)
                    } else {
                        flowOf(DeparturesState(isLoading = false))
                    }
                }
                .collect { state ->
                    log("[$LOG_TAG] t=${nowMs()} repo state received: isLoading=${state.isLoading} silentLoading=${state.silentLoading} isError=${state.isError} departures=${state.departures.size} prevDepartures=${state.previousDepartures.size}")
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
                                calculateTimeDifferenceFromNow(departure.departureUtcDateTime)
                                    .toGenericFormattedTimeString()
                            }.getOrDefault(""),
                        )
                    }.toImmutableList(),
                    previousDepartures = current.previousDepartures.map { departure ->
                        departure.copy(
                            relativeTimeText = runCatching {
                                calculateTimeDifferenceFromNow(departure.departureUtcDateTime)
                                    .toGenericFormattedTimeString()
                            }.getOrDefault(""),
                        )
                    }.toImmutableList(),
                )
                log("[$LOG_TAG] t=$t relativeTime tick=#$tick updated departures=${updated.departures.size} prev=${updated.previousDepartures.size}")
                updated
            }
        }

    fun onEvent(event: DeparturesUiEvent) {
        val t = nowMs()
        when (event) {
            is DeparturesUiEvent.LoadDepartures -> {
                val current = activeStopId.value
                if (event.stopId != current) {
                    log("[$LOG_TAG] t=$t onEvent LoadDepartures → stopId=${event.stopId} (was $current)")
                    // Switching activeStopId causes flatMapLatest to cancel the old pollStop
                    // flow and start a new one — no manual stopIfActive/setActiveStop needed.
                    activeStopId.value = event.stopId
                } else {
                    log("[$LOG_TAG] t=$t onEvent LoadDepartures SAME STOP — already polling ${event.stopId}")
                }
            }

            DeparturesUiEvent.Refresh -> {
                val stopId = activeStopId.value
                if (stopId != null) {
                    log("[$LOG_TAG] t=$t onEvent Refresh → stopId=$stopId")
                    viewModelScope.launch { repository.refresh(stopId) }
                } else {
                    log("[$LOG_TAG] t=$t onEvent Refresh NOOP — no active stop")
                }
            }

            is DeparturesUiEvent.LoadPreviousDepartures -> {
                log("[$LOG_TAG] t=$t onEvent LoadPreviousDepartures → stopId=${event.stopId}")
                viewModelScope.launch { repository.loadPreviousDepartures(event.stopId) }
            }

            DeparturesUiEvent.StopPolling -> {
                val stopId = activeStopId.value
                if (stopId != null) {
                    log("[$LOG_TAG] t=$t onEvent StopPolling → stopId=$stopId")
                    // Setting to null switches flatMapLatest to flowOf(idle state),
                    // which cancels the pollStop flow and stops the polling loop.
                    activeStopId.value = null
                } else {
                    log("[$LOG_TAG] t=$t onEvent StopPolling NOOP — no active stop")
                }
            }
        }
    }

    override fun onCleared() {
        log("[$LOG_TAG] t=${nowMs()} ViewModel CLEARED activeStopId=${activeStopId.value}")
        // viewModelScope cancellation propagates to the flatMapLatest → pollStop chain,
        // which clears loading state via its finally block. No manual cleanup needed.
        super.onCleared()
    }

    companion object {
        const val LOG_TAG = "DEPARTURES"
        private const val REFRESH_TIME_TEXT_DURATION = 10_000L
    }
}
