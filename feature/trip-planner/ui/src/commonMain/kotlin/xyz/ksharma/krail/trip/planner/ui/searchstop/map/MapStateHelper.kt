package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.data.model.NearbyStop
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
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
    ): SearchStopState {
        return state.withMapState {
            copy(mapDisplay = mapDisplay.copy(mapCenter = center))
        }
    }

    /**
     * Update nearby stops in the state.
     */
    fun updateNearbyStops(
        state: SearchStopState,
        stops: List<NearbyStop>,
        isLoading: Boolean = false,
    ): SearchStopState {
        return state.withMapState {
            copy(
                mapDisplay = mapDisplay.copy(
                    nearbyStops = stops.map { it.toFeature() }.toImmutableList(),
                ),
                isLoadingNearbyStops = isLoading,
            )
        }
    }

    /**
     * Set loading state for nearby stops.
     */
    fun setLoadingState(
        state: SearchStopState,
        isLoading: Boolean,
    ): SearchStopState {
        return state.withMapState {
            copy(isLoadingNearbyStops = isLoading)
        }
    }

    /**
     * Toggle a transport mode in the selected modes set.
     */
    fun toggleTransportMode(
        state: SearchStopState,
        mode: TransportMode,
    ): SearchStopState {
        return state.withMapState {
            val currentModes = mapDisplay.selectedTransportModes.toMutableSet()
            if (currentModes.contains(mode.productClass)) {
                currentModes.remove(mode.productClass)
            } else {
                currentModes.add(mode.productClass)
            }
            copy(
                mapDisplay = mapDisplay.copy(
                    selectedTransportModes = currentModes.toImmutableSet(),
                ),
            )
        }
    }

    /**
     * Update the search radius.
     */
    fun updateSearchRadius(
        state: SearchStopState,
        radiusKm: Double,
    ): SearchStopState {
        return state.withMapState {
            copy(mapDisplay = mapDisplay.copy(searchRadiusKm = radiusKm))
        }
    }

    /**
     * Toggle distance scale visibility.
     */
    fun toggleDistanceScale(
        state: SearchStopState,
        enabled: Boolean,
    ): SearchStopState {
        return state.withMapState {
            copy(mapDisplay = mapDisplay.copy(showDistanceScale = enabled))
        }
    }

    /**
     * Toggle compass visibility.
     */
    fun toggleCompass(
        state: SearchStopState,
        enabled: Boolean,
    ): SearchStopState {
        return state.withMapState {
            copy(mapDisplay = mapDisplay.copy(showCompass = enabled))
        }
    }

    /**
     * Helper extension to safely update map state.
     */
    private fun SearchStopState.withMapState(
        block: MapUiState.Ready.() -> MapUiState.Ready,
    ): SearchStopState {
        val currentMapState = mapUiState as? MapUiState.Ready ?: return this

        return copy(
            mapUiState = currentMapState.block(),
        )
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
}
