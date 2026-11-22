package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.SoftwareKeyboardController
import kotlinx.collections.immutable.ImmutableList
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.LabelAssignSurface
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * What a row in the list needs to react to a tap. One holder so the lazy sections below take a
 * handful of arguments each instead of ten.
 */
internal data class StopRowActions(
    val keyboard: SoftwareKeyboardController?,
    val focusRequester: FocusRequester,
    val stopLabels: ImmutableList<StopLabel>,
    val expandedStopKey: String?,
    val onToggleExpandStop: (String) -> Unit,
    val onStopSelect: (StopItem) -> Unit,
    val onLabelPillClick: (StopItem, StopLabel, LabelAssignSurface) -> Unit,
    val onNewLabelClick: (StopItem, List<TransportMode>, LabelAssignSurface) -> Unit,
    val onEvent: (SearchStopUiEvent) -> Unit,
)

/** Picks [stopItem]: puts the keyboard away, hands the stop on and reports the choice. */
internal fun StopRowActions.pick(stopItem: StopItem, isRecentSearch: Boolean = false) {
    keyboard?.hide()
    focusRequester.freeFocus()
    onStopSelect(stopItem)
    onEvent(SearchStopUiEvent.TrackStopSelected(stopItem = stopItem, isRecentSearch = isRecentSearch))
}
