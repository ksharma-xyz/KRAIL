package xyz.ksharma.krail.core.maps.ui.utils

import org.maplibre.compose.camera.CameraState
import xyz.ksharma.krail.core.location.Location

/**
 * Controls map camera movements for user location features.
 *
 * Provides utilities for smooth camera animations and proximity checks.
 * Works with MapLibre CameraState.
 */
interface MapLocationController {
    /**
     * Move camera to user's location with smooth animation.
     *
     * @param location User's current location
     * @param cameraState MapLibre camera state to animate
     * @param zoom Target zoom level (default: 16.0 for street-level view)
     * @param animationDuration Duration in milliseconds (default: 1000ms)
     */
    suspend fun moveCameraToUserLocation(
        location: Location,
        cameraState: CameraState,
        zoom: Double = DEFAULT_USER_LOCATION_ZOOM,
        animationDuration: Long = 1000L
    )

    /**
     * Check if camera is already near user location.
     *
     * Useful to avoid re-centering if user manually moved map.
     *
     * @param location User's current location
     * @param cameraState Current camera state
     * @param thresholdMeters Distance threshold in meters (default: 500m)
     * @return true if camera is within threshold of location
     */
    fun isCameraNearLocation(
        location: Location,
        cameraState: CameraState,
        thresholdMeters: Double = DEFAULT_THRESHOLD_METERS
    ): Boolean

    companion object {
        /** Default zoom level for user location (street-level view) */
        const val DEFAULT_USER_LOCATION_ZOOM = 16.0

        /** Default threshold for proximity check (meters) */
        const val DEFAULT_THRESHOLD_METERS = 500.0
    }
}

