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
 *   block to the Options button under it.
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

/**
 * The margin maplibre-compose 0.12.1 adds on every side of the padding it is given before it places the logo, the
 * compass and the info icon (it sets each one's margin to the padding plus this). So an ornament padding of zero still
 * stands this far in from the map's edge, which is the closest the info icon can be to the bottom left corner.
 */
internal val MAP_ORNAMENT_LIBRARY_MARGIN = 8.dp

/**
 * The size of the map's own info icon on Android (its attribution, a licence requirement that is never turned off): the
 * library's 21 dp vector, drawn at its own size with no padding and no larger touch box. Read from the library's
 * resources, not measured on a device, so it is a number to check on a phone. The Options button stands above it.
 */
internal val MAP_ATTRIBUTION_ICON_SIZE = 21.dp

/**
 * How far the visible "i" starts in from the edge of that 21 dp icon: the vector is 24 units wide and the circle starts
 * 2 units in. It is what puts the Options button's left edge on the "i" and not on the empty edge of its box.
 */
internal val MAP_ATTRIBUTION_GLYPH_INSET = 1.75.dp

/**
 * How far above the system bottom inset the Options button stands: the info icon's own margin from the map's bottom edge
 * (the library's margin, with the padding at zero), the icon, and one [SearchStopGaps.betweenCards] gap, so Options is
 * directly above the icon and the icon is the bottom most element at the left. An open stop card lifts the stack above
 * itself instead, by [clearance], the same value that lifts the location control, once that is higher.
 */
internal fun modeControlsBottomPadding(gaps: SearchStopGaps, clearance: Dp = 0.dp): Dp =
    maxOf(MAP_ORNAMENT_LIBRARY_MARGIN + MAP_ATTRIBUTION_ICON_SIZE + gaps.betweenCards, gaps.edge + clearance)

/**
 * The margin of the map's own ornaments in Search Stop's map state, so none of them is under the box and the info icon
 * rests in the bottom left corner. At the top the compass starts [SearchStopGaps.betweenCards] under the box (which is
 * [statusBarTop] and one edge down and [headerHeight] tall) and at the right it is one edge in like the box. The info
 * icon is the only ornament at the left and the bottom (the scale bar and the logo are off): its padding at the bottom is
 * zero above [navigationBarBottom], so it rests on the library's own margin, and at the start it is what puts the "i"
 * itself one [SearchStopGaps.edge] in, on the same line as the box and the Options button.
 */
internal fun mapOrnamentPadding(
    gaps: SearchStopGaps,
    statusBarTop: Dp,
    navigationBarBottom: Dp,
    headerHeight: Dp,
): PaddingValues = PaddingValues(
    start = (gaps.edge - MAP_ORNAMENT_LIBRARY_MARGIN - MAP_ATTRIBUTION_GLYPH_INSET).coerceAtLeast(0.dp),
    top = statusBarTop + gaps.edge + headerHeight + gaps.betweenCards,
    end = gaps.edge,
    bottom = navigationBarBottom,
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
