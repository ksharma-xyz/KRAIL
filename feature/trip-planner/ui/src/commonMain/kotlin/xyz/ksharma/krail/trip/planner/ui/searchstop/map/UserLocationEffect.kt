package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.LifecycleStartEffect
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.dhruva.location.LocationConfig
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.data.location.UserLocationManager
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.maps.state.UserLocationConfig

/**
 * Lifecycle-aware side-effect that tracks the user's location while the screen is visible.
 *
 * - Starts on ON_START (re-checks permission on every screen return, e.g. after visiting Settings).
 * - Stops on ON_STOP (screen goes to background or user navigates away).
 *
 * Permission behaviour:
 * - If permission is already [PermissionStatus.Granted]: starts tracking immediately.
 * - If permission is [PermissionStatus.Denied]: calls [onPermissionDeny], no request.
 * - If permission is not yet determined / temporarily denied AND [allowPermissionRequest] is false:
 *   does nothing — waits for the user to tap the location button.
 * - If permission is not yet determined / temporarily denied AND [allowPermissionRequest] is true:
 *   calls [userLocationManager.locationUpdates] which triggers the system permission dialog.
 *   This should only be true after an explicit user action (location button tap).
 *
 * **Camera control is intentionally not handled here.** Animating the camera from inside the
 * `collect { ... }` is fragile across lifecycle bounces — the iOS permission dialog can
 * trigger ON_STOP/ON_START, cancelling the in-flight tracking job and dropping the closure
 * state (`hasAutoCentered`) before the first fix lands. Instead, screens drive the camera
 * from a `LaunchedEffect` keyed on the resulting user-location state, which is recomposition-
 * aware and survives the lifecycle bounce.
 */
@Composable
internal fun TrackUserLocation(
    userLocationManager: UserLocationManager,
    onLocationUpdate: (LatLng) -> Unit,
    onPermissionDeny: (PermissionStatus) -> Unit,
    allowPermissionRequest: Boolean,
) {
    val scope = rememberCoroutineScope()

    LifecycleStartEffect(allowPermissionRequest) {
        val trackingJob = scope.launch {
            val status = userLocationManager.checkPermissionStatus()
            when {
                status is PermissionStatus.Denied -> {
                    log("[USER_LOCATION] Permission denied, showing banner")
                    onPermissionDeny(status)
                    return@launch
                }
                // Only relevant on Android — iOS knows natively via CLAuthorizationStatus.notDetermined.
                !allowPermissionRequest && status !is PermissionStatus.Granted -> {
                    log("[USER_LOCATION] Permission not granted; awaiting explicit user action")
                    return@launch
                }
            }

            log("[USER_LOCATION] Starting tracking (status=$status, allowRequest=$allowPermissionRequest)")
            userLocationManager.locationUpdates(
                config = LocationConfig(updateIntervalMs = UserLocationConfig.UPDATE_INTERVAL_MS),
            )
                .catch { error ->
                    log("[USER_LOCATION] Location updates stopped: ${error.message}")
                    val currentStatus = userLocationManager.checkPermissionStatus()
                    if (currentStatus is PermissionStatus.Denied) onPermissionDeny(currentStatus)
                }
                .collect { location ->
                    log("[USER_LOCATION] Location update: loc=$location")
                    onLocationUpdate(LatLng(location.latitude, location.longitude))
                }
        }

        onStopOrDispose {
            log("[USER_LOCATION] Stopping tracking (screen not visible)")
            trackingJob.cancel()
        }
    }
}
