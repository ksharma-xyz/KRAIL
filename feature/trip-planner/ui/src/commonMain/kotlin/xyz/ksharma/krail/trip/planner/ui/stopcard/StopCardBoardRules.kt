package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.departures.ui.state.DeparturesState

/** Where the departure board on a stop card is, from what the rider has done and what has landed. */
internal enum class BoardPhase {
    /** The chevron has not been opened, so nothing has been asked of the network. */
    Collapsed,

    /** Opened, and the first board has not landed. */
    Loading,

    /** Opened, and the request failed with nothing to show. */
    Failed,

    /** Opened, and the stop has no departures. */
    Empty,

    /** Opened, with rows. A refresh that failed leaves the rows as they were, so it reads as this. */
    Content,
}

/**
 * What the board's heading and body show, decided from the departures state alone.
 *
 * Rule 22 of `docs/MAP_HOME_UX.md`: nothing jumps as data arrives. So an open board holds a floor
 * of height from the moment it opens ([holdsBodyFloor]), which the loading dots, the error, the
 * empty message and a short list all stand inside.
 *
 * @property refreshing a refresh is in flight and the rows stay on screen.
 */
@Stable
internal data class StopCardBoard(
    val phase: BoardPhase,
    val refreshing: Boolean,
    val holdsBodyFloor: Boolean,
)

/**
 * The board's phase and the parts that follow from it. A failed request with rows already on
 * screen is not [BoardPhase.Failed]: the repository keeps the rows and clears the error, and the
 * card keeps them too, with no banner and no change in height.
 */
internal fun stopCardBoard(state: DeparturesState, expanded: Boolean): StopCardBoard {
    if (!expanded) {
        return StopCardBoard(BoardPhase.Collapsed, refreshing = false, holdsBodyFloor = false)
    }
    val phase = when {
        state.isLoading -> BoardPhase.Loading
        state.isError -> BoardPhase.Failed
        state.departures.isEmpty() -> BoardPhase.Empty
        else -> BoardPhase.Content
    }
    return StopCardBoard(
        phase = phase,
        refreshing = phase == BoardPhase.Content && state.silentLoading,
        holdsBodyFloor = true,
    )
}

/**
 * The height an open board never shrinks below at 100 percent text. It is the height of the tallest
 * thing a board shows in place of rows, the failed message with its Retry, which the layout test
 * measured at 296 dp at normal text on a density 1 window, so the loading dots, the empty message
 * and a short list all stand in the room the failed message takes and the card does not change
 * height from one phase to the next. A board with four or more rows is taller than this and grows
 * past it. Measured in a host test, not on a device: read it against a device.
 */
internal val BOARD_FLOOR_BASE_HEIGHT: Dp = 296.dp

/** The board's floor at [fontScale], growing with the text because the rows do. */
internal fun boardFloorHeight(fontScale: Float): Dp = BOARD_FLOOR_BASE_HEIGHT * fontScale.coerceAtLeast(1f)
