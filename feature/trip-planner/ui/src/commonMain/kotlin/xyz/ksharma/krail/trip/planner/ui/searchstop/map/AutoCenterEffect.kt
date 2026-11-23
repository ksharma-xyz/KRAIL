package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.maps.state.UserLocationConfig
import kotlin.time.Duration.Companion.milliseconds

/**
 * Auto-centers the camera on the first user location, exactly once, smoothly.
 *
 * CRITICAL: the effect is keyed on a STABLE boolean (hasUserLocation), NOT on the changing LatLng. GPS emits a stream
 * of slightly different fixes, and keying on the value re-launched the effect on every jitter, which CANCELLED the
 * in-flight animateTo. Because hasAutoCentered had already flipped true, it never fired again, so the camera was left
 * frozen wherever the cancelled animation stopped: at the Sydney seed, or (on iOS) at an intermediate zoomed all the
 * way out frame. Rotating "fixed" it only because rememberCameraState re-seeded firstPosition at the now known
 * location (the seed path, not this animate path). hasUserLocation flips from false to true once and then stays true,
 * so later jitter no longer re-keys the effect and the animation runs to completion.
 *
 * If location permission is absent, [userLocation] stays null, nothing animates, and the camera stays at the Sydney
 * default seed. Only when a real fix arrives does the camera move to it.
 */
@Composable
internal fun AutoCenterOnFirstFix(userLocation: LatLng?, cameraState: CameraState) {
    val hasUserLocation = userLocation != null
    // Saved, so turning the phone never glides the camera back from where the rider panned it.
    var hasAutoCentered by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(hasUserLocation) {
        val target = userLocation
        if (target != null && shouldAutoCenter(hasUserLocation, hasAutoCentered)) {
            hasAutoCentered = true
            cameraState.animateTo(
                CameraPosition(
                    target = target.toPosition(),
                    zoom = UserLocationConfig.AUTO_CENTER_ZOOM,
                ),
                duration = UserLocationConfig.AUTO_CENTER_ANIMATION_MS.milliseconds,
            )
        }
    }
}
