package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.departures.ui.state.model.StopDeparture

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
 * empty message and a short list all stand inside, and the Lines chip keeps its room in the
 * heading while the board loads ([reservesLinesChip]) so the chevron does not move when it lands.
 *
 * @property showsLinesChip the one Lines chip, only when the stop has more than one line (decision
 * G2). With one line there is nothing to choose between, so there is no chip.
 * @property refreshing a refresh is in flight and the rows stay on screen.
 */
@Stable
internal data class StopCardBoard(
    val phase: BoardPhase,
    val lineCount: Int,
    val showsLinesChip: Boolean,
    val reservesLinesChip: Boolean,
    val refreshing: Boolean,
    val holdsBodyFloor: Boolean,
)

/** How many lines, not departures, a board has: the same line leaving four times is one line. */
internal fun distinctLineCount(departures: List<StopDeparture>): Int =
    departures.distinctBy { it.lineNumber }.size

/** The chip is for choosing between lines, so a stop with one line, or none yet, has no chip. */
internal fun showsLinesChip(lineCount: Int): Boolean = lineCount > 1

/**
 * The board's phase and the parts that follow from it. A failed request with rows already on
 * screen is not [BoardPhase.Failed]: the repository keeps the rows and clears the error, and the
 * card keeps them too, with no banner and no change in height.
 */
internal fun stopCardBoard(state: DeparturesState, expanded: Boolean): StopCardBoard {
    if (!expanded) {
        return StopCardBoard(BoardPhase.Collapsed, 0, false, false, false, false)
    }
    val phase = when {
        state.isLoading -> BoardPhase.Loading
        state.isError -> BoardPhase.Failed
        state.departures.isEmpty() -> BoardPhase.Empty
        else -> BoardPhase.Content
    }
    val lineCount = if (phase == BoardPhase.Content) distinctLineCount(state.departures) else 0
    return StopCardBoard(
        phase = phase,
        lineCount = lineCount,
        showsLinesChip = phase == BoardPhase.Content && showsLinesChip(lineCount),
        reservesLinesChip = phase == BoardPhase.Loading,
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

/** One line a stop's board lists, for the Lines chip's badges. */
@Stable
internal data class LineBadge(val lineNumber: String, val colorHex: String, val modeName: String)

/**
 * The lines of a board, one badge per line, grouped by mode in the app's mode order (train before
 * bus and so on) and in the order the departures list them inside a mode. A mode the app does not
 * know sorts last.
 */
internal fun lineBadges(departures: List<StopDeparture>): ImmutableList<LineBadge> =
    departures
        .distinctBy { it.lineNumber }
        .map { LineBadge(it.lineNumber, it.lineColorCode, it.transportModeName) }
        .sortedBy { badge -> TransportMode.all.find { it.name == badge.modeName }?.priority ?: Int.MAX_VALUE }
        .toImmutableList()

/** Tapping a line badge selects it, and tapping the selected one clears the filter. */
internal fun lineFilterAfterTap(selected: String?, tapped: String): String? =
    if (selected == tapped) null else tapped
