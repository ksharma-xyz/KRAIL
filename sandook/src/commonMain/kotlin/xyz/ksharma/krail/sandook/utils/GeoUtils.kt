package xyz.ksharma.krail.sandook.utils

import xyz.ksharma.krail.core.maps.state.BoundingBox
import xyz.ksharma.krail.core.maps.state.LatLng
import kotlin.math.*

object GeoUtils {
    private const val EARTH_RADIUS_KM = 6371.0
    private const val DEGREES_TO_RADIANS = PI / 180.0

    /**
     * Calculate bounding box from center point and radius.
     * This is used for pre-filtering stops before applying Haversine formula.
     *
     * @param centerLat Center latitude in degrees
     * @param centerLon Center longitude in degrees
     * @param radiusKm Radius in kilometers
     * @return BoundingBox containing min/max lat/lon
     */
    fun calculateBoundingBox(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double
    ): BoundingBox {
        // Latitude: 1 degree ≈ 111km
        val latDelta = radiusKm / 111.0

        // Longitude: varies by latitude (narrower near poles)
        val lonDelta = radiusKm / (111.0 * cos(centerLat * DEGREES_TO_RADIANS))

        return BoundingBox(
            southwest = LatLng(
                latitude = centerLat - latDelta,
                longitude = centerLon - lonDelta
            ),
            northeast = LatLng(
                latitude = centerLat + latDelta,
                longitude = centerLon + lonDelta
            )
        )
    }

    /**
     * Calculate distance between two points using Haversine formula.
     * Used for validation and testing.
     *
     * @return Distance in kilometers
     */
    fun haversineDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val dLat = (lat2 - lat1) * DEGREES_TO_RADIANS
        val dLon = (lon2 - lon1) * DEGREES_TO_RADIANS

        val a = sin(dLat / 2).pow(2) +
                cos(lat1 * DEGREES_TO_RADIANS) * cos(lat2 * DEGREES_TO_RADIANS) *
                sin(dLon / 2).pow(2)

        val c = 2 * asin(sqrt(a))
        return EARTH_RADIUS_KM * c
    }
}


