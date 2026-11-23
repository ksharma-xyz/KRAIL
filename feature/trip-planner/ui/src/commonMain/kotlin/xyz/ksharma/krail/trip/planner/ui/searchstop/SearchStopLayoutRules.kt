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

/**
 * The margin maplibre-compose 0.12.1 adds on every side of the padding it is given before it places the logo, the
 * compass and the info icon (it sets each one's margin to the padding plus this). So an ornament padding of zero still
 * stands this far in from the map's edge, which is the closest the info icon can be to the bottom left corner.
 */
internal val MAP_ORNAMENT_LIBRARY_MARGIN = 8.dp

/**
 * The size of the map's own info icon on Android (its attribution, a licence requirement that is never turned off): the
 * library's 21 dp vector, drawn at its own size with no padding and no larger touch box. Read from the library's
 * resources, not measured on a device, so it is a number to check on a phone.
 */
internal val MAP_ATTRIBUTION_ICON_SIZE = 21.dp

/**
 * How far the visible "i" starts in from the edge of that 21 dp icon: the vector is 24 units wide and the circle starts
 * 2 units in. It is what puts the control row's left edge on the "i" and not on the empty edge of its box, and what makes
 * the gap above the icon the visible 8 dp.
 */
internal val MAP_ATTRIBUTION_GLYPH_INSET = 1.75.dp

/** How far above the bottom of the map the visible "i" starts: the library's margin, the icon, less the icon's own inset. */
internal val MAP_ATTRIBUTION_GLYPH_TOP = MAP_ORNAMENT_LIBRARY_MARGIN + MAP_ATTRIBUTION_ICON_SIZE - MAP_ATTRIBUTION_GLYPH_INSET

/** The location control is one 48 dp target, and the finger target of Options is at least as tall. */
internal val MAP_CONTROL_ROW_HEIGHT = 48.dp

/**
 * What the Options pill is drawn at 100 percent text, 20 dp tall, used until its real height is measured so the first
 * frame is already where it ends up at the usual text size. It grows with the text, to 29 at 150 and 34 at 200 percent.
 */
internal val MAP_OPTIONS_PILL_REST_HEIGHT = 20.dp

/**
 * The bottom of the map state is two lines. The lower one is the map's own info icon alone, at the bottom most left,
 * resting on the library's own margin with no padding of ours. The upper one, the control line, is Options at the
 * start and the location control at the end on one centre line: the Options pill's bottom edge is
 * [SearchStopGaps.aboveIcon] above the visible "i", so its centre is half its [pillHeight] higher, and the location control
 * is centred on the same line, so the two never part at any text size.
 */
internal fun controlLineCentre(gaps: SearchStopGaps, pillHeight: Dp): Dp =
    MAP_ATTRIBUTION_GLYPH_TOP + gaps.aboveIcon + pillHeight / 2

/**
 * How far an open stop card lifts the control line, the same value that lifts the location control above the card: nothing
 * with no card ([clearance] zero) and otherwise what puts the location control one edge above [clearance]'s card.
 */
internal fun controlLineLift(gaps: SearchStopGaps, pillHeight: Dp, clearance: Dp): Dp = when {
    clearance <= 0.dp -> 0.dp
    else -> maxOf(0.dp, gaps.edge + clearance - (controlLineCentre(gaps, pillHeight) - MAP_CONTROL_ROW_HEIGHT / 2))
}

/** How far above the system bottom inset the location control stands, centred on [controlLineCentre]. */
internal fun locationControlBottomPadding(gaps: SearchStopGaps, pillHeight: Dp, clearance: Dp = 0.dp): Dp =
    controlLineCentre(gaps, pillHeight) - MAP_CONTROL_ROW_HEIGHT / 2 + controlLineLift(gaps, pillHeight, clearance)

/**
 * How far above the system bottom inset the Options finger target stands: its bottom edge is the visible "i"'s top, so it
 * touches the "i" and never covers it, and it reaches 1.75 dp over the top of the icon's own empty edge. The pill is
 * [SearchStopGaps.aboveIcon] above that bottom edge, which is what makes the visible gap that small and constant.
 */
internal fun optionsBottomPadding(gaps: SearchStopGaps, pillHeight: Dp, clearance: Dp = 0.dp): Dp =
    MAP_ATTRIBUTION_GLYPH_TOP + controlLineLift(gaps, pillHeight, clearance)

/** The finger target of Options: at least 48 dp tall, and the pill with its gap above the "i" if that is taller. */
internal fun optionsTargetHeight(gaps: SearchStopGaps, pillHeight: Dp): Dp =
    maxOf(MAP_CONTROL_ROW_HEIGHT, pillHeight + gaps.aboveIcon)

/**
 * How far the Options card is pulled down over the invisible top of Options' finger target, so it stands
 * [SearchStopGaps.betweenCards] above the visible top of the control line (the location control's top, 24 dp over the
 * centre) and not above the finger target. The card's own bottom gap is the full [SearchStopGaps.betweenCards].
 */
internal fun optionsCardOverlap(gaps: SearchStopGaps, pillHeight: Dp): Dp =
    maxOf(0.dp, optionsTargetHeight(gaps, pillHeight) - (gaps.aboveIcon + pillHeight / 2 + MAP_CONTROL_ROW_HEIGHT / 2))

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
