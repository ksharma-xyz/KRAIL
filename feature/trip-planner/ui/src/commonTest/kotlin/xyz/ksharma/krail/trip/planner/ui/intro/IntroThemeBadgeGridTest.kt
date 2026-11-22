package xyz.ksharma.krail.trip.planner.ui.intro

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The loose grid the four theme badges sit in, as pure numbers: where each badge goes for a badge size and a room, that
 * the right column is lowered by half a badge and moved out, that no two badges ever overlap or sit closer than the gap,
 * that the drift and the dance of one badge cannot reach another, and that the grid wraps to a single staggered column only
 * when two badges do not fit the width.
 */
class IntroThemeBadgeGridTest {

    private class Row(
        val name: String,
        val count: Int,
        val badge: Int,
        val available: Int,
        val xs: List<Int>,
        val ys: List<Int>,
        val width: Int,
        val height: Int,
        val columns: Int,
    )

    private val gap = 24
    private val shift = 16

    private fun grid(count: Int, badge: Int, available: Int) =
        themeBadgeGrid(count, badge, gapX = gap, gapY = gap, shiftX = shift, available = available)

    @Test
    fun `the four badges sit where the table says, two columns when two fit and one staggered column when they do not`() {
        val rows = listOf(
            // A phone: 64 dp badges in 379 dp. The right column is 32 down and 104 across, the second row of the left is in 16.
            Row("a phone", 4, 64, 379, listOf(0, 104, 16, 104), listOf(0, 32, 88, 120), 168, 184, 2),
            // Exactly as wide as two badges, the gap and the shift: still two columns.
            Row("exactly fits", 4, 64, 168, listOf(0, 104, 16, 104), listOf(0, 32, 88, 120), 168, 184, 2),
            // One pixel short: a single column, every second badge moved in by the shift.
            Row("one short", 4, 64, 167, listOf(0, 16, 0, 16), listOf(0, 88, 176, 264), 80, 328, 1),
            // 200 percent text on a 320 dp window: the badge is still the fixed 64 dp, in 288 dp, so it is the phone's grid.
            Row("320 dp at 200 percent", 4, 64, 288, listOf(0, 104, 16, 104), listOf(0, 32, 88, 120), 168, 184, 2),
            // A room that is barely a badge: no room to move in, so the badges are in a straight column.
            Row("a room of one badge", 4, 64, 64, listOf(0, 0, 0, 0), listOf(0, 88, 176, 264), 64, 328, 1),
            // A room a little over a badge: the shift is what is left of the room.
            Row("a room a little over a badge", 4, 64, 70, listOf(0, 6, 0, 6), listOf(0, 88, 176, 264), 70, 328, 1),
        )

        rows.forEach { row ->
            val grid = grid(row.count, row.badge, row.available)

            assertEquals(row.xs, grid.xs, "${row.name}: xs")
            assertEquals(row.ys, grid.ys, "${row.name}: ys")
            assertEquals(row.width, grid.width, "${row.name}: width")
            assertEquals(row.height, grid.height, "${row.name}: height")
            assertEquals(row.columns, grid.columns, "${row.name}: columns")
        }
    }

    @Test
    fun `the right column is lowered by half a badge and the grid is not symmetrical`() {
        listOf(48, 64).forEach { badge ->
            val grid = grid(count = 4, badge = badge, available = 1000)

            assertEquals(badge / 2, grid.ys[1] - grid.ys[0], "the right column is half a badge lower, badge $badge")
            assertEquals(badge / 2, grid.ys[3] - grid.ys[2], "badge $badge")
            assertTrue(grid.xs[2] != grid.xs[0], "the second row lines up with the first, badge $badge")
            assertTrue(grid.xs[1] > grid.xs[0] + badge, "the right column is not clear of the left, badge $badge")
        }
    }

    @Test
    fun `no two badges overlap or sit closer than the gap, for every size, room and number of badges`() {
        val separation = minOf(gap, gap)

        listOf(48, 64).forEach { badge ->
            listOf(64, 120, 168, 200, 244, 288, 330, 379, 480, 900).forEach { available ->
                (1..6).forEach { count ->
                    val grid = grid(count, badge, available)

                    grid.xs.indices.forEach { i ->
                        grid.xs.indices.filter { it > i }.forEach { j ->
                            val across = maxOf(grid.xs[i] - (grid.xs[j] + badge), grid.xs[j] - (grid.xs[i] + badge))
                            val down = maxOf(grid.ys[i] - (grid.ys[j] + badge), grid.ys[j] - (grid.ys[i] + badge))
                            assertTrue(
                                maxOf(across, down) >= separation,
                                "badges $i and $j are $across across and $down down: size $badge, room $available, count $count",
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `every badge is inside the grid, which is as big as the badges and no bigger, and inside the room when it can be`() {
        listOf(48, 64).forEach { badge ->
            listOf(168, 200, 288, 379, 900).forEach { available ->
                val grid = grid(count = 4, badge = badge, available = available)

                grid.xs.indices.forEach { i ->
                    assertTrue(grid.xs[i] >= 0 && grid.xs[i] + badge <= grid.width, "badge $i across, size $badge in $available")
                    assertTrue(grid.ys[i] >= 0 && grid.ys[i] + badge <= grid.height, "badge $i down, size $badge in $available")
                }
                assertEquals(grid.ys.max() + badge, grid.height)
                assertEquals(grid.xs.max() + badge, grid.width)
                assertTrue(grid.width <= maxOf(available, badge), "the grid is ${grid.width} wide in $available, size $badge")
            }
        }
    }

    @Test
    fun `two columns need two badges, the gap and the shift to fit, and never fewer`() {
        listOf(48, 64).forEach { badge ->
            val needed = 2 * badge + gap + shift

            assertEquals(2, grid(4, badge, needed).columns, "size $badge, room $needed")
            assertEquals(1, grid(4, badge, needed - 1).columns, "size $badge, room ${needed - 1}")
            assertEquals(1, grid(1, badge, 1000).columns, "a lone badge is a column of one")
        }
    }

    @Test
    fun `the drift and the dance of one badge cannot reach the next, at the fixed 64 dp size`() {
        // The most a badge moves from its place: the drift sideways and up and down, and the dance's rise and its growth.
        // The badge is a fixed 64 dp at every text size, so that is the one size there is.
        listOf(64f).forEach { badgeDp ->
            val growth = (DANCE_SCALE_TO - 1f) / 2f * badgeDp
            val dancing = maxOf(DANCE_RISE_TO_DP, FLOAT_MAX_VERTICAL_DP) + growth
            val drifting = FLOAT_MAX_VERTICAL_DP
            // One badge dances at a time and the rest drift, so the two nearest are a dancer and a drifter at worst.
            assertTrue(
                dancing + drifting <= gap,
                "a dancing badge ($dancing dp) and a drifting one ($drifting dp) can touch across $gap dp, badge $badgeDp",
            )
        }
    }

    @Test
    fun `the badge is a fixed 64 dp, so the grid is the same at every text size and wraps on the room alone`() {
        // The room is in dp here, as the badge is: 288 dp is a 320 dp window at 200 percent text, 379 dp a phone.
        val atLargeText = grid(count = 4, badge = 64, available = 288)
        val onAPhone = grid(count = 4, badge = 64, available = 379)

        assertEquals(onAPhone, atLargeText)
        assertEquals(2, atLargeText.columns)
        assertEquals(2 * 64 + gap + shift, atLargeText.width)
    }

    @Test
    fun `nothing at all is an empty grid`() {
        val grid = grid(count = 0, badge = 64, available = 300)

        assertEquals(emptyList(), grid.xs)
        assertEquals(0, grid.width)
        assertEquals(0, grid.height)
    }
}
