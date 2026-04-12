package xyz.ksharma.krail.core.maps.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraMoveReason
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.spatialk.geojson.Position
import xyz.ksharma.krail.core.maps.state.LatLng
import kotlin.time.Duration.Companion.milliseconds

/**
 * Manages camera follow-mode state for a map.
 *
 * When [isFollowing] is true, the camera animates to every incoming location update.
 * Follow mode is disengaged automatically when the user manually pans the map, detected
 * via [manualPan].
 *
 * [manualPan] uses MapLibre's own [CameraMoveReason] to distinguish user gestures from
 * programmatic animations — this avoids race conditions that occur when multiple animations
 * overlap and a shared boolean flag gets reset prematurely.
 */
@Stable
class CameraFollowState(
    private val cameraState: CameraState,
    private val scope: CoroutineScope,
) {
    var isFollowing: Boolean by mutableStateOf(false)
        private set

    /**
     * True while a recenter animation is in progress (camera moving *toward* the dot).
     * Cleared automatically when [animateTo] completes so the dot switches to camera-mirror mode.
     */
    var isRecentering: Boolean by mutableStateOf(false)
        private set

    /**
     * Emits [Unit] whenever the user manually pans the map.
     * Uses [CameraMoveReason.GESTURE] from MapLibre so that our own programmatic animations
     * never falsely trigger this — even when two animations overlap.
     */
    val manualPan: Flow<Unit> = snapshotFlow { cameraState.moveReason }
        .filter { it == CameraMoveReason.GESTURE }
        .map { }

    fun startFollowing() {
        isFollowing = true
        isRecentering = true
    }

    fun stopFollowing() {
        isFollowing = false
        isRecentering = false
    }

    /**
     * Animates the camera to [latLng].
     *
     * @param zoom Target zoom level. When null the current zoom is preserved — useful for
     *   follow-mode updates where the user may have zoomed in/out and we should respect that.
     * @param durationMs Animation duration in milliseconds.
     */
    fun animateTo(latLng: LatLng, zoom: Double? = null, durationMs: Long = 1_000L) {
        scope.launch {
            cameraState.animateTo(
                CameraPosition(
                    target = Position(latitude = latLng.latitude, longitude = latLng.longitude),
                    zoom = zoom ?: cameraState.position.zoom,
                ),
                duration = durationMs.milliseconds,
            )
            // Camera has arrived at the target. Clear recentering so the dot switches to
            // camera-mirror mode for subsequent GPS-driven animations.
            isRecentering = false
        }
    }
}

@Composable
fun rememberCameraFollowState(cameraState: CameraState): CameraFollowState {
    val scope = rememberCoroutineScope()
    return remember(cameraState) { CameraFollowState(cameraState, scope) }
}

/**
 * Returns the position to display for the user location dot.
 *
 * - **Not following**: returns [userLocation] so the dot stays on the last GPS fix.
 * - **Following, recentering**: returns [userLocation]. The camera is moving *toward* the dot
 *   (recenter animation), so the dot should stay put — not jump to the camera's current position.
 * - **Following, not recentering**: mirrors the camera's animated target, keeping the dot
 *   frame-perfectly in sync with no vibration from two independent animation systems.
 *
 * Call this inside the [MaplibreMap] content lambda so that state reads are scoped to
 * the inner recomposition scope — only the map content recomposes on each animation frame,
 * not the entire screen composable.
 *
 * @param userLocation Last known GPS fix, or null if no fix has been received yet.
 * @param cameraState The map camera whose animated position is used in follow mode.
 */
@Composable
fun CameraFollowState.dotLocation(
    userLocation: LatLng?,
    cameraState: CameraState,
): LatLng? = when {
    userLocation == null -> null
    isFollowing && !isRecentering -> {
        val t = cameraState.position.target
        LatLng(latitude = t.latitude, longitude = t.longitude)
    }
    else -> userLocation
}
