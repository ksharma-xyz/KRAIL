package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.data.location.rememberUserLocationManager
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.maps.state.NearbyStopsConfig
import xyz.ksharma.krail.core.maps.state.UserLocationConfig
import xyz.ksharma.krail.core.maps.ui.components.LocationPermissionBanner
import xyz.ksharma.krail.core.maps.ui.components.MapTimetableDataBadge
import xyz.ksharma.krail.core.maps.ui.config.MapConfig.Ornaments.ATTRIBUTION_ENABLED
import xyz.ksharma.krail.core.maps.ui.config.MapConfig.Ornaments.LOGO_ENABLED
import xyz.ksharma.krail.core.maps.ui.config.MapTileProvider.OPEN_FREE_MAP_LIBERTY
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.core.transport.nsw.NswTransportMode
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.components.map.StopActionButton
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapDisplay
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SearchStopMap(
    mapUiState: MapUiState,
    modifier: Modifier = Modifier,
    keyboard: SoftwareKeyboardController? = null,
    focusRequester: FocusRequester? = null,
    ornamentTopPadding: Dp = 0.dp,
    autoShowOptionsSheet: Boolean = false,
    onShowOptionsSheet: () -> Unit = {},
    onEvent: (SearchStopUiEvent) -> Unit = {},
    onStopSelect: (StopItem) -> Unit = {},
    onPermissionBannerVisibilityChanged: (Boolean) -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (mapUiState) {
            is MapUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            is MapUiState.Ready -> {
                MapContent(
                    mapState = mapUiState,
                    keyboard = keyboard,
                    focusRequester = focusRequester,
                    ornamentTopPadding = ornamentTopPadding,
                    autoShowOptionsSheet = autoShowOptionsSheet,
                    onShowOptionsSheet = onShowOptionsSheet,
                    onEvent = onEvent,
                    onStopSelect = onStopSelect,
                    onPermissionBannerVisibilityChanged = onPermissionBannerVisibilityChanged,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is MapUiState.Error -> {
                ErrorMessage(
                    message = mapUiState.message ?: "Something went wrong",
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = message,
        style = KrailTheme.typography.bodyMedium,
        color = KrailTheme.colors.error,
        modifier = modifier.padding(16.dp),
    )
}

@Composable
private fun MapContent(
    mapState: MapUiState.Ready,
    onEvent: (SearchStopUiEvent) -> Unit,
    onStopSelect: (StopItem) -> Unit,
    modifier: Modifier = Modifier,
    keyboard: SoftwareKeyboardController? = null,
    focusRequester: FocusRequester? = null,
    ornamentTopPadding: Dp = 0.dp,
    autoShowOptionsSheet: Boolean = false,
    onShowOptionsSheet: () -> Unit = {},
    onPermissionBannerVisibilityChanged: (Boolean) -> Unit = {},
) {
    log(
        "[NEARBY_STOPS_UI] MapContent rendered: nearbyStops.size=${mapState.mapDisplay.nearbyStops.size}, " +
            "isLoading=${mapState.isLoadingNearbyStops}, " +
            "selectedModes=${mapState.mapDisplay.selectedTransportModes}",
    )

    var showOptionsBottomSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(autoShowOptionsSheet) {
        if (autoShowOptionsSheet && !showOptionsBottomSheet) {
            showOptionsBottomSheet = true
            onShowOptionsSheet()
        }
    }
    var selectedStop by remember { mutableStateOf<NearbyStopFeature?>(null) }

    // User location state
    var permissionStatus by remember { mutableStateOf<PermissionStatus>(PermissionStatus.NotDetermined) }
    var showPermissionBanner by remember { mutableStateOf(false) }
    LaunchedEffect(showPermissionBanner) {
        onPermissionBannerVisibilityChanged(showPermissionBanner)
    }
    // False until the user explicitly taps the location button.
    // When true, TrackUserLocation is allowed to trigger the system permission dialog.
    var allowPermissionRequest by remember { mutableStateOf(false) }

    // Badge expand animation — rememberSaveable so rotation doesn't replay it.
    var badgeExpanded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!badgeExpanded) {
            delay(500)
            badgeExpanded = true
        }
    }
    val userLocationManager = rememberUserLocationManager()
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        // Seed from known user location so re-entry after a composition bounce
        // (dual-pane layout shift, rotation) doesn't flash at Sydney default.
        val knownLocation = mapState.mapDisplay.userLocation
        val cameraState = rememberCameraState(
            firstPosition = initialCameraPosition(knownLocation),
        )

        // Trigger initial nearby-stops load for the camera's starting position.
        val initialCenter = knownLocation ?: LatLng(
            NearbyStopsConfig.DEFAULT_CENTER_LAT,
            NearbyStopsConfig.DEFAULT_CENTER_LON,
        )
        LaunchedEffect(Unit) {
            val label = if (knownLocation != null) "user location" else "Sydney default"
            log("[NEARBY_STOPS_UI] Map initialized at $label")
            onEvent(SearchStopUiEvent.MapCenterChanged(initialCenter))
        }

        TrackUserLocation(
            userLocationManager = userLocationManager,
            allowPermissionRequest = allowPermissionRequest,
            onLocationUpdate = { latLng ->
                permissionStatus = PermissionStatus.Granted
                showPermissionBanner = false
                onEvent(SearchStopUiEvent.UserLocationUpdated(latLng))
            },
            onPermissionDeny = { status ->
                permissionStatus = status
                showPermissionBanner = true
            },
        )

        // Auto-center on first user location, exactly once, smoothly.
        //
        // CRITICAL: key the effect on a STABLE boolean (hasUserLocation), NOT on the changing
        // LatLng. GPS emits a stream of slightly-different fixes; keying on the value re-launched
        // this effect on every jitter, which CANCELLED the in-flight animateTo. Because
        // hasAutoCentered had already flipped true, it never re-fired — so the camera was left
        // frozen wherever the cancelled animation stopped: at the Sydney seed, or (on iOS) at an
        // intermediate zoomed-all-the-way-out frame. Rotating "fixed" it only because
        // rememberCameraState re-seeded firstPosition at the now-known location (the seed path,
        // not this animate path). hasUserLocation flips false→true once and then stays true, so
        // subsequent jitter no longer re-keys the effect and the animation runs to completion.
        //
        // If location permission is absent, userLocation stays null → no animation → the camera
        // stays at the Sydney default seed. Only when a real fix arrives do we move to it.
        val userLocation = mapState.mapDisplay.userLocation
        val hasUserLocation = userLocation != null
        var hasAutoCentered by remember { mutableStateOf(false) }
        LaunchedEffect(hasUserLocation) {
            val target = userLocation
            if (target != null && !hasAutoCentered) {
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

        // Track camera moves to update map center and reload stops
        @OptIn(FlowPreview::class)
        LaunchedEffect(cameraState) {
            snapshotFlow { cameraState.position.target }
                .debounce(NearbyStopsConfig.CAMERA_PAN_DEBOUNCE_MS)
                // Defensive: during an iOS resize transient MapLibre can briefly report a
                // projected target outside valid lat/lon bounds. Dropping those stops the
                // runaway nearby-stops query (and its "Invalid longitude" errors) instead of
                // feeding garbage coordinates downstream.
                .filter { it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 }
                .collect { target ->
                    onEvent(
                        SearchStopUiEvent.MapCenterChanged(
                            LatLng(target.latitude, target.longitude),
                        ),
                    )
                }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            MapSurface(
                mapState = mapState,
                cameraState = cameraState,
                ornamentTopPadding = ornamentTopPadding,
                onStopSelected = { stop ->
                    selectedStop = stop
                    onEvent(SearchStopUiEvent.NearbyStopClicked(stop))
                },
            )

            // Bottom left action buttons (Options and Location).
            // All location business logic lives here, not inside MapActionButtons.
            MapActionButtons(
                onOptionsClick = {
                    onEvent(SearchStopUiEvent.MapOptionsButtonClicked)
                    showOptionsBottomSheet = true
                },
                isLocationActive = mapState.mapDisplay.userLocation != null,
                onLocationButtonClick = {
                    onEvent(
                        SearchStopUiEvent.LocationButtonClicked(hadLocation = mapState.mapDisplay.userLocation != null),
                    )
                    scope.launch {
                        handleLocationButtonClick(
                            userLoc = mapState.mapDisplay.userLocation,
                            cameraState = cameraState,
                            userLocationManager = userLocationManager,
                            onPermissionDenied = { status ->
                                permissionStatus = status
                                showPermissionBanner = true
                            },
                            onRequestPermission = { allowPermissionRequest = true },
                        )
                    }
                },
                modifier = Modifier.align(Alignment.BottomStart),
            )

            // Permission badge — shown when permission is not yet determined.
            // Gives users a visible entry point to grant location access beyond the
            // small location button. Disappears once the request is in-flight or
            // a location fix arrives. Mutually exclusive with the banner (denied state).
            if (permissionStatus is PermissionStatus.NotDetermined) {
                MapTimetableDataBadge(
                    text = if (badgeExpanded) "Location Permission Required" else "",
                    onClick = { allowPermissionRequest = true },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = ornamentTopPadding)
                        .defaultMinSize(minWidth = 36.dp, minHeight = 36.dp)
                        .animateContentSize(
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                        ),
                )
            }

            // Permission banner (shown when permission is denied).
            // Top padding matches ornamentTopPadding so it clears the floating search bar
            // (single-pane) or the status bar (dual-pane). Horizontal padding gives the
            // rounded card visual some breathing room from screen edges.
            if (showPermissionBanner) {
                LocationPermissionBanner(
                    onGoToSettings = {
                        onEvent(SearchStopUiEvent.LocationPermissionSettingsClicked)
                        userLocationManager.openAppSettings()
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = ornamentTopPadding)
                        .padding(horizontal = 16.dp),
                )
            }

            // Options Bottom Sheet
            if (showOptionsBottomSheet) {
                MapOptionsBottomSheet(
                    searchRadiusKm = mapState.mapDisplay.searchRadiusKm,
                    selectedTransportModes = mapState.mapDisplay.selectedTransportModes,
                    showDistanceScale = mapState.mapDisplay.showDistanceScale,
                    showCompass = mapState.mapDisplay.showCompass,
                    onDismiss = { showOptionsBottomSheet = false },
                    onEvent = onEvent,
                )
            }

            // Stop Details Bottom Sheet
            selectedStop?.let { stop ->
                StopDetailsBottomSheet(
                    stop = stop,
                    onDismiss = { selectedStop = null },
                    actionContent = {
                        StopActionButton(
                            text = "Select Stop",
                            onClick = {
                                // Dismiss keyboard and clear focus - IMPORTANT: Order matters!
                                keyboard?.hide()
                                focusRequester?.freeFocus()

                                val stopItem = StopItem(
                                    stopId = stop.stopId,
                                    stopName = stop.stopName,
                                )
                                onStopSelect(stopItem)

                                onEvent(
                                    SearchStopUiEvent.TrackStopSelected(
                                        stopItem = stopItem,
                                        isRecentSearch = false,
                                    ),
                                )

                                onEvent(
                                    SearchStopUiEvent.TrackStopSelectedFromMap(
                                        stopId = stop.stopId,
                                        searchRadiusKm = mapState.mapDisplay.searchRadiusKm,
                                        enabledModesCount = mapState.mapDisplay.selectedTransportModes.size,
                                        nearbyStopsCount = mapState.mapDisplay.nearbyStops.size,
                                        hadUserLocation = mapState.mapDisplay.userLocation != null,
                                    ),
                                )

                                selectedStop = null
                            },
                        )
                    },
                )
            }
        }
    }
}

/**
 * The MapLibre GL surface plus its on-map layers (nearby stops, user location).
 *
 * iOS UIKitView interop: if [MaplibreMap] is instantiated while EITHER axis of the container
 * is 0, the native MLNMapView is created with a degenerate frame. With both axes 0 the Metal
 * layer fails to allocate a drawable ("CAMetalLayer ... width=0 height=0" / "nextDrawable
 * returning nil") and the map is blank. With one axis 0 (the adaptive window reports a
 * transient `840×0dp` / `0×480dp` during rotation) MapLibre projects camera moves against a
 * zero-area viewport and the camera target runs away to invalid coords (e.g. longitude > 180,
 * mid-Pacific), so the map pans off the world AND the nearby-stops query errors out.
 *
 * Gate creation until BOTH width and height are non-zero so the native view always initialises
 * with a valid frame. Android is unaffected (AndroidView re-lays-out cleanly) but the gate is
 * harmless there.
 */
@Composable
private fun MapSurface(
    mapState: MapUiState.Ready,
    cameraState: org.maplibre.compose.camera.CameraState,
    ornamentTopPadding: Dp,
    onStopSelected: (NearbyStopFeature) -> Unit,
    modifier: Modifier = Modifier,
) {
    // LATCH the "has had a real size" flag — never reset it to false. During a rotation the
    // container momentarily reports a 0 axis (the adaptive window flips through 840×0dp);
    // tearing the map down on that transient and recreating it leaves the right pane blank
    // until a SECOND rotation. Once we've seen a valid size we keep the map mounted; MapLibre
    // resizes its own surface as the container settles.
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
                baseStyle = BaseStyle.Uri(OPEN_FREE_MAP_LIBERTY),
                options = MapOptions(
                    ornamentOptions = OrnamentOptions(
                        padding = PaddingValues(top = ornamentTopPadding),
                        isLogoEnabled = LOGO_ENABLED,
                        isAttributionEnabled = ATTRIBUTION_ENABLED,
                        attributionAlignment = Alignment.BottomEnd,
                        isCompassEnabled = mapState.mapDisplay.showCompass,
                        compassAlignment = Alignment.TopEnd,
                        isScaleBarEnabled = mapState.mapDisplay.showDistanceScale,
                        scaleBarAlignment = Alignment.TopStart,
                    ),
                ),
            ) {
                if (mapState.mapDisplay.nearbyStops.isNotEmpty()) {
                    log(
                        "[NEARBY_STOPS_UI] Rendering ${mapState.mapDisplay.nearbyStops.size} " +
                            "stops on map",
                    )
                    NearbyStopsLayer(
                        stops = mapState.mapDisplay.nearbyStops,
                        onStopClick = { stop ->
                            log("[NEARBY_STOPS_UI] Stop clicked: ${stop.stopName}")
                            onStopSelected(stop)
                        },
                    )
                }

                // Render user location as red circle (always on top)
                UserLocationLayer(
                    userLocation = mapState.mapDisplay.userLocation,
                )
            }
        }
    }
}

private fun LatLng.toPosition(): Position = Position(latitude = latitude, longitude = longitude)

private suspend fun handleLocationButtonClick(
    userLoc: LatLng?,
    cameraState: org.maplibre.compose.camera.CameraState,
    userLocationManager: xyz.ksharma.krail.core.maps.data.location.UserLocationManager,
    onPermissionDenied: (xyz.ksharma.aagya.permission.PermissionStatus) -> Unit,
    onRequestPermission: () -> Unit,
) {
    if (userLoc != null) {
        cameraState.animateTo(
            CameraPosition(target = userLoc.toPosition(), zoom = UserLocationConfig.RECENTER_ZOOM),
            duration = UserLocationConfig.RECENTER_ANIMATION_MS.milliseconds,
        )
    } else {
        val status = userLocationManager.checkPermissionStatus()
        if (status is xyz.ksharma.aagya.permission.PermissionStatus.Denied) {
            onPermissionDenied(status)
        } else {
            onRequestPermission()
        }
    }
}

private fun initialCameraPosition(knownLocation: LatLng?): CameraPosition =
    knownLocation?.let {
        CameraPosition(target = it.toPosition(), zoom = UserLocationConfig.AUTO_CENTER_ZOOM)
    } ?: CameraPosition(
        target = Position(
            latitude = NearbyStopsConfig.DEFAULT_CENTER_LAT,
            longitude = NearbyStopsConfig.DEFAULT_CENTER_LON,
        ),
        zoom = NearbyStopsConfig.DEFAULT_ZOOM,
    )

// region Previews

@PreviewScreen
@Composable
private fun PreviewSearchStopMapLoading() {
    PreviewTheme {
        SearchStopMap(
            mapUiState = MapUiState.Loading,
        )
    }
}

@PreviewScreen
@Composable
private fun PreviewSearchStopMapReady() {
    PreviewTheme {
        val mapDisplay = MapDisplay(
            nearbyStops = persistentListOf(
                NearbyStopFeature(
                    stopId = "stop_1",
                    stopName = "Central Station",
                    position = LatLng(
                        latitude = -33.8688,
                        longitude = 151.2093,
                    ),
                    transportModes = persistentListOf(TransportMode.Train),
                ),
                NearbyStopFeature(
                    stopId = "stop_2",
                    stopName = "Town Hall",
                    position = LatLng(
                        latitude = -33.8734,
                        longitude = 151.2069,
                    ),
                    transportModes = persistentListOf(TransportMode.Metro),
                ),
            ),
            selectedTransportModes = persistentSetOf(
                NswTransportMode.Train.productClass,
                NswTransportMode.Metro.productClass,
            ),
            showCompass = true,
            showDistanceScale = true,
        )

        SearchStopMap(
            mapUiState = MapUiState.Ready(mapDisplay = mapDisplay),
        )
    }
}

@PreviewScreen
@Composable
private fun PreviewSearchStopMapError() {
    PreviewTheme {
        SearchStopMap(
            mapUiState = MapUiState.Error(message = "Failed to load map data. Please try again."),
        )
    }
}

// endregion
