package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.Dp
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent

/**
 * What the map draws on its bottom line for the map state: the Options button and its mode block, or nothing. One rule
 * says where it is ([modeControlsFor]): shown only with the location allowed ([locationGranted]) and no stop card open
 * ([stopCardOpen]). A block that was open is shut as the button goes, and stays shut when it returns, so the rider
 * comes back to the map as they left it. [modeBlockOpen] is the screen's, and [onModeBlockEvent] moves it.
 */
@Composable
internal fun BoxScope.SearchStopBottomControls(
    mode: SearchStopMode,
    mapState: MapUiState,
    locationGranted: Boolean,
    stopCardOpen: Boolean,
    modeBlockOpen: Boolean,
    onModeBlockEvent: (ModeBlockEvent) -> Unit,
    pillHeight: Dp,
    onPillHeight: (Dp) -> Unit,
    clearance: Dp,
    onEvent: (SearchStopUiEvent) -> Unit,
) {
    val ready = mapState as? MapUiState.Ready
    val controls = modeControlsFor(
        mode = mode,
        mapReady = ready != null,
        locationGranted = locationGranted,
        stopCardOpen = stopCardOpen,
    )
    LaunchedEffect(controls) { modeBlockEventFor(controls)?.let(onModeBlockEvent) }
    if (controls != ModeControls.Absent && ready != null) {
        SearchStopModeControls(
            open = modeBlockOpen,
            visible = controls == ModeControls.Shown,
            selectedModes = ready.mapDisplay.selectedTransportModes,
            onButtonClick = { onModeBlockEvent(ModeBlockEvent.ButtonTapped) },
            onToggleMode = { onEvent(SearchStopUiEvent.TransportModeFilterToggled(it)) },
            pillHeight = pillHeight,
            onPillHeight = onPillHeight,
            clearance = clearance,
        )
    }
}
