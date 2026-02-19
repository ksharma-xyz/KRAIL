package xyz.ksharma.krail.core.maps.data.location

import kotlinx.coroutines.flow.Flow
import xyz.ksharma.krail.core.location.Location
import xyz.ksharma.krail.core.location.LocationConfig
import xyz.ksharma.krail.core.location.data.LocationTracker
import xyz.ksharma.krail.core.permission.data.PermissionController
import xyz.ksharma.krail.core.permission.PermissionStatus

/**
 * Manages user location access with permission handling.
 *
 * Combines [PermissionController] + [LocationTracker] for map use cases.
 * Handles the complete flow: check permission → request if needed → get location.
 */
interface UserLocationManager {
    /**
     * Get current user location with automatic permission handling.
     *
     * Flow:
     * 1. Checks if permission is already granted
     * 2. If not granted, requests permission from user
     * 3. If granted, fetches current location
     *
     * @return Result.success(Location) if successful, Result.failure with appropriate error otherwise
     */
    suspend fun getCurrentLocation(): Result<Location>

    /**
     * Continuous location updates as a Flow.
     *
     * Requests permission automatically if not yet determined.
     * Throws [xyz.ksharma.krail.core.location.LocationError.PermissionDenied] if denied.
     *
     * Cancelling the flow stops all location updates — this happens automatically
     * when collected inside a [androidx.compose.runtime.LaunchedEffect] that leaves composition.
     *
     * @param config Controls update interval and accuracy. Defaults to high-accuracy updates.
     */
    fun locationUpdates(config: LocationConfig = LocationConfig()): Flow<Location>

    /**
     * Check current location permission status without requesting.
     *
     * @return Current [PermissionStatus]
     */
    suspend fun checkPermissionStatus(): PermissionStatus

    /**
     * Open app settings page where user can manually grant location permission.
     *
     * Call this when permission is permanently denied.
     */
    fun openAppSettings()
}

/**
 * Factory for [UserLocationManager].
 * Called from a Composable context (rememberUserLocationManager in the feature layer).
 */
fun createUserLocationManager(
    permissionController: PermissionController,
    locationTracker: LocationTracker,
): UserLocationManager = UserLocationManagerImpl(permissionController, locationTracker)
