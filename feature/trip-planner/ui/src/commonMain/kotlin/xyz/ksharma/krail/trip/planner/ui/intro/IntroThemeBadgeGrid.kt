package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import kotlin.math.max

/**
 * Where the theme badges sit, in pixels from the top left of the grid, and how big the grid is. [xs] and [ys] are the
 * top left of each badge in the order they were given, and [columns] is 2 for the loose grid and 1 for the column the
 * badges wrap to when two do not fit the width.
 */
internal data class ThemeBadgeGrid(
    val xs: List<Int>,
    val ys: List<Int>,
    val width: Int,
    val height: Int,
    val columns: Int,
)

/**
 * The loose, slightly lopsided grid the four theme badges sit in, as a pure function of pixels so a table can hold it.
 * Every badge is a square of [badge] pixels. The badges go in reading order, left then right, then down.
 *
 * Two columns when two badges, the [gapX] between them and the [shiftX] fit [available]: the right column is lowered by
 * half a badge and sits [shiftX] further out, and the second row of the left column is moved in by [shiftX], so no row
 * lines up with the next and the whole reads as scattered by hand. Otherwise one column, the badges one under the next
 * [gapY] apart, every second one moved in by [shiftX] (less if the room is short) so it still reads as staggered, and
 * never two to a row.
 *
 * No two badges ever overlap or sit closer than the smaller of [gapX] and [gapY]: in a column the neighbours are [gapY]
 * apart, and across the columns the nearest edges are [gapX] apart whatever the shift. That room is what the drift and the
 * dance of a badge use, so they cannot touch a neighbour. The grid is as wide and as tall as the badges, nothing more, and
 * the caller centres it.
 */
internal fun themeBadgeGrid(
    count: Int,
    badge: Int,
    gapX: Int,
    gapY: Int,
    shiftX: Int,
    available: Int,
): ThemeBadgeGrid {
    if (count <= 0) return ThemeBadgeGrid(emptyList(), emptyList(), width = 0, height = 0, columns = 1)
    val twoColumns = count > 1 && 2 * badge + gapX + shiftX <= available
    return if (twoColumns) {
        twoColumnGrid(count, badge, gapX, gapY, shiftX)
    } else {
        oneColumnGrid(count, badge, gapY, shiftX, available)
    }
}

private fun twoColumnGrid(count: Int, badge: Int, gapX: Int, gapY: Int, shiftX: Int): ThemeBadgeGrid {
    val rightX = badge + gapX + shiftX
    val lowered = badge / 2
    val step = badge + gapY
    val xs = List(count) { i -> if (i % 2 == 1) rightX else (i / 2) % 2 * shiftX }
    val ys = List(count) { i -> i / 2 * step + if (i % 2 == 1) lowered else 0 }
    return ThemeBadgeGrid(
        xs = xs,
        ys = ys,
        width = rightX + badge,
        height = ys.max() + badge,
        columns = 2,
    )
}

private fun oneColumnGrid(count: Int, badge: Int, gapY: Int, shiftX: Int, available: Int): ThemeBadgeGrid {
    val shift = shiftX.coerceAtMost(max(0, available - badge))
    val step = badge + gapY
    val xs = List(count) { i -> i % 2 * shift }
    val ys = List(count) { i -> i * step }
    return ThemeBadgeGrid(
        xs = xs,
        ys = ys,
        width = badge + if (count > 1) shift else 0,
        height = ys.max() + badge,
        columns = 1,
    )
}

/**
 * Lays its children out as [themeBadgeGrid] says and centres the grid across the room it is given. Each child is measured
 * at its own size and the grid is built from the largest, so every badge keeps its whole self and is never squeezed.
 * It is as tall as the grid, and the middle zone of the screen scrolls when that is more than the room.
 */
@Composable
internal fun IntroThemeBadgeGrid(
    gapX: Dp,
    gapY: Dp,
    shiftX: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val placeables = measurables.map { it.measure(Constraints()) }
        val badge = placeables.maxOfOrNull { max(it.width, it.height) } ?: 0
        val room = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val grid = themeBadgeGrid(
            count = placeables.size,
            badge = badge,
            gapX = gapX.roundToPx(),
            gapY = gapY.roundToPx(),
            shiftX = shiftX.roundToPx(),
            available = room,
        )
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else grid.width
        layout(width, grid.height) {
            val left = (width - grid.width) / 2
            placeables.forEachIndexed { i, placeable ->
                placeable.place(
                    x = left + grid.xs[i] + (badge - placeable.width) / 2,
                    y = grid.ys[i] + (badge - placeable.height) / 2,
                )
            }
        }
    }
}
