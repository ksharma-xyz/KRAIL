package xyz.ksharma.krail.trip.planner.ui.state.journeymap

import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.LatLng

/**
 * Platform-agnostic types for journey map UI state.
 * No MapLibre types here - just pure Kotlin.
 * Reuses existing models from searchstop and TransportMode.
 */

/**
 * Represents a single leg of a journey for map visualization.
 */
data class JourneyLegFeature(
    val legId: String,
    val transportMode: TransportMode?,
    val routeSegment: RouteSegment,
)

/**
 * Sealed class representing different types of route segments.
 */
sealed class RouteSegment {
    /**
     * A path with actual coordinates (e.g., walking/interchange paths).
     * This is used when the API provides explicit coordinate paths.
     */
    data class PathSegment(
        val points: List<LatLng>,
    ) : RouteSegment()

    /**
     * A segment connecting stops with straight lines between them.
     * This is used for transit legs where we only have stop coordinates.
     */
    data class StopConnectorSegment(
        val stops: List<JourneyStopFeature>,
    ) : RouteSegment()
}

/**
 * Represents a stop/station in the journey.
 */
data class JourneyStopFeature(
    val stopId: String,
    val stopName: String,
    val position: LatLng?,
    val stopType: StopType,
    val time: String?,
    val platform: String?,
)

/**
 * Type of stop in the journey context.
 */
enum class StopType {
    ORIGIN,
    DESTINATION,
    INTERCHANGE,
    REGULAR,
}

/**
 * UI state for the journey map.
 */
sealed class JourneyMapUiState {
    object Loading : JourneyMapUiState()

    data class Ready(
        val mapDisplay: JourneyMapDisplay,
        val cameraFocus: CameraFocus? = null,
    ) : JourneyMapUiState()

    data class Error(val message: String) : JourneyMapUiState()
}

/**
 * The actual map display data.
 */
data class JourneyMapDisplay(
    val legs: List<JourneyLegFeature> = emptyList(),
    val stops: List<JourneyStopFeature> = emptyList(),
    val selectedLeg: JourneyLegFeature? = null,
)

/**
 * Camera focus configuration for auto-focusing on the journey.
 */
data class CameraFocus(
    val bounds: BoundingBox,
    val padding: Int = 50,
)

/**
 * Bounding box for map camera positioning.
 */
data class BoundingBox(
    val southwest: LatLng,
    val northeast: LatLng,
)
