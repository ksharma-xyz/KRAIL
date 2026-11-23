package xyz.ksharma.krail.trip.planner.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The arrival of the Park & Ride figures as a table of times: three beats, each a 250 ms ease out fade with an 8 dp rise, started 80 ms
 * apart, so the card is complete 410 ms after the first one starts. Reduced motion is one plain 150 ms fade of all three together. The
 * real card on a stepped clock is held by `IntroParkRideFiguresFadeTest`.
 */
class ParkRideFigureRevealTest {

    private val beats = 0 until FIGURE_BEAT_COUNT

    @Test
    fun `the beats are 250 ms long, 80 ms apart and rise 8 dp, and the whole thing is 410 ms`() {
        assertEquals(250, FIGURE_BEAT_MS)
        assertEquals(80, FIGURE_STAGGER_MS)
        assertEquals(8, FIGURE_RISE_DP)
        assertEquals(3, FIGURE_BEAT_COUNT)
        assertEquals(410, FIGURE_TOTAL_MS)
        assertEquals(410, figureRevealMs(reduceMotion = false))
    }

    @Test
    fun `nothing is drawn and everything is low when the numbers land`() {
        beats.forEach { beat ->
            val frame = figureFrame(elapsedMs = 0f, beat = beat, reduceMotion = false)
            assertEquals(0f, frame.alpha)
            assertEquals(1f, frame.rise)
        }
    }

    @Test
    fun `each line starts 80 ms after the one before it and not a moment sooner`() {
        beats.forEach { beat ->
            val start = (beat * FIGURE_STAGGER_MS).toFloat()
            assertEquals(0f, figureFrame(start, beat, reduceMotion = false).alpha, "line $beat has started by $start")
            assertTrue(figureFrame(start + 1f, beat, reduceMotion = false).alpha > 0f, "line $beat has not started by ${start + 1f}")
            if (beat > 0) assertEquals(0f, figureFrame(start - 1f, beat, reduceMotion = false).alpha)
        }
    }

    @Test
    fun `every line is the first line's curve moved later by the stagger, so it is the same beat`() {
        listOf(0f, 40f, 90f, 160f, 250f, 330f).forEach { elapsed ->
            beats.forEach { beat ->
                val shifted = figureFrame(elapsed - beat * FIGURE_STAGGER_MS, 0, reduceMotion = false)
                assertEquals(shifted, figureFrame(elapsed, beat, reduceMotion = false), "line $beat at $elapsed ms")
            }
        }
    }

    @Test
    fun `a line is complete 250 ms after it starts and the last is complete at 410 ms`() {
        beats.forEach { beat ->
            val end = (beat * FIGURE_STAGGER_MS + FIGURE_BEAT_MS).toFloat()
            assertEquals(1f, figureFrame(end, beat, reduceMotion = false).alpha)
            assertEquals(0f, figureFrame(end, beat, reduceMotion = false).rise)
            assertTrue(figureFrame(end - 1f, beat, reduceMotion = false).alpha < 1f, "line $beat is complete before $end")
        }
        assertEquals(FIGURE_TOTAL_MS.toFloat(), (FIGURE_BEAT_COUNT - 1) * FIGURE_STAGGER_MS + FIGURE_BEAT_MS.toFloat())
        beats.forEach { assertEquals(1f, figureFrame(FIGURE_TOTAL_MS.toFloat(), it, reduceMotion = false).alpha) }
    }

    @Test
    fun `lines never go backwards and the rise is what the fade has left`() {
        beats.forEach { beat ->
            var before = figureFrame(0f, beat, reduceMotion = false)
            for (ms in 1..FIGURE_TOTAL_MS) {
                val frame = figureFrame(ms.toFloat(), beat, reduceMotion = false)
                assertTrue(frame.alpha >= before.alpha, "line $beat fades back at $ms ms")
                assertTrue(frame.rise <= before.rise, "line $beat sinks at $ms ms")
                assertEquals(1f, frame.alpha + frame.rise, 1e-5f, "the rise is not what the fade leaves at $ms ms")
                before = frame
            }
        }
    }

    @Test
    fun `a beat eases out, so more than half of the fade is done by the half way mark and most of it before the end`() {
        val half = figureFrame(FIGURE_BEAT_MS / 2f, 0, reduceMotion = false)
        val quarter = figureFrame(FIGURE_BEAT_MS / 4f, 0, reduceMotion = false)
        val threeQuarters = figureFrame(FIGURE_BEAT_MS * 3 / 4f, 0, reduceMotion = false)

        assertTrue(half.alpha > 0.6f, "half way through the beat it is only ${half.alpha} drawn")
        // The first half covers more ground than the second, which is the ease out.
        assertTrue(half.alpha - 0f > 1f - half.alpha, "the first half does no more than the second")
        assertTrue(threeQuarters.alpha > 0.9f, "three quarters through the beat it is only ${threeQuarters.alpha} drawn")
        assertTrue(quarter.alpha < half.alpha)
    }

    @Test
    fun `with reduced motion the three lines are one fade of 150 ms, together, with no rise`() {
        assertEquals(150, FIGURE_REDUCED_FADE_MS)
        assertEquals(150, figureRevealMs(reduceMotion = true))
        listOf(0f, 20f, 60f, 100f, 149f, 150f, 300f).forEach { elapsed ->
            val frames = beats.map { figureFrame(elapsed, it, reduceMotion = true) }
            assertEquals(1, frames.distinct().size, "the lines are staggered at $elapsed ms")
            assertEquals(0f, frames.first().rise, "a line rises at $elapsed ms")
        }
        assertEquals(0f, figureFrame(0f, 0, reduceMotion = true).alpha)
        assertEquals(1f, figureFrame(150f, 0, reduceMotion = true).alpha)
        assertTrue(figureFrame(75f, 2, reduceMotion = true).alpha in 0.1f..0.95f)
    }
}
