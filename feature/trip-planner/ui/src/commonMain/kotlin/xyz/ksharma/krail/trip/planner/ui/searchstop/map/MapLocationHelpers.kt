package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.spatialk.geojson.Position
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.krail.core.maps.data.location.UserLocationManager
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.maps.state.NearbyStopsConfig
import xyz.ksharma.krail.core.maps.state.UserLocationConfig
import kotlin.time.Duration.Companion.milliseconds

/**
 * Location-button and camera-seeding behaviour shared by every full map surface (SearchStop,
 * the Park & Ride picker).
 *
 * Extracted from `SearchStopMap` so a second map cannot drift into a slightly different
 * permission flow or a different default camera — the behaviour riders see has to be the same
 * wherever a map appears.
 */
internal fun LatLng.toPosition(): Position = Position(latitude = latitude, longitude = longitude)

/**
 * Recentres on a known fix, otherwise routes into the permission flow: a hard denial goes to
 * system settings, anything else asks.
 */
internal suspend fun handleLocationButtonClick(
    userLoc: LatLng?,
    cameraState: CameraState,
    userLocationManager: UserLocationManager,
    onPermissionDenied: (PermissionStatus) -> Unit,
    onRequestPermission: () -> Unit,
) {
    if (userLoc != null) {
        cameraState.animateTo(
            CameraPosition(target = userLoc.toPosition(), zoom = UserLocationConfig.RECENTER_ZOOM),
            duration = UserLocationConfig.RECENTER_ANIMATION_MS.milliseconds,
        )
    } else {
        val status = userLocationManager.checkPermissionStatus()
        if (status is PermissionStatus.Denied) {
            onPermissionDenied(status)
        } else {
            onRequestPermission()
        }
    }
}

/**
 * Seeds the camera from a known location so re-entry after a composition bounce (dual-pane
 * layout shift, rotation) does not flash at the Sydney default.
 */
internal fun initialCameraPosition(knownLocation: LatLng?): CameraPosition =
    knownLocation?.let {
        CameraPosition(target = it.toPosition(), zoom = UserLocationConfig.AUTO_CENTER_ZOOM)
    } ?: CameraPosition(
        target = Position(
            latitude = NearbyStopsConfig.DEFAULT_CENTER_LAT,
            longitude = NearbyStopsConfig.DEFAULT_CENTER_LON,
        ),
        zoom = NearbyStopsConfig.DEFAULT_ZOOM,
    )
