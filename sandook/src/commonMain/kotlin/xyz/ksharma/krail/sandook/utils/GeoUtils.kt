package xyz.ksharma.krail.sandook.utils

import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.state.BoundingBox
import xyz.ksharma.krail.core.maps.state.LatLng
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {
    private const val EARTH_RADIUS_KM = 6371.0
    private const val DEGREES_TO_RADIANS = PI / 180.0
    private const val KM_PER_DEGREE_LATITUDE = 111.0
    private const val KM_PER_DEGREE_LONGITUDE_AT_EQUATOR = 111.0

    // Geographic coordinate validation constants
    private const val MIN_LATITUDE = -90.0
    private const val MAX_LATITUDE = 90.0
    private const val MIN_LONGITUDE = -180.0
    private const val MAX_LONGITUDE = 180.0

    /**
     * Calculate bounding box from center point and radius.
     * This is used for pre-filtering stops before applying Haversine formula.
     *
     * @param centerLat Center latitude in degrees
     * @param centerLon Center longitude in degrees
     * @param radiusKm Radius in kilometers
     * @return BoundingBox containing min/max lat/lon, or null if calculation fails
     */
    fun calculateBoundingBox(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double,
    ): BoundingBox? = runCatching {
        // Validate inputs
        require(centerLat in MIN_LATITUDE..MAX_LATITUDE) {
            "Invalid latitude: $centerLat. Must be between $MIN_LATITUDE and $MAX_LATITUDE degrees."
        }
        require(centerLon in MIN_LONGITUDE..MAX_LONGITUDE) {
            "Invalid longitude: $centerLon. Must be between $MIN_LONGITUDE and $MAX_LONGITUDE degrees."
        }
        require(radiusKm > 0) {
            "Invalid radius: $radiusKm. Must be positive."
        }

        // Latitude: 1 degree ≈ 111km
        val latDelta = radiusKm / KM_PER_DEGREE_LATITUDE

        // Longitude: varies by latitude (narrower near poles)
        val lonDelta = radiusKm / (KM_PER_DEGREE_LONGITUDE_AT_EQUATOR * cos(centerLat * DEGREES_TO_RADIANS))

        // Ensure longitude delta is valid (avoid division issues near poles)
        if (!lonDelta.isFinite() || lonDelta <= 0) {
            log("[GeoUtils] Invalid longitude delta: $lonDelta at latitude $centerLat")
            return@runCatching null
        }

        BoundingBox(
            southwest = LatLng(
                latitude = (centerLat - latDelta).coerceIn(MIN_LATITUDE, MAX_LATITUDE),
                longitude = (centerLon - lonDelta).coerceIn(MIN_LONGITUDE, MAX_LONGITUDE),
            ),
            northeast = LatLng(
                latitude = (centerLat + latDelta).coerceIn(MIN_LATITUDE, MAX_LATITUDE),
                longitude = (centerLon + lonDelta).coerceIn(MIN_LONGITUDE, MAX_LONGITUDE),
            ),
        )
    }.onFailure { error ->
        log("[GeoUtils] Error calculating bounding box: ${error.message}")
    }.getOrNull()

    /**
     * Calculate distance between two points using Haversine formula.
     * Used for validation and testing.
     *
     * @return Distance in kilometers, or null if calculation fails
     */
    fun haversineDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double? = runCatching {
        // Validate inputs
        require(lat1 in MIN_LATITUDE..MAX_LATITUDE) {
            "Invalid latitude1: $lat1. Must be between $MIN_LATITUDE and $MAX_LATITUDE degrees."
        }
        require(lat2 in MIN_LATITUDE..MAX_LATITUDE) {
            "Invalid latitude2: $lat2. Must be between $MIN_LATITUDE and $MAX_LATITUDE degrees."
        }
        require(lon1 in MIN_LONGITUDE..MAX_LONGITUDE) {
            "Invalid longitude1: $lon1. Must be between $MIN_LONGITUDE and $MAX_LONGITUDE degrees."
        }
        require(lon2 in MIN_LONGITUDE..MAX_LONGITUDE) {
            "Invalid longitude2: $lon2. Must be between $MIN_LONGITUDE and $MAX_LONGITUDE degrees."
        }

        val dLat = (lat2 - lat1) * DEGREES_TO_RADIANS
        val dLon = (lon2 - lon1) * DEGREES_TO_RADIANS

        val a = sin(dLat / 2).pow(2) +
            cos(lat1 * DEGREES_TO_RADIANS) * cos(lat2 * DEGREES_TO_RADIANS) *
            sin(dLon / 2).pow(2)

        val c = 2 * asin(sqrt(a))
        val distance = EARTH_RADIUS_KM * c

        // Ensure result is valid
        if (!distance.isFinite() || distance < 0) {
            log("[GeoUtils] Invalid distance calculated: $distance")
            return@runCatching null
        }

        distance
    }.onFailure { error ->
        log("[GeoUtils] Error calculating haversine distance: ${error.message}")
    }.getOrNull()
}
