package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import xyz.ksharma.krail.departures.ui.DepartureBoardRepository
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

/**
 * Represents a single stop entry in the Departure Board section of the saved trips screen.
 *
 * @param stopId NSW Transport stop ID.
 * @param stopName Human-readable stop name.
 * @param state Current departure state for this stop, updated by [DepartureBoardRepository].
 */
data class StopDepartureBoardEntry(
    val stopId: String,
    val stopName: String,
    val state: DeparturesState,
)

/**
 * ViewModel for the Departure Board section on the saved trips screen.
 *
 * Manages an accordion: at most one card is expanded at a time. Expanding a card
 * delegates to [DepartureBoardRepository.setActiveStop], which enforces the single
 * polling loop constraint across the whole app.
 */
class DepartureBoardViewModel(
    private val repository: DepartureBoardRepository,
) : ViewModel() {

    // Unique stops derived from the saved trips list (both from- and to-stops, deduplicated).
    private val _stops = MutableStateFlow<List<Pair<String, String>>>(emptyList())

    private val _expandedStopId = MutableStateFlow<String?>(null)

    /** The stop ID of the currently expanded card, or `null` if all cards are collapsed. */
    val expandedStopId: StateFlow<String?> = _expandedStopId.asStateFlow()

    /**
     * One entry per unique stop from the saved trips list.
     * Each entry's [StopDepartureBoardEntry.state] is kept live via [DepartureBoardRepository].
     */
    val entries: StateFlow<ImmutableList<StopDepartureBoardEntry>> = _stops
        .flatMapLatest { stops ->
            if (stops.isEmpty()) {
                flowOf(persistentListOf())
            } else {
                combine(
                    stops.map { (stopId, stopName) ->
                        repository.observeStop(stopId).map { state ->
                            StopDepartureBoardEntry(
                                stopId = stopId,
                                stopName = stopName,
                                state = state,
                            )
                        }
                    },
                ) { array -> array.toList().toImmutableList() }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = persistentListOf(),
        )

    /**
     * Updates the stop list derived from [trips].
     *
     * Both the from- and to-stop of each trip are included; duplicates are removed
     * by stop ID so each stop appears at most once.
     */
    fun setTrips(trips: ImmutableList<Trip>) {
        val uniqueStops = trips
            .flatMap { trip ->
                listOf(
                    trip.fromStopId to trip.fromStopName,
                    trip.toStopId to trip.toStopName,
                )
            }
            .distinctBy { it.first }
        _stops.value = uniqueStops

        // If the expanded stop is no longer in the new stop list (e.g. the trip was deleted),
        // stop polling and collapse the card so no dangling background fetch continues.
        val expandedId = _expandedStopId.value
        if (expandedId != null && uniqueStops.none { it.first == expandedId }) {
            repository.stopIfActive(expandedId)
            _expandedStopId.value = null
        }
    }

    /** Expands the card for [stopId], collapsing any previously open card instantly. */
    fun onCardExpand(stopId: String) {
        if (_expandedStopId.value == stopId) return
        _expandedStopId.value?.let { repository.stopIfActive(it) }
        _expandedStopId.value = stopId
        repository.setActiveStop(stopId)
    }

    /** Triggers an immediate silent refresh for [stopId] without disrupting the poll loop. */
    fun onRefreshStop(stopId: String) {
        repository.refresh(stopId)
    }

    /**
     * Triggers a one-shot fetch of past departures (~15 min window) for [stopId].
     * Results land in [DeparturesState.previousDepartures] and are shown when the user
     * has toggled "Show previous" in the UI.
     */
    fun onLoadPreviousDepartures(stopId: String) {
        repository.loadPreviousDepartures(stopId)
    }

    /** Collapses the currently open card and stops polling. */
    fun onCardCollapse() {
        val current = _expandedStopId.value ?: return
        repository.stopIfActive(current)
        _expandedStopId.value = null
    }

    override fun onCleared() {
        _expandedStopId.value?.let { repository.stopIfActive(it) }
        super.onCleared()
    }
}
