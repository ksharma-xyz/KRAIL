package xyz.ksharma.krail.core.maps.state

/**
 * Configuration constants for nearby stops queries and map behavior.
 * Shared across all features that display nearby stops on a map.
 */
object NearbyStopsConfig {
    const val MAX_NEARBY_RESULTS = 200
    const val DEFAULT_RADIUS_KM = 1.0
    const val MAX_RADIUS_KM = 5.0
    const val MIN_RADIUS_KM = 0.5

    // Default Sydney coordinates (CBD)
    const val DEFAULT_CENTER_LAT = -33.8727
    const val DEFAULT_CENTER_LON = 151.2057
    const val DEFAULT_ZOOM = 13.0

    // Performance tuning
    const val QUERY_DEBOUNCE_MS = 300L
    const val CAMERA_PAN_DEBOUNCE_MS = 500L
    const val CACHE_EXPIRY_MS = 60_000L // 1 minute
    const val MIN_DISTANCE_FOR_RELOAD_KM = 0.2 // 200m
}
