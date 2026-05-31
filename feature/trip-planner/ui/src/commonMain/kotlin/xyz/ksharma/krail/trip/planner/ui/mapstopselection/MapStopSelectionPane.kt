package xyz.ksharma.krail.trip.planner.ui.mapstopselection

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.SearchStopMap
import xyz.ksharma.krail.trip.planner.ui.state.mapstopselection.MapStopSelectionEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapDisplay
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * Edge-to-edge map pane showing nearby stops. Reusable by any screen.
 *
 * - [mapUiState] is collected by the entry and passed in — the pane holds no VM reference.
 * - [onEvent] forwards map interactions (center change, user location) back to the caller.
 * - [onStopSelected] fires when the user confirms a stop pick from the map's bottom sheet.
 *   Defaults to a no-op — callers that don't need stop-picking can omit it (read-only map).
 *
 * See docs/TABLET_FOLDABLE_UX.md §4.
 */
@Composable
fun MapStopSelectionPane(
    mapUiState: MapUiState,
    onEvent: (MapStopSelectionEvent) -> Unit,
    modifier: Modifier = Modifier,
    onStopSelected: (StopItem) -> Unit = {},
) {
    val statusBarTopPadding = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()

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
                else -> Unit
            }
        },
        onStopSelect = onStopSelected,
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
