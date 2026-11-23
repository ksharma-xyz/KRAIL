package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import xyz.ksharma.krail.taj.theme.KrailDimensions
import xyz.ksharma.krail.taj.theme.KrailTheme

// The grid of the Search Stop card (`docs/MAP_HOME_UX.md`, The box). Every number comes from the spacing and icon scales,
// and the rules that decide the layout are plain functions, so each is pinned without a layout.

/**
 * The card's grid, at 411 dp and 100 percent text (the numbers of the approved picture):
 * - [startPad] 16 is the card's left padding and the marker's left edge.
 * - [marker] 20 is the marker's box, and [markerGap] 8 follows it, so the text starts at [textStart], 44, at every size and width.
 * - [circle] 40 is the go circle, and [circleGap] 8 is how far the text stops before the controls.
 * - [endPad] 16 is how far the go circle's right edge is from the card's right edge, equal to [startPad]. The controls share one
 *   vertical axis [axis] from the right edge: the go circle's centre and the swap glyph's centre, 36.
 * - [rowMin] 56 is a row's least height. A row is [rowChrome] 24 (12 above and below) plus its lines, whichever is taller.
 * - [swapGlyph] 24 is the bare swap glyph, in a [target] 48 box.
 */
@Immutable
internal data class CardGrid(
    val startPad: Dp,
    val marker: Dp,
    val markerGap: Dp,
    val circle: Dp,
    val circleGap: Dp,
    val endPad: Dp,
    val rowMin: Dp,
    val rowVerticalPad: Dp,
    val swapGlyph: Dp,
    val target: Dp,
) {
    /** Where the text starts, from the card's left edge: beside the marker's column, at every size. */
    val textStart: Dp get() = startPad + marker + markerGap

    /** What a row gives up at its end for the controls: the text and the divider stop this far from the card's right edge. */
    val endReserved: Dp get() = endPad + circle + circleGap

    /** Where the controls' shared vertical axis is, from the card's right edge: the go circle's centre, 16 plus half of 40. */
    val axis: Dp get() = endPad + circle / 2f

    /** What a row keeps above and below its lines, 12 each: [rowMin] less a 20 dp line is 36, and a taller line grows the row. */
    val rowChrome: Dp get() = rowVerticalPad * 2f
}

internal fun cardGrid(dim: KrailDimensions = KrailDimensions()): CardGrid = CardGrid(
    startPad = dim.spacingXL,
    marker = dim.iconSmall,
    markerGap = dim.spacingM,
    circle = CIRCLE_DIAMETER,
    circleGap = dim.spacingM,
    endPad = dim.spacingXL,
    rowMin = CIRCLE_DIAMETER + dim.spacingM * 2,
    rowVerticalPad = dim.spacingL,
    swapGlyph = dim.iconM,
    target = TARGET,
)

@Composable
internal fun rememberCardGrid(): CardGrid = cardGrid(KrailTheme.dimensions)

/** A row's height: 56 dp or the [lines] of [lineHeight] with 12 dp above and below, whichever is taller. A line is never cut. */
internal fun cardRowHeight(grid: CardGrid, lines: Int, lineHeight: Dp): Dp =
    maxOf(grid.rowMin, grid.rowChrome + lineHeight * lines.toFloat())

/**
 * The swap's two resting heights, from the top of the card, for a From row [fromHeight] tall and a divider [divider] thick:
 * the divider's centre while at most one stop is chosen, and the From row's centre line once both are.
 */
internal fun swapCentreY(fromHeight: Dp, divider: Dp, lifted: Boolean): Dp =
    if (lifted) fromHeight / 2f else fromHeight + divider / 2f

/** How far the swap glides between its two resting heights: half the From row plus half the divider, 28.5 dp at 56 and 1. */
internal fun swapGlideDistance(fromHeight: Dp, divider: Dp): Dp = fromHeight / 2f + divider / 2f
