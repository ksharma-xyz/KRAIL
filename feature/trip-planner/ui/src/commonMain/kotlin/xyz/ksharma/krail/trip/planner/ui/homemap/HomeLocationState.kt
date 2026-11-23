package xyz.ksharma.krail.trip.planner.ui.homemap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleStartEffect
import kotlinx.coroutines.launch
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.krail.core.maps.data.location.UserLocationManager
import xyz.ksharma.krail.trip.planner.ui.searchstop.LocationPill
import xyz.ksharma.krail.trip.planner.ui.searchstop.locationPillFor

/**
 * What the home location pill says and does: the permission, looked up on every return to the screen, and
 * the two things a tap can do, which are ask the system or open Settings.
 *
 * [allowPermissionRequest] is false until the rider taps the pill, the only moment the map's tracking may
 * raise the system dialog, so home never asks for location unprompted. It is the same contract Search Stop's
 * pill keeps (`searchstop/SearchStopScreen.kt`), and a tap raises no analytics event of its own.
 */
@Immutable
internal class HomeLocationState(
    val pill: LocationPill,
    val allowPermissionRequest: Boolean,
    val onPermissionStatus: (PermissionStatus) -> Unit,
    val onPillClick: () -> Unit,
)

@Composable
internal fun rememberHomeLocationState(
    userLocationManager: UserLocationManager,
    hasUserLocation: Boolean,
): HomeLocationState {
    val scope = rememberCoroutineScope()
    // Null until it has been looked up, so the pill never flashes in on a rider who already said yes.
    var status by remember { mutableStateOf<PermissionStatus?>(null) }
    var allowPermissionRequest by remember { mutableStateOf(false) }
    val pill = locationPillFor(status, hasUserLocation)

    // Looked up again on every return to the screen, so coming back from Settings with the permission
    // granted drops the pill at once. It stops with the screen, so nothing runs while home is behind.
    LifecycleStartEffect(userLocationManager) {
        val job = scope.launch { status = userLocationManager.checkPermissionStatus() }
        onStopOrDispose { job.cancel() }
    }

    return HomeLocationState(
        pill = pill,
        allowPermissionRequest = allowPermissionRequest,
        onPermissionStatus = { status = it },
        onPillClick = {
            when (pill) {
                LocationPill.Ask -> allowPermissionRequest = true
                LocationPill.OpenSettings -> userLocationManager.openAppSettings()
                LocationPill.Hidden -> Unit
            }
        },
    )
}
