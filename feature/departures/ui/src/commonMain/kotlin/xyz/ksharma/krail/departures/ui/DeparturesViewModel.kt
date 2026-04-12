package xyz.ksharma.krail.departures.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.core.datetime.DateTimeHelper.calculateTimeDifferenceFromNow
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toGenericFormattedTimeString
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.departures.ui.state.DeparturesUiEvent
import kotlin.time.ExperimentalTime

class DeparturesViewModel(
    private val repository: DepartureBoardRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    // Tracks which stop this ViewModel instance is responsible for polling.
    private val activeStopId = MutableStateFlow<String?>(null)

    private val _uiState: MutableStateFlow<DeparturesState> = MutableStateFlow(DeparturesState())
    val uiState: StateFlow<DeparturesState> = _uiState.asStateFlow()

    /**
     * Ticks every [REFRESH_TIME_TEXT_DURATION] ms while subscribed, recomputing the
     * "in X mins" text for each departure — same pattern as TimeTableViewModel.
     */
    val isActive: StateFlow<Boolean> = MutableStateFlow(false).onStart {
        while (true) {
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

    init {
        // Collect repository flow into _uiState so updateRelativeTimeText() can mutate it.
        viewModelScope.launch {
            activeStopId
                .flatMapLatest { stopId ->
                    if (stopId != null) {
                        repository.observeStop(stopId)
                    } else {
                        flowOf(DeparturesState(isLoading = false))
                    }
                }
                .collect { state -> _uiState.value = state }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun updateRelativeTimeText() = viewModelScope.launch {
        val updated = withContext(ioDispatcher) {
            _uiState.value.departures.map { departure ->
                departure.copy(
                    relativeTimeText = runCatching {
                        calculateTimeDifferenceFromNow(departure.departureUtcDateTime)
                            .toGenericFormattedTimeString()
                    }.getOrDefault(""),
                )
            }.toImmutableList()
        }
        _uiState.update { it.copy(departures = updated) }
    }

    fun onEvent(event: DeparturesUiEvent) {
        when (event) {
            is DeparturesUiEvent.LoadDepartures -> {
                val current = activeStopId.value
                // Guard: same stop already loaded without error → no-op (rotation-safe)
                if (event.stopId == current &&
                    !uiState.value.isError &&
                    !uiState.value.isLoading
                ) {
                    log("[$LOG_TAG] LoadDepartures ignored — stop ${event.stopId} already loaded")
                    return
                }
                log("[$LOG_TAG] LoadDepartures → stopId=${event.stopId}")
                // Stop polling the previous stop if this ViewModel owns it
                current?.let { repository.stopIfActive(it) }
                activeStopId.value = event.stopId
                repository.setActiveStop(event.stopId)
            }

            DeparturesUiEvent.Refresh -> {
                val stopId = activeStopId.value ?: return
                log("[$LOG_TAG] Refresh → stopId=$stopId")
                repository.refresh(stopId)
            }
        }
    }

    override fun onCleared() {
        activeStopId.value?.let { repository.stopIfActive(it) }
        super.onCleared()
    }

    companion object {
        const val LOG_TAG = "DEPARTURES"
        private const val REFRESH_TIME_TEXT_DURATION = 10_000L
    }
}
