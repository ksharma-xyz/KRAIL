package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import xyz.ksharma.krail.coroutines.ext.coroutineExceptionHandler
import kotlin.time.Duration.Companion.milliseconds
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import xyz.ksharma.krail.core.location.data.rememberLocationTracker
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.data.location.UserLocationManager
import xyz.ksharma.krail.core.maps.data.location.createUserLocationManager
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.maps.state.NearbyStopsConfig
import xyz.ksharma.krail.core.maps.ui.components.LocationPermissionBanner
import xyz.ksharma.krail.core.maps.ui.utils.MapLocationController
import xyz.ksharma.krail.core.maps.ui.config.MapTileProvider.OPEN_FREE_MAP_LIBERTY
import xyz.ksharma.krail.core.permission.PermissionStatus
import xyz.ksharma.krail.core.permission.data.rememberPermissionController

import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapDisplay
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * Creates a [UserLocationManager] in the Compose composition.
 *
 * Lives here (feature layer) because PermissionController and LocationTracker both
 * require a Compose/Activity context to be constructed, so the glue cannot live in
 * the pure-UI :core:maps:ui module.
 */
@Composable
fun rememberUserLocationManager(): UserLocationManager {
    val permissionController = rememberPermissionController()
    val locationTracker = rememberLocationTracker()
    return remember(permissionController, locationTracker) {
        createUserLocationManager(permissionController, locationTracker)
    }
}

@Composable
fun SearchStopMap(
    mapUiState: MapUiState,
    modifier: Modifier = Modifier,
    keyboard: SoftwareKeyboardController? = null,
    focusRequester: FocusRequester? = null,
    onEvent: (SearchStopUiEvent) -> Unit = {},
    onStopSelect: (StopItem) -> Unit = {},
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
                    onEvent = onEvent,
                    onStopSelect = onStopSelect,
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
) {
    log(
        "[NEARBY_STOPS_UI] MapContent rendered: nearbyStops.size=${mapState.mapDisplay.nearbyStops.size}, " +
            "isLoading=${mapState.isLoadingNearbyStops}, " +
            "selectedModes=${mapState.mapDisplay.selectedTransportModes}",
    )

    var showOptionsBottomSheet by rememberSaveable { mutableStateOf(false) }
    var selectedStop by remember { mutableStateOf<NearbyStopFeature?>(null) }

    // User location state
    var permissionStatus by remember { mutableStateOf<PermissionStatus>(PermissionStatus.NotDetermined) }
    var showPermissionBanner by remember { mutableStateOf(false) }
    val userLocationManager = rememberUserLocationManager()
    val mapLocationController = remember { MapLocationController() }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        // Start at default Sydney coordinates
        val cameraState = rememberCameraState(
            firstPosition = CameraPosition(
                target = Position(
                    latitude = NearbyStopsConfig.DEFAULT_CENTER_LAT,
                    longitude = NearbyStopsConfig.DEFAULT_CENTER_LON,
                ),
                zoom = NearbyStopsConfig.DEFAULT_ZOOM,
            ),
        )

        // Trigger initial load with default camera position
        LaunchedEffect(Unit) {
            log("[NEARBY_STOPS_UI] Map initialized at default position")
            onEvent(
                SearchStopUiEvent.MapCenterChanged(
                    LatLng(
                        NearbyStopsConfig.DEFAULT_CENTER_LAT,
                        NearbyStopsConfig.DEFAULT_CENTER_LON,
                    ),
                ),
            )
        }

        // Auto-fetch user location on map init if permission granted
        LaunchedEffect(Unit) {
            ensureActive() // Check if coroutine is still active

            // Check permission status first
            val permStatus = userLocationManager.checkPermissionStatus()
            log("[NEARBY_STOPS_UI] Auto-fetch location - permission status: $permStatus")

            if (permStatus is PermissionStatus.Granted) {
                // Permission already granted, fetch location
                log("[NEARBY_STOPS_UI] Permission granted, fetching location...")
                userLocationManager.getCurrentLocation().onSuccess { location ->
                    ensureActive() // Check again before updating

                    val userLatLng = LatLng(location.latitude, location.longitude)
                    log("[NEARBY_STOPS_UI] Auto-fetched user location: lat=${location.latitude}, lon=${location.longitude}")

                    // Update ViewModel state with user location
                    onEvent(SearchStopUiEvent.UserLocationUpdated(userLatLng))
                }.onFailure { error ->
                    log("[NEARBY_STOPS_UI] Failed to get user location: ${error.message}")
                }
            } else {
                log("[NEARBY_STOPS_UI] Permission not granted, skipping auto-fetch")
            }
        }

        // Move camera to user location when available (state-driven, no logic in UI)
        LaunchedEffect(mapState.mapDisplay.userLocation) {
            val userLoc = mapState.mapDisplay.userLocation
            if (userLoc != null) {
                ensureActive() // Check if coroutine is still active

                log("[NEARBY_STOPS_UI] User location available, moving camera")
                // Note: cameraState.animateTo uses LatLng internally, no conversion needed
                val targetPosition = CameraPosition(
                    target = Position(
                        latitude = userLoc.latitude,
                        longitude = userLoc.longitude
                    ),
                    zoom = 15.0
                )
                cameraState.animateTo(targetPosition, duration = 1500.milliseconds)
            }
        }

        // Track camera moves to update map center and reload stops
        @OptIn(FlowPreview::class)
        @Suppress("MagicNumber")
        LaunchedEffect(cameraState) {
            snapshotFlow { cameraState.position.target }
                .debounce(500) // Only update after user stops moving
                .collect { target ->
                    log(
                        "[NEARBY_STOPS_UI] Camera moved to: lat=${target.latitude}, " +
                            "lon=${target.longitude}",
                    )
                    onEvent(
                        SearchStopUiEvent.MapCenterChanged(
                            LatLng(target.latitude, target.longitude),
                        ),
                    )
                }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            MaplibreMap(
                modifier = Modifier.fillMaxSize(),
                cameraState = cameraState,
                baseStyle = BaseStyle.Uri(OPEN_FREE_MAP_LIBERTY),
                options = MapOptions(
                    ornamentOptions = OrnamentOptions(
                        padding = PaddingValues(0.dp),
                        isLogoEnabled = false,
                        isAttributionEnabled = true,
                        attributionAlignment = Alignment.BottomEnd,
                        isCompassEnabled = mapState.mapDisplay.showCompass,
                        compassAlignment = Alignment.TopEnd,
                        isScaleBarEnabled = mapState.mapDisplay.showDistanceScale,
                        scaleBarAlignment = Alignment.TopStart,
                    ),
                ),
            ) {
                // Render nearby stops
                if (mapState.mapDisplay.nearbyStops.isNotEmpty()) {
                    log(
                        "[NEARBY_STOPS_UI] Rendering ${mapState.mapDisplay.nearbyStops.size} " +
                            "stops on map",
                    )
                    NearbyStopsLayer(
                        stops = mapState.mapDisplay.nearbyStops,
                        onStopClick = { stop ->
                            log("[NEARBY_STOPS_UI] Stop clicked: ${stop.stopName}")
                            selectedStop = stop
                            onEvent(SearchStopUiEvent.NearbyStopClicked(stop))
                        },
                    )
                } else {
                    log("[NEARBY_STOPS_UI] No stops to render")
                }

                // Render user location as red circle (always on top)
                UserLocationLayer(
                    userLocation = mapState.mapDisplay.userLocation
                )
            }

            // Bottom left action buttons (Options and Location).
            // All location business logic lives here, not inside MapActionButtons.
            MapActionButtons(
                onOptionsClick = { showOptionsBottomSheet = true },
                onLocationButtonClick = {
                    scope.launch(coroutineExceptionHandler(message = "SearchStopMap:LocationButton")) {
                        userLocationManager.getCurrentLocation()
                            .onSuccess { location ->
                                val userLatLng = LatLng(location.latitude, location.longitude)
                                onEvent(SearchStopUiEvent.UserLocationUpdated(userLatLng))
                                mapLocationController.moveCameraToUserLocation(
                                    location = location,
                                    cameraState = cameraState,
                                    zoom = 16.0,
                                    animationDuration = 1000L,
                                )
                            }
                            .onFailure {
                                val status = userLocationManager.checkPermissionStatus()
                                if (status is PermissionStatus.Denied) {
                                    permissionStatus = status
                                    showPermissionBanner = true
                                }
                            }
                    }
                },
                modifier = Modifier.align(Alignment.BottomStart),
            )

            // Permission banner (shown when permission is denied)
            if (showPermissionBanner) {
                LocationPermissionBanner(
                    permissionStatus = permissionStatus,
                    onGoToSettings = { userLocationManager.openAppSettings() },
                    backgroundColor = KrailTheme.colors.surface,
                    modifier = Modifier.align(Alignment.TopCenter)
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
                    onSelectStop = {
                        // Dismiss keyboard and clear focus - IMPORTANT: Order matters!
                        keyboard?.hide()
                        focusRequester?.freeFocus()

                        // Convert NearbyStopFeature to StopItem and call onStopSelect
                        val stopItem = StopItem(
                            stopId = stop.stopId,
                            stopName = stop.stopName,
                        )
                        onStopSelect(stopItem)

                        // Track the selection
                        onEvent(
                            SearchStopUiEvent.TrackStopSelected(
                                stopItem = stopItem,
                                isRecentSearch = false,
                                searchQuery = null,
                            ),
                        )

                        selectedStop = null
                    },
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
                    transportModes = persistentListOf(TransportMode.Train()),
                ),
                NearbyStopFeature(
                    stopId = "stop_2",
                    stopName = "Town Hall",
                    position = LatLng(
                        latitude = -33.8734,
                        longitude = 151.2069,
                    ),
                    transportModes = persistentListOf(TransportMode.Metro()),
                ),
            ),
            selectedTransportModes = persistentSetOf(
                TransportMode.Train().productClass,
                TransportMode.Metro().productClass,
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
