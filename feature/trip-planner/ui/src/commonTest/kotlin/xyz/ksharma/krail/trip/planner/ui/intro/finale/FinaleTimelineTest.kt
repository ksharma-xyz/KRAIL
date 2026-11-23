package xyz.ksharma.krail.trip.planner.ui.intro.finale

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The finale's timeline is a pure function of the elapsed time, so every rule is held here with no clock: the
 * phases and where they change, the bounds of every number at every sampled moment, that colour, flood and
 * fade only go one way, that the app is asked to navigate at the covered moment and nowhere else, the
 * letters' spring, and the reduced motion frames.
 */
class FinaleTimelineTest {

    // region the phases

    private data class PhaseRow(val elapsedMs: Long, val phase: FinalePhase)

    private val phaseTable = listOf(
        PhaseRow(-50, FinalePhase.ENTER),
        PhaseRow(0, FinalePhase.ENTER),
        PhaseRow(599, FinalePhase.ENTER),
        PhaseRow(600, FinalePhase.HOLD),
        PhaseRow(1499, FinalePhase.HOLD),
        PhaseRow(1500, FinalePhase.BLEND),
        PhaseRow(1999, FinalePhase.BLEND),
        PhaseRow(2000, FinalePhase.FLOOD),
        PhaseRow(2499, FinalePhase.FLOOD),
        PhaseRow(2500, FinalePhase.NAVIGATE),
        PhaseRow(2501, FinalePhase.FADE),
        PhaseRow(3199, FinalePhase.FADE),
        PhaseRow(3200, FinalePhase.DONE),
        PhaseRow(9000, FinalePhase.DONE),
    )

    @Test
    fun `the phase changes at 600, 1500, 2000, 2500 and 3200 milliseconds`() {
        phaseTable.forEach { row ->
            assertEquals(row.phase, finaleFrameAt(row.elapsedMs, reduceMotion = false).phase, "at ${row.elapsedMs} ms")
        }
    }

    @Test
    fun `the nominal timeline is 3200 milliseconds, the covered moment plus the fade`() {
        assertEquals(3200L, FINALE_COVER_MS + FINALE_FADE_MS)
        assertEquals(FinalePhase.DONE, finaleFrameAt(FINALE_COVER_MS + FINALE_FADE_MS, reduceMotion = false).phase)
    }

    @Test
    fun `the phases only ever go forward as time goes on`() {
        val phases = (0L..4000L).map { finaleFrameAt(it, reduceMotion = false).phase.ordinal }

        assertEquals(phases.sorted(), phases)
        assertEquals(FinalePhase.entries.size, phases.distinct().size, "every phase is reached")
    }

    // endregion

    // region bounds and direction

    @Test
    fun `every number stays inside its range at every millisecond`() {
        (0L..4000L).forEach { elapsed ->
            val frame = finaleFrameAt(elapsed, reduceMotion = false)
            val where = "at $elapsed ms: $frame"
            assertTrue(frame.overlayAlpha in 0f..1f, where)
            assertTrue(frame.wordmarkAlpha in 0f..1f, where)
            assertTrue(frame.groundAlpha in 0f..1f, where)
            assertTrue(frame.glow in 0f..1f, where)
            assertTrue(frame.colourBlend in 0f..1f, where)
            assertTrue(frame.floodRadius in 0f..1f, where)
        }
    }

    @Test
    fun `the flood and the colour blend only grow, and the wordmark and the overlay only fade`() {
        val frames = (0L..4000L).map { finaleFrameAt(it, reduceMotion = false) }

        frames.zipWithNext().forEach { (before, after) ->
            assertTrue(after.floodRadius >= before.floodRadius, "flood shrank at ${after.wordmarkMs}")
            assertTrue(after.colourBlend >= before.colourBlend, "blend went back at ${after.wordmarkMs}")
            assertTrue(after.wordmarkAlpha <= before.wordmarkAlpha, "wordmark came back at ${after.wordmarkMs}")
            assertTrue(after.overlayAlpha <= before.overlayAlpha, "overlay came back at ${after.wordmarkMs}")
        }
    }

    private data class MomentRow(
        val elapsedMs: Long,
        val flood: Float,
        val blend: Float,
        val overlay: Float,
        val wordmark: Float,
    )

    @Test
    fun `the flood, the blend, the overlay and the wordmark at the moments that matter`() {
        listOf(
            MomentRow(0, flood = 0f, blend = 0f, overlay = 1f, wordmark = 1f),
            MomentRow(1500, flood = 0f, blend = 0f, overlay = 1f, wordmark = 1f),
            MomentRow(1750, flood = 0f, blend = 0.5f, overlay = 1f, wordmark = 1f),
            MomentRow(2000, flood = 0f, blend = 1f, overlay = 1f, wordmark = 1f),
            MomentRow(2250, flood = 0.5f, blend = 1f, overlay = 1f, wordmark = 0.5f),
            MomentRow(2500, flood = 1f, blend = 1f, overlay = 1f, wordmark = 0f),
            MomentRow(2850, flood = 1f, blend = 1f, overlay = 0.5f, wordmark = 0f),
            MomentRow(3200, flood = 1f, blend = 1f, overlay = 0f, wordmark = 0f),
        ).forEach { row ->
            val frame = finaleFrameAt(row.elapsedMs, reduceMotion = false)
            val where = "at ${row.elapsedMs} ms"
            assertNear(row.flood, frame.floodRadius, where)
            assertNear(row.blend, frame.colourBlend, where)
            assertNear(row.overlay, frame.overlayAlpha, where)
            assertNear(row.wordmark, frame.wordmarkAlpha, where)
        }
    }

    @Test
    fun `the ground fades in at the start and is gone once the colour covers the screen`() {
        assertNear(0f, finaleFrameAt(0, reduceMotion = false).groundAlpha, "first frame")
        assertNear(1f, finaleFrameAt(250, reduceMotion = false).groundAlpha, "250 ms")
        assertNear(1f, finaleFrameAt(2499, reduceMotion = false).groundAlpha, "just before cover")
        assertNear(0f, finaleFrameAt(2500, reduceMotion = false).groundAlpha, "covered")
    }

    @Test
    fun `a shorter fade finishes sooner and ends in the same place`() {
        val quick = quickFadeAt(FINALE_SKIP_FADE_MS)
        val half = quickFadeAt(FINALE_SKIP_FADE_MS / 2)

        assertEquals(FinalePhase.DONE, quick.phase)
        assertNear(0f, quick.overlayAlpha, "end of the quick fade")
        assertNear(0.5f, half.overlayAlpha, "middle of the quick fade")
    }

    // endregion

    // region the glow

    @Test
    fun `the glow rises, breathes once, holds, and goes out as the flood takes over`() {
        assertNear(0f, finaleFrameAt(0, reduceMotion = false).glow, "start")
        assertNear(GLOW_BASE, finaleFrameAt(600, reduceMotion = false).glow, "the end of the entrance is the base")
        assertNear(GLOW_PEAK, finaleFrameAt(1050, reduceMotion = false).glow, "the middle of the hold is the peak")
        assertNear(GLOW_BASE, finaleFrameAt(1500, reduceMotion = false).glow, "back to the base after one breath")
        assertNear(GLOW_BASE, finaleFrameAt(1999, reduceMotion = false).glow, "the base while it blends")
        assertNear(0f, finaleFrameAt(2500, reduceMotion = false).glow, "out at cover")
    }

    @Test
    fun `the glow swells once only during the hold`() {
        val hold = (600L..1500L).map { finaleFrameAt(it, reduceMotion = false).glow }
        val peakAt = hold.indexOf(hold.max())

        assertTrue(hold.take(peakAt + 1).zipWithNext().all { (a, b) -> b >= a - EPSILON }, "rises to the peak")
        assertTrue(hold.drop(peakAt).zipWithNext().all { (a, b) -> b <= a + EPSILON }, "falls from the peak")
    }

    // endregion

    // region navigating

    @Test
    fun `the app is asked to navigate at exactly one moment, when the colour covers the whole screen`() {
        val navigating = (0L..4000L).map { it to finaleFrameAt(it, reduceMotion = false) }
            .filter { (_, frame) -> frame.phase == FinalePhase.NAVIGATE }

        assertEquals(listOf(FINALE_COVER_MS), navigating.map { it.first })
        navigating.forEach { (_, frame) ->
            assertNear(1f, frame.floodRadius, "the flood is complete")
            assertNear(1f, frame.overlayAlpha, "the overlay is opaque")
            assertNear(0f, frame.wordmarkAlpha, "no wordmark is left to show")
        }
    }

    @Test
    fun `no frame before the covered moment is fully covered`() {
        (0L until FINALE_COVER_MS).forEach { elapsed ->
            val frame = finaleFrameAt(elapsed, reduceMotion = false)
            assertTrue(frame.floodRadius < 1f, "at $elapsed ms: $frame")
            assertTrue(frame.phase < FinalePhase.NAVIGATE, "at $elapsed ms")
        }
    }

    // endregion

    // region the letters

    @Test
    fun `a letter is not started, springs past its place, and settles at exactly one`() {
        assertEquals(0f, letterProgressAt(0, 0))
        assertEquals(0f, letterProgressAt(0, -10))
        assertEquals(1f, letterProgressAt(0, WORDMARK_LETTER_MS))
        assertEquals(1f, letterProgressAt(0, 5000))

        val samples = (0L..WORDMARK_LETTER_MS).map { letterProgressAt(0, it) }
        assertTrue(samples.max() > 1.05f, "it overshoots by a visible amount")
        assertTrue(samples.max() < 1.15f, "and by no more than a small spring")
        assertTrue(samples.all { it >= 0f }, "and never goes below where it started")
    }

    @Test
    fun `the letters start in order, so the ones that have started are always the first ones`() {
        val at = 200L
        val started = (0 until LETTERS).map { letterProgressAt(it, at) > 0f }

        assertEquals(started.sortedDescending(), started, "$started")
        assertTrue(started.first(), "the first letter has started")
        assertTrue(!started.last(), "the last has not")
        assertTrue(letterProgressAt(0, at) > 0.9f, "and the first is nearly in place")
    }

    @Test
    fun `the last of the eleven letters settles exactly when the entrance ends`() {
        assertEquals(FINALE_ENTER_END_MS, (LETTERS - 1) * WORDMARK_LETTER_STAGGER_MS + WORDMARK_LETTER_MS)
        assertTrue(letterProgressAt(LETTERS - 1, FINALE_ENTER_END_MS - 1) != 1f, "still springing a moment before")

        val frame = finaleFrameAt(FINALE_ENTER_END_MS, reduceMotion = false)
        (0 until LETTERS).forEach { assertEquals(1f, frame.letterProgress(it), "letter $it") }
    }

    @Test
    fun `the brand gradient travels over the entrance and the hold and then rests`() {
        assertNear(0f, gradientTravelAt(0), "start")
        assertNear(1f, gradientTravelAt(FINALE_HOLD_END_MS), "end of the hold")
        assertNear(1f, gradientTravelAt(FINALE_COVER_MS), "after")
        val travel = (0L..FINALE_HOLD_END_MS).map { gradientTravelAt(it) }
        assertEquals(travel.sorted(), travel)
    }

    // endregion

    // region reduced motion

    private data class ReducedRow(
        val elapsedMs: Long,
        val phase: FinalePhase,
        val overlay: Float,
    )

    @Test
    fun `reduced motion has no wordmark, no glow and no flood, only a cross fade of 200 milliseconds`() {
        listOf(
            ReducedRow(0, FinalePhase.NAVIGATE, overlay = 1f),
            ReducedRow(1, FinalePhase.FADE, overlay = 0.995f),
            ReducedRow(100, FinalePhase.FADE, overlay = 0.5f),
            ReducedRow(199, FinalePhase.FADE, overlay = 0.005f),
            ReducedRow(200, FinalePhase.DONE, overlay = 0f),
            ReducedRow(5000, FinalePhase.DONE, overlay = 0f),
        ).forEach { row ->
            val frame = finaleFrameAt(row.elapsedMs, reduceMotion = true)
            val where = "at ${row.elapsedMs} ms"
            assertEquals(row.phase, frame.phase, where)
            assertNear(row.overlay, frame.overlayAlpha, where)
            assertEquals(0f, frame.wordmarkAlpha, where)
            assertEquals(0f, frame.glow, where)
            assertEquals(0f, frame.groundAlpha, where)
            assertEquals(1f, frame.floodRadius, where)
            assertEquals(1f, frame.colourBlend, where)
        }
    }

    @Test
    fun `reduced motion asks the app to navigate at the start and nowhere else`() {
        val navigating = (0L..400L).filter { finaleFrameAt(it, reduceMotion = true).phase == FinalePhase.NAVIGATE }

        assertEquals(listOf(0L), navigating)
        assertEquals(0L, finaleCoverMs(reduceMotion = true))
    }

    @Test
    fun `the fade lasts 700 milliseconds, 300 after a tap and 200 with reduced motion`() {
        assertEquals(700L, finaleFadeMs(skipped = false, reduceMotion = false))
        assertEquals(300L, finaleFadeMs(skipped = true, reduceMotion = false))
        assertEquals(200L, finaleFadeMs(skipped = false, reduceMotion = true))
        assertEquals(200L, finaleFadeMs(skipped = true, reduceMotion = true))
    }

    // endregion

    // region a tap

    @Test
    fun `a tap goes to the covered frame, where the app is asked to navigate, and then fades out fast`() {
        val target = finaleFrameAt(
            elapsedMs = finaleCoverMs(reduceMotion = false),
            reduceMotion = false,
            fadeMs = FINALE_SKIP_FADE_MS,
        )

        assertEquals(FinalePhase.NAVIGATE, target.phase)
        assertNear(1f, target.floodRadius, "covered")
        assertNear(1f, target.overlayAlpha, "opaque")
        assertEquals(0f, target.wordmarkAlpha)
        assertEquals(0f, target.glow)
        assertEquals(FinalePhase.DONE, quickFadeAt(FINALE_SKIP_FADE_MS).phase)
    }

    // endregion

    // The frame this long after the covered moment, on the fade a tap uses.
    private fun quickFadeAt(sinceCoverMs: Long): FinaleFrame = finaleFrameAt(
        elapsedMs = FINALE_COVER_MS + sinceCoverMs,
        reduceMotion = false,
        fadeMs = FINALE_SKIP_FADE_MS,
    )

    private fun assertNear(expected: Float, actual: Float, where: String) {
        assertTrue(abs(expected - actual) <= TOLERANCE, "$where: expected $expected but was $actual")
    }

    private companion object {
        const val LETTERS = 11
        const val TOLERANCE = 0.011f
        const val EPSILON = 0.0001f
    }
}
