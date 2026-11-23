package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * How the badges are laid out for the room there is. [Wave] is the screen's own layout: one row of four, alternately high and low.
 * [Zigzag] is the same four in two staggered rows, used where the wave would let two badges that are floating come too close.
 * [Column] is one staggered column, for a window too narrow for either.
 */
internal enum class ThemeBadgeLayout { Wave, Zigzag, Column }

/**
 * Where the theme badges sit, in dp from the top left of the grid, and how big the grid is. [xsDp] and [ysDp] are the top left of
 * each badge in the order they were given. [closestGapDp] is the least space between two badges at rest, edge to edge, and
 * [floatAmplitudeDp] is how far each may float from its place (`floatAmplitudeDp`), which is less where the room is smaller, so
 * that two badges that float, or one that dances, never touch.
 */
internal data class ThemeBadgeGrid(
    val xsDp: List<Float>,
    val ysDp: List<Float>,
    val widthDp: Float,
    val heightDp: Float,
    val layout: ThemeBadgeLayout,
    val closestGapDp: Float,
    val floatAmplitudeDp: Float,
)

/** How far a badge of the wave sits lower than its neighbours, the high ones at the top and the low ones this far down. */
internal const val WAVE_DROP_DP = 48f

/** How far the second badge of each row of the zigzag is lowered, and the step from the first row to the second is a badge and this. */
internal const val ZIGZAG_LOWER_DP = 32f

/** The most the second row of the zigzag is moved in from the first, so no two badges share a column. */
internal const val ZIGZAG_MAX_INSET_DP = 72f

/** The room between badges of the single column, and the most every second one is moved in, so it reads as staggered. */
internal const val COLUMN_GAP_DP = 24f
internal const val COLUMN_SHIFT_DP = 16f

/** The least two badges that float at their widest may come to each other for the wave to be used. */
internal const val MIN_FLOATING_GAP_DP = 8f

/**
 * The layout of [count] badges of [badgeDp], in a room [availableDp] wide, as a pure function of dp so a table can hold it. The room
 * is the window's width, at most the content cap, less the page padding. The badge is a fixed size at every text size, so the choice
 * is the room alone.
 *
 * - **Wave (the first choice).** One row, the badges spread across the whole room with the same step between their centres, alternately
 *   at the top and [WAVE_DROP_DP] lower: high, low, high, low. It is used when two badges that float at their widest
 *   ([FLOAT_MAX_DP], which moves a badge by up to [SQRT_TWO] times that, in any direction) would still keep [MIN_FLOATING_GAP_DP]
 *   between them.
 * - **Zigzag.** Four badges in two rows. In the first the second badge is at the far side and lowered by [ZIGZAG_LOWER_DP]. The second
 *   row starts a badge and that far down, with the same two moved in by `h`, which is a third of the room and at most
 *   [ZIGZAG_MAX_INSET_DP]. So no two badges share a column. It is used when the wave is not and its badges are at least
 *   [minClearGapDp] apart at rest, so a dance can never touch the next badge even where there is no room to float.
 * - **Column.** The badges one under the next [COLUMN_GAP_DP] apart, every second one moved in by [COLUMN_SHIFT_DP], or less
 *   where the room is short. It is the last choice, and never has two to a row.
 *
 * The grid is as wide as the layout it has and as tall as it needs, and the caller centres it.
 */
internal fun themeBadgeGrid(count: Int, availableDp: Float, badgeDp: Float): ThemeBadgeGrid {
    if (count <= 0) {
        return ThemeBadgeGrid(
            emptyList(),
            emptyList(),
            widthDp = 0f,
            heightDp = 0f,
            layout = ThemeBadgeLayout.Column,
            closestGapDp = Float.POSITIVE_INFINITY,
            floatAmplitudeDp = FLOAT_MAX_DP,
        )
    }
    val wave = waveGrid(count, availableDp, badgeDp)
    val waveFloats = count == 1 || wave.closestGapDp - 2f * SQRT_TWO * FLOAT_MAX_DP >= MIN_FLOATING_GAP_DP
    val zigzag = if (!waveFloats && count == FOUR) zigzagGrid(availableDp, badgeDp) else null
    return when {
        waveFloats -> wave
        zigzag != null && zigzag.closestGapDp >= minClearGapDp(badgeDp) -> zigzag
        else -> columnGrid(count, availableDp, badgeDp)
    }
}

// The grid for the points, with the least space between any two of them and the float that space allows.
private fun gridOf(
    xs: List<Float>,
    ys: List<Float>,
    badgeDp: Float,
    layout: ThemeBadgeLayout,
): ThemeBadgeGrid {
    val gap = closestGapDp(xs, ys, badgeDp)
    return ThemeBadgeGrid(
        xsDp = xs,
        ysDp = ys,
        widthDp = xs.max() + badgeDp,
        heightDp = ys.max() + badgeDp,
        layout = layout,
        closestGapDp = gap,
        floatAmplitudeDp = floatAmplitudeDp(gap, badgeDp),
    )
}

/** The least space, edge to edge, between any two of the round badges at [xs] and [ys], which is negative when two overlap. */
internal fun closestGapDp(xs: List<Float>, ys: List<Float>, badgeDp: Float): Float {
    var closest = Float.POSITIVE_INFINITY
    for (i in xs.indices) {
        for (j in i + 1 until xs.size) {
            closest = min(closest, hypot(xs[i] - xs[j], ys[i] - ys[j]) - badgeDp)
        }
    }
    return closest
}

private fun waveGrid(count: Int, availableDp: Float, badgeDp: Float): ThemeBadgeGrid {
    val span = availableDp - badgeDp
    val xs = List(count) { i -> if (count == 1) 0f else i * span / (count - 1) }
    val ys = List(count) { i -> if (i % 2 == 0) 0f else WAVE_DROP_DP }
    return gridOf(xs, ys, badgeDp, ThemeBadgeLayout.Wave)
}

private fun zigzagGrid(availableDp: Float, badgeDp: Float): ThemeBadgeGrid {
    val span = availableDp - badgeDp
    val inset = min(ZIGZAG_MAX_INSET_DP, span / THIRD)
    val across = span - inset
    val rowStep = badgeDp + ZIGZAG_LOWER_DP
    return gridOf(
        xs = listOf(0f, across, inset, inset + across),
        ys = listOf(0f, ZIGZAG_LOWER_DP, rowStep, rowStep + ZIGZAG_LOWER_DP),
        badgeDp = badgeDp,
        layout = ThemeBadgeLayout.Zigzag,
    )
}

private fun columnGrid(count: Int, availableDp: Float, badgeDp: Float): ThemeBadgeGrid {
    val shift = min(COLUMN_SHIFT_DP, max(0f, availableDp - badgeDp))
    val step = badgeDp + COLUMN_GAP_DP
    return gridOf(
        xs = List(count) { i -> i % 2 * shift },
        ys = List(count) { i -> i * step },
        badgeDp = badgeDp,
        layout = ThemeBadgeLayout.Column,
    )
}

private const val FOUR = 4
private const val THIRD = 3f

/**
 * Lays its children out as [grid] says and centres the grid across the room it is given. Each child is measured at its own size
 * and is centred on its place in the grid, so every badge keeps its whole self and is never squeezed. It is as tall as the grid,
 * and the middle zone of the screen scrolls when that is more than the room.
 */
@Composable
internal fun IntroThemeBadgeGrid(
    grid: ThemeBadgeGrid,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val placeables = measurables.map { it.measure(Constraints()) }
        val badge = placeables.maxOfOrNull { max(it.width, it.height) } ?: 0
        val gridWidth = grid.widthDp.dp.roundToPx()
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else gridWidth
        layout(width, grid.heightDp.dp.roundToPx()) {
            val left = (width - gridWidth) / 2
            placeables.forEachIndexed { i, placeable ->
                placeable.place(
                    x = left + grid.xsDp[i].dp.roundToPx() + (badge - placeable.width) / 2,
                    y = grid.ysDp[i].dp.roundToPx() + (badge - placeable.height) / 2,
                )
            }
        }
    }
}
