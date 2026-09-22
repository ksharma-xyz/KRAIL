package xyz.ksharma.krail.core.maps.ui.config

import androidx.compose.ui.Alignment

/**
 * Map tile provider configuration.
 * Centralized place for all map style URLs.
 */
object MapTileProvider {
    /**
     * OpenFreeMap Liberty style - default map style.
     *
     * These tiles are built from OpenStreetMap data. The ODbL requires visible credit to
     * "OpenStreetMap contributors" **wherever the map is shown**, and OpenFreeMap asks for
     * credit of its own, so [MapConfig.Ornaments.ATTRIBUTION_ENABLED] is not a display
     * preference: it is the licence being met. Held by `MapAttributionTest`.
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

        /**
         * MapLibre's own wordmark. Not required by any licence, unlike [ATTRIBUTION_ENABLED].
         */
        const val LOGO_ENABLED = false

        /**
         * The ⓘ button that opens the style's attribution, which for
         * [MapTileProvider.OPEN_FREE_MAP_LIBERTY] credits OpenStreetMap contributors and
         * OpenFreeMap.
         *
         * **Never turn this off.** The tiles are OpenStreetMap data and the ODbL requires the
         * credit to be visible wherever the map is. It shipped `false`, so every map in the app
         * displayed OSM data with no attribution anywhere, including no licences screen. A
         * future ornament tidy-up must not be able to do that again quietly, which is what
         * `MapAttributionTest` is for.
         */
        const val ATTRIBUTION_ENABLED = true

        /**
         * Bottom left, the corner the OSM community conventionally uses and the one least
         * likely to sit under a control: the compass and the scale bar take the top corners,
         * and bottom right is where a floating action button would go.
         */
        val ATTRIBUTION_ALIGNMENT: Alignment = Alignment.BottomStart

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
