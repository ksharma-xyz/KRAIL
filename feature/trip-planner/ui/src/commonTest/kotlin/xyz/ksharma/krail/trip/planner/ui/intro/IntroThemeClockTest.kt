package xyz.ksharma.krail.trip.planner.ui.intro

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class IntroThemeClockTest {

    private class Row(val name: String, val previous: Float, val current: Float, val advance: Float)

    @Test
    fun `the clock moves forward by what the transition moved, and carries on across the wrap`() {
        val rows = listOf(
            Row("a frame", 10f, 10.016f, 0.016f),
            Row("no movement", 30f, 30f, 0f),
            Row("the start", 0f, 0.016f, 0.016f),
            Row("just before the end", 59.9f, 59.99f, 0.09f),
            Row("across the wrap", 59.99f, 0.01f, 0.02f),
            Row("onto the wrap exactly", 59.99f, 0f, 0.01f),
            Row("a long gap that wrapped", 59f, 2f, 3f),
        )

        rows.forEach { row ->
            val advance = clockAdvance(row.previous, row.current)
            assertTrue(abs(row.advance - advance) <= TOLERANCE, "${row.name}: expected ${row.advance} but was $advance")
            assertTrue(advance >= 0f, "${row.name} went backwards")
        }
    }

    @Test
    fun `a loop of a different length wraps at its own length`() {
        assertTrue(abs(1f - clockAdvance(previous = 9.5f, current = 0.5f, loop = 10f)) <= TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 0.0005f
    }
}
