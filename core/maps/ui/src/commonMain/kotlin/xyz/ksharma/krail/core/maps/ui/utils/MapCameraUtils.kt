package xyz.ksharma.krail.core.maps.ui.utils

import xyz.ksharma.krail.core.maps.state.BoundingBox
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.maps.ui.config.MapConfig
import kotlin.math.abs
import kotlin.math.max

/**
 * Utility functions for map camera operations.
 */
object MapCameraUtils {

    /**
     * Calculate the center position from a bounding box.
     */
    fun calculateCenter(bounds: BoundingBox): LatLng {
        val centerLat = (bounds.southwest.latitude + bounds.northeast.latitude) / 2
        val centerLng = (bounds.southwest.longitude + bounds.northeast.longitude) / 2
        return LatLng(latitude = centerLat, longitude = centerLng)
    }

    /**
     * Calculate appropriate zoom level based on bounding box size.
     * Returns zoom level where 0 = whole world, 22 = individual buildings.
     */
    fun calculateZoomLevel(bounds: BoundingBox): Double {
        val latDiff = abs(bounds.northeast.latitude - bounds.southwest.latitude)
        val lngDiff = abs(bounds.northeast.longitude - bounds.southwest.longitude)
        val maxDiff = max(latDiff, lngDiff)

        return when {
            maxDiff > MapConfig.BoundsThresholds.LARGE_AREA_DEGREES -> MapConfig.ZoomLevels.LARGE_AREA
            maxDiff > MapConfig.BoundsThresholds.MEDIUM_AREA_DEGREES -> MapConfig.ZoomLevels.MEDIUM_AREA
            maxDiff > MapConfig.BoundsThresholds.CITY_AREA_DEGREES -> MapConfig.ZoomLevels.CITY_AREA
            maxDiff > MapConfig.BoundsThresholds.SUBURB_AREA_DEGREES -> MapConfig.ZoomLevels.SUBURB_AREA
            maxDiff > MapConfig.BoundsThresholds.NEIGHBORHOOD_DEGREES -> MapConfig.ZoomLevels.NEIGHBORHOOD
            else -> MapConfig.ZoomLevels.SMALL_AREA
        }
    }

    /**
     * Calculate bounding box from a list of coordinates.
     * Returns null if the list is empty.
     */
    fun calculateBounds(coordinates: List<LatLng>): BoundingBox? {
        if (coordinates.isEmpty()) return null

        val minLat = coordinates.minOf { it.latitude }
        val maxLat = coordinates.maxOf { it.latitude }
        val minLng = coordinates.minOf { it.longitude }
        val maxLng = coordinates.maxOf { it.longitude }

        return BoundingBox(
            southwest = LatLng(minLat, minLng),
            northeast = LatLng(maxLat, maxLng),
        )
    }
}
