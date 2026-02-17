package xyz.ksharma.krail.core.maps.ui.utils

import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.spatialk.geojson.Position
import xyz.ksharma.krail.core.location.Location
import kotlin.math.*
import kotlin.time.Duration.Companion.milliseconds

/**
 * Implementation of MapLocationController.
 *
 * Provides camera control utilities for user location on maps.
 */
class MapLocationControllerImpl : MapLocationController {

    override suspend fun moveCameraToUserLocation(
        location: Location,
        cameraState: CameraState,
        zoom: Double,
        animationDuration: Long
    ) {
        // Create new camera position targeting user location
        val newPosition = CameraPosition(
            target = Position(
                latitude = location.latitude,
                longitude = location.longitude
            ),
            zoom = zoom
        )

        // Animate to new position with duration
        cameraState.animateTo(newPosition, duration = animationDuration.milliseconds)
    }

    override fun isCameraNearLocation(
        location: Location,
        cameraState: CameraState,
        thresholdMeters: Double
    ): Boolean {
        val currentPosition = cameraState.position.target

        val distance = calculateDistance(
            lat1 = currentPosition.latitude,
            lon1 = currentPosition.longitude,
            lat2 = location.latitude,
            lon2 = location.longitude
        )

        return distance <= thresholdMeters
    }

    /**
     * Calculate distance between two coordinates using Haversine formula.
     *
     * @return Distance in meters
     */
    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadius = 6371000.0 // Earth radius in meters

        val dLat = (lat2 - lat1).toRadians()
        val dLon = (lon2 - lon1).toRadians()

        val a = sin(dLat / 2).pow(2) +
                cos(lat1.toRadians()) * cos(lat2.toRadians()) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    /**
     * Convert degrees to radians.
     */
    private fun Double.toRadians(): Double = this * PI / 180.0
}

/**
 * Create a MapLocationController instance.
 */
fun MapLocationController(): MapLocationController = MapLocationControllerImpl()

