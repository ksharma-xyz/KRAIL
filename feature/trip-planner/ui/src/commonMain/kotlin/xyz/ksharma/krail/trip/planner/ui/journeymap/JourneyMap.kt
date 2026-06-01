package xyz.ksharma.krail.trip.planner.ui.journeymap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.layout.onSizeChanged
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
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.krail.core.maps.data.location.rememberUserLocationManager
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.maps.state.UserLocationConfig
import xyz.ksharma.krail.core.maps.ui.components.LocationPermissionBanner
import xyz.ksharma.krail.core.maps.ui.components.MapTimetableDataBadge
import xyz.ksharma.krail.core.maps.ui.components.UserLocationButton
import xyz.ksharma.krail.core.maps.ui.config.MapConfig
import xyz.ksharma.krail.core.maps.ui.config.MapTileProvider
import xyz.ksharma.krail.core.maps.ui.utils.MapCameraUtils
import xyz.ksharma.krail.taj.theme.KrailTheme
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
    showFreshnessBadge: Boolean = true,
    onLocationButtonClick: (isLocationActive: Boolean) -> Unit = {},
    onPermissionSettingsClick: () -> Unit = {},
    extraMapContent: @Composable () -> Unit = {},
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
                showFreshnessBadge = showFreshnessBadge,
                onLocationButtonClick = onLocationButtonClick,
                onPermissionSettingsClick = onPermissionSettingsClick,
                extraMapContent = extraMapContent,
                modifier = modifier,
            )
        }
    }
}

/**
 * The actual map content when data is ready.
 */
@Suppress("CyclomaticComplexMethod")
// Complexity is dominated by Compose state wiring (location/permission/freshness/camera effects
// + the map's stop and route layers); splitting it fragments shared map state across composables.
@Composable
private fun JourneyMapContent(
    mapState: JourneyMapUiState.Ready,
    modifier: Modifier = Modifier,
    showFreshnessBadge: Boolean = true,
    onLocationButtonClick: (isLocationActive: Boolean) -> Unit = {},
    onPermissionSettingsClick: () -> Unit = {},
    extraMapContent: @Composable () -> Unit = {},
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

    val initialCameraPosition = remember { calculateInitialCameraPosition(mapState) }
    val cameraState = rememberCameraState(firstPosition = initialCameraPosition)

    // Single source of truth for the camera target. ONE effect, so the journey-change case
    // and the empty-state user-location case can't race two competing animateTo calls (the
    // bug where the empty TimeTable map landed on Sydney sometimes and the user location other
    // times, depending on which effect won).
    //
    // Keyed on:
    //  - cameraFocus + stops → re-fit when the displayed journey changes.
    //  - hasUserLocation (a STABLE boolean, NOT the churning LatLng) → re-evaluate once a fix
    //    first arrives. Keying on the boolean (not the value) means GPS jitter can't re-launch
    //    and cancel the in-flight animateTo.
    //
    // Priority inside cameraTargetForState: a selected journey wins; otherwise (empty) it flies
    // to the user location if known, else the Sydney default. So with no location permission the
    // empty map stays at Sydney; with a fix it centres on the user — deterministically, no race.
    val hasUserLocation = userLocation != null
    LaunchedEffect(mapState.cameraFocus, mapState.mapDisplay.stops, hasUserLocation) {
        val target = cameraTargetForState(mapState, userLocation)
        cameraState.animateTo(target, duration = CAMERA_TRANSITION_MS.milliseconds)
    }

    TrackUserLocation(
        userLocationManager = userLocationManager,
        allowPermissionRequest = allowPermissionRequest,
        onLocationUpdate = { latLng ->
            showPermissionBanner = false
            userLocation = latLng
        },
        onPermissionDeny = {
            showPermissionBanner = true
        },
    )

    // Gate MaplibreMap creation until the container has a real non-zero size.
    // On iOS (dual-pane / rotation transients) instantiating it at a 0x0 frame makes the
    // native MLNMapView's Metal layer fail to allocate a drawable ("CAMetalLayer ... width=0" /
    // "nextDrawable returning nil") and the camera projects against a dead viewport, leaving it
    // zoomed all the way out (whole-of-Australia). Same gate as MapContent in SearchStopMap.
    //
    // LATCH the flag — never reset it to false. During a rotation the container momentarily
    // reports a 0 axis (the adaptive window flips through 840×0dp); tearing the map down on that
    // transient and recreating it leaves the pane blank until a SECOND rotation. Once we've seen
    // a valid size we keep the map mounted; MapLibre resizes its own surface as it settles.
    var hasHadValidSize by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                if (it.width > 0 && it.height > 0) hasHadValidSize = true
            },
    ) {
        if (hasHadValidSize) {
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
                extraMapContent()
            }
        }

        // User location button (bottom-end corner)
        UserLocationButton(
            onClick = {
                onLocationButtonClick(userLocation != null)
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
                .padding(KrailTheme.dimensions.spacingXL),
        )

        // Top overlays — permission banner (if shown) then freshness badge directly below it.
        // fillMaxWidth + horizontal padding here so both children get consistent spacingXL screen
        // margins without each child needing to specify it individually.
        if (showPermissionBanner || (showFreshnessBadge && badgeText != null)) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = KrailTheme.dimensions.spacingXL)
                    .padding(top = KrailTheme.dimensions.spacingM),
            ) {
                if (showPermissionBanner) {
                    LocationPermissionBanner(
                        onGoToSettings = {
                            onPermissionSettingsClick()
                            userLocationManager.openAppSettings()
                        },
                    )
                }
                if (showFreshnessBadge) {
                    badgeText?.let { text ->
                        MapTimetableDataBadge(
                            text = text,
                            isStale = isStale,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(vertical = KrailTheme.dimensions.spacingM),
                        )
                    }
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

private fun cameraTargetForState(mapState: JourneyMapUiState.Ready, userLocation: LatLng?): CameraPosition {
    val isEmpty = mapState.mapDisplay.stops.isEmpty() && mapState.cameraFocus == null
    if (isEmpty && userLocation != null) {
        return CameraPosition(
            target = Position(latitude = userLocation.latitude, longitude = userLocation.longitude),
            zoom = UserLocationConfig.RECENTER_ZOOM,
        )
    }
    return calculateInitialCameraPosition(mapState)
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

// Duration for smooth camera fly-to when switching between journey routes or
// returning to the empty (no-journey-selected) state.
private const val CAMERA_TRANSITION_MS = 600L
