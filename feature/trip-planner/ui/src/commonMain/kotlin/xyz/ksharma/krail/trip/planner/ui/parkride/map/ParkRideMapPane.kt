package xyz.ksharma.krail.trip.planner.ui.parkride.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import app.krail.taj.resources.ic_location
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.style.BaseStyle
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.krail.core.maps.data.location.rememberUserLocationManager
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.maps.state.UserLocationConfig
import xyz.ksharma.krail.core.maps.ui.components.LocationPermissionBanner
import xyz.ksharma.krail.core.maps.ui.components.UserLocationButton
import xyz.ksharma.krail.core.maps.ui.config.MapConfig.Ornaments.ATTRIBUTION_ENABLED
import xyz.ksharma.krail.core.maps.ui.config.MapConfig.Ornaments.LOGO_ENABLED
import xyz.ksharma.krail.core.maps.ui.config.MapTileProvider.OPEN_FREE_MAP_LIBERTY
import xyz.ksharma.krail.taj.LocalContentColor
import xyz.ksharma.krail.taj.components.AnimatedDots
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.components.ParkRideIcon
import xyz.ksharma.krail.trip.planner.ui.components.map.StopDetailsBottomSheet
import xyz.ksharma.krail.trip.planner.ui.savedtrips.ParkRideLoadedContent
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.TrackUserLocation
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.UserLocationLayer
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.handleLocationButtonClick
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.initialCameraPosition
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.toPosition
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.StationPosition
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState.ParkRideFacilityDetail
import kotlin.time.Duration.Companion.milliseconds
import app.krail.taj.resources.Res as TajRes

/**
 * Map pane for the Park & Ride picker, shown on the right in dual-pane layouts.
 *
 * Tapping a `P` opens the same [StopDetailsBottomSheet] the SearchStop map uses for a stop,
 * with the primary action adding or removing the station — so selecting from a map feels the
 * same in both places.
 *
 * The map plots from locally-stored GTFS coordinates only. It never triggers an availability
 * fetch, so the polling lifecycle in `docs/POLLING_LIFECYCLE.md` is unaffected.
 */
@Composable
internal fun ParkRideMapPane(
    stations: List<ParkRideStationPickerItem>,
    selectedStation: ParkRideStationPickerItem?,
    details: ImmutableList<ParkRideFacilityDetail>,
    isLoadingDetails: Boolean,
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
            options = MapOptions(
                ornamentOptions = OrnamentOptions(
                    padding = PaddingValues(),
                    isLogoEnabled = LOGO_ENABLED,
                    isAttributionEnabled = ATTRIBUTION_ENABLED,
                    attributionAlignment = Alignment.BottomEnd,
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
                .navigationBarsPadding()
                .padding(KrailTheme.dimensions.spacingXL),
        )

        selectedStation?.let { station ->
            StopDetailsBottomSheet(
                stopId = station.stationId,
                stopName = station.stationName,
                transportModes = persistentListOf(),
                onDismiss = onDismissStation,
                // Same badge the home card and picker rows use, so a station is recognisable
                // the moment the sheet opens.
                leadingIcon = { ParkRideIcon() },
                additionalInfo = {
                    ParkRideSheetAvailability(
                        details = details,
                        isLoading = isLoadingDetails,
                    )
                },
                actionButton = {
                    ParkRideSheetActions(
                        isAdded = station.added,
                        // Only offer directions when the station actually has a position;
                        // otherwise the button would open a map at null island.
                        canShowDirections = station.position != null,
                        onToggle = {
                            onToggleStation(station)
                            onDismissStation()
                        },
                        onDirections = {
                            station.position?.let { position ->
                                onDirectionsClick(position, station.stationName)
                            }
                        },
                    )
                },
            )
        }
    }
}

/**
 * Live availability for the tapped station, rendered with the same
 * [ParkRideLoadedContent] the home card uses so the numbers read identically in both places.
 *
 * Cached rows appear immediately; the spinner only shows while a fetch is genuinely in
 * flight and nothing is cached yet.
 */
@Composable
private fun ParkRideSheetAvailability(
    details: ImmutableList<ParkRideFacilityDetail>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dim.spacingXL),
        verticalArrangement = Arrangement.spacedBy(dim.spacingM),
    ) {
        when {
            // Cached rows win over the spinner: a facility still on cooldown has data to show
            // immediately and never hits the network, so it should not flash a loader.
            details.isNotEmpty() -> details.forEach { detail ->
                ParkRideLoadedContent(parkRideFacilityDetail = detail)
            }

            isLoading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dim.spacingXXXL),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedDots(
                    modifier = Modifier.size(width = LOADING_DOTS_WIDTH, height = dim.spacingXXXL),
                    color = KrailTheme.colors.onSurface,
                )
            }

            else -> Text(
                text = "Parking numbers are not available right now.",
                style = KrailTheme.typography.bodyMedium,
                color = KrailTheme.colors.softLabel,
            )
        }
    }

    Spacer(modifier = Modifier.height(dim.spacingXXL))
}

private val LOADING_DOTS_WIDTH = 80.dp

/**
 * Sheet actions: add/remove the station, and drive to it.
 *
 * Directions is a secondary action next to the primary add button — it is useful, but it is
 * not why the picker exists, so it does not compete for the filled button.
 */
@Composable
private fun ParkRideSheetActions(
    isAdded: Boolean,
    canShowDirections: Boolean,
    onToggle: () -> Unit,
    onDirections: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dim.spacingXL),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingL),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Weighted, because largeButtonSize fills the available width: without a weight the
        // first button takes the whole row and pushes Directions off-screen entirely.
        Button(
            onClick = onToggle,
            dimensions = ButtonDefaults.largeButtonSize(),
            modifier = Modifier.weight(1f),
        ) {
            Text(text = if (isAdded) "Remove Park & Ride" else "Add Park & Ride")
        }

        if (canShowDirections) {
            Button(
                onClick = onDirections,
                colors = ButtonDefaults.subtleButtonColors(),
                dimensions = ButtonDefaults.largeButtonSize(),
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dim.spacingXS),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(TajRes.drawable.ic_location),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(LocalContentColor.current),
                        modifier = Modifier.size(dim.iconS),
                    )
                    Text(text = "Directions")
                }
            }
        }
    }
}
