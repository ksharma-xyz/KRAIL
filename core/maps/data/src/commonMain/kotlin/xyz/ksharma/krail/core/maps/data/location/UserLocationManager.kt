package xyz.ksharma.krail.core.maps.data.location

import kotlinx.coroutines.flow.Flow
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.dhruva.location.Location
import xyz.ksharma.dhruva.location.LocationConfig
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.PermissionResultEvent

/**
 * Manages user location access with permission handling.
 *
 * Handles the complete flow: check permission → request if needed → get location.
 */
interface UserLocationManager {
    /**
     * Get current user location with automatic permission handling.
     *
     * Flow:
     * 1. Checks if permission is already granted
     * 2. If never asked, requests permission from user
     * 3. If granted, fetches current location
     *
     * @param source The place in the app that wants it, reported with the answer when the rider is asked.
     * @return Result.success(Location) if successful, Result.failure with appropriate error otherwise
     */
    suspend fun getCurrentLocation(source: PermissionResultEvent.Source): Result<Location>

    /**
     * Continuous location updates as a Flow.
     *
     * Throws [xyz.ksharma.dhruva.location.LocationError.PermissionDenied] if denied.
     *
     * Cancelling the flow stops all location updates — this happens automatically
     * when collected inside a [androidx.compose.runtime.LaunchedEffect] that leaves composition.
     *
     * @param config Controls update interval and accuracy. Defaults to high-accuracy updates.
     * @param requestSource The place the rider asked from, when this may ask for the permission. Null never asks.
     */
    fun locationUpdates(
        config: LocationConfig = LocationConfig(),
        requestSource: PermissionResultEvent.Source? = null,
    ): Flow<Location>

    /**
     * Check current location permission status without requesting.
     *
     * @return Current [PermissionStatus]
     */
    suspend fun checkPermissionStatus(): PermissionStatus

    /**
     * [checkPermissionStatus] for analytics. A rider who was asked, did not allow it, and whom the system now
     * reports as never asked reads as denied. Nothing the rider sees uses this.
     */
    suspend fun reportedPermissionStatus(): PermissionStatus

    /**
     * Open app settings page where user can manually grant location permission.
     *
     * Call this when permission is permanently denied.
     */
    fun openAppSettings()
}
