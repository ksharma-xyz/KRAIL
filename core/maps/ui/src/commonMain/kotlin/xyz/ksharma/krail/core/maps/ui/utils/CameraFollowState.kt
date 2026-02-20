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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
 * [isProgrammaticMove] suppresses [manualPan] emissions during our own [animateTo] calls so
 * that programmatic camera moves don't inadvertently stop follow mode.
 */
@Stable
class CameraFollowState(
    private val cameraState: CameraState,
    private val scope: CoroutineScope,
) {
    var isFollowing: Boolean by mutableStateOf(false)
        private set

    // True while we are animating programmatically — suppresses manual-pan detection.
    private var isProgrammaticMove: Boolean by mutableStateOf(false)

    /**
     * Emits [Unit] whenever the user manually pans the map (i.e. a camera target change that is
     * NOT caused by one of our own [animateTo] calls).
     */
    val manualPan: Flow<Unit> = snapshotFlow { cameraState.position.target }
        .distinctUntilChanged()
        .filter { !isProgrammaticMove }
        .map { }

    fun startFollowing() {
        isFollowing = true
    }

    fun stopFollowing() {
        isFollowing = false
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
            isProgrammaticMove = true
            try {
                cameraState.animateTo(
                    CameraPosition(
                        target = Position(latitude = latLng.latitude, longitude = latLng.longitude),
                        zoom = zoom ?: cameraState.position.zoom,
                    ),
                    duration = durationMs.milliseconds,
                )
            } finally {
                isProgrammaticMove = false
            }
        }
    }
}

@Composable
fun rememberCameraFollowState(cameraState: CameraState): CameraFollowState {
    val scope = rememberCoroutineScope()
    return remember(cameraState) { CameraFollowState(cameraState, scope) }
}
