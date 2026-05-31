package xyz.ksharma.krail.trip.planner.ui.mapstopselection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.SearchStopMap
import xyz.ksharma.krail.trip.planner.ui.state.mapstopselection.MapStopSelectionEvent
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
 * - [topOverlay] is a `BoxScope` slot for an optional contextual banner. Suppressed
 *   automatically while the location-permission banner is visible.
 *
 * See docs/TABLET_FOLDABLE_UX.md §4.
 */
@Composable
fun MapStopSelectionPane(
    mapUiState: MapUiState,
    onEvent: (MapStopSelectionEvent) -> Unit,
    modifier: Modifier = Modifier,
    onStopSelected: (StopItem) -> Unit = {},
    topOverlay: @Composable BoxScope.() -> Unit = {},
) {
    val statusBarTopPadding = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()

    // Suppress the consumer's topOverlay while the location-permission banner is
    // visible inside SearchStopMap so the two don't overlap.
    var isPermissionBannerShowing by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        SearchStopMap(
            modifier = Modifier.fillMaxSize(),
            mapUiState = mapUiState,
            ornamentTopPadding = statusBarTopPadding,
            onPermissionBannerVisibilityChanged = { visible ->
                isPermissionBannerShowing = visible
            },
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

        if (!isPermissionBannerShowing) {
            topOverlay()
        }
    }
}
