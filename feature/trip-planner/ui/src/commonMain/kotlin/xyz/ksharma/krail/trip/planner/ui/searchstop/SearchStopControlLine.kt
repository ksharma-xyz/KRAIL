package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// The bottom of the Search Stop map state, `docs/MAP_HOME_UX.md` rules 9, 10, 33, 35, 36 and 39: the map's own info
// icon, the control line above it, the Options card over that and the margin of the map's ornaments. Every number
// is a pure function of the gaps, so each is pinned without a layout.

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
internal val MAP_ATTRIBUTION_GLYPH_TOP =
    MAP_ORNAMENT_LIBRARY_MARGIN + MAP_ATTRIBUTION_ICON_SIZE - MAP_ATTRIBUTION_GLYPH_INSET

/** The location control is one 48 dp target, and the finger target of Options is at least as tall. */
internal val MAP_CONTROL_ROW_HEIGHT = 48.dp

/**
 * What the Options pill is drawn at 100 percent text, 32 dp tall (taj's medium button as shipped, measured by
 * `SearchStopOptionsPillTest`: the 20 dp line of its bold label and 6 dp above and below), used until its real height is
 * measured so the first frame is already where it ends up at the usual text size. It was 20 dp as an extra small button. It
 * grows with the text.
 */
internal val MAP_OPTIONS_PILL_REST_HEIGHT = 32.dp

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
 * How far the Options card is moved down (or up, when negative) so it stands [SearchStopGaps.betweenCards] above the visible top of
 * the control line. The visible top is the taller of the two things on the line: the location control, whose top is 24 dp over the
 * line's centre, and the Options pill, which is taller than the control once its text is large. The card's own bottom gap is
 * the full [SearchStopGaps.betweenCards] above Options' finger target, so the card is moved down over the target's invisible top
 * while the pill is the shorter, and up by what the control stands above the target when the pill and its gap are between 40
 * and 48 dp.
 */
internal fun optionsCardOverlap(gaps: SearchStopGaps, pillHeight: Dp): Dp =
    optionsTargetHeight(gaps, pillHeight) -
        (gaps.aboveIcon + maxOf(pillHeight / 2 + MAP_CONTROL_ROW_HEIGHT / 2, pillHeight))

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
