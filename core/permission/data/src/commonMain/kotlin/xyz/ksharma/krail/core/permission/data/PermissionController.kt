package xyz.ksharma.krail.core.permission.data

import xyz.ksharma.krail.core.permission.AppPermission
import xyz.ksharma.krail.core.permission.PermissionResult
import xyz.ksharma.krail.core.permission.PermissionStatus

/**
 * Controller for managing permissions across platforms.
 *
 * This interface provides a unified API for requesting and checking permissions
 * on both Android and iOS. It is generic over [AppPermission] so new permission
 * types (camera, microphone, etc.) can be added without changing this interface.
 */
interface PermissionController {
    /**
     * Request a permission from the user.
     *
     * Shows the system dialog if not yet determined. If previously denied permanently,
     * returns [PermissionResult.Denied] with isPermanent=true without showing a dialog.
     *
     * @param permission The permission to request.
     * @return The result of the permission request.
     */
    suspend fun requestPermission(permission: AppPermission): PermissionResult

    /**
     * Check the current status of a permission without requesting it.
     *
     * @param permission The permission to check.
     * @return The current status of the permission.
     */
    suspend fun checkPermissionStatus(permission: AppPermission): PermissionStatus

    /**
     * Returns true if [permission] was requested at least once in this session.
     *
     * Uses in-memory tracking — resets on app restart.
     *
     * @param permission The permission to check.
     */
    fun wasPermissionRequested(permission: AppPermission): Boolean

    /**
     * Open the app's settings page where the user can manually grant permissions.
     *
     * Call this when a permission is permanently denied.
     */
    fun openAppSettings()
}
