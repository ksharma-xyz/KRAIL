package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.data.model.NearbyStop
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.ParkRideMapCardUi
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState

/**
 * Helper for map state operations.
 * Handles state transformations, validations, and updates for map-related functionality.
 */
object MapStateHelper {

    /**
     * Get the Ready map state from the current state, or null if not available.
     * Logs an error if the state is not in the expected format.
     */
    fun getReadyMapState(state: SearchStopState): MapUiState.Ready? {
        val mapState = state.mapUiState as? MapUiState.Ready

        if (mapState == null) {
            log(
                "[NEARBY_STOPS] ERROR: Cannot get Ready map state. " +
                    "MapUiState: ${state.mapUiState?.let { it::class.simpleName } ?: "null"}",
            )
        }

        return mapState
    }

    /**
     * Update the map center in the state.
     */
    fun updateMapCenter(
        state: SearchStopState,
        center: LatLng,
    ): SearchStopState = state.withMapState {
        copy(mapDisplay = mapDisplay.copy(mapCenter = center))
    }

    /**
     * Update user location in the state.
     */
    fun updateUserLocation(
        state: SearchStopState,
        location: LatLng?,
    ): SearchStopState = state.withMapState {
        copy(mapDisplay = mapDisplay.copy(userLocation = location))
    }

    /**
     * Update nearby stops in the state.
     */
    fun updateNearbyStops(
        state: SearchStopState,
        stops: List<NearbyStop>,
        isLoading: Boolean = false,
    ): SearchStopState = state.withMapState {
        copy(
            mapDisplay = mapDisplay.copy(
                nearbyStops = stops.map { it.toFeature() }.toImmutableList(),
            ),
            isLoadingNearbyStops = isLoading,
        )
    }

    /**
     * Set loading state for nearby stops.
     */
    fun setLoadingState(
        state: SearchStopState,
        isLoading: Boolean,
    ): SearchStopState = state.withMapState {
        copy(isLoadingNearbyStops = isLoading)
    }

    /**
     * Set the stations the map marks with a `P`.
     */
    fun setParkRideStops(
        state: SearchStopState,
        stops: ImmutableList<NearbyStopFeature>,
    ): SearchStopState = state.withMapState {
        copy(mapDisplay = mapDisplay.copy(parkRideStops = stops))
    }

    /**
     * Set the parking card on screen, or null for none.
     */
    fun setParkRideCard(
        state: SearchStopState,
        card: ParkRideMapCardUi?,
    ): SearchStopState = state.withMapState {
        copy(mapDisplay = mapDisplay.copy(parkRideCard = card))
    }

    /**
     * Toggle a transport mode in the selected modes set.
     */
    fun toggleTransportMode(
        state: SearchStopState,
        mode: TransportMode,
    ): SearchStopState = state.withMapState {
        val currentModes = mapDisplay.selectedTransportModes.toMutableSet()
        val pc = NswTransportConfig.productClassFor(mode)
        if (currentModes.contains(pc)) {
            currentModes.remove(pc)
        } else {
            currentModes.add(pc)
        }
        copy(
            mapDisplay = mapDisplay.copy(
                selectedTransportModes = currentModes.toImmutableSet(),
            ),
        )
    }
}

/**
 * Helper extension to safely update map state.
 */
private fun SearchStopState.withMapState(
    block: MapUiState.Ready.() -> MapUiState.Ready,
): SearchStopState {
    val currentMapState = mapUiState as? MapUiState.Ready ?: return this
    return copy(mapUiState = currentMapState.block())
}

/**
 * Map NearbyStop domain model to UI feature.
 */
private fun NearbyStop.toFeature() = NearbyStopFeature(
    stopId = stopId,
    stopName = stopName,
    position = LatLng(latitude, longitude),
    transportModes = transportModes.toImmutableList(),
)
