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
     * OpenFreeMap Positron style - light theme.
     */
    const val OPEN_FREE_MAP_POSITRON = "https://tiles.openfreemap.org/styles/positron"

    /**
     * OpenFreeMap Dark Matter style - dark theme.
     */
    const val OPEN_FREE_MAP_DARK = "https://tiles.openfreemap.org/styles/dark-matter"

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
        const val ATTRIBUTION_ENABLED = true
        const val COMPASS_ENABLED = true
        const val SCALE_BAR_ENABLED = false
    }

    /**
     * Zoom level thresholds for different area sizes.
     */
    object ZoomLevels {
        const val LARGE_AREA = 9.0       // >100km
        const val MEDIUM_AREA = 10.0     // ~50-100km
        const val CITY_AREA = 12.0       // ~10-50km
        const val SUBURB_AREA = 13.0     // ~5-10km
        const val NEIGHBORHOOD = 14.0    // ~2-5km
        const val SMALL_AREA = 15.0      // <2km
    }
}
