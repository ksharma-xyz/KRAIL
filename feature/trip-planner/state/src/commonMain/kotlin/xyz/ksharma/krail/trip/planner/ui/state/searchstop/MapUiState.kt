package xyz.ksharma.krail.trip.planner.ui.state.searchstop

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableSet
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode

/**
 * Small, platform\-agnostic types for map UI state.
 * No Maplibre types here just pure kotlin
 */
data class LatLng(val latitude: Double, val longitude: Double)

data class RouteFeature(
    val id: String,
    val colorHex: String,
    val points: List<LatLng>,
)

data class StopFeature(
    val stopId: String,
    val stopName: String,
    val lineId: String?,
    val position: LatLng,
)

/** Simple UI payload for the currently selected stop */
data class SelectedStopUi(
    val id: String,
    val name: String,
    val lineId: String?,
)

/** Nearby stop feature for map rendering */
data class NearbyStopFeature(
    val stopId: String,
    val stopName: String,
    val position: LatLng,
    val distanceKm: Double,
    val transportModes: List<TransportMode>,
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
    val searchRadiusKm: Double = NearbyStopsConfig.DEFAULT_RADIUS_KM,
    val showDistanceScale: Boolean = false,
    val showCompass: Boolean = true,
)
