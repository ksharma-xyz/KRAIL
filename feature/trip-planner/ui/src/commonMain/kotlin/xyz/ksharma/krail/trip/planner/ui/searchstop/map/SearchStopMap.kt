package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.data.location.rememberUserLocationManager
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.maps.state.NearbyStopsConfig
import xyz.ksharma.krail.core.maps.state.UserLocationConfig
import xyz.ksharma.krail.core.maps.ui.config.MapConfig.Ornaments.ATTRIBUTION_ALIGNMENT
import xyz.ksharma.krail.core.maps.ui.config.MapConfig.Ornaments.ATTRIBUTION_ENABLED
import xyz.ksharma.krail.core.maps.ui.config.MapConfig.Ornaments.COMPASS_ENABLED
import xyz.ksharma.krail.core.maps.ui.config.MapConfig.Ornaments.LOGO_ENABLED
import xyz.ksharma.krail.core.maps.ui.config.MapConfig.Ornaments.SCALE_BAR_ENABLED
import xyz.ksharma.krail.core.maps.ui.config.MapTileProvider.OPEN_FREE_MAP_LIBERTY
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.core.transport.nsw.NswTransportMode
import xyz.ksharma.krail.taj.components.LoadingDotsPill
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapDisplay
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.stopcard.AnimatedStopCardPlacement
import xyz.ksharma.krail.trip.planner.ui.stopcard.StopCardBackHandlers
import xyz.ksharma.krail.trip.planner.ui.stopcard.StopCardOptions
import xyz.ksharma.krail.trip.planner.ui.stopcard.StopCardPickActions
import xyz.ksharma.krail.trip.planner.ui.stopcard.StopCardWithDepartures
import xyz.ksharma.krail.trip.planner.ui.stopcard.controlsClearanceFor
import xyz.ksharma.krail.trip.planner.ui.stopcard.nearbyStopFor
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SearchStopMap(
    mapUiState: MapUiState,
    modifier: Modifier = Modifier,
    keyboard: SoftwareKeyboardController? = null,
    focusRequester: FocusRequester? = null,
    ornamentTopPadding: Dp = 0.dp,
    // The whole margin of the map's own ornaments (compass, scale bar, info icon), for a host that stands controls
    // in the same corners. Null keeps just [ornamentTopPadding] at the top.
    ornamentPadding: PaddingValues? = null,
    // True while a host covers the map and keeps it composed (Search Stop's list state): an open stop card is closed and
    // system back is the host's, so it never closes a card the rider cannot see.
    isCovered: Boolean = false,
    onEvent: (SearchStopUiEvent) -> Unit = {},
    onStopSelect: (StopItem) -> Unit = {},
    allowPermissionRequest: Boolean = false,
    onPermissionStatus: (PermissionStatus) -> Unit = {},
    onRequestPermission: () -> Unit = {},
    onMapTap: () -> Unit = {},
    // A host that draws the stop card itself, above everything else, takes the pin taps here and the map
    // opens none of its own. Null keeps the card on the map, as Search Stop draws it.
    onStopPinTap: ((NearbyStopFeature) -> Unit)? = null,
    // False where the rider's dot is the locate control and there is no button (`docs/MAP_HOME_UX.md` rule 20).
    showsLocationButton: Boolean = true,
    // More pins to draw between the stops and the rider's dot, such as the Park & Ride `P` and the labels.
    extraLayers: @Composable () -> Unit = {},
    // What the floating stop card offers besides the stop's board: Save as, and Go here or Start here.
    stopCardOptions: StopCardOptions = StopCardOptions(),
    // Controls a host draws over the map's bottom edge. They get the height that lifts them above an open stop card,
    // the same value that lifts the location button, so the card never covers them, and whether they are hidden for the
    // card instead (see [hidesControlsForCard]). Search Stop's mode button.
    bottomControls: @Composable BoxScope.(clearance: Dp, controlsHidden: Boolean) -> Unit = { _, _ -> },
    // How far above the bottom inset the location button stands, given the clearance an open stop card asks for. Null keeps
    // one edge and the clearance, as every other host has it. Search Stop stands it on the line Options is on.
    locationBottomPadding: ((clearance: Dp) -> Dp)? = null,
    // True where the map's controls get out of the way of an open stop card by hiding (Search Stop): the location button
    // shrinks away and back, the host's bottom controls are told, and no clearance lifts anything. False keeps the lift
    // above the card, as the home map pane and every other host have it.
    hidesControlsForCard: Boolean = false,
    // How much room the stop card keeps clear at its top. Null keeps [ornamentTopPadding], the form's height. Search Stop
    // shrinks it to nothing as it slides its box away while the Departure Board is expanded.
    stopCardTopReserve: Dp? = null,
    // Told whether the open card's Departure Board is expanded, so a host can hand its own room to the card.
    onBoardExpandedChange: (Boolean) -> Unit = {},
    // Counts each stretch of this map being on screen for analytics (`map_viewed`, `map_dismissed`) under [mapEntry].
    // Null counts nothing, as in a preview.
    mapOpen: MapOpenTracker? = null,
    mapEntry: MapEntry = MapEntry.SEARCH_STOP,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (mapUiState) {
            is MapUiState.Loading -> {
                LoadingDotsPill(
                    isLoading = true,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            is MapUiState.Ready -> {
                MapContent(
                    mapState = mapUiState,
                    keyboard = keyboard,
                    focusRequester = focusRequester,
                    ornamentTopPadding = ornamentTopPadding,
                    ornamentPadding = ornamentPadding,
                    isCovered = isCovered,
                    onEvent = onEvent,
                    onStopSelect = onStopSelect,
                    allowPermissionRequest = allowPermissionRequest,
                    onPermissionStatus = onPermissionStatus,
                    onRequestPermission = onRequestPermission,
                    onMapTap = onMapTap,
                    onStopPinTap = onStopPinTap,
                    showsLocationButton = showsLocationButton,
                    extraLayers = extraLayers,
                    stopCardOptions = stopCardOptions,
                    bottomControls = bottomControls,
                    locationBottomPadding = locationBottomPadding,
                    hidesControlsForCard = hidesControlsForCard,
                    stopCardTopReserve = stopCardTopReserve,
                    onBoardExpandedChange = onBoardExpandedChange,
                    mapOpen = mapOpen,
                    mapEntry = mapEntry,
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
    ornamentPadding: PaddingValues? = null,
    isCovered: Boolean = false,
    allowPermissionRequest: Boolean = false,
    onPermissionStatus: (PermissionStatus) -> Unit = {},
    onRequestPermission: () -> Unit = {},
    onMapTap: () -> Unit = {},
    onStopPinTap: ((NearbyStopFeature) -> Unit)? = null,
    showsLocationButton: Boolean = true,
    extraLayers: @Composable () -> Unit = {},
    stopCardOptions: StopCardOptions = StopCardOptions(),
    bottomControls: @Composable BoxScope.(clearance: Dp, controlsHidden: Boolean) -> Unit = { _, _ -> },
    locationBottomPadding: ((clearance: Dp) -> Dp)? = null,
    hidesControlsForCard: Boolean = false,
    stopCardTopReserve: Dp? = null,
    onBoardExpandedChange: (Boolean) -> Unit = {},
    mapOpen: MapOpenTracker? = null,
    mapEntry: MapEntry = MapEntry.SEARCH_STOP,
) {
    log(
        "[NEARBY_STOPS_UI] MapContent rendered: nearbyStops.size=${mapState.mapDisplay.nearbyStops.size}, " +
            "isLoading=${mapState.isLoadingNearbyStops}, " +
            "selectedModes=${mapState.mapDisplay.selectedTransportModes}",
    )

    // Which stop's card is open, held as the stop id because that is the part worth saving: the
    // feature itself is map-rendering data the ViewModel still holds after the Activity is recreated,
    // so it can be looked up again rather than serialised. Held in a plain `remember` this closed the
    // card on every rotation, theme switch and font-size change. A stop that has since left the nearby
    // set leaves the card closed.
    var selectedStopId by rememberSaveable { mutableStateOf<String?>(null) }
    val nearbyStops = mapState.mapDisplay.nearbyStops
    val selectedStop = remember(selectedStopId, nearbyStops) { nearbyStopFor(selectedStopId, nearbyStops) }
    var cardHeight by remember { mutableStateOf(0.dp) }
    // The stop the card was last open for, held so the card can slide away still showing it once it is dismissed.
    var lastStop by remember { mutableStateOf<NearbyStopFeature?>(null) }
    LaunchedEffect(selectedStop) { if (selectedStop != null) lastStop = selectedStop }
    val shownStop = selectedStop ?: lastStop

    // Whether the open card's Departure Board is expanded, held here for the back order and for the host, which hides its box
    // while it is. Keyed on the stop, so another pin's card starts with its board shut.
    var boardExpanded by rememberSaveable(selectedStopId) { mutableStateOf(false) }
    val cardOpen = selectedStop != null
    LaunchedEffect(cardOpen, boardExpanded) { onBoardExpandedChange(cardOpen && boardExpanded) }
    DisposableEffect(Unit) { onDispose { onBoardExpandedChange(false) } }

    // System back, one step at a time while a card is up: an expanded board shuts first, then the card is dismissed, then it
    // is the screen's own two step back. These are registered after the screen's, so while a card is open they answer, and
    // with none they are off and the screen's is. While the host covers the map the card is closed and back is the host's.
    LaunchedEffect(isCovered) { if (isCovered) selectedStopId = null }
    StopCardBackHandlers(
        cardOpen = cardOpen,
        boardExpanded = boardExpanded,
        covered = isCovered,
        onCollapseBoard = { boardExpanded = false },
        onDismissCard = { selectedStopId = null },
    )

    // The permission is the screen's now: the location pill under the form says what is wrong and
    // asks or opens Settings, and [allowPermissionRequest] is false until the rider taps it (or the
    // location button), the only moments tracking may raise the system dialog.
    val userLocationManager = rememberUserLocationManager()
    val scope = rememberCoroutineScope()

    // One open for each stretch this map is on screen and not covered, for `map_viewed` and `map_dismissed`.
    MapOpenEffect(
        mapOpen = mapOpen,
        entry = mapEntry,
        covered = isCovered,
        hadUserLocation = mapState.mapDisplay.userLocation != null,
        stopCount = mapState.mapDisplay.nearbyStops.size,
        userLocationManager = userLocationManager,
    )

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
                onPermissionStatus(PermissionStatus.Granted)
                onEvent(SearchStopUiEvent.UserLocationUpdated(latLng))
            },
            onPermissionDeny = onPermissionStatus,
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
                ornamentPadding = ornamentPadding ?: PaddingValues(top = ornamentTopPadding),
                onStopSelected = { stop ->
                    if (onStopPinTap != null) {
                        onStopPinTap(stop)
                    } else {
                        selectedStopId = stop.stopId
                        onEvent(SearchStopUiEvent.NearbyStopClicked(stop))
                    }
                },
                extraLayers = extraLayers,
                // A tap on the map closes an open card and is the whole of that tap. With none open it
                // is the screen's first step back to the map state.
                onMapTap = { if (selectedStopId != null) selectedStopId = null else onMapTap() },
            )

            // The location button, bottom end, and the host's own bottom controls. All location business logic lives
            // here, not inside MapActionButtons.
            MapControlsLayer(
                showsLocationButton = showsLocationButton,
                isLocationActive = mapState.mapDisplay.userLocation != null,
                onLocationButtonClick = {
                    onEvent(
                        SearchStopUiEvent.LocationButtonClicked(
                            hadLocation = mapState.mapDisplay.userLocation != null,
                        ),
                    )
                    scope.launch {
                        handleLocationButtonClick(
                            userLoc = mapState.mapDisplay.userLocation,
                            cameraState = cameraState,
                            userLocationManager = userLocationManager,
                            onPermissionDenied = onPermissionStatus,
                            onRequestPermission = onRequestPermission,
                        )
                    }
                },
                cardOpen = selectedStop != null,
                cardHeight = cardHeight,
                hidesControlsForCard = hidesControlsForCard,
                locationBottomPadding = locationBottomPadding,
                bottomControls = bottomControls,
            )

            // The floating stop card, over the map and never a sheet. It slides up with a fade and back down as it is dismissed,
            // so the stop it shows is held for the length of that exit.
            AnimatedStopCardPlacement(
                visible = selectedStop != null,
                topReserve = stopCardTopReserve ?: ornamentTopPadding,
                onHeight = { cardHeight = it },
            ) {
                shownStop?.let { stop ->
                    StopCardWithDepartures(
                        stop = stop,
                        onClose = { selectedStopId = null },
                        boardExpanded = boardExpanded,
                        onBoardExpandedChange = { boardExpanded = it },
                        // Save as, then Go here or Start here by the field being filled in.
                        actions = {
                            StopCardPickActions(
                                stop = StopItem(stopId = stop.stopId, stopName = stop.stopName),
                                options = stopCardOptions,
                                onPick = {
                                    // Dismiss keyboard and clear focus - IMPORTANT: Order matters!
                                    keyboard?.hide()
                                    focusRequester?.freeFocus()

                                    val stopItem = StopItem(stopId = stop.stopId, stopName = stop.stopName)
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
                                    // After the pick event, which is stamped with this open's id, so the pick
                                    // joins the open it ends.
                                    mapOpen?.stopPicked()

                                    selectedStopId = null
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}

/**
 * The map's own controls over its bottom edge: the location button and whatever the host draws there ([bottomControls]).
 *
 * While a stop card is open ([cardOpen]) they do one of two things. Where [hidesControlsForCard] is set (Search Stop) they
 * get out of the card's way by hiding: the location button shrinks to nothing about its own centre while it fades and
 * grows back when the card is dismissed, the host is told through `controlsHidden`, and no clearance is given, so nothing
 * is lifted. Everywhere else they stay and ride one gap above the card by [controlsClearanceFor].
 */
@Composable
internal fun BoxScope.MapControlsLayer(
    showsLocationButton: Boolean,
    isLocationActive: Boolean,
    onLocationButtonClick: () -> Unit,
    cardOpen: Boolean,
    cardHeight: Dp,
    hidesControlsForCard: Boolean,
    locationBottomPadding: ((clearance: Dp) -> Dp)?,
    bottomControls: @Composable BoxScope.(clearance: Dp, controlsHidden: Boolean) -> Unit,
) {
    val clearance = controlsClearanceFor(
        cardHeight = cardHeight,
        cardOpen = cardOpen,
        controlsHide = hidesControlsForCard,
    )
    val controlsHidden = cardOpen && hidesControlsForCard
    if (showsLocationButton) {
        MapActionButtons(
            isLocationActive = isLocationActive,
            onLocationButtonClick = onLocationButtonClick,
            modifier = Modifier.align(Alignment.BottomStart),
            bottomPadding = locationBottomPadding?.invoke(clearance) ?: (KrailTheme.dimensions.spacingXL + clearance),
            visible = !controlsHidden,
        )
    }
    bottomControls(clearance, controlsHidden)
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
    ornamentPadding: PaddingValues,
    onStopSelected: (NearbyStopFeature) -> Unit,
    onMapTap: () -> Unit,
    modifier: Modifier = Modifier,
    extraLayers: @Composable () -> Unit = {},
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
        // Previews and snapshot tests run with no GPU and no native MapLibre, so they draw a flat
        // stand-in for the ground and keep everything that floats over it.
        if (LocalInspectionMode.current) {
            Box(modifier = Modifier.fillMaxSize().background(KrailTheme.colors.onSurface.copy(alpha = 0.08f)))
        } else if (hasHadValidSize) {
            MaplibreMap(
                modifier = Modifier.fillMaxSize(),
                cameraState = cameraState,
                baseStyle = BaseStyle.Uri(OPEN_FREE_MAP_LIBERTY),
                // A tap on the map itself, not on a stop (the stop layer consumes its own taps),
                // is the first step back to the map state. Passed on so nothing else loses it.
                onMapClick = { _, _ ->
                    onMapTap()
                    ClickResult.Pass
                },
                options = MapOptions(
                    ornamentOptions = OrnamentOptions(
                        padding = ornamentPadding,
                        isLogoEnabled = LOGO_ENABLED,
                        isAttributionEnabled = ATTRIBUTION_ENABLED,
                        attributionAlignment = ATTRIBUTION_ALIGNMENT,
                        isCompassEnabled = COMPASS_ENABLED,
                        compassAlignment = Alignment.TopEnd,
                        isScaleBarEnabled = SCALE_BAR_ENABLED,
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

                extraLayers()

                // Render user location as red circle (always on top)
                UserLocationLayer(
                    userLocation = mapState.mapDisplay.userLocation,
                )
            }
        }
    }
}

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
