package xyz.ksharma.krail.core.permission.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import xyz.ksharma.krail.core.permission.AppPermission
import xyz.ksharma.krail.core.permission.PermissionResult
import xyz.ksharma.krail.core.permission.PermissionStatus
import xyz.ksharma.krail.sandook.SandookPreferences
import kotlin.coroutines.resume

/**
 * Android implementation of [PermissionController].
 *
 * Uses [ActivityResultContracts.RequestMultiplePermissions] to request permissions
 * and binds to the activity lifecycle to clean up resources.
 */
internal class AndroidPermissionController(
    private val context: Context,
    private val launcher: ActivityResultLauncher<Array<String>>,
    private val setLauncherCallback: ((Map<String, Boolean>) -> Unit) -> Unit,
    private val sandookPreferences: SandookPreferences,
) : PermissionController {

    private var boundActivity: ComponentActivity? = null

    /**
     * Bind this controller to a ComponentActivity.
     * Must be called before requesting permissions.
     */
    fun bind(activity: ComponentActivity) {
        if (boundActivity === activity) return // Already bound to this exact instance

        boundActivity = activity

        // Clears the reference when this activity is destroyed (e.g. rotation).
        // The next bind() call after rotation will supply the new activity instance.
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                boundActivity = null
            }
        })
    }

    override suspend fun requestPermission(permission: AppPermission): PermissionResult {
        val permissions = permission.toAndroidPermissions()
        resolveExistingStatus(permission, permissions)?.let { return it }
        sandookPreferences.setBoolean(permission.toPreferenceKey(), true)
        return suspendCancellableCoroutine { continuation ->
            val callback: (Map<String, Boolean>) -> Unit = { results ->
                val allGranted = results.values.all { it }

                when {
                    allGranted -> continuation.resume(PermissionResult.Granted)
                    results.isEmpty() -> continuation.resume(PermissionResult.Cancelled)
                    else -> continuation.resume(PermissionResult.Denied)
                }
            }

            setLauncherCallback(callback)
            continuation.invokeOnCancellation { }
            launcher.launch(permissions.toTypedArray())
        }
    }

    override suspend fun checkPermissionStatus(permission: AppPermission): PermissionStatus {
        val allGranted = permission.toAndroidPermissions().all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
        // Ask-once policy: asked and denied → Denied. Matches iOS which also allows only one
        // in-app request. The OS handles the "can ask again" state; we don't need to track it.
        // Persistent tracking via SandookPreferences survives app restarts.
        return when {
            allGranted -> PermissionStatus.Granted
            sandookPreferences.getBoolean(permission.toPreferenceKey()) == true -> PermissionStatus.Denied
            else -> PermissionStatus.NotDetermined
        }
    }

    override fun wasPermissionRequested(permission: AppPermission): Boolean =
        sandookPreferences.getBoolean(permission.toPreferenceKey()) == true

    override fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Checks the current system permission state before launching the OS dialog.
     * Returns a [PermissionResult] if the request can be short-circuited, or null
     * if the OS dialog should be shown.
     */
    private fun resolveExistingStatus(
        permission: AppPermission,
        permissions: List<String>,
    ): PermissionResult? {
        val allGranted = permissions.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            sandookPreferences.setBoolean(permission.toPreferenceKey(), true)
            return PermissionResult.Granted
        }
        // Ask-once policy: already denied → skip dialog, direct user to Settings.
        return if (sandookPreferences.getBoolean(permission.toPreferenceKey()) == true) {
            PermissionResult.Denied
        } else {
            null
        }
    }
}

private fun AppPermission.toPreferenceKey(): String = when (this) {
    is AppPermission.Location -> SandookPreferences.KEY_LOCATION_PERMISSION_EVER_REQUESTED
}
