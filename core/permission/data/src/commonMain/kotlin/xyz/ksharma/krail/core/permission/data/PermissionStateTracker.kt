package xyz.ksharma.krail.core.permission.data

import xyz.ksharma.krail.core.permission.LocationPermissionType

/**
 * In-memory tracker for permission request history.
 *
 * This tracks which permissions have been requested during the current app session.
 * The tracking is reset when the app restarts.
 */
internal class PermissionStateTracker {
    private val requestedPermissions = mutableSetOf<LocationPermissionType>()

    /**
     * Mark a permission as having been requested.
     */
    fun markAsRequested(type: LocationPermissionType) {
        requestedPermissions.add(type)
    }

    /**
     * Check if a permission was requested in this session.
     */
    fun wasRequested(type: LocationPermissionType): Boolean {
        return type in requestedPermissions
    }

    /**
     * Clear all tracking data.
     */
    fun clear() {
        requestedPermissions.clear()
    }
}

