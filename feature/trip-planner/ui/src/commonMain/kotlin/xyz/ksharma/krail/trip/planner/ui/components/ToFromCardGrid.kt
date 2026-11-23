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
 * - [marker] 20 is the marker's box, and [markerGap] 8 follows it, so the text starts at [textStart], 44.
 * - [circle] 40 is both round controls, and [circleGap] 8 is how far the text stops before them.
 * - [endPad] 16 is how far the circles' right edge is from the card's right edge, equal to [startPad].
 * - [rowMin] 56 is a row: a circle and [circleGap] above and below it, so the circle is centred on its row.
 * - [rowVerticalPad] 8 is the least a row keeps above and below its text, so at large text, when the text is taller than [rowMin]
 *   allows, the row grows by it.
 * - [stackedGap] 4 is the gap between a marker and the name under it, when the marker has wrapped above.
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
    val stackedGap: Dp,
) {
    /** Where the text starts when the marker is beside it, from the card's left edge. */
    val textStart: Dp get() = startPad + marker + markerGap

    /** What a row gives up at its end for the circles: the text and the divider stop this far from the card's right edge. */
    val endReserved: Dp get() = endPad + circle + circleGap

    /** The width left for the text beside the marker in a card [cardWidth] wide. */
    fun besideWidth(cardWidth: Dp): Dp = cardWidth - startPad - endReserved - marker - markerGap
}

internal fun cardGrid(dim: KrailDimensions = KrailDimensions()): CardGrid = CardGrid(
    startPad = dim.spacingXL,
    marker = dim.iconSmall,
    markerGap = dim.spacingM,
    circle = CIRCLE_DIAMETER,
    circleGap = dim.spacingM,
    endPad = dim.spacingXL,
    rowMin = CIRCLE_DIAMETER + dim.spacingM * 2,
    rowVerticalPad = dim.spacingM,
    stackedGap = dim.spacingXS,
)

@Composable
internal fun rememberCardGrid(): CardGrid = cardGrid(KrailTheme.dimensions)

/**
 * Whether a row's marker stands beside its text: the text, as wide as it is on one line ([natural]), fits in what is left
 * beside the marker in a card [cardWidth] wide. When it does not, the marker wraps whole above the name and the name takes the
 * whole width, wrapping onto lines of its own. Nothing is truncated and nothing is squeezed to make it fit.
 */
internal fun markerBeside(natural: Dp, cardWidth: Dp, grid: CardGrid): Boolean = natural <= grid.besideWidth(cardWidth)

/** Where the divider starts: at the text, or at the card's padding when either row has its marker wrapped above its name. */
internal fun dividerStart(grid: CardGrid, anyStacked: Boolean): Dp = if (anyStacked) grid.startPad else grid.textStart

/**
 * What a row keeps above and below its one line of text so the line is centred in [CardGrid.rowMin]: half of what the line
 * leaves, and never less than [CardGrid.rowVerticalPad]. At large text the line fills the row and the 8 dp wins.
 */
internal fun rowTextPadding(grid: CardGrid, lineHeight: Dp): Dp = maxOf(grid.rowVerticalPad, (grid.rowMin - lineHeight) / 2f)
