package xyz.ksharma.krail.core.permission.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS implementation of [xyz.ksharma.krail.core.permission.data.rememberPermissionController].
 *
 * Creates a controller that uses CLLocationManager for permission handling.
 */
@Composable
actual fun rememberPermissionController(): PermissionController {
    return remember {
        IosPermissionController()
    }
}

