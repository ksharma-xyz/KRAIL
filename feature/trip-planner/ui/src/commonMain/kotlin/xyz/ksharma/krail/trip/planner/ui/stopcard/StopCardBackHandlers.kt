package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler

/**
 * System back while a stop card is up, one step at a time: it shuts an expanded Departure Board first, then dismisses the
 * card, and with neither to do it is not answered here, so it is the screen's own two step back (`docs/MAP_HOME_UX.md`).
 * [stopCardBackStepFor] decides which of the three it is. The card's handler is registered first and the board's after it,
 * because the handler registered last that is enabled is the one that answers.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun StopCardBackHandlers(
    cardOpen: Boolean,
    boardExpanded: Boolean,
    covered: Boolean,
    onCollapseBoard: () -> Unit,
    onDismissCard: () -> Unit,
) {
    val step = stopCardBackStepFor(cardOpen = cardOpen, boardExpanded = boardExpanded, covered = covered)
    BackHandler(
        enabled = step == StopCardBackStep.DismissCard || step == StopCardBackStep.CollapseBoard,
        onBack = onDismissCard,
    )
    BackHandler(enabled = step == StopCardBackStep.CollapseBoard, onBack = onCollapseBoard)
}
