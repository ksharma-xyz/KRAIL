package xyz.ksharma.krail.trip.planner.ui.mapstopselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.maps.state.NearbyStopsConfig
import xyz.ksharma.krail.coroutines.ext.launchWithExceptionHandler
import xyz.ksharma.krail.trip.planner.ui.parkride.map.NoParkRideMapSource
import xyz.ksharma.krail.trip.planner.ui.parkride.map.ParkRideMapCardHolder
import xyz.ksharma.krail.trip.planner.ui.parkride.map.ParkRideMapSource
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.NearbyStopsManager
import xyz.ksharma.krail.trip.planner.ui.state.mapstopselection.MapStopSelectionEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapDisplay
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature

/**
 * Owns right-pane map state for a single nav entry (SavedTrips or SearchStop dual-pane).
 *
 * Scoped to the NavEntry ViewModel store — each screen gets its own instance so
 * SavedTrips and SearchStop never share state or compete for NearbyStopsManager queries.
 * The shared [NearbyStopsManager] (Koin single) still deduplcates network work under the hood.
 *
 * State is **Ready from construction** — no initialise event needed.
 *
 * Lifecycle (WhileSubscribed pattern):
 * - Stops work when the last UI consumer drops [mapUiState], with a 5-second timeout
 *   to ride out rotation / config changes without cancelling in-flight queries.
 * - viewModelScope is cancelled when the entry leaves the back stack.
 */
class MapStopSelectionViewModel(
    private val nearbyStopsManager: NearbyStopsManager,
    // The one radius `nearby_stops_radius_km` sets, read once as the map starts, as Search Stop does.
    searchRadiusKm: Double = NearbyStopsConfig.DEFAULT_RADIUS_KM,
    private val parkRideMapSource: ParkRideMapSource = NoParkRideMapSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private var parkRideStopsRequested = false

    private val parkRideCard by lazy {
        ParkRideMapCardHolder(source = parkRideMapSource, scope = viewModelScope, dispatcher = ioDispatcher) { card ->
            updateMapDisplay { copy(parkRideCard = card) }
        }
    }

    init {
        log("[MAP_STOP_SEL] VM init — seeded MapUiState.Ready")
    }

    private val initialState = MapUiState.Ready(mapDisplay = MapDisplay(searchRadiusKm = searchRadiusKm))

    private val _mapUiState: MutableStateFlow<MapUiState> = MutableStateFlow(initialState)

    val mapUiState: StateFlow<MapUiState> = _mapUiState
        .onStart { log("[MAP_STOP_SEL] consumer attached") }
        .onCompletion { log("[MAP_STOP_SEL] last consumer detached") }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIBER_TIMEOUT_MS),
            initialValue = initialState,
        )

    fun onEvent(event: MapStopSelectionEvent) {
        loadParkRideStopsOnce()
        when (event) {
            is MapStopSelectionEvent.UserLocationUpdated -> onUserLocationUpdated(event.location)
            is MapStopSelectionEvent.MapCenterChanged -> onMapCenterChanged(event.center)
            is MapStopSelectionEvent.ParkRideCardOpened -> parkRideCard.open(event.stopId)
            MapStopSelectionEvent.ParkRideCardClosed -> parkRideCard.close()
            is MapStopSelectionEvent.ParkRideDirectionsClicked -> parkRideMapSource.openDirections(
                latitude = event.latitude,
                longitude = event.longitude,
                label = event.label,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        nearbyStopsManager.cancelOngoingQuery()
        log("[MAP_STOP_SEL] VM cleared — cancelled active queries")
    }

    private fun onUserLocationUpdated(location: LatLng?) {
        updateMapDisplay { copy(userLocation = location) }
        loadNearbyStops()
    }

    private fun onMapCenterChanged(center: LatLng) {
        updateMapDisplay { copy(mapCenter = center) }
        loadNearbyStops()
    }

    // On the first event, which only a map on screen sends: a phone holds this view model too and draws no
    // map from it, so it never reads the database for one.
    private fun loadParkRideStopsOnce() {
        if (parkRideStopsRequested) return
        parkRideStopsRequested = true
        viewModelScope.launchWithExceptionHandler<MapStopSelectionViewModel>(ioDispatcher) {
            val stops = parkRideMapSource.stops()
            updateMapDisplay { copy(parkRideStops = stops) }
        }
    }

    // The new display is built from the state as it is at the write, so a change that lands from another
    // thread in between, such as the parking stops, is kept.
    private inline fun updateMapDisplay(block: MapDisplay.() -> MapDisplay) {
        _mapUiState.update { current ->
            if (current is MapUiState.Ready) current.copy(mapDisplay = current.mapDisplay.block()) else current
        }
    }

    private fun loadNearbyStops() {
        val mapState = _mapUiState.value as? MapUiState.Ready ?: return
        nearbyStopsManager.loadNearbyStops(
            mapState = mapState,
            center = mapState.mapDisplay.mapCenter,
            scope = viewModelScope,
            onLoadingStateChanged = { isLoading ->
                _mapUiState.update { current ->
                    if (current is MapUiState.Ready) current.copy(isLoadingNearbyStops = isLoading) else current
                }
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

    companion object {
        private const val SUBSCRIBER_TIMEOUT_MS = 5000L
    }
}
