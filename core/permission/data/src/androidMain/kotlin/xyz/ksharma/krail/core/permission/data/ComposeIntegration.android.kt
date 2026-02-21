package xyz.ksharma.krail.core.permission.data

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import org.koin.compose.koinInject
import xyz.ksharma.krail.sandook.SandookPreferences

/**
 * Android implementation of [rememberPermissionController].
 *
 * The launcher is registered via [rememberLauncherForActivityResult] (stable across rotation).
 * The controller itself is retained across rotations via [remember] without an activity key,
 * so the ask-once policy survives configuration changes.
 * [DisposableEffect] rebinds the controller to the new activity after rotation.
 */
@Composable
actual fun rememberPermissionController(): PermissionController {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
        ?: error("PermissionController requires ComponentActivity.")
    val sandookPreferences: SandookPreferences = koinInject()

    // Mutable state so the launcher callback can be updated without recreating the controller.
    var permissionCallback by remember { mutableStateOf<((Map<String, Boolean>) -> Unit)?>(null) }

    // rememberLauncherForActivityResult returns the same stable wrapper across recompositions.
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        permissionCallback?.invoke(permissions)
        permissionCallback = null
    }

    // No activity key — controller is retained across rotations so permission state is preserved.
    val controller = remember(launcher) {
        AndroidPermissionController(
            context = context.applicationContext,
            launcher = launcher,
            setLauncherCallback = { callback -> permissionCallback = callback },
            sandookPreferences = sandookPreferences,
        )
    }

    // Rebind to the new activity after rotation.
    // bind() is a no-op if already bound to the same activity; clears on onDestroy.
    DisposableEffect(activity) {
        controller.bind(activity)
        onDispose { }
    }

    return controller
}
