package xyz.ksharma.krail.core.permission.data

import androidx.compose.runtime.Composable

/**
 * Remember a [PermissionController] instance for the current composition.
 *
 * On Android, this binds to the current ComponentActivity and handles lifecycle automatically.
 * On iOS, this creates a controller that uses CLLocationManager.
 *
 * @return A platform-specific [PermissionController] instance.
 */
@Composable
expect fun rememberPermissionController(): PermissionController
