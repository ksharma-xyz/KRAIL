package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.theme.KrailDimensions
import xyz.ksharma.krail.taj.theme.KrailTheme

// The layout of the Search Stop map state, `docs/MAP_HOME_UX.md` rules 9, 10, 33, 35, 36 and 39. Every
// decision is a pure function or one table, so the order of the rows, which of them show and every gap are
// pinned without a layout.

/** One row of the group card at the top, in the order it is drawn. The card holds only the two pills. */
internal enum class GroupRow { To, From }

/**
 * The rows of the group card, top to bottom. The To pill is always first. The From pill comes under it, one
 * gap away with no divider, when the form is [FormShape.ToAndFrom], and a single bar (a label, or one leg of a
 * trip) has only the one. The location status chip and the mode chips are not in this card at all: the chip is
 * under it ([locationChipShownFor]) and the mode chips are in [SearchStopModeControls].
 */
internal fun groupCardRows(shape: FormShape): List<GroupRow> = buildList {
    add(GroupRow.To)
    if (shape == FormShape.ToAndFrom) add(GroupRow.From)
}

/** Whether the location status chip is drawn under the group card: only while the location is not allowed. */
internal fun locationChipShownFor(pill: LocationPill): Boolean = pill != LocationPill.Hidden

/**
 * Every gap of the map state, from the one spacing scale in `KrailDimensions` (4, 8, 12, 16):
 * - [edge] is the system top inset to the group card, and the card to the screen's left and right edges.
 * - [cardPadding] is the inner padding of every card, the card's own padding token on all four sides.
 * - [cardCorner] is the corner of every card, the card's own radius token.
 * - [betweenRows] is the one gap between rows inside the group card and around its divider.
 * - [betweenCards] is the one gap between floating elements: group card to the location chip under it, and the mode
 *   block to the control row under it.
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

/** The height of the control row at the map's bottom edge: the location control is one 48 dp touch target. */
internal val MAP_CONTROL_ROW_HEIGHT = 48.dp

/**
 * The width the map's own info icon (its attribution, a licence requirement that is never turned off) takes at the
 * start of the bottom row: a 48 dp touch box. The Options button stands right after it. Taken from the map's
 * one control size and not measured from the native icon, so it is the one number to check on a device.
 */
internal val MAP_ATTRIBUTION_SLOT = 48.dp

/**
 * How far above the system bottom inset the Options row stands: the same [SearchStopGaps.edge] the location control
 * stands above it, so the two rows are one row and share one centre line. [clearance] is what rides the row above
 * an open stop card, the same value that lifts the location control.
 */
internal fun modeControlsBottomPadding(gaps: SearchStopGaps, clearance: Dp = 0.dp): Dp =
    gaps.edge + clearance

/**
 * The margin of the map's own ornaments in Search Stop's map state, so none of them is under the box and the info icon
 * stands on the bottom row. At the top the compass and scale bar start [SearchStopGaps.betweenCards] under the box
 * (which is [statusBarTop] and one edge down and [headerHeight] tall), at the sides they are one edge in like the box,
 * and at the bottom the info icon is one edge above [navigationBarBottom], the row the Options button and the
 * location control are on.
 */
internal fun mapOrnamentPadding(
    gaps: SearchStopGaps,
    statusBarTop: Dp,
    navigationBarBottom: Dp,
    headerHeight: Dp,
): PaddingValues = PaddingValues(
    start = gaps.edge,
    top = statusBarTop + gaps.edge + headerHeight + gaps.betweenCards,
    end = gaps.edge,
    bottom = navigationBarBottom + gaps.edge,
)

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
