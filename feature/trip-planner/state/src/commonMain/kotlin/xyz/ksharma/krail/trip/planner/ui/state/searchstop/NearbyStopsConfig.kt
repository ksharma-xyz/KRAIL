package xyz.ksharma.krail.trip.planner.ui.state.searchstop

object NearbyStopsConfig {
    const val MAX_NEARBY_RESULTS = 200
    const val DEFAULT_RADIUS_KM = 1.0
    const val MAX_RADIUS_KM = 5.0
    const val MIN_RADIUS_KM = 0.5

    // Default Sydney coordinates (no location permission in Phase 1)
    const val DEFAULT_CENTER_LAT = -33.8727
    const val DEFAULT_CENTER_LON = 151.2057
    const val DEFAULT_ZOOM = 13.0

    // Performance tuning
    const val QUERY_DEBOUNCE_MS = 300L
    const val CACHE_EXPIRY_MS = 60_000L // 1 minute
    const val MIN_DISTANCE_FOR_RELOAD_KM = 0.2 // 200m
}

