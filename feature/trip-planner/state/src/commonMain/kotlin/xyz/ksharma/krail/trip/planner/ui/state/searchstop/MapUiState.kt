package xyz.ksharma.krail.trip.planner.ui.state.searchstop

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

/** UI state for the map screen */

sealed class MapUiState {
    object Loading : MapUiState()
    data class Ready(
        val mapDisplay: MapDisplay = MapDisplay(),
    ) : MapUiState()

    data class Error(val message: String?) : MapUiState()
}


data class MapDisplay(
    val routes: List<RouteFeature> = emptyList(),
    val stops: List<StopFeature> = emptyList(),
    val selectedStop: SelectedStopUi? = null,
)
