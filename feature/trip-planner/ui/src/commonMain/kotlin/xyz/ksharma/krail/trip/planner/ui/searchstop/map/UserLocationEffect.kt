package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.LifecycleStartEffect
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.location.LocationConfig
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.data.location.UserLocationManager
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.maps.state.UserLocationConfig
import xyz.ksharma.krail.core.maps.ui.utils.CameraFollowState
import xyz.ksharma.krail.core.permission.PermissionStatus

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
 * @param userLocationManager Provides location updates and permission check/request APIs.
 * @param cameraFollowState Follow-mode camera state. Used to auto-center on first fix when
 *   [autoCenter] is true, and to track the user on every update when
 *   [CameraFollowState.isFollowing] is true.
 * @param onLocationUpdate Called on every location fix with the latest [LatLng]. Use this to update
 *   the user-location dot on the map or any other UI that depends on the current position.
 * @param onPermissionDeny Called when location permission is permanently denied so the screen can
 *   show a settings-redirect banner.
 * @param allowPermissionRequest When false the effect waits silently for an explicit user action
 *   (e.g. tapping the location button) before triggering the system permission dialog. Flip to true
 *   only after the user has intentionally requested their location.
 * @param autoCenter When true, animates the camera to the user's position on the first location fix.
 *   Set to false when the screen already has a meaningful starting position (e.g. journey origin)
 *   and the auto-pan would override it.
 */
@Composable
internal fun TrackUserLocation(
    userLocationManager: UserLocationManager,
    cameraFollowState: CameraFollowState,
    onLocationUpdate: (LatLng) -> Unit,
    onPermissionDeny: (PermissionStatus) -> Unit,
    allowPermissionRequest: Boolean,
    autoCenter: Boolean = true,
) {
    val scope = rememberCoroutineScope()

    LifecycleStartEffect(allowPermissionRequest) {
        var hasAutoCentered = false

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

            log("[USER_LOCATION] Starting location tracking (status=$status, allowRequest=$allowPermissionRequest)")
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
                    val latLng = LatLng(location.latitude, location.longitude)
                    onLocationUpdate(latLng)

                    if (autoCenter && !hasAutoCentered) {
                        // Auto-center on first fix and enter follow mode so the camera
                        // continues tracking the user on every subsequent GPS update.
                        hasAutoCentered = true
                        cameraFollowState.startFollowing()
                        cameraFollowState.animateTo(
                            latLng = latLng,
                            zoom = UserLocationConfig.AUTO_CENTER_ZOOM,
                            durationMs = UserLocationConfig.AUTO_CENTER_ANIMATION_MS,
                        )
                    } else if (cameraFollowState.isFollowing) {
                        // Follow mode: animate to new position, preserve user's current zoom.
                        cameraFollowState.animateTo(
                            latLng = latLng,
                            durationMs = UserLocationConfig.FOLLOW_ANIMATION_MS,
                        )
                    }
                }
        }

        onStopOrDispose {
            log("[USER_LOCATION] Stopping location tracking (screen not visible)")
            trackingJob.cancel()
        }
    }
}
