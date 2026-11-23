package xyz.ksharma.krail.trip.planner.ui.intro

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Where the four theme badges sit, as pure numbers in dp: the wave row where the room allows it, the zigzag of two rows where it does
 * not and the single column where even that does not fit, the thresholds between them, the least space between any two badges at rest
 * and while they float, and the float that space allows. The room a window gives is its width, at most the 480 dp content cap, less the
 * 32 dp of page padding, so a table of window widths is a table of rooms.
 */
class IntroThemeBadgeGridTest {

    private val badge = 64f

    private fun room(windowDp: Int) = min(windowDp, CONTENT_CAP) - PAGE_PADDING_TWICE

    private fun grid(windowDp: Int, count: Int = 4) = themeBadgeGrid(count, room(windowDp).toFloat(), badge)

    private fun assertNear(expected: Float, actual: Float, what: String, tolerance: Float = 0.02f) =
        assertTrue(abs(expected - actual) <= tolerance, "$what: expected $expected, was $actual")

    private fun assertNear(expected: List<Float>, actual: List<Float>, what: String) {
        assertEquals(expected.size, actual.size, "$what: size")
        expected.indices.forEach { assertNear(expected[it], actual[it], "$what[$it]") }
    }

    private class Row(
        val windowDp: Int,
        val layout: ThemeBadgeLayout,
        val xs: List<Float>,
        val ys: List<Float>,
        val gap: Float,
        val amplitude: Float,
    )

    // region the table of widths

    @Test
    fun `the four badges sit where the table says at every window width, in the order Train, Metro, Bus, Ferry`() {
        val rows = listOf(
            // The narrowest window of the table is too narrow for two rows that a dance cannot touch across, so it is one column.
            Row(200, ThemeBadgeLayout.Column, listOf(0f, 16f, 0f, 16f), listOf(0f, 88f, 176f, 264f), 25.44f, 7.995f),
            Row(240, ThemeBadgeLayout.Zigzag, listOf(0f, 96f, 48f, 144f), listOf(0f, 32f, 96f, 128f), 16f, 1.394f),
            Row(
                280,
                ThemeBadgeLayout.Zigzag,
                listOf(0f, 122.67f, 61.33f, 184f),
                listOf(0f, 32f, 96f, 128f),
                24.64f,
                7.506f,
            ),
            Row(320, ThemeBadgeLayout.Zigzag, listOf(0f, 152f, 72f, 224f), listOf(0f, 32f, 96f, 128f), 38.45f, 8f),
            Row(352, ThemeBadgeLayout.Wave, listOf(0f, 85.33f, 170.67f, 256f), listOf(0f, 48f, 0f, 48f), 33.91f, 8f),
            Row(360, ThemeBadgeLayout.Wave, listOf(0f, 88f, 176f, 264f), listOf(0f, 48f, 0f, 48f), 36.24f, 8f),
            Row(411, ThemeBadgeLayout.Wave, listOf(0f, 105f, 210f, 315f), listOf(0f, 48f, 0f, 48f), 51.45f, 8f),
            Row(480, ThemeBadgeLayout.Wave, listOf(0f, 128f, 256f, 384f), listOf(0f, 48f, 0f, 48f), 72.70f, 8f),
            // A tablet and an open fold use the 480 dp content column, centred, so the room is the same as at 480.
            Row(600, ThemeBadgeLayout.Wave, listOf(0f, 128f, 256f, 384f), listOf(0f, 48f, 0f, 48f), 72.70f, 8f),
            Row(840, ThemeBadgeLayout.Wave, listOf(0f, 128f, 256f, 384f), listOf(0f, 48f, 0f, 48f), 72.70f, 8f),
        )

        rows.forEach { row ->
            val grid = grid(row.windowDp)
            val name = "${row.windowDp} dp"

            assertEquals(row.layout, grid.layout, "$name: layout")
            assertNear(row.xs, grid.xsDp, "$name: xs")
            assertNear(row.ys, grid.ysDp, "$name: ys")
            assertNear(row.gap, grid.closestGapDp, "$name: closest gap", tolerance = 0.05f)
            assertNear(row.amplitude, grid.floatAmplitudeDp, "$name: float amplitude", tolerance = 0.01f)
        }
    }

    @Test
    fun `the wave is one row spread across the whole room, high low high low, 112 dp tall`() {
        listOf(352, 360, 411, 480).forEach { window ->
            val grid = grid(window)

            assertEquals(ThemeBadgeLayout.Wave, grid.layout, "$window dp")
            assertEquals(
                listOf(0f, WAVE_DROP_DP, 0f, WAVE_DROP_DP),
                grid.ysDp,
                "$window dp: Train and Bus high, Metro and Ferry low",
            )
            assertNear(0f, grid.xsDp.first(), "$window dp: the first badge is at the left edge")
            assertNear(room(window) - badge, grid.xsDp.last(), "$window dp: the last badge is at the right edge")
            assertNear(room(window).toFloat(), grid.widthDp, "$window dp: the grid is as wide as the room")
            assertNear(badge + WAVE_DROP_DP, grid.heightDp, "$window dp: the grid is 64 and 48 tall")
            val steps = grid.xsDp.zipWithNext { a, b -> b - a }
            steps.forEach { assertNear(steps.first(), it, "$window dp: an even step across the room") }
        }
    }

    @Test
    fun `the zigzag has two rows with no two badges in one column, and is as wide as the room`() {
        listOf(240, 260, 280, 320, 340).forEach { window ->
            val grid = grid(window)

            assertEquals(ThemeBadgeLayout.Zigzag, grid.layout, "$window dp")
            // 64 dp across the room for the badge, less the inset, which is a third of it and at most 72 dp, is the other offset.
            val span = room(window) - badge
            val inset = min(ZIGZAG_MAX_INSET_DP, span / 3f)
            assertNear(listOf(0f, span - inset, inset, span), grid.xsDp, "$window dp: xs")
            assertEquals(
                listOf(0f, ZIGZAG_LOWER_DP, badge + ZIGZAG_LOWER_DP, badge + 2 * ZIGZAG_LOWER_DP),
                grid.ysDp,
                "$window dp: ys",
            )
            val columns = grid.xsDp.map { it }
            columns.indices.forEach { i ->
                columns.indices.filter { it > i }.forEach { j ->
                    assertTrue(abs(columns[i] - columns[j]) > 1f, "badges $i and $j share a column at $window dp")
                }
            }
            assertNear(room(window).toFloat(), grid.widthDp, "$window dp: width")
            assertNear(192f, grid.heightDp, "$window dp: 64 and 128 tall")
        }
    }

    // endregion

    // region where it changes

    @Test
    fun `the wave is used from a room of 309 dp, the zigzag from 198 dp and the single column below that`() {
        // A window of 341 dp has a room of 309 dp. The wave is kept while two badges that float at their widest keep 8 dp apart.
        assertEquals(ThemeBadgeLayout.Wave, themeBadgeGrid(4, 309f, badge).layout)
        assertEquals(ThemeBadgeLayout.Zigzag, themeBadgeGrid(4, 308f, badge).layout)
        // The zigzag needs the reach of a dance and the margin between its nearest badges at rest, 14 dp, which is a room of 198 dp.
        assertEquals(ThemeBadgeLayout.Zigzag, themeBadgeGrid(4, 198f, badge).layout)
        assertEquals(ThemeBadgeLayout.Column, themeBadgeGrid(4, 197f, badge).layout)
        assertEquals(ThemeBadgeLayout.Column, themeBadgeGrid(4, 64f, badge).layout)
        assertEquals(ThemeBadgeLayout.Column, themeBadgeGrid(4, 30f, badge).layout)
    }

    @Test
    fun `the wave is only ever chosen when two floating badges keep 8 dp, and the choice only moves toward the wave as the room grows`() {
        var rank = 0
        (20..900).forEach { available ->
            val grid = themeBadgeGrid(4, available.toFloat(), badge)
            if (grid.layout == ThemeBadgeLayout.Wave) {
                val floating = grid.closestGapDp - 2f * SQRT_TWO * FLOAT_MAX_DP
                assertTrue(
                    floating >= MIN_FLOATING_GAP_DP - 0.001f,
                    "two floating badges are only $floating dp apart in $available dp",
                )
            }
            val now = when (grid.layout) {
                ThemeBadgeLayout.Column -> 0
                ThemeBadgeLayout.Zigzag -> 1
                ThemeBadgeLayout.Wave -> 2
            }
            assertTrue(now >= rank, "the layout went back from rank $rank to $now at $available dp")
            rank = now
        }
    }

    @Test
    fun `the single column is the staggered column it was, one badge under the next 24 dp apart`() {
        val grid = themeBadgeGrid(4, 100f, badge)

        assertEquals(ThemeBadgeLayout.Column, grid.layout)
        assertEquals(listOf(0f, 16f, 0f, 16f), grid.xsDp)
        assertEquals(listOf(0f, 88f, 176f, 264f), grid.ysDp)
        assertEquals(80f, grid.widthDp)
        assertEquals(328f, grid.heightDp)
        // A room that is barely a badge: no room to move in, so the badges are in a straight column.
        assertEquals(listOf(0f, 0f, 0f, 0f), themeBadgeGrid(4, 64f, badge).xsDp)
        assertEquals(listOf(0f, 6f, 0f, 6f), themeBadgeGrid(4, 70f, badge).xsDp)
    }

    // endregion

    // region no two badges touch

    @Test
    fun `no two badges overlap, and a dance has room beside a still neighbour, at every room, for the four badges`() {
        (20..900).forEach { available ->
            val grid = themeBadgeGrid(4, available.toFloat(), badge)
            val gap = closestGapDp(grid.xsDp, grid.ysDp, badge)

            assertEquals(gap, grid.closestGapDp, "the gap in the grid is the real one at $available dp")
            assertTrue(gap >= minClearGapDp(badge) - 0.001f, "badges are only $gap dp apart at rest in $available dp")
        }
    }

    @Test
    fun `no two badges overlap for any number of badges and any room`() {
        (1..6).forEach { count ->
            listOf(64, 100, 154, 200, 288, 328, 379, 448, 900).forEach { available ->
                val grid = themeBadgeGrid(count, available.toFloat(), badge)

                grid.xsDp.indices.forEach { i ->
                    grid.xsDp.indices.filter { it > i }.forEach { j ->
                        val distance = hypot(grid.xsDp[i] - grid.xsDp[j], grid.ysDp[i] - grid.ysDp[j])
                        assertTrue(
                            distance > badge,
                            "badges $i and $j overlap: $count of them in $available dp, $distance apart",
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `every badge is inside the grid, and the grid is as big as the badges and no bigger`() {
        (150..900 step 5).forEach { available ->
            val grid = themeBadgeGrid(4, available.toFloat(), badge)

            grid.xsDp.indices.forEach { i ->
                assertTrue(
                    grid.xsDp[i] >= -0.001f && grid.xsDp[i] + badge <= grid.widthDp + 0.001f,
                    "badge $i across in $available dp",
                )
                assertTrue(
                    grid.ysDp[i] >= 0f && grid.ysDp[i] + badge <= grid.heightDp + 0.001f,
                    "badge $i down in $available dp",
                )
            }
            assertNear(grid.ysDp.max() + badge, grid.heightDp, "height in $available dp")
            assertNear(grid.xsDp.max() + badge, grid.widthDp, "width in $available dp")
            assertTrue(
                grid.widthDp <= maxOf(available.toFloat(), badge) + 0.01f,
                "the grid is wider than the room of $available dp",
            )
        }
    }

    // endregion

    // region the float that the room allows

    @Test
    fun `the float is 8 dp where there is room, and less where the badges are closer, never under 0 or over 8`() {
        (20..900).forEach { available ->
            val amplitude = themeBadgeGrid(4, available.toFloat(), badge).floatAmplitudeDp

            assertTrue(amplitude in 0f..FLOAT_MAX_DP, "$amplitude dp in $available dp")
        }
        assertEquals(FLOAT_MAX_DP, grid(411).floatAmplitudeDp)
        assertEquals(FLOAT_MAX_DP, grid(320).floatAmplitudeDp)
    }

    @Test
    fun `within one layout the float never shrinks as the room grows`() {
        val last = mutableMapOf<ThemeBadgeLayout, Float>()
        (20..900).forEach { available ->
            val grid = themeBadgeGrid(4, available.toFloat(), badge)
            val before = last[grid.layout] ?: -1f

            assertTrue(
                grid.floatAmplitudeDp >= before - 0.001f,
                "the float shrank from $before to ${grid.floatAmplitudeDp} at $available dp",
            )
            last[grid.layout] = grid.floatAmplitudeDp
        }
    }

    @Test
    fun `the amplitude is the gap over twice root two less a dp, and the dance of a chosen badge is held in too`() {
        // 24.64 dp apart, as at 280 dp: floating together allows 24.64 / 2.83 less 1, and a dancing badge reaches 11.2 dp toward one.
        val floating = 24.64f / (2f * SQRT_TWO) - 1f
        val dancing = (24.64f - minClearGapDp(badge)) / SQRT_TWO

        assertNear(
            min(FLOAT_MAX_DP, min(floating, dancing)),
            floatAmplitudeDp(24.64f, badge),
            "amplitude",
            tolerance = 0.001f,
        )
        assertNear(floating, 24.64f / (2f * SQRT_TWO) - 1f, "the brief's formula")
        // Plenty of room is the full float, no room is none, and a lone badge has nothing to touch.
        assertEquals(FLOAT_MAX_DP, floatAmplitudeDp(200f, badge))
        assertEquals(0f, floatAmplitudeDp(0f, badge))
        assertEquals(0f, floatAmplitudeDp(-5f, badge))
        assertEquals(FLOAT_MAX_DP, floatAmplitudeDp(Float.POSITIVE_INFINITY, badge))
    }

    @Test
    fun `two badges that float never touch, over the whole loop of all four, at every room`() {
        // The whole loop of every badge at once is 6435 s, which is every period's common multiple, so it is walked through whole.
        val loop = 6435f
        val rooms = listOf(154, 168, 198, 208, 248, 288, 309, 320, 328, 379, 448, 600)
        rooms.forEach { available ->
            val grid = themeBadgeGrid(4, available.toFloat(), badge)
            val a = grid.floatAmplitudeDp
            val xs = FloatArray(4)
            val ys = FloatArray(4)

            var closest = Float.POSITIVE_INFINITY
            var t = 0f
            while (t < loop) {
                for (i in 0 until 4) {
                    val o = floatOffsetAt(i, t, a)
                    xs[i] = grid.xsDp[i] + o.xDp
                    ys[i] = grid.ysDp[i] + o.yDp
                }
                for (i in 0 until 4) {
                    for (j in i + 1 until 4) closest = min(closest, hypot(xs[i] - xs[j], ys[i] - ys[j]) - badge)
                }
                t += 0.25f
            }

            assertTrue(
                closest >= KEPT_GAP_DP,
                "two floating badges come to $closest dp in a room of $available dp, float $a",
            )
        }
    }

    @Test
    fun `a badge that dances never touches a neighbour that floats, at every moment of the dance and every share of it, at every room`() {
        val rooms = listOf(154, 168, 198, 208, 248, 288, 309, 320, 379, 448)
        rooms.forEach { available ->
            val grid = themeBadgeGrid(4, available.toFloat(), badge)
            val a = grid.floatAmplitudeDp
            val xs = FloatArray(4)
            val ys = FloatArray(4)
            val radius = FloatArray(4)

            var closest = Float.POSITIVE_INFINITY
            for (dancer in 0 until 4) {
                for (weight in listOf(0.25f, 0.5f, 0.75f, 1f)) {
                    // The dance and the float are walked on their own times, which is more than the one clock gives them, so every
                    // beat of the dance meets every place of the float.
                    var danceTime = 0f
                    while (danceTime < 2 * DANCE_LEG_SECONDS) {
                        val dance = danceAt(danceTime)
                        var floatTime = 0f
                        while (floatTime < LOOP_SECONDS) {
                            for (i in 0 until 4) {
                                val o = floatOffsetAt(i, floatTime, a)
                                if (i == dancer) {
                                    xs[i] = grid.xsDp[i] + o.xDp + (dance.xDp - o.xDp) * weight
                                    ys[i] = grid.ysDp[i] + o.yDp + (dance.yDp - o.yDp) * weight
                                    radius[i] = badge / 2f * (1f + (dance.scale - 1f) * weight)
                                } else {
                                    xs[i] = grid.xsDp[i] + o.xDp
                                    ys[i] = grid.ysDp[i] + o.yDp
                                    radius[i] = badge / 2f
                                }
                            }
                            for (i in 0 until 4) {
                                for (j in i + 1 until 4) {
                                    closest = min(closest, hypot(xs[i] - xs[j], ys[i] - ys[j]) - radius[i] - radius[j])
                                }
                            }
                            floatTime += FLOAT_WALK_STEP
                        }
                        danceTime += DANCE_WALK_STEP
                    }
                }
            }

            assertTrue(
                closest >= KEPT_GAP_DP,
                "a dancing badge comes to $closest dp of a neighbour in a room of $available dp, float $a",
            )
        }
    }

    // endregion

    // region the edges

    @Test
    fun `nothing at all is an empty grid, and one badge is a grid of one with nothing to touch`() {
        val none = themeBadgeGrid(0, 300f, badge)
        assertEquals(emptyList(), none.xsDp)
        assertEquals(0f, none.widthDp)
        assertEquals(0f, none.heightDp)

        val one = themeBadgeGrid(1, 300f, badge)
        assertEquals(listOf(0f), one.xsDp)
        assertEquals(badge, one.widthDp)
        assertEquals(FLOAT_MAX_DP, one.floatAmplitudeDp)
    }

    @Test
    fun `the badge is a fixed 64 dp so the grid is a function of the room and never of the text size`() {
        // 320 dp at 200 percent text has the same 288 dp room, in dp, as at 100 percent, and so the same grid.
        assertEquals(themeBadgeGrid(4, 288f, badge), themeBadgeGrid(4, 288f, badge))
        assertEquals(ThemeBadgeLayout.Zigzag, grid(320).layout)
    }

    // endregion

    private companion object {
        const val CONTENT_CAP = 480
        const val PAGE_PADDING_TWICE = 32

        // The layout keeps 2.83 dp between two badges in the worst case, a little under which is what the walk may find.
        const val KEPT_GAP_DP = 2.7f
        const val LOOP_SECONDS = 6435f
        const val FLOAT_WALK_STEP = 3.7f
        const val DANCE_WALK_STEP = 0.1f
    }
}
