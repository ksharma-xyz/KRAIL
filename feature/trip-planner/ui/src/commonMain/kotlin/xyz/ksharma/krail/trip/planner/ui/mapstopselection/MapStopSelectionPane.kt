package xyz.ksharma.krail.trip.planner.ui.mapstopselection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.SearchStopMap
import xyz.ksharma.krail.trip.planner.ui.state.mapstopselection.MapStopSelectionEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * Edge-to-edge map pane reusable by any screen that needs spatial stop picking.
 *
 * - Map data lives in a shared [MapStopSelectionViewModel] (Koin singleton). The
 *   pane subscribes to its state, fires Initialize once on first attach, and
 *   forwards SearchStopMap's internal user-location event back to the VM. All other
 *   SearchStopUiEvent variants (analytics, options sheet, radius/mode filters) are
 *   ignored — the consumer screen owns those behaviours.
 * - [onStopSelected] fires when the user confirms a stop pick from the map's
 *   bottom sheet. The consumer decides what to do with it (set From, set To, etc).
 * - [topOverlay] is a `BoxScope` slot for the consumer's contextual banner — typically
 *   a "tap a stop to set start" pill aligned to TopCenter with statusBarsPadding.
 *   The pane stays opinion-free about the banner's text and tap behaviour.
 *
 * See docs/TABLET_FOLDABLE_UX.md §4.
 */
@Composable
fun MapStopSelectionPane(
    onStopSelected: (StopItem) -> Unit,
    modifier: Modifier = Modifier,
    topOverlay: @Composable BoxScope.() -> Unit = {},
) {
    log("[MAP_STOP_SEL] Pane composing")

    val viewModel: MapStopSelectionViewModel = koinInject()
    val mapState by viewModel.mapUiState.collectAsStateWithLifecycle()

    SideEffect {
        val readyKind = when (val state = mapState) {
            is MapUiState.Ready ->
                "Ready(nearby=${state.mapDisplay.nearbyStops.size}, " +
                    "userLoc=${state.mapDisplay.userLocation != null}, " +
                    "isLoadingNearby=${state.isLoadingNearbyStops})"
            is MapUiState.Loading -> "Loading"
            is MapUiState.Error -> "Error(${state.message})"
        }
        log("[MAP_STOP_SEL] Pane mapState=$readyKind")
    }

    val statusBarTopPadding = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = Color.Red) // DEBUG — if you see RED, layout works, map lib is the issue
            .onSizeChanged { log("[MAP_STOP_SEL] Pane onSizeChanged size=${it.width}x${it.height}") },
    ) {
        SearchStopMap(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { log("[MAP_STOP_SEL] SearchStopMap onSizeChanged size=${it.width}x${it.height}") },
            mapUiState = mapState,
            ornamentTopPadding = statusBarTopPadding,
            onEvent = { sse ->
                // Translate the events MapStopSelectionViewModel cares about.
                // Everything else (sheet open, options, analytics) is ignored at
                // this layer; SearchStopMap manages those internally.
                when (sse) {
                    is SearchStopUiEvent.UserLocationUpdated ->
                        viewModel.onEvent(
                            MapStopSelectionEvent.UserLocationUpdated(sse.location),
                        )
                    else -> Unit
                }
            },
            onStopSelect = onStopSelected,
        )

        topOverlay()
    }
}
