package xyz.ksharma.krail.core.permission.data

import xyz.ksharma.krail.core.permission.LocationPermissionType
import xyz.ksharma.krail.core.permission.PermissionResult
import xyz.ksharma.krail.core.permission.PermissionStatus

/**
 * Controller for managing permissions across platforms.
 *
 * This interface provides a unified API for requesting and checking permissions
 * on both Android and iOS.
 */
interface PermissionController {
    /**
     * Request a permission from the user.
     *
     * This will show the system permission dialog if the permission has not been
     * determined yet. If the permission was previously denied permanently,
     * this will return [xyz.ksharma.krail.core.permission.PermissionResult.Denied] with isPermanent=true.
     *
     * @param type The type of permission to request.
     * @return The result of the permission request.
     */
    suspend fun requestPermission(type: LocationPermissionType): PermissionResult

    /**
     * Check the current status of a permission without requesting it.
     *
     * @param type The type of permission to check.
     * @return The current status of the permission.
     */
    suspend fun checkPermissionStatus(type: LocationPermissionType): PermissionStatus

    /**
     * Check if a permission was requested before in this session.
     *
     * This uses in-memory tracking and will reset when the app restarts.
     *
     * @param type The type of permission to check.
     * @return true if the permission was requested in this session, false otherwise.
     */
    fun wasPermissionRequested(type: LocationPermissionType): Boolean

    /**
     * Open the app's settings page where the user can manually grant permissions.
     *
     * This should be called when a permission is permanently denied and the user
     * needs to grant it manually.
     */
    fun openAppSettings()
}

