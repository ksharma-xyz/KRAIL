package xyz.ksharma.krail.core.maps.ui.config

/**
 * Map tile provider configuration.
 * Centralized place for all map style URLs.
 */
object MapTileProvider {
    /**
     * OpenFreeMap Liberty style - default map style.
     */
    const val OPEN_FREE_MAP_LIBERTY = "https://tiles.openfreemap.org/styles/liberty"

    /**
     * Default map style to use across the app.
     */
    const val DEFAULT = OPEN_FREE_MAP_LIBERTY
}

/**
 * Map configuration constants.
 */
object MapConfig {
    /**
     * Default camera position for Sydney.
     */
    object DefaultPosition {
        const val LATITUDE = -33.8727
        const val LONGITUDE = 151.2057
        const val ZOOM = 12.0
    }

    /**
     * Map ornament (controls) configuration.
     */
    object Ornaments {
        const val DEFAULT_PADDING_DP = 16
        const val LOGO_ENABLED = false
        const val ATTRIBUTION_ENABLED = false
        const val COMPASS_ENABLED = true
        const val SCALE_BAR_ENABLED = false
    }

    /**
     * Zoom level thresholds for different area sizes.
     */
    object ZoomLevels {
        const val LARGE_AREA = 9.0 // >100km
        const val MEDIUM_AREA = 10.0 // ~50-100km
        const val CITY_AREA = 12.0 // ~10-50km
        const val SUBURB_AREA = 13.0 // ~5-10km
        const val NEIGHBORHOOD = 14.0 // ~2-5km
        const val SMALL_AREA = 15.0 // <2km
    }

    /**
     * Degree thresholds for calculating zoom levels from bounding box size.
     * These values represent the maximum difference in degrees (lat/lng) for each zoom level.
     */
    object BoundsThresholds {
        const val LARGE_AREA_DEGREES = 1.0 // >100km
        const val MEDIUM_AREA_DEGREES = 0.5 // ~50-100km
        const val CITY_AREA_DEGREES = 0.1 // ~10-50km
        const val SUBURB_AREA_DEGREES = 0.05 // ~5-10km
        const val NEIGHBORHOOD_DEGREES = 0.02 // ~2-5km
    }
}
