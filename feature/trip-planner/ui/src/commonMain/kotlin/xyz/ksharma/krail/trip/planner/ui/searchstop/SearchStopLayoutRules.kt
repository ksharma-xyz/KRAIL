package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.theme.KrailDimensions
import xyz.ksharma.krail.taj.theme.KrailTheme

// The layout of the Search Stop map state, `docs/MAP_HOME_UX.md` rules 9, 10, 33, 35, 36 and 39. Every
// decision is a pure function or one table, so the order of the rows, which of them show and every gap are
// pinned without a layout.

/** One row of the group card at the top, in the order it is drawn. */
internal enum class GroupRow { To, Divider, From, LocationChip }

/**
 * The rows of the group card, top to bottom. The To bar is always first. The From bar comes under it with a
 * thin divider between them when the form is [FormShape.ToAndFrom], and a single bar (a label, or one leg of
 * a trip) has neither. The location status chip is the last row and only while [pill] is not hidden, so it
 * never sits between the bars. The mode chips are not in this card at all (see [SearchStopModeControls]).
 */
internal fun groupCardRows(shape: FormShape, pill: LocationPill): List<GroupRow> = buildList {
    add(GroupRow.To)
    if (shape == FormShape.ToAndFrom) {
        add(GroupRow.Divider)
        add(GroupRow.From)
    }
    if (pill != LocationPill.Hidden) add(GroupRow.LocationChip)
}

/**
 * Every gap of the map state, from the one spacing scale in `KrailDimensions` (4, 8, 12, 16):
 * - [edge] is the system top inset to the group card, and the card to the screen's left and right edges.
 * - [cardPadding] is the inner padding of every card, the card's own padding token on all four sides.
 * - [cardCorner] is the corner of every card, the card's own radius token.
 * - [betweenRows] is the one gap between rows inside the group card, around its divider, and above the chip.
 * - [betweenCards] is the one gap between floating elements: group card to list card, mode block to its button,
 *   and the mode controls to the control row beneath them.
 */
internal data class SearchStopGaps(
    val edge: Dp,
    val cardPadding: Dp,
    val cardCorner: Dp,
    val betweenRows: Dp,
    val betweenCards: Dp,
)

internal fun searchStopGaps(dim: KrailDimensions = KrailDimensions()): SearchStopGaps = SearchStopGaps(
    edge = dim.spacingXL,
    cardPadding = dim.cardHorizontalPadding,
    cardCorner = dim.cardCornerRadius,
    betweenRows = dim.spacingM,
    betweenCards = dim.spacingM,
)

@Composable
internal fun rememberSearchStopGaps(): SearchStopGaps = searchStopGaps(KrailTheme.dimensions)

/** The height of the control row at the map's bottom edge: the location button is one 48 dp touch target. */
internal val MAP_CONTROL_ROW_HEIGHT = 48.dp

/**
 * How far above the system bottom inset the mode controls stand. The location button stands [edge] above the
 * inset with a 48 dp row, and the map's attribution icon sits in the same bottom row at the left, so the mode
 * button stands one row and one [SearchStopGaps.betweenCards] above that, never over either. [clearance] is
 * what rides the controls above an open stop card, the same value that lifts the location button.
 */
internal fun modeControlsBottomPadding(gaps: SearchStopGaps, clearance: Dp = 0.dp): Dp =
    gaps.edge + MAP_CONTROL_ROW_HEIGHT + gaps.betweenCards + clearance

/** The mode button and its block are drawn in the map state of a map that is ready, never with the keyboard up. */
internal fun modeControlsShown(mode: SearchStopMode, mapReady: Boolean): Boolean =
    mode == SearchStopMode.Map && mapReady

/** The button shows its active look while any mode is switched off ([allClasses] is every mode the filter offers). */
internal fun modeFilterActive(allClasses: List<Int>, selected: Set<Int>): Boolean =
    allClasses.any { it !in selected }

/** What moves the mode block open or shut. */
internal enum class ModeBlockEvent { ButtonTapped, MapTapped, LeftMapState }

/** Open on a tap of the button when shut and shut by the button again, a tap on the map, or leaving the map state. */
internal fun reduceModeBlock(open: Boolean, event: ModeBlockEvent): Boolean = when (event) {
    ModeBlockEvent.ButtonTapped -> !open
    ModeBlockEvent.MapTapped, ModeBlockEvent.LeftMapState -> false
}

/**
 * The most height the stack at the top (group card and list card) may take, so it ends above the mode controls
 * by one [SearchStopGaps.betweenCards] and the list card, which scrolls inside, is what gives. [controlsTop] is
 * where the controls start, measured from the top of the screen, or null with no controls.
 */
internal fun topStackMaxHeight(controlsTop: Dp?, gaps: SearchStopGaps): Dp =
    if (controlsTop == null) Dp.Unspecified else (controlsTop - gaps.betweenCards).coerceAtLeast(0.dp)
