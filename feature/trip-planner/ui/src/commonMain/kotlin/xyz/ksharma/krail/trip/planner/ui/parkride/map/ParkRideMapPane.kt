package xyz.ksharma.krail.trip.planner.ui.parkride.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.krail.core.maps.data.location.rememberUserLocationManager
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.maps.state.UserLocationConfig
import xyz.ksharma.krail.core.maps.ui.components.LocationPermissionBanner
import xyz.ksharma.krail.core.maps.ui.components.UserLocationButton
import xyz.ksharma.krail.core.maps.ui.config.MapConfig.Ornaments.ATTRIBUTION_ALIGNMENT
import xyz.ksharma.krail.core.maps.ui.config.MapConfig.Ornaments.ATTRIBUTION_ENABLED
import xyz.ksharma.krail.core.maps.ui.config.MapConfig.Ornaments.LOGO_ENABLED
import xyz.ksharma.krail.core.maps.ui.config.MapTileProvider.OPEN_FREE_MAP_LIBERTY
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.TrackUserLocation
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.UserLocationLayer
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.handleLocationButtonClick
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.initialCameraPosition
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.toPosition
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.StationPosition
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState.ParkRideFacilityDetail
import xyz.ksharma.krail.trip.planner.ui.stopcard.ParkRidePinCard
import xyz.ksharma.krail.trip.planner.ui.stopcard.StopCardPlacement
import xyz.ksharma.krail.trip.planner.ui.stopcard.controlsClearance
import xyz.ksharma.krail.trip.planner.ui.stopcard.toPinCardTile
import kotlin.time.Duration.Companion.milliseconds

/**
 * Map pane for the Park & Ride picker, shown on the right in dual-pane layouts.
 *
 * Tapping a `P` opens the floating [ParkRidePinCard], the stop card's own shell, with the station's
 * car parks stacked and its action adding or removing the station, so selecting from a map feels
 * the same in both places. It is a card over the map and never a bottom sheet.
 *
 * The map plots from locally-stored GTFS coordinates only. It never triggers an availability
 * fetch, so the polling lifecycle in `docs/POLLING_LIFECYCLE.md` is unaffected.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ParkRideMapPane(
    stations: List<ParkRideStationPickerItem>,
    selectedStation: ParkRideStationPickerItem?,
    details: ImmutableList<ParkRideFacilityDetail>,
    onStationSelected: (ParkRideStationPickerItem) -> Unit,
    onDismissStation: () -> Unit,
    onToggleStation: (ParkRideStationPickerItem) -> Unit,
    onDirectionsClick: (StationPosition, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Same location UX as the SearchStop map: auto-centre on a known fix, a recentre button
    // bottom-right, and a permission banner that routes to settings on a hard denial.
    val userLocationManager = rememberUserLocationManager()
    val scope = rememberCoroutineScope()
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var showPermissionBanner by remember { mutableStateOf(false) }
    // False until the rider taps the location button, so the screen never asks for permission
    // unprompted.
    var allowPermissionRequest by remember { mutableStateOf(false) }
    var cardHeight by remember { mutableStateOf(0.dp) }

    // System back closes the card first, and with none open it is off and the screen's own answers.
    BackHandler(enabled = selectedStation != null) { onDismissStation() }

    Box(modifier = modifier.fillMaxSize()) {
        val cameraState = rememberCameraState(firstPosition = initialCameraPosition(userLocation))

        TrackUserLocation(
            userLocationManager = userLocationManager,
            allowPermissionRequest = allowPermissionRequest,
            onLocationUpdate = { latLng ->
                showPermissionBanner = false
                userLocation = latLng
            },
            onPermissionDeny = { status ->
                showPermissionBanner = status is PermissionStatus.Denied
            },
        )

        // Key on a STABLE boolean, never the churning LatLng: GPS jitter re-keying this effect
        // cancels the in-flight animateTo and leaves the camera frozen mid-animation.
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

        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            cameraState = cameraState,
            baseStyle = BaseStyle.Uri(OPEN_FREE_MAP_LIBERTY),
            // A tap on the map itself, not on a pin (the pin layer consumes its own taps), closes the card.
            onMapClick = { _, _ ->
                if (selectedStation != null) onDismissStation()
                ClickResult.Pass
            },
            options = MapOptions(
                ornamentOptions = OrnamentOptions(
                    padding = PaddingValues(),
                    isLogoEnabled = LOGO_ENABLED,
                    isAttributionEnabled = ATTRIBUTION_ENABLED,
                    attributionAlignment = ATTRIBUTION_ALIGNMENT,
                    isCompassEnabled = false,
                    isScaleBarEnabled = false,
                ),
            ),
        ) {
            ParkRideStationsLayer(
                stations = stations,
                onStationClick = onStationSelected,
            )

            // Always drawn last so the fix sits above the station pins.
            UserLocationLayer(userLocation = userLocation)
        }

        if (showPermissionBanner) {
            LocationPermissionBanner(
                onGoToSettings = { userLocationManager.openAppSettings() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(KrailTheme.dimensions.spacingXL),
            )
        }

        UserLocationButton(
            isActive = hasUserLocation,
            onClick = {
                scope.launch {
                    handleLocationButtonClick(
                        userLoc = userLocation,
                        cameraState = cameraState,
                        userLocationManager = userLocationManager,
                        onPermissionDenied = { showPermissionBanner = true },
                        onRequestPermission = { allowPermissionRequest = true },
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                // Rides above an open card, so the card never covers it.
                .padding(bottom = if (selectedStation != null) controlsClearance(cardHeight) else 0.dp)
                .navigationBarsPadding()
                .padding(KrailTheme.dimensions.spacingXL),
        )

        // The floating Park & Ride card, over the map and never a sheet. A tap on the map or system
        // back closes it. Its action adds or removes the station, so selecting from a map feels the
        // same here as on the stop card, and Directions go to each car park's own position.
        selectedStation?.let { station ->
            val tile = remember(station, details) { station.toPinCardTile(details) }
            StopCardPlacement(topReserve = 0.dp, onHeight = { cardHeight = it }) {
                ParkRidePinCard(
                    tile = tile,
                    isOnHome = station.added,
                    onToggleHome = {
                        onToggleStation(station)
                        onDismissStation()
                    },
                    onDirectionsClick = { carPark ->
                        carPark.directionsTo?.let { target ->
                            onDirectionsClick(StationPosition(target.latitude, target.longitude), carPark.name)
                        }
                    },
                    onClose = onDismissStation,
                )
            }
        }
    }
}
