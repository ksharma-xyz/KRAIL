package xyz.ksharma.krail.core.maps.data.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import xyz.ksharma.aagya.permission.data.rememberPermissionController
import xyz.ksharma.dhruva.location.data.rememberLocationTracker

/**
 * Remember a [UserLocationManager] instance for the current composition.
 *
 * Wires [rememberPermissionController] and [rememberLocationTracker] together
 * so callers only depend on [UserLocationManager].
 */
@Composable
fun rememberUserLocationManager(): UserLocationManager {
    val permissionController = rememberPermissionController()
    val locationTracker = rememberLocationTracker()
    return remember(permissionController, locationTracker) {
        UserLocationManagerImpl(permissionController, locationTracker)
    }
}
