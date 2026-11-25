package xyz.ksharma.krail.trip.planner.ui.homemap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.parkride.map.ParkRideStationsLayer
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.MapEntry
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.MapOpenTracker
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.SearchStopMap
import xyz.ksharma.krail.trip.planner.ui.state.mapstopselection.MapStopSelectionEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.StationPosition
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapDisplay
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent

/**
 * The one map behind the home cards, or beside them on a wide window. It is the Search Stop map with the
 * rider's dot, the stop pins, the `P` pins and the Home and Work pins, and no locate button: the dot does
 * that job (`docs/MAP_HOME_UX.md` rules 20 and 30).
 *
 * It draws no card. A tap on a pin reports the key of the card it opens through [onCardKey], and a tap on
 * the map reports null, so the screen draws the card above the cards and the chips and nothing is ever laid
 * over content (rule 42). It is a sibling of everything with a gradient or a blur, never a child of one, so
 * the native map renders on iOS (`docs/MAPLIBRE_IOS_PITFALLS.md` section 1).
 *
 * It is decorative to a screen reader. A pin is a drawn feature with no node of its own, so the stops are
 * reached through the cards and Search Stop, and the map region holds no focus.
 */
@Composable
internal fun HomeMapPane(
    mapUiState: MapUiState,
    pins: HomePins,
    allowPermissionRequest: Boolean,
    onPermissionStatus: (PermissionStatus) -> Unit,
    onMapEvent: (MapStopSelectionEvent) -> Unit,
    onCardKey: (String?) -> Unit,
    modifier: Modifier = Modifier,
    ornamentTopPadding: Dp = 0.dp,
    mapOpen: MapOpenTracker? = null,
) {
    val stationItems = remember(pins.parkRide) { pins.parkRide.map { it.toPickerItem() } }
    val drawn = remember(mapUiState, pins.stops) { mapUiState.withNearbyStops(pins.stops) }

    Box(modifier = modifier.fillMaxSize().clearAndSetSemantics { }) {
        SearchStopMap(
            mapUiState = drawn,
            ornamentTopPadding = ornamentTopPadding,
            onEvent = { event ->
                when (event) {
                    is SearchStopUiEvent.MapCenterChanged ->
                        onMapEvent(MapStopSelectionEvent.MapCenterChanged(event.center))
                    is SearchStopUiEvent.UserLocationUpdated ->
                        onMapEvent(MapStopSelectionEvent.UserLocationUpdated(event.location))
                    else -> Unit
                }
            },
            allowPermissionRequest = allowPermissionRequest,
            onPermissionStatus = onPermissionStatus,
            onMapTap = { onCardKey(null) },
            onStopPinTap = { stop -> onCardKey(stopCardKey(stop.stopId)) },
            showsLocationButton = false,
            // Home draws its own `P` pins below, for the stations its cards open for.
            showsParkRideStops = false,
            mapOpen = mapOpen,
            mapEntry = MapEntry.HOME,
            extraLayers = {
                ParkRideStationsLayer(
                    stations = stationItems,
                    onStationClick = { station -> onCardKey(parkRideCardKey(station.stationId)) },
                )
                LabelPinsLayer(
                    labels = pins.labels,
                    onLabelClick = { pin -> onCardKey(stopCardKey(pin.stopId)) },
                )
            },
        )
    }
}

/** The picker's own item for a saved station, so the `P` layer the picker map draws draws this one too. */
private fun ParkRidePin.toPickerItem() = ParkRideStationPickerItem(
    stationId = stationId,
    stationName = stationName,
    stopName = stationName,
    carParkNames = emptyList(),
    mappings = emptyList(),
    position = StationPosition(latitude = position.latitude, longitude = position.longitude),
)

// region Previews

@PreviewScreen
@Composable
private fun PreviewHomeMapPane() {
    PreviewTheme {
        HomeMapPane(
            mapUiState = MapUiState.Ready(mapDisplay = MapDisplay()),
            pins = HomePins(),
            allowPermissionRequest = false,
            onPermissionStatus = {},
            onMapEvent = {},
            onCardKey = {},
        )
    }
}

// endregion
