package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LifecycleStartEffect
import kotlinx.coroutines.launch
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.IntroLetsKrailClickEvent.LocationResult
import xyz.ksharma.krail.core.maps.data.location.UserLocationManager

/**
 * Tells [mapOpen] when a map open starts and ends. With [mapOpen] null (a preview, a host that does not count) it
 * does nothing.
 *
 * The map is visible while the lifecycle is started and nothing [covered] it. That is one effect keyed on the
 * cover, so every way an open can end is the same `onStopOrDispose`:
 *
 * - the app leaves the foreground (ON_STOP), so a rider who quits on an empty map is counted;
 * - the cover goes up, which is Search Stop stepping from the map state to the list state;
 * - the map leaves the composition, which is back, or going on to another screen, or a stop being picked on a
 *   screen that then closes.
 *
 * Each of those ends the open at most once, because [MapOpenTracker.dismissed] ends only an open that exists. When
 * the lifecycle starts again, or the cover comes down, the effect starts again and begins a new open, which has a
 * new id. A stop being picked ends the open in the map itself ([MapOpenTracker.stopPicked]).
 *
 * A stop that [isChangingConfigurations] reports is the screen being rebuilt, as for a rotation. It ends nothing:
 * the open carries on into the new screen, which finds it already open and starts none.
 *
 * `map_viewed` waits for one permission lookup (a local call) so it can say what the permission was. A stop before
 * the lookup returns cancels it, so an open that was never reported is never dismissed.
 */
@Composable
internal fun MapOpenEffect(
    mapOpen: MapOpenTracker?,
    entry: MapEntry,
    covered: Boolean,
    hadUserLocation: Boolean,
    stopCount: Int,
    userLocationManager: UserLocationManager,
    isChangingConfigurations: () -> Boolean = rememberIsChangingConfigurations(),
) {
    if (mapOpen == null) return
    val scope = rememberCoroutineScope()
    val latestHadUserLocation by rememberUpdatedState(hadUserLocation)
    val latestStopCount by rememberUpdatedState(stopCount)
    val latestIsChanging by rememberUpdatedState(isChangingConfigurations)

    LaunchedEffect(mapOpen, stopCount) { mapOpen.stopsOnMap(stopCount) }

    LifecycleStartEffect(mapOpen, entry, covered, userLocationManager) {
        val lookup = if (covered) {
            null
        } else {
            scope.launch {
                val permission = userLocationManager.checkPermissionStatus().toMapLocationResult()
                mapOpen.viewed(
                    entry = entry,
                    hadUserLocation = latestHadUserLocation,
                    locationPermission = permission,
                    stopCount = latestStopCount,
                )
            }
        }
        onStopOrDispose {
            lookup?.cancel()
            if (!latestIsChanging()) mapOpen.dismissed()
        }
    }
}

/**
 * The permission as `map_viewed` reports it, in the Intro's `locationResult` words. A system that forbids location
 * (restricted) is unavailable: the rider was not asked and cannot be.
 */
internal fun PermissionStatus.toMapLocationResult(): LocationResult = when (this) {
    is PermissionStatus.Granted -> LocationResult.GRANTED
    is PermissionStatus.NotDetermined -> LocationResult.NOT_ASKED
    is PermissionStatus.Denied -> LocationResult.DENIED
    PermissionStatus.Restricted -> LocationResult.UNAVAILABLE
}
