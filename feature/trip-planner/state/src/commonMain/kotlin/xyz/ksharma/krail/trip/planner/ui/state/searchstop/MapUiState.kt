package xyz.ksharma.krail.trip.planner.ui.state.searchstop

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableSet
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.maps.state.NearbyStopsConfig
import xyz.ksharma.krail.core.maps.state.RouteFeature
import xyz.ksharma.krail.core.maps.state.SelectedStopUi
import xyz.ksharma.krail.core.maps.state.StopFeature
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode

/**
 * Map UI state for Search Stop screen.
 * Uses core map models (RouteFeature, StopFeature, etc.) from core:maps:state.
 */

/** Nearby stop feature for map rendering */
data class NearbyStopFeature(
    val stopId: String,
    val stopName: String,
    val position: LatLng,
    val transportModes: ImmutableList<TransportMode>,
    val hasParkAndRide: Boolean = false,
)

/** UI state for the map screen */

sealed class MapUiState {
    object Loading : MapUiState()
    data class Ready(
        val mapDisplay: MapDisplay = MapDisplay(),
        val isLoadingNearbyStops: Boolean = false,
    ) : MapUiState()

    data class Error(val message: String?) : MapUiState()
}

data class MapDisplay(
    val routes: ImmutableList<RouteFeature> = persistentListOf(),
    val stops: ImmutableList<StopFeature> = persistentListOf(),
    val selectedStop: SelectedStopUi? = null,
    val nearbyStops: ImmutableList<NearbyStopFeature> = persistentListOf(),
    val selectedTransportModes: ImmutableSet<Int> = TransportMode.allProductClasses().toImmutableSet(),
    val mapCenter: LatLng = LatLng(
        NearbyStopsConfig.DEFAULT_CENTER_LAT,
        NearbyStopsConfig.DEFAULT_CENTER_LON,
    ),
    val userLocation: LatLng? = null, // User's current GPS location (null if unknown)
    val searchRadiusKm: Double = NearbyStopsConfig.DEFAULT_RADIUS_KM,
    val showDistanceScale: Boolean = false,
    val showCompass: Boolean = true,
)
