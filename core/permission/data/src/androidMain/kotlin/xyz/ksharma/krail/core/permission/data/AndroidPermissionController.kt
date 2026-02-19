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
) : PermissionController {

    private val stateTracker = PermissionStateTracker()
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
        stateTracker.markAsRequested(permission)
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
        // In-memory tracking (resets on restart) is intentional — no DataStore needed.
        return when {
            allGranted -> PermissionStatus.Granted
            stateTracker.wasRequested(permission) -> PermissionStatus.Denied
            else -> PermissionStatus.NotDetermined
        }
    }

    override fun wasPermissionRequested(permission: AppPermission): Boolean =
        stateTracker.wasRequested(permission)

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
            stateTracker.markAsRequested(permission)
            return PermissionResult.Granted
        }
        // Ask-once policy: already denied → skip dialog, direct user to Settings.
        return if (stateTracker.wasRequested(permission)) PermissionResult.Denied else null
    }
}
