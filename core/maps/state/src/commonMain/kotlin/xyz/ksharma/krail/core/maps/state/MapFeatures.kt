package xyz.ksharma.krail.core.maps.state

/**
 * Generic map feature models for visualizing routes, stops, and selections.
 * These are reusable across all features that display maps.
 */

/**
 * Represents a route/path on a map.
 * Used for displaying transit lines, walking paths, etc.
 */
data class RouteFeature(
    val id: String,
    val colorHex: String,
    val points: List<LatLng>,
)

/**
 * Represents a stop/station on a map.
 * Generic enough to be used by any feature displaying stops.
 */
data class StopFeature(
    val stopId: String,
    val stopName: String,
    val lineId: String?,
    val position: LatLng,
)

/**
 * Represents a selected stop on a map.
 * Simple UI model for the currently selected/highlighted stop.
 */
data class SelectedStopUi(
    val id: String,
    val name: String,
    val lineId: String?,
)
