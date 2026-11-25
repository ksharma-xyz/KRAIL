package xyz.ksharma.krail.trip.planner.ui.mapstopselection

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.modifier.cornerSafeTopPadding
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.MapEntry
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.MapOpenTracker
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.SearchStopMap
import xyz.ksharma.krail.trip.planner.ui.state.mapstopselection.MapStopSelectionEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapDisplay
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.stopcard.StopCardOptions

/**
 * Edge-to-edge map pane showing nearby stops. Reusable by any screen.
 *
 * - [mapUiState] is collected by the entry and passed in — the pane holds no VM reference.
 * - [onEvent] forwards map interactions (center change, user location) back to the caller.
 * - [onStopSelected] fires when the user confirms a stop pick from the floating stop card a pin opens.
 *   Defaults to a no-op — callers that don't need stop-picking can omit it (read-only map).
 * - [stopCardOptions] is what the card offers besides the board: Save as, and Go here or Start here.
 * - [allowPermissionRequest] and [onPermissionStatus] are the host's say over the location permission: the map
 *   never raises the system dialog until the host allows it, and reports what the permission turned out to be.
 * - [mapOpen] and [mapEntry] count each stretch of this map being on screen for analytics, under the entry that
 *   hosts it. Null counts nothing.
 *
 * See docs/TABLET_FOLDABLE_UX.md §4.
 */
@Composable
fun MapStopSelectionPane(
    mapUiState: MapUiState,
    onEvent: (MapStopSelectionEvent) -> Unit,
    modifier: Modifier = Modifier,
    onStopSelected: (StopItem) -> Unit = {},
    stopCardOptions: StopCardOptions = StopCardOptions(),
    allowPermissionRequest: Boolean = false,
    onPermissionStatus: (PermissionStatus) -> Unit = {},
    mapOpen: MapOpenTracker? = null,
    mapEntry: MapEntry = MapEntry.SEARCH_STOP,
) {
    // Corner-safe rather than the status bar's height: on iPhone Duo's inner display the
    // status bar sits on the side, the top inset is zero, and the location pill touched the top.
    val statusBarTopPadding = cornerSafeTopPadding()

    SearchStopMap(
        modifier = modifier.fillMaxSize(),
        mapUiState = mapUiState,
        ornamentTopPadding = statusBarTopPadding,
        onEvent = { sse ->
            when (sse) {
                is SearchStopUiEvent.MapCenterChanged ->
                    onEvent(MapStopSelectionEvent.MapCenterChanged(sse.center))
                is SearchStopUiEvent.UserLocationUpdated ->
                    onEvent(MapStopSelectionEvent.UserLocationUpdated(sse.location))
                is SearchStopUiEvent.ParkRideCardOpened ->
                    onEvent(MapStopSelectionEvent.ParkRideCardOpened(sse.stopId))
                SearchStopUiEvent.ParkRideCardClosed ->
                    onEvent(MapStopSelectionEvent.ParkRideCardClosed)
                is SearchStopUiEvent.ParkRideDirectionsClicked -> onEvent(
                    MapStopSelectionEvent.ParkRideDirectionsClicked(sse.latitude, sse.longitude, sse.label),
                )
                else -> Unit
            }
        },
        onStopSelect = onStopSelected,
        stopCardOptions = stopCardOptions,
        allowPermissionRequest = allowPermissionRequest,
        onPermissionStatus = onPermissionStatus,
        mapOpen = mapOpen,
        mapEntry = mapEntry,
    )
}

// region Previews

@ScreenshotTest
@PreviewComponent
@Composable
private fun PreviewMapStopSelectionPane_Loading() {
    PreviewTheme {
        MapStopSelectionPane(
            mapUiState = MapUiState.Loading,
            onEvent = {},
        )
    }
}

@ScreenshotTest
@PreviewScreen
@Composable
private fun PreviewMapStopSelectionPane_Ready() {
    PreviewTheme {
        MapStopSelectionPane(
            mapUiState = MapUiState.Ready(mapDisplay = MapDisplay()),
            onEvent = {},
        )
    }
}

// endregion
