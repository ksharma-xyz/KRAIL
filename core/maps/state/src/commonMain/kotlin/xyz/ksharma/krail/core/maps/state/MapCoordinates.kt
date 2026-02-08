package xyz.ksharma.krail.core.maps.state

/**
 * Represents a latitude/longitude coordinate.
 * Shared across all map features.
 */
data class LatLng(
    val latitude: Double,
    val longitude: Double,
)

/**
 * Bounding box for map camera positioning.
 */
data class BoundingBox(
    val southwest: LatLng,
    val northeast: LatLng,
)

/**
 * Camera focus configuration for auto-focusing on map areas.
 */
data class CameraFocus(
    val bounds: BoundingBox,
    val padding: Int = 50,
)
