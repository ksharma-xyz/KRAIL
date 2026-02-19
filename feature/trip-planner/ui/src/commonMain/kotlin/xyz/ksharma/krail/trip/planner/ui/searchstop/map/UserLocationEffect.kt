package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.LifecycleStartEffect
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.spatialk.geojson.Position
import xyz.ksharma.krail.core.location.Location
import xyz.ksharma.krail.core.location.LocationConfig
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.data.location.UserLocationManager
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.maps.state.UserLocationConfig
import xyz.ksharma.krail.core.permission.PermissionStatus

/**
 * Lifecycle-aware side-effect that tracks the user's location while the screen is visible.
 *
 * - Starts on ON_START (re-checks permission on every screen return, e.g. after visiting Settings).
 * - Stops on ON_STOP (screen goes to background or user navigates away).
 * - Auto-centers the camera on the first location fix; subsequent fixes only update the dot.
 */
@Composable
internal fun TrackUserLocation(
    userLocationManager: UserLocationManager,
    cameraState: CameraState,
    onLocationUpdate: (LatLng) -> Unit,
    onPermissionDenied: (PermissionStatus) -> Unit,
) {
    val scope = rememberCoroutineScope()

    LifecycleStartEffect(Unit) {
        var hasAutoCentered = false

        val trackingJob = scope.launch {
            log("[USER_LOCATION] Starting location tracking")
            userLocationManager.locationUpdates(
                config = LocationConfig(updateIntervalMs = UserLocationConfig.UPDATE_INTERVAL_MS),
            )
                .catch { error ->
                    log("[USER_LOCATION] Location updates stopped: ${error.message}")
                    val status = userLocationManager.checkPermissionStatus()
                    if (status is PermissionStatus.Denied) onPermissionDenied(status)
                }
                .collect { location ->
                    log("[USER_LOCATION] Location update: loc=${location}")
                    onLocationUpdate(LatLng(location.latitude, location.longitude))
                    if (!hasAutoCentered) {
                        hasAutoCentered = true
                        cameraState.animateTo(
                            CameraPosition(
                                target = location.toPosition(),
                                zoom = UserLocationConfig.AUTO_CENTER_ZOOM,
                            ),
                            duration = UserLocationConfig.AUTO_CENTER_ANIMATION_MS.milliseconds,
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

private fun Location.toPosition(): Position = Position(latitude = latitude, longitude = longitude)
