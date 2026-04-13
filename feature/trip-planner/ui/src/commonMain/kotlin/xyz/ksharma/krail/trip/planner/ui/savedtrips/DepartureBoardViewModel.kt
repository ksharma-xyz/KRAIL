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
import kotlinx.coroutines.launch
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
 * Manages an accordion: at most one card is expanded at a time.
 *
 * **Polling is collection-driven**: expanding a card switches [_expandedStopId], which causes
 * [entries]'s [flatMapLatest] to cancel the previous [DepartureBoardRepository.pollStop] flow
 * and start a new one. When the UI stops collecting [entries] (app backgrounded,
 * [SharingStarted.WhileSubscribed] times out), the entire chain — including network polling —
 * stops automatically. No lifecycle hooks are required.
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
     *
     * The **expanded** stop is polled via [DepartureBoardRepository.pollStop] — its network loop
     * runs exactly while this StateFlow has active subscribers. All other stops observe the cache
     * via [DepartureBoardRepository.observeStop] (no network calls).
     *
     * [flatMapLatest] ensures that switching the expanded stop (or changing the stop list)
     * cancels the previous inner subscription before starting the new one, so only one polling
     * loop ever runs at a time.
     */
    val entries: StateFlow<ImmutableList<StopDepartureBoardEntry>> =
        combine(_stops, _expandedStopId) { stops, expandedId -> stops to expandedId }
            .flatMapLatest { (stops, expandedId) ->
                if (stops.isEmpty()) {
                    flowOf(persistentListOf())
                } else {
                    combine(
                        stops.map { (stopId, stopName) ->
                            (if (stopId == expandedId) repository.pollStop(stopId)
                            else repository.observeStop(stopId))
                                .map { state -> StopDepartureBoardEntry(stopId, stopName, state) }
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
        // collapse the card — flatMapLatest will cancel its pollStop flow automatically.
        val expandedId = _expandedStopId.value
        if (expandedId != null && uniqueStops.none { it.first == expandedId }) {
            _expandedStopId.value = null
        }
    }

    /**
     * Expands the card for [stopId].
     *
     * Setting [_expandedStopId] triggers [flatMapLatest] in [entries] which cancels the previous
     * [DepartureBoardRepository.pollStop] flow and starts a new one for [stopId] — the old
     * polling loop is stopped and loading state is cleaned up via the flow's `finally` block.
     */
    fun onCardExpand(stopId: String) {
        if (_expandedStopId.value == stopId) return
        _expandedStopId.value = stopId
    }

    /** Collapses the currently open card. [flatMapLatest] cancels the polling loop. */
    fun onCardCollapse() {
        _expandedStopId.value = null
    }

    /** Triggers an immediate silent refresh for [stopId] without disrupting the poll loop. */
    fun onRefreshStop(stopId: String) {
        viewModelScope.launch { repository.refresh(stopId) }
    }

    /**
     * Triggers a one-shot fetch of past departures (~15 min window) for [stopId].
     * Results land in [DeparturesState.previousDepartures] and are shown when the user
     * has toggled "Show previous" in the UI.
     */
    fun onLoadPreviousDepartures(stopId: String) {
        viewModelScope.launch { repository.loadPreviousDepartures(stopId) }
    }
}
