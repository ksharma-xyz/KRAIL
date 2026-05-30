package xyz.ksharma.krail.trip.planner.ui.mapstopselection

import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.NearbyStopsManager
import xyz.ksharma.krail.trip.planner.ui.state.mapstopselection.MapStopSelectionEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapDisplay
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature

/**
 * Shared owner of right-pane map state. One Koin singleton, reused by both
 * SavedTrips and SearchStop dual-pane (SearchStop migration lands in a follow-up PR).
 *
 * State is **Ready from construction** — no initialise event needed, no race against
 * the first composition. Pane just collects.
 *
 * Lifecycle (mirrors TimeTableViewModel's WhileSubscribed pattern):
 * - Stops doing work as soon as the last consumer drops [mapUiState], with a small
 *   timeout to ride out config changes.
 * - Cancels any in-flight NearbyStopsManager query via [stopActiveWork].
 * - State is kept in-memory across attach/detach so the next consumer sees the last
 *   known map immediately (no flicker).
 */
class MapStopSelectionViewModel(
    private val nearbyStopsManager: NearbyStopsManager,
    @Suppress("UnusedPrivateProperty")
    private val ioDispatcher: CoroutineDispatcher,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) {
    private val _mapUiState: MutableStateFlow<MapUiState> = MutableStateFlow(MapUiState.Ready())

    val mapUiState: StateFlow<MapUiState> = _mapUiState
        .onStart { log("[MAP_STOP_SEL] consumer attached") }
        .onCompletion { stopActiveWork() }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(SUBSCRIBER_TIMEOUT_MS),
            initialValue = MapUiState.Ready(),
        )

    fun onEvent(event: MapStopSelectionEvent) {
        when (event) {
            is MapStopSelectionEvent.UserLocationUpdated -> onUserLocationUpdated(event.location)
        }
    }

    private fun onUserLocationUpdated(location: LatLng?) {
        updateMapDisplay { copy(userLocation = location) }
        loadNearbyStops()
    }

    private inline fun updateMapDisplay(block: MapDisplay.() -> MapDisplay) {
        val ready = _mapUiState.value as? MapUiState.Ready ?: return
        _mapUiState.update {
            ready.copy(mapDisplay = ready.mapDisplay.block())
        }
    }

    private fun loadNearbyStops() {
        val mapState = _mapUiState.value as? MapUiState.Ready ?: return
        nearbyStopsManager.loadNearbyStops(
            mapState = mapState,
            center = mapState.mapDisplay.mapCenter,
            scope = scope,
            onLoadingStateChanged = { isLoading ->
                val ready = _mapUiState.value as? MapUiState.Ready ?: return@loadNearbyStops
                _mapUiState.value = ready.copy(isLoadingNearbyStops = isLoading)
            },
            onStopsLoaded = { stops ->
                updateMapDisplay {
                    copy(
                        nearbyStops = stops.map { nearby ->
                            NearbyStopFeature(
                                stopId = nearby.stopId,
                                stopName = nearby.stopName,
                                position = LatLng(nearby.latitude, nearby.longitude),
                                transportModes = nearby.transportModes.toImmutableList(),
                            )
                        }.toImmutableList(),
                    )
                }
            },
            onError = { error ->
                log("[MAP_STOP_SEL] loadNearbyStops error: ${error.message}")
            },
        )
    }

    private fun stopActiveWork() {
        log("[MAP_STOP_SEL] last consumer detached — cancelling active queries")
        nearbyStopsManager.cancelOngoingQuery()
    }

    companion object {
        // Matches the SearchStop pattern — long enough to ride out rotation, short
        // enough that backgrounded screens stop work.
        private const val SUBSCRIBER_TIMEOUT_MS = 5000L
    }
}
