package xyz.ksharma.krail.core.location.data

import kotlinx.coroutines.flow.Flow
import xyz.ksharma.krail.core.location.Location
import xyz.ksharma.krail.core.location.LocationConfig

/**
 * Interface for tracking device location.
 *
 * Provides both single location requests and continuous location tracking
 * for foreground use only (when app is visible).
 *
 * **Important**: Location permission must be granted before using this tracker.
 * Use [xyz.ksharma.krail.core.permission.PermissionController] to request permission first.
 */
interface LocationTracker {
    /**
     * Get a single location update.
     *
     * This method will attempt to get the device's current location and return
     * once a location is determined or the timeout expires.
     *
     * @param timeoutMs Maximum time to wait for location in milliseconds.
     *                  Default: 10 seconds
     * @return The current location
     * @throws xyz.ksharma.krail.core.location.LocationError.PermissionDenied if location permission is not granted
     * @throws xyz.ksharma.krail.core.location.LocationError.LocationDisabled if location services are disabled
     * @throws xyz.ksharma.krail.core.location.LocationError.Timeout if location couldn't be determined within timeout
     * @throws xyz.ksharma.krail.core.location.LocationError.Unknown for other errors
     */
    suspend fun getCurrentLocation(timeoutMs: Long = 10_000L): Location

    /**
     * Start continuous location tracking.
     *
     * Returns a Flow that emits location updates based on the provided configuration.
     * The Flow will continue emitting updates until cancelled or [stopTracking] is called.
     *
     * **Lifecycle**: On Android, tracking is automatically paused/resumed based on
     * activity lifecycle. On iOS, tracking stops when app enters background.
     *
     * @param config Configuration for location updates (interval, distance, priority)
     * @return Flow of location updates. Collect to receive updates.
     * @throws xyz.ksharma.krail.core.location.LocationError.PermissionDenied if location permission is not granted
     * @throws xyz.ksharma.krail.core.location.LocationError.LocationDisabled if location services are disabled
     * @throws xyz.ksharma.krail.core.location.LocationError.Unknown for other errors during tracking
     */
    fun startTracking(config: LocationConfig = LocationConfig()): Flow<Location>

    /**
     * Stop all active location tracking.
     *
     * Cancels any active location tracking started with [startTracking].
     * Safe to call even if tracking is not active.
     */
    fun stopTracking()

    /**
     * Check if location services are enabled on the device.
     *
     * @return true if location services are enabled, false otherwise
     */
    suspend fun isLocationEnabled(): Boolean
}
