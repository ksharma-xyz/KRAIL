package xyz.ksharma.krail.core.permission.data

import android.Manifest
import android.os.Build
import xyz.ksharma.krail.core.permission.AppPermission

/**
 * Convert an [AppPermission] to the Android manifest permission strings needed to request it.
 *
 * Handles SDK version differences (e.g. Android 12+ requires FINE + COARSE together).
 */
internal fun AppPermission.toAndroidPermissions(): List<String> = when (this) {
    is AppPermission.Location.WhenInUse -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requires requesting both FINE and COARSE together
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    is AppPermission.Location.Coarse -> listOf(Manifest.permission.ACCESS_COARSE_LOCATION)
}
