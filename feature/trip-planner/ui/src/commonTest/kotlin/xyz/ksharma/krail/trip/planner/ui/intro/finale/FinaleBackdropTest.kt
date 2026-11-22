package xyz.ksharma.krail.trip.planner.ui.intro.finale

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What the backdrop draws at each frame that matters, as the plan the drawing follows: nothing but the app at the
 * start, the ground fading in, a circle part way through the flood, the whole screen as a rectangle once it has
 * covered it (so no pixel is left to the edge of a circle), and the colour lifting together with the ground.
 */
class FinaleBackdropTest {

    private data class PlanRow(
        val elapsedMs: Long,
        val ground: Float,
        val flood: FloodShape,
        val floodAlpha: Float,
        val share: Float,
    )

    private fun plan(elapsedMs: Long, reduceMotion: Boolean = false) =
        backdropPlan(finaleFrameAt(elapsedMs, reduceMotion))

    @Test
    fun `the plan at each moment that matters`() {
        listOf(
            PlanRow(0, ground = 0f, flood = FloodShape.NONE, floodAlpha = 1f, share = 0f),
            PlanRow(250, ground = 1f, flood = FloodShape.NONE, floodAlpha = 1f, share = 0f),
            PlanRow(1000, ground = 1f, flood = FloodShape.NONE, floodAlpha = 1f, share = 0f),
            PlanRow(1999, ground = 1f, flood = FloodShape.NONE, floodAlpha = 1f, share = 0f),
            PlanRow(2250, ground = 1f, flood = FloodShape.CIRCLE, floodAlpha = 1f, share = 0.5f),
            PlanRow(2499, ground = 1f, flood = FloodShape.CIRCLE, floodAlpha = 1f, share = 1f),
            PlanRow(2500, ground = 0f, flood = FloodShape.FULL, floodAlpha = 1f, share = 1f),
            PlanRow(2850, ground = 0f, flood = FloodShape.FULL, floodAlpha = 0.5f, share = 1f),
            PlanRow(3200, ground = 0f, flood = FloodShape.FULL, floodAlpha = 0f, share = 1f),
        ).forEach { row ->
            val plan = plan(row.elapsedMs)
            val where = "at ${row.elapsedMs} ms: $plan"
            assertNear(row.ground, plan.groundAlpha, where)
            assertEquals(row.flood, plan.flood, where)
            assertNear(row.floodAlpha, plan.floodAlpha, where)
            assertNear(row.share, plan.circleShare, where)
        }
    }

    @Test
    fun `the whole screen is a rectangle from the covered moment on, never a circle that only reaches the corners`() {
        (FINALE_COVER_MS..4000L).forEach { assertEquals(FloodShape.FULL, plan(it).flood, "at $it ms") }
        (0L until FINALE_COVER_MS).forEach { assertTrue(plan(it).flood != FloodShape.FULL, "at $it ms") }
    }

    @Test
    fun `the ground and the colour lift together, so the fade never shows the ground through the colour`() {
        (FINALE_COVER_MS..4000L).forEach { elapsed ->
            assertEquals(0f, plan(elapsed).groundAlpha, "the ground is already gone at $elapsed ms")
        }
        val alphas = (FINALE_COVER_MS..4000L).map { plan(it).floodAlpha }
        assertEquals(alphas.sortedDescending(), alphas)
    }

    @Test
    fun `with reduced motion the colour is there at once, as the whole screen, and fades over the app`() {
        val first = plan(0, reduceMotion = true)
        assertEquals(FloodShape.FULL, first.flood)
        assertEquals(1f, first.floodAlpha)
        assertEquals(0f, first.groundAlpha)

        assertNear(0.5f, plan(100, reduceMotion = true).floodAlpha, "half way")
        assertEquals(0f, plan(FINALE_REDUCED_FADE_MS, reduceMotion = true).floodAlpha)
    }

    private fun assertNear(expected: Float, actual: Float, where: String) {
        assertTrue(abs(expected - actual) <= TOLERANCE, "$where: expected $expected but was $actual")
    }

    private companion object {
        const val TOLERANCE = 0.011f
    }
}
