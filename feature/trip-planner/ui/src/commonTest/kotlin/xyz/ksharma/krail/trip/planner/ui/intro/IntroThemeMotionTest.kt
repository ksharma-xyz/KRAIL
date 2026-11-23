package xyz.ksharma.krail.trip.planner.ui.intro

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
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

    // region The float

    @Test
    fun `the float never goes further than its amplitude up and down or side to side, for any badge, any amplitude and any time`() {
        listOf(0f, 1.4f, 7.5f, FLOAT_MAX_DP).forEach { amplitude ->
            FloatSpecs.indices.forEach { index ->
                sampleTimes.forEach { t ->
                    val offset = floatOffsetAt(index, t, amplitude)
                    val spec = FloatSpecs[index]

                    assertTrue(abs(offset.yDp) <= amplitude * spec.verticalShare + TOLERANCE, "badge $index at $t: ${offset.yDp}")
                    assertTrue(abs(offset.xDp) <= amplitude * spec.horizontalShare + TOLERANCE, "badge $index at $t: ${offset.xDp}")
                    // The most it is from its place, which is what the room between two badges is made for.
                    assertTrue(hypot(offset.xDp, offset.yDp) <= amplitude * SQRT_TWO + TOLERANCE, "badge $index at $t")
                }
            }
        }
    }

    @Test
    fun `with no amplitude a badge does not move at all`() {
        FloatSpecs.indices.forEach { index ->
            sampleTimes.take(50).forEach { t ->
                val offset = floatOffsetAt(index, t, 0f)
                assertEquals(0f, abs(offset.xDp))
                assertEquals(0f, abs(offset.yDp))
            }
        }
    }

    @Test
    fun `each badge's loop is between 9 and 15 seconds, and no two badges share a period or a phase`() {
        FloatSpecs.forEach { assertTrue(it.periodSeconds in MIN_PERIOD..MAX_PERIOD, "${it.periodSeconds}") }
        assertEquals(FloatSpecs.size, FloatSpecs.map { it.periodSeconds }.toSet().size, "periods differ")
        assertEquals(FloatSpecs.size, FloatSpecs.map { it.phase }.toSet().size, "phases differ")
        FloatSpecs.forEach {
            assertTrue(it.verticalShare in 0f..1f, "vertical ${it.verticalShare}")
            assertTrue(it.horizontalShare in 0f..1f, "horizontal ${it.horizontalShare}")
        }
        // The full float, 8 dp, is reached up and down and side to side by some badge, so it is the number the layout reads.
        assertEquals(FLOAT_MAX_DP, 8f)
        assertTrue(FloatSpecs.any { it.verticalShare == 1f } && FloatSpecs.any { it.horizontalShare == 1f })
    }

    @Test
    fun `a badge comes back to where it started after exactly one period, and is on the other side halfway`() {
        FloatSpecs.forEachIndexed { index, spec ->
            val start = floatOffsetAt(index, 0f, FLOAT_MAX_DP)
            val after = floatOffsetAt(index, spec.periodSeconds, FLOAT_MAX_DP)
            val halfway = floatOffsetAt(index, spec.periodSeconds / 2f, FLOAT_MAX_DP)

            assertNear(start.xDp, after.xDp, "badge $index x", tolerance = LOOSE)
            assertNear(start.yDp, after.yDp, "badge $index y", tolerance = LOOSE)
            assertNear(start.xDp, -halfway.xDp, "badge $index mirrored x", tolerance = LOOSE)
            assertNear(start.yDp, -halfway.yDp, "badge $index mirrored y", tolerance = LOOSE)
        }
    }

    @Test
    fun `at the same moment the badges are in different places, so they never move together`() {
        sampleTimes.take(SAMPLE_COUNT / 10).forEach { t ->
            val places = FloatSpecs.indices.map { floatOffsetAt(it, t, FLOAT_MAX_DP) }

            assertEquals(places.size, places.toSet().size, "at $t: $places")
        }
    }

    @Test
    fun `a badge past the last spec takes the one at its place in the loop of specs`() {
        FloatSpecs.indices.forEach { index ->
            assertEquals(
                floatOffsetAt(index, 3f, FLOAT_MAX_DP),
                floatOffsetAt(index + FloatSpecs.size, 3f, FLOAT_MAX_DP),
                "badge $index",
            )
        }
    }

    @Test
    fun `the float is slow, no badge moving faster than about 6 dp a second at its widest`() {
        FloatSpecs.forEach { spec ->
            val fastest = 2f * PI.toFloat() * FLOAT_MAX_DP / spec.periodSeconds

            assertTrue(fastest <= SLOW_DP_PER_SECOND, "a badge goes $fastest dp a second at its fastest")
        }
    }

    @Test
    fun `the float at known moments`() {
        // Badge 0: period 9 s, phase 0, 8 dp up and down, 6.4 dp sideways (0.8 of it). A quarter turn is 2.25 s.
        assertFrame(BadgeFrame(6.4f, 0f, 0f, 1f), floatOffsetAt(0, 0f, FLOAT_MAX_DP).asFrame(), "badge 0 at 0 s")
        assertFrame(BadgeFrame(0f, 8f, 0f, 1f), floatOffsetAt(0, 2.25f, FLOAT_MAX_DP).asFrame(), "badge 0 at 2.25 s")
        assertFrame(BadgeFrame(-6.4f, 0f, 0f, 1f), floatOffsetAt(0, 4.5f, FLOAT_MAX_DP).asFrame(), "badge 0 at 4.5 s")
        assertFrame(BadgeFrame(0f, -8f, 0f, 1f), floatOffsetAt(0, 6.75f, FLOAT_MAX_DP).asFrame(), "badge 0 at 6.75 s")
        // At half the amplitude it is half the distance.
        assertFrame(BadgeFrame(0f, 4f, 0f, 1f), floatOffsetAt(0, 2.25f, 4f).asFrame(), "badge 0 at half the float")
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
            val drift = floatOffsetAt(2, t, FLOAT_MAX_DP)
            val none = badgeFrame(2, t, danceWeight = 0f, amplitudeDp = FLOAT_MAX_DP)

            assertFrame(BadgeFrame(drift.xDp, drift.yDp, 0f, 1f), none, "drift at $t")
            assertFrame(danceAt(t), badgeFrame(2, t, danceWeight = 1f, amplitudeDp = FLOAT_MAX_DP), "dance at $t")
        }
    }

    @Test
    fun `half the weight is half way between, and a weight outside 0 to 1 is held to it`() {
        val t = 1.9f
        val drift = badgeFrame(1, t, 0f, FLOAT_MAX_DP)
        val dance = badgeFrame(1, t, 1f, FLOAT_MAX_DP)
        val half = badgeFrame(1, t, 0.5f, FLOAT_MAX_DP)

        assertNear((drift.yDp + dance.yDp) / 2f, half.yDp, "y")
        assertNear((drift.scale + dance.scale) / 2f, half.scale, "scale")
        assertFrame(drift, badgeFrame(1, t, -2f, FLOAT_MAX_DP), "below 0")
        assertFrame(dance, badgeFrame(1, t, 2f, FLOAT_MAX_DP), "above 1")
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

    private companion object {
        const val TOLERANCE = 0.0005f
        const val LOOSE = 0.01f
        const val SAMPLE_STEP_SECONDS = 1.1f
        const val SAMPLE_COUNT = 800
        const val MIN_PERIOD = 9f
        const val MAX_PERIOD = 15f
        const val SLOW_DP_PER_SECOND = 6f
        const val JOIN_EPSILON = 0.001f
    }
}
