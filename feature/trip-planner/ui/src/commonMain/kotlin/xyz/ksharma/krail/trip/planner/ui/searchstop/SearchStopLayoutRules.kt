package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import xyz.ksharma.krail.taj.theme.KrailDimensions
import xyz.ksharma.krail.taj.theme.KrailTheme

// The layout of the Search Stop map state, `docs/MAP_HOME_UX.md` rules 9, 10, 33, 35, 36 and 39. Every
// decision is a pure function or one table, so the order of the rows, which of them show and every gap are
// pinned without a layout.

/** One row of the form at the top, in the order it is drawn. */
internal enum class GroupRow { From, To }

/**
 * The rows of the form, top to bottom. From is the top pill and To is the pill below it, one gap away with no divider,
 * when the form is [FormShape.ToAndFrom]. A single bar (a label, or one leg of a trip) has only the one, the To row. The
 * location status chip and the mode chips are not in the form at all: the chip is under it ([locationChipShownFor]) and
 * the mode chips are in [SearchStopModeControls].
 */
internal fun groupCardRows(shape: FormShape): List<GroupRow> = buildList {
    if (shape == FormShape.ToAndFrom) add(GroupRow.From)
    add(GroupRow.To)
}

/**
 * Whether the form is drawn on a card: only when two fields, From and To, are there. A single field is one bare rounded
 * pill on the map. It depends on how many rows the shape shows and on nothing else, so no screen picks it by name.
 */
internal fun groupCardShownFor(shape: FormShape): Boolean = groupCardRows(shape).size > 1

/** Whether the location status chip is drawn under the group card: only while the location is not allowed. */
internal fun locationChipShownFor(pill: LocationPill): Boolean = pill != LocationPill.Hidden

/**
 * Every gap of the map state, from the one spacing scale in `KrailDimensions` (4, 8, 12, 16):
 * - [edge] is the system top inset to the group card, and the card to the screen's left and right edges.
 * - [cardPadding] is the inner padding of every card, the card's own padding token on all four sides.
 * - [cardCorner] is the corner of every card, the card's own radius token.
 * - [betweenRows] is the one gap between rows inside the group card and around its divider.
 * - [betweenCards] is the one gap between floating elements: group card to the location chip under it, and the mode
 *   block to the control line under it.
 * - [aboveIcon] is the small gap between the Options pill and the visible "i" of the map's info icon under it.
 */
internal data class SearchStopGaps(
    val edge: Dp,
    val cardPadding: Dp,
    val cardCorner: Dp,
    val betweenRows: Dp,
    val betweenCards: Dp,
    val aboveIcon: Dp,
)

internal fun searchStopGaps(dim: KrailDimensions = KrailDimensions()): SearchStopGaps = SearchStopGaps(
    edge = dim.spacingXL,
    cardPadding = dim.cardHorizontalPadding,
    cardCorner = dim.cardCornerRadius,
    betweenRows = dim.spacingM,
    betweenCards = dim.spacingM,
    aboveIcon = dim.spacingXS,
)

@Composable
internal fun rememberSearchStopGaps(): SearchStopGaps = searchStopGaps(KrailTheme.dimensions)
