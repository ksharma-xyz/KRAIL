package xyz.ksharma.krail.trip.planner.ui.journeymap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import xyz.ksharma.krail.core.maps.data.location.rememberUserLocationManager
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.maps.state.UserLocationConfig
import xyz.ksharma.krail.core.maps.ui.components.LocationPermissionBanner
import xyz.ksharma.krail.core.maps.ui.components.MapTimetableDataBadge
import xyz.ksharma.krail.core.maps.ui.components.UserLocationButton
import xyz.ksharma.krail.core.maps.ui.config.MapConfig
import xyz.ksharma.krail.core.maps.ui.config.MapTileProvider
import xyz.ksharma.krail.core.maps.ui.utils.MapCameraUtils
import xyz.ksharma.krail.core.permission.PermissionStatus
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.TrackUserLocation
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyMapUiState
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.StopType
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Main composable for displaying a journey on a map.
 * Shows routes as lines (dashed for walking, solid for transit) and stops as circles.
 *
 * Note: In normal flow, journeyMapState is always Ready since data is pre-loaded from TimeTable.
 * Loading/Error states exist as defensive fallbacks only.
 */
@Composable
fun JourneyMap(
    journeyMapState: JourneyMapUiState,
    modifier: Modifier = Modifier,
) {
    when (journeyMapState) {
        // Defensive fallback - data transformation is instant, users never see this
        JourneyMapUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }

        // Expected state - journey data is already loaded
        is JourneyMapUiState.Ready -> {
            JourneyMapContent(
                mapState = journeyMapState,
                modifier = modifier,
            )
        }
    }
}

/**
 * The actual map content when data is ready.
 */
@Composable
private fun JourneyMapContent(
    mapState: JourneyMapUiState.Ready,
    modifier: Modifier = Modifier,
) {
    // Track selected stop for details bottom sheet
    // Keyed to mapState so it resets when viewing a different journey
    var selectedStop by remember(mapState) { mutableStateOf<JourneyStopFeature?>(null) }

    // User location state
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var showPermissionBanner by remember { mutableStateOf(false) }
    var allowPermissionRequest by remember { mutableStateOf(false) }
    val userLocationManager = rememberUserLocationManager()
    val scope = rememberCoroutineScope()

    // Data freshness badge — survives rotation via rememberSaveable
    // baseMinutes captures the saved value at composition time so the tick
    // always adds fresh elapsed time on top of any previously accumulated minutes.
    var elapsedMinutes by rememberSaveable { mutableLongStateOf(0L) }
    val baseMinutes = remember { elapsedMinutes }
    val loadedAt = remember { TimeSource.Monotonic.markNow() }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30.seconds)
            elapsedMinutes = baseMinutes + loadedAt.elapsedNow().inWholeMinutes
        }
    }
    val isStale = elapsedMinutes >= 2
    val badgeText: String? = when {
        elapsedMinutes < 1 -> null
        elapsedMinutes < 5 -> "Updated $elapsedMinutes mins ago"
        else -> "Map showing scheduled times only."
    }

    val cameraPosition = remember(mapState.cameraFocus, mapState.mapDisplay.stops) {
        calculateInitialCameraPosition(mapState)
    }

    val cameraState = rememberCameraState(firstPosition = cameraPosition)

    TrackUserLocation(
        userLocationManager = userLocationManager,
        cameraState = cameraState,
        allowPermissionRequest = allowPermissionRequest,
        onLocationUpdate = { latLng ->
            showPermissionBanner = false
            userLocation = latLng
        },
        onPermissionDeny = {
            showPermissionBanner = true
        },
    )

    Box(modifier = modifier.fillMaxSize()) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            cameraState = cameraState,
            baseStyle = BaseStyle.Uri(MapTileProvider.DEFAULT),
            options = MapOptions(
                ornamentOptions = OrnamentOptions(
                    padding = PaddingValues(MapConfig.Ornaments.DEFAULT_PADDING_DP.dp),
                    isLogoEnabled = MapConfig.Ornaments.LOGO_ENABLED,
                    isAttributionEnabled = MapConfig.Ornaments.ATTRIBUTION_ENABLED,
                    attributionAlignment = Alignment.BottomEnd,
                    isCompassEnabled = MapConfig.Ornaments.COMPASS_ENABLED,
                    compassAlignment = Alignment.TopEnd,
                    isScaleBarEnabled = MapConfig.Ornaments.SCALE_BAR_ENABLED,
                ),
            ),
        ) {
            JourneyMapLayers(
                mapState = mapState,
                userLocation = userLocation,
                onStopSelect = { selectedStop = it },
            )
        }

        // User location button (bottom-end corner)
        UserLocationButton(
            onClick = {
                scope.launch {
                    val userLoc = userLocation
                    if (userLoc != null) {
                        // Re-center camera on latest known position
                        cameraState.animateTo(
                            CameraPosition(
                                target = Position(latitude = userLoc.latitude, longitude = userLoc.longitude),
                                zoom = UserLocationConfig.RECENTER_ZOOM,
                            ),
                            duration = UserLocationConfig.RECENTER_ANIMATION_MS.milliseconds,
                        )
                    } else {
                        val status = userLocationManager.checkPermissionStatus()
                        if (status is PermissionStatus.Denied) {
                            showPermissionBanner = true
                        } else {
                            // Trigger system permission dialog via TrackUserLocation
                            allowPermissionRequest = true
                        }
                    }
                }
            },
            isActive = userLocation != null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
        )

        // Top overlays — permission banner (if shown) then freshness badge directly below it
        if (showPermissionBanner || badgeText != null) {
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                if (showPermissionBanner) {
                    LocationPermissionBanner(
                        onGoToSettings = { userLocationManager.openAppSettings() },
                    )
                }
                badgeText?.let { text ->
                    MapTimetableDataBadge(
                        text = text,
                        isStale = isStale,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }

        // Stop Details Bottom Sheet
        selectedStop?.let { stop ->
            JourneyStopDetailsBottomSheet(
                stop = stop,
                onDismiss = { selectedStop = null },
            )
        }
    }
}

private fun calculateInitialCameraPosition(mapState: JourneyMapUiState.Ready): CameraPosition {
    val originStop = mapState.mapDisplay.stops.firstOrNull { it.stopType == StopType.ORIGIN }

    val target = originStop?.position?.let { origin ->
        Position(longitude = origin.longitude, latitude = origin.latitude)
    } ?: mapState.cameraFocus?.let { focus ->
        val center = MapCameraUtils.calculateCenter(focus.bounds)
        Position(longitude = center.longitude, latitude = center.latitude)
    } ?: Position(
        latitude = MapConfig.DefaultPosition.LATITUDE,
        longitude = MapConfig.DefaultPosition.LONGITUDE,
    )

    val zoom = mapState.cameraFocus?.let { focus ->
        MapCameraUtils.calculateZoomLevel(focus.bounds)
    } ?: MapConfig.DefaultPosition.ZOOM

    return CameraPosition(target = target, zoom = zoom)
}
