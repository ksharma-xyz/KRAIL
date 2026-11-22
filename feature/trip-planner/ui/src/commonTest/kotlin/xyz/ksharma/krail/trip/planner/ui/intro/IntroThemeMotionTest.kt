package xyz.ksharma.krail.trip.planner.ui.intro

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntroThemeMotionTest {

    // A spread of moments, 1.1 seconds apart for about a quarter of an hour, which is more than any one loop of any badge.
    private val sampleTimes = (0..SAMPLE_COUNT).map { it * SAMPLE_STEP_SECONDS }

    private fun assertNear(expected: Float, actual: Float, message: String, tolerance: Float = TOLERANCE) {
        assertTrue(abs(expected - actual) <= tolerance, "$message: expected $expected but was $actual")
    }

    private fun assertFrame(expected: BadgeFrame, actual: BadgeFrame, message: String, tolerance: Float = TOLERANCE) {
        assertNear(expected.xDp, actual.xDp, "$message x", tolerance)
        assertNear(expected.yDp, actual.yDp, "$message y", tolerance)
        assertNear(expected.rotationDegrees, actual.rotationDegrees, "$message rotation", tolerance)
        assertNear(expected.scale, actual.scale, "$message scale", tolerance)
    }

    // region The drift

    @Test
    fun `the drift never goes further than 6 dp up or down or 4 dp sideways, for any badge at any time`() {
        FloatSpecs.indices.forEach { index ->
            sampleTimes.forEach { t ->
                val offset = floatOffsetAt(index, t)

                assertTrue(abs(offset.yDp) <= FLOAT_MAX_VERTICAL_DP + TOLERANCE, "badge $index at $t: ${offset.yDp}")
                assertTrue(abs(offset.xDp) <= FLOAT_MAX_HORIZONTAL_DP + TOLERANCE, "badge $index at $t: ${offset.xDp}")
            }
        }
    }

    @Test
    fun `each badge's loop is between 8 and 13 seconds, and no two badges share a period or a phase`() {
        FloatSpecs.forEach { assertTrue(it.periodSeconds in MIN_PERIOD..MAX_PERIOD, "${it.periodSeconds}") }
        assertEquals(FloatSpecs.size, FloatSpecs.map { it.periodSeconds }.toSet().size, "periods differ")
        assertEquals(FloatSpecs.size, FloatSpecs.map { it.phase }.toSet().size, "phases differ")
        FloatSpecs.forEach {
            assertTrue(it.verticalDp <= FLOAT_MAX_VERTICAL_DP, "vertical ${it.verticalDp}")
            assertTrue(it.horizontalDp <= FLOAT_MAX_HORIZONTAL_DP, "horizontal ${it.horizontalDp}")
        }
    }

    @Test
    fun `a badge comes back to where it started after exactly one period, and is somewhere else halfway`() {
        FloatSpecs.forEachIndexed { index, spec ->
            val start = floatOffsetAt(index, 0f)
            val after = floatOffsetAt(index, spec.periodSeconds)
            val halfway = floatOffsetAt(index, spec.periodSeconds / 2f)

            assertNear(start.xDp, after.xDp, "badge $index x", tolerance = LOOSE)
            assertNear(start.yDp, after.yDp, "badge $index y", tolerance = LOOSE)
            assertNear(start.xDp, -halfway.xDp, "badge $index mirrored x", tolerance = LOOSE)
            assertNear(start.yDp, -halfway.yDp, "badge $index mirrored y", tolerance = LOOSE)
        }
    }

    @Test
    fun `at the same moment the badges are in different places, so they never move together`() {
        sampleTimes.take(SAMPLE_COUNT / 10).forEach { t ->
            val places = FloatSpecs.indices.map { floatOffsetAt(it, t) }

            assertEquals(places.size, places.toSet().size, "at $t: $places")
        }
    }

    @Test
    fun `a badge past the last spec takes the one at its place in the loop of specs`() {
        FloatSpecs.indices.forEach { index ->
            assertEquals(floatOffsetAt(index, 3f), floatOffsetAt(index + FloatSpecs.size, 3f), "badge $index")
        }
    }

    @Test
    fun `the drift at known moments`() {
        // Badge 0: period 8 s, phase 0, 6 dp up and down, 3 dp sideways. A quarter turn is 2 s.
        assertFrame(
            BadgeFrame(xDp = 3f, yDp = 0f, rotationDegrees = 0f, scale = 1f),
            floatOffsetAt(0, 0f).asFrame(),
            "badge 0 at 0 s",
        )
        assertFrame(
            BadgeFrame(xDp = 0f, yDp = 6f, rotationDegrees = 0f, scale = 1f),
            floatOffsetAt(0, 2f).asFrame(),
            "badge 0 at 2 s",
        )
        assertFrame(
            BadgeFrame(xDp = -3f, yDp = 0f, rotationDegrees = 0f, scale = 1f),
            floatOffsetAt(0, 4f).asFrame(),
            "badge 0 at 4 s",
        )
        assertFrame(
            BadgeFrame(xDp = 0f, yDp = -6f, rotationDegrees = 0f, scale = 1f),
            floatOffsetAt(0, 6f).asFrame(),
            "badge 0 at 6 s",
        )
    }

    private fun FloatOffset.asFrame() = BadgeFrame(xDp, yDp, rotationDegrees = 0f, scale = 1f)

    // endregion

    // region The dance

    @Test
    fun `the dance at known moments, the two ends and the middle of each leg`() {
        val table = listOf(
            Triple("start, the first end", 0f, BadgeFrame(0f, -2f, -10f, 1.05f)),
            Triple("halfway out", 0.325f, BadgeFrame(0f, -5f, 0f, 1.075f)),
            Triple("the far end", 0.65f, BadgeFrame(0f, -8f, 10f, 1.10f)),
            Triple("halfway home", 0.975f, BadgeFrame(0f, -5f, 0f, 1.075f)),
            Triple("home again", 1.3f, BadgeFrame(0f, -2f, -10f, 1.05f)),
            Triple("a long time on, at the far end", 0.65f + 1.3f * 100f, BadgeFrame(0f, -8f, 10f, 1.10f)),
        )

        table.forEach { (name, t, expected) -> assertFrame(expected, danceAt(t), name) }
    }

    @Test
    fun `the dance stays inside minus 10 to plus 10 degrees, rises 2 to 8 dp and scales 1_05 to 1_10, always`() {
        sampleTimes.forEach { t ->
            val frame = danceAt(t)

            assertTrue(frame.rotationDegrees in -DANCE_ROTATION_DEGREES - TOLERANCE..DANCE_ROTATION_DEGREES + TOLERANCE)
            assertTrue(frame.yDp in -DANCE_RISE_TO_DP - TOLERANCE..-DANCE_RISE_FROM_DP + TOLERANCE, "$t ${frame.yDp}")
            assertTrue(frame.scale in DANCE_SCALE_FROM - TOLERANCE..DANCE_SCALE_TO + TOLERANCE, "$t ${frame.scale}")
        }
    }

    @Test
    fun `the dance repeats every 1_3 seconds and has no jump where one leg meets the next`() {
        sampleTimes.take(SAMPLE_COUNT / 20).forEach { t ->
            assertFrame(danceAt(t), danceAt(t + 2 * DANCE_LEG_SECONDS), "at $t")
        }
        listOf(DANCE_LEG_SECONDS, 2 * DANCE_LEG_SECONDS, 3 * DANCE_LEG_SECONDS).forEach { join ->
            assertFrame(danceAt(join - JOIN_EPSILON), danceAt(join + JOIN_EPSILON), "the join at $join", LOOSE)
        }
    }

    @Test
    fun `the easing is flat at both ends and half way at the middle`() {
        assertNear(0f, easeInOut(0f), "start")
        assertNear(0.5f, easeInOut(0.5f), "middle")
        assertNear(1f, easeInOut(1f), "end")
        assertNear(0f, easeInOut(-3f), "before the start is held at the start")
        assertNear(1f, easeInOut(3f), "after the end is held at the end")
        assertTrue(easeInOut(0.1f) < 0.1f, "slower than a straight line at the start")
        assertTrue(easeInOut(0.9f) > 0.9f, "slower than a straight line at the end")
    }

    // endregion

    // region Blending the drift and the dance

    @Test
    fun `with no dance weight the badge only drifts, and with the full weight it only dances`() {
        listOf(0f, 1.7f, 9.3f).forEach { t ->
            val drift = floatOffsetAt(2, t)
            val none = badgeFrame(2, t, danceWeight = 0f)

            assertFrame(BadgeFrame(drift.xDp, drift.yDp, 0f, 1f), none, "drift at $t")
            assertFrame(danceAt(t), badgeFrame(2, t, danceWeight = 1f), "dance at $t")
        }
    }

    @Test
    fun `half the weight is half way between, and a weight outside 0 to 1 is held to it`() {
        val t = 1.9f
        val drift = badgeFrame(1, t, 0f)
        val dance = badgeFrame(1, t, 1f)
        val half = badgeFrame(1, t, 0.5f)

        assertNear((drift.yDp + dance.yDp) / 2f, half.yDp, "y")
        assertNear((drift.scale + dance.scale) / 2f, half.scale, "scale")
        assertFrame(drift, badgeFrame(1, t, -2f), "below 0")
        assertFrame(dance, badgeFrame(1, t, 2f), "above 1")
    }

    // endregion

    // region No motion

    @Test
    fun `with no motion a badge sits at its place and the chosen one lifts 4 dp`() {
        assertFrame(RestFrame, settledFrame(selected = false), "not chosen")
        assertFrame(BadgeFrame(0f, -4f, 0f, 1f), settledFrame(selected = true), "chosen")
        assertEquals(SETTLED_LIFT_DP, 4f)
    }

    @Test
    fun `the motion runs only when the rider has not asked for less and it is not a preview or a screenshot`() {
        assertTrue(themeMotionRuns(reduceMotion = false, inspection = false))
        assertFalse(themeMotionRuns(reduceMotion = true, inspection = false))
        assertFalse(themeMotionRuns(reduceMotion = false, inspection = true))
        assertFalse(themeMotionRuns(reduceMotion = true, inspection = true))
    }

    // endregion

    // region Size and ring

    @Test
    fun `the badge is 64 dp at the default text size, grows with the text to at most 96 dp, and never shrinks`() {
        val table = listOf(
            0.85f to 64f,
            1f to 64f,
            1.15f to 73.6f,
            1.3f to 83.2f,
            1.5f to 96f,
            2f to 96f,
            3f to 96f,
        )

        table.forEach { (scale, size) -> assertNear(size, badgeSizeDp(scale), "scale $scale", tolerance = LOOSE) }
    }

    @Test
    fun `the touch target is never under 48 dp, and two of the largest badges fit side by side at 320 dp`() {
        listOf(0.5f, 1f, 1.5f, 2f).forEach { assertTrue(badgeSizeDp(it) >= MIN_TOUCH_DP, "$it") }
        val widthAt320 = NARROW_WIDTH_DP - 2 * PAGE_PADDING_DP
        assertTrue(2 * badgeSizeDp(2f) + MIN_GAP_DP <= widthAt320, "two of the largest side by side")
    }

    @Test
    fun `the ring is thicker only for the chosen badge when there is no motion to show it`() {
        val table = listOf(
            Triple("not chosen, no motion", ringWidthDp(false, motionRuns = false, regularDp = 3f, thickDp = 4f), 3f),
            Triple("not chosen, motion", ringWidthDp(false, motionRuns = true, regularDp = 3f, thickDp = 4f), 3f),
            Triple("chosen, motion", ringWidthDp(true, motionRuns = true, regularDp = 3f, thickDp = 4f), 3f),
            Triple("chosen, no motion", ringWidthDp(true, motionRuns = false, regularDp = 3f, thickDp = 4f), 4f),
        )

        table.forEach { (name, actual, expected) -> assertEquals(expected, actual, name) }
    }

    // endregion

    private companion object {
        const val TOLERANCE = 0.0005f
        const val LOOSE = 0.01f
        const val SAMPLE_STEP_SECONDS = 1.1f
        const val SAMPLE_COUNT = 800
        const val MIN_PERIOD = 8f
        const val MAX_PERIOD = 13f
        const val JOIN_EPSILON = 0.001f
        const val MIN_TOUCH_DP = 48f
        const val NARROW_WIDTH_DP = 320f
        const val PAGE_PADDING_DP = 16f
        const val MIN_GAP_DP = 16f
    }
}
