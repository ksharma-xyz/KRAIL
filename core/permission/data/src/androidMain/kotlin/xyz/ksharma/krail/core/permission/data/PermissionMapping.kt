package xyz.ksharma.krail.core.permission.data

import android.Manifest
import android.os.Build
import xyz.ksharma.krail.core.permission.LocationPermissionType

/**
 * Convert [xyz.ksharma.krail.core.permission.LocationPermissionType] to Android manifest permission strings.
 *
 * Handles SDK version differences for location permissions.
 */
internal fun LocationPermissionType.toAndroidPermissions(): List<String> {
    return when (this) {
        LocationPermissionType.LOCATION_WHEN_IN_USE -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ requires requesting both FINE and COARSE together
                listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            } else {
                listOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        LocationPermissionType.COARSE_LOCATION -> {
            listOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }
}

