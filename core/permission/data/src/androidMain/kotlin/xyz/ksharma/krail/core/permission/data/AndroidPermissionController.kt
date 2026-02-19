package xyz.ksharma.krail.core.permission.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import xyz.ksharma.krail.core.permission.LocationPermissionType
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

    override suspend fun requestPermission(type: LocationPermissionType): PermissionResult {
        val permissions = type.toAndroidPermissions()

        // Check if already granted
        val allGranted = permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            stateTracker.markAsRequested(type)
            return PermissionResult.Granted
        }

        // Check if permanently denied
        val activity = boundActivity
        if (activity != null) {
            val anyPermanentlyDenied = permissions.any { permission ->
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) &&
                    ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED &&
                    stateTracker.wasRequested(type)
            }

            if (anyPermanentlyDenied) {
                return PermissionResult.Denied(isPermanent = true)
            }
        }

        // Mark as requested before launching
        stateTracker.markAsRequested(type)

        // Request permission
        return suspendCancellableCoroutine { continuation ->
            val callback: (Map<String, Boolean>) -> Unit = { results ->
                val allGranted = results.values.all { it }

                when {
                    allGranted -> {
                        continuation.resume(PermissionResult.Granted)
                    }
                    results.isEmpty() -> {
                        // User dismissed dialog without choosing
                        continuation.resume(PermissionResult.Cancelled)
                    }
                    else -> {
                        // Check if permanently denied
                        val activity = boundActivity
                        val isPermanent = if (activity != null) {
                            permissions.any { permission ->
                                !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) &&
                                    results[permission] == false
                            }
                        } else {
                            false
                        }

                        continuation.resume(PermissionResult.Denied(isPermanent = isPermanent))
                    }
                }
            }

            // Set the callback for the launcher
            setLauncherCallback(callback)

            continuation.invokeOnCancellation {
                // Callback will be cleared when result arrives
            }

            launcher.launch(permissions.toTypedArray())
        }
    }

    override suspend fun checkPermissionStatus(type: LocationPermissionType): PermissionStatus {
        val permissions = type.toAndroidPermissions()

        val allGranted = permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) return PermissionStatus.Granted

        // TODO: Persist the request count across sessions (e.g. DataStore) so the ask-once
        //       policy survives app restarts. Currently resets to NotDetermined on restart.
        // Ask-once policy: if we already asked and user denied, treat as Permanent so the
        // caller directs the user to Settings — matching iOS behaviour.
        if (stateTracker.wasRequested(type)) {
            return PermissionStatus.Denied.Permanent
        }

        return PermissionStatus.NotDetermined
    }

    override fun wasPermissionRequested(type: LocationPermissionType): Boolean {
        return stateTracker.wasRequested(type)
    }

    override fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
