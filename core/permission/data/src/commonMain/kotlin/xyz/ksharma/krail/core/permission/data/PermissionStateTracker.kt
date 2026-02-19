package xyz.ksharma.krail.core.permission.data

import xyz.ksharma.krail.core.permission.AppPermission

/**
 * In-memory tracker for permission request history.
 *
 * Tracks which [AppPermission]s have been requested during the current session.
 * Resets on app restart.
 */
internal class PermissionStateTracker {
    private val requestedPermissions = mutableSetOf<AppPermission>()

    /** Mark [permission] as having been requested. */
    fun markAsRequested(permission: AppPermission) {
        requestedPermissions.add(permission)
    }

    /** Returns true if [permission] was requested in this session. */
    fun wasRequested(permission: AppPermission): Boolean = permission in requestedPermissions

    /** Clear all tracking data. */
    fun clear() {
        requestedPermissions.clear()
    }
}
