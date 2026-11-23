package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import xyz.ksharma.krail.trip.planner.ui.components.ToFromField
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/** Everything the form needs, in one holder so the two layouts take it as one argument. */
internal data class SearchStopFormModel(
    val shape: FormShape,
    val editing: ToFromField?,
    val toPlaceholder: String,
    val fromPlaceholder: String,
    val toStop: StopItem?,
    val fromStop: StopItem?,
    val fieldState: TextFieldState,
    val focusRequester: FocusRequester,
    val onFlowEvent: (SearchStopFlowEvent) -> Unit,
    val onGoClick: () -> Unit,
    val onTextChange: (String) -> Unit,
    /** The saved label of the stop each row holds (Home, Work), or null. The card names the stop with it and draws its icon. */
    val fromLabel: String? = null,
    val toLabel: String? = null,
    /** Reverses From and To. Null draws no swap: the rider has picked no stop yet, so there is nothing to reverse. */
    val onSwap: (() -> Unit)? = null,
)
