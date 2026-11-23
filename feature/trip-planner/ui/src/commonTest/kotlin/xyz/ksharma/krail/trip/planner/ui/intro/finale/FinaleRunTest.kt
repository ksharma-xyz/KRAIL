package xyz.ksharma.krail.trip.planner.ui.intro.finale

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The run is one pure function over the frames, so the rules that decide when the app navigates and when the
 * colour lifts are held here with a simulated clock and a simulated screen, and no real time. The simulation
 * draws a frame every [FRAME_MS] and plays the Intro's part: it says it has navigated [navigateAfterMs] after
 * it was asked, or never.
 */
class FinaleRunTest {

    private class Run(
        val frames: List<FinaleStep>,
        val navigateAskedAt: List<Long>,
        val fadeStartedAt: Long?,
        val finishedAt: Long?,
    )

    /**
     * Runs until the finale finishes or [limitMs]. [tapAtMs] is when the rider taps, if ever, and [navigateAfterMs] is
     * how long after being asked the Intro says it has navigated, or null for never. [releaseAtMs] is when the Intro
     * releases the run, after the writes and the wait for the stops, or null for never.
     */
    private fun simulate(
        reduceMotion: Boolean = false,
        tapAtMs: Long? = null,
        navigateAfterMs: Long? = 0L,
        limitMs: Long = LIMIT_MS,
        releaseAtMs: Long? = 0L,
    ): Run {
        var state = FinaleRunState()
        val frames = mutableListOf<FinaleStep>()
        val asked = mutableListOf<Long>()
        var finishedAt: Long? = null
        var elapsed = 0L
        while (elapsed <= limitMs && finishedAt == null) {
            val askedAt = asked.firstOrNull()
            val tick = FinaleTick(
                elapsedMs = elapsed,
                navigated = askedAt != null && navigateAfterMs != null && elapsed >= askedAt + navigateAfterMs,
                tapped = tapAtMs != null && elapsed >= tapAtMs && elapsed < tapAtMs + FRAME_MS,
                navigationReady = releaseAtMs != null && elapsed >= releaseAtMs,
            )
            val step = finaleStep(state, tick, reduceMotion)
            state = step.state
            frames += step
            if (step.navigateNow) asked += elapsed
            if (step.finished) finishedAt = elapsed
            elapsed += FRAME_MS
        }
        return Run(frames, asked, state.fadeStartedAtMs, finishedAt)
    }

    // region the hold for the Intro

    @Test
    fun `a run that is not released covers the screen and holds there, never asking to navigate and never fading`() {
        val run = simulate(releaseAtMs = null, limitMs = 15_000L)

        assertEquals(
            emptyList(),
            run.navigateAskedAt,
            "the app was asked to navigate before the Intro released the run",
        )
        assertNull(run.fadeStartedAt)
        assertNull(run.finishedAt)
        val held = run.frames.filter { it.frame.phase == FinalePhase.NAVIGATE }
        assertTrue(held.size > 100, "the clock did not wait at the cover")
        held.forEach { assertEquals(1f, it.frame.floodRadius, "the screen is not fully covered while it waits") }
    }

    @Test
    fun `the app is asked to navigate on the first frame after the release, and the fade counts from there`() {
        val run = simulate(releaseAtMs = 9000L, limitMs = 13_000L)

        assertEquals(1, run.navigateAskedAt.size)
        assertTrue(run.navigateAskedAt.single() in 9000L..(9000L + FRAME_MS), "asked at ${run.navigateAskedAt}")
        assertNotNull(run.finishedAt)
        assertTrue(
            run.fadeStartedAt!! - run.navigateAskedAt.single() <= SETTLE_MAX_MS + FRAME_MS,
            "the fade started ${run.fadeStartedAt} after the ask at ${run.navigateAskedAt}",
        )
    }

    @Test
    fun `a release before the cover changes nothing, the run is as it always was`() {
        val held = simulate(releaseAtMs = 0L)
        val early = simulate(releaseAtMs = 1000L)

        assertEquals(held.navigateAskedAt, early.navigateAskedAt)
        assertEquals(held.finishedAt, early.finishedAt)
    }

    @Test
    fun `a tap while held skips the animation to full cover but the app still waits for the release`() {
        val run = simulate(tapAtMs = 400L, releaseAtMs = 7000L, limitMs = 12_000L)

        assertEquals(1, run.navigateAskedAt.size)
        assertTrue(run.navigateAskedAt.single() >= 7000L, "asked at ${run.navigateAskedAt} before the release")
    }

    @Test
    fun `the navigation is ready on the release, and at the ceiling whatever the Intro has done`() {
        assertEquals(true, navigationReadyAt(released = true, elapsedMs = 0L, coverMs = FINALE_COVER_MS))
        assertEquals(
            false,
            navigationReadyAt(released = false, elapsedMs = FINALE_COVER_MS + 1000L, coverMs = FINALE_COVER_MS),
        )
        assertEquals(
            false,
            navigationReadyAt(
                released = false,
                elapsedMs = FINALE_COVER_MS + FINALE_RELEASE_CEILING_MS - 1,
                coverMs = FINALE_COVER_MS,
            ),
        )
        assertEquals(
            true,
            navigationReadyAt(
                released = false,
                elapsedMs = FINALE_COVER_MS + FINALE_RELEASE_CEILING_MS,
                coverMs = FINALE_COVER_MS,
            ),
        )
        assertTrue(FINALE_RELEASE_CEILING_MS > 15_000L, "the ceiling must be past the Intro's own 15 s wait")
    }

    // endregion

    // region navigating

    @Test
    fun `the app is asked to navigate once, at the covered moment, with the screen completely covered`() {
        val run = simulate()

        assertEquals(1, run.navigateAskedAt.size)
        val askedAt = run.navigateAskedAt.single()
        assertTrue(askedAt in FINALE_COVER_MS until FINALE_COVER_MS + FRAME_MS, "asked at $askedAt")
        val asking = run.frames.single { it.navigateNow }.frame
        assertEquals(FinalePhase.NAVIGATE, asking.phase)
        assertEquals(1f, asking.floodRadius)
        assertEquals(1f, asking.overlayAlpha)
        assertEquals(0f, asking.wordmarkAlpha)
    }

    @Test
    fun `no frame before the covered moment asks the app to navigate`() {
        simulate().frames.filter { it.frame.phase < FinalePhase.NAVIGATE }.forEach {
            assertFalse(it.navigateNow, "asked early at ${it.frame}")
        }
    }

    @Test
    fun `a long frame that jumps past the covered moment still asks once and still draws the covered frame first`() {
        val first = finaleStep(FinaleRunState(), FinaleTick(elapsedMs = 2400), reduceMotion = false)
        val jumped = finaleStep(first.state, FinaleTick(elapsedMs = 2900), reduceMotion = false)

        assertTrue(jumped.navigateNow)
        assertEquals(FinalePhase.NAVIGATE, jumped.frame.phase)
        val after = finaleStep(jumped.state, FinaleTick(elapsedMs = 2916), reduceMotion = false)
        assertFalse(after.navigateNow, "asked a second time")
    }

    @Test
    fun `the clock waits at the covered frame, and the colour stays solid, until the destination has settled`() {
        val run = simulate(navigateAfterMs = null, limitMs = FINALE_COVER_MS + SETTLE_MAX_MS - FRAME_MS)

        val covered = run.frames.filter { it.frame.phase == FinalePhase.NAVIGATE }
        assertTrue(covered.size > 10, "the clock waited at the covered frame")
        covered.forEach { assertEquals(1f, it.frame.overlayAlpha) }
        assertNull(run.fadeStartedAt)
        assertNull(run.finishedAt)
    }

    // endregion

    // region settling

    @Test
    fun `the colour lifts after two drawn frames and the minimum hold, and then fades over 700 milliseconds`() {
        val run = simulate(navigateAfterMs = 0L)

        val asked = run.navigateAskedAt.single()
        val fadeStart = assertNotNull(run.fadeStartedAt)
        assertTrue(fadeStart - asked >= SETTLE_MIN_HOLD_MS, "held ${fadeStart - asked} ms")
        assertTrue(fadeStart - asked < SETTLE_MIN_HOLD_MS + 2 * FRAME_MS, "held ${fadeStart - asked} ms, too long")
        val faded = assertNotNull(run.finishedAt) - fadeStart
        assertTrue(faded in FINALE_FADE_MS until FINALE_FADE_MS + FRAME_MS, "faded $faded ms")
    }

    private data class SettleRow(
        val describe: String,
        val state: FinaleRunState,
        val nowMs: Long,
        val settled: Boolean,
    )

    // Asked to navigate at 2500, and the Intro said it had navigated at 2510, with [frames] drawn since.
    private fun navigated(frames: Int, skipped: Boolean = false) = FinaleRunState(
        skipped = skipped,
        navigateIssuedAtMs = 2500,
        navigatedAtMs = 2510,
        framesSinceNavigated = frames,
    )

    @Test
    fun `the colour lifts only when the table says so`() {
        val asked = FinaleRunState(navigateIssuedAtMs = 2500)
        listOf(
            SettleRow("the app was never asked", FinaleRunState(), 9000, false),
            SettleRow("asked, not navigated, inside the maximum", asked, 3900, false),
            SettleRow("asked, not navigated, the maximum passed", asked, 4000, true),
            SettleRow(
                "navigated but one frame drawn",
                navigated(frames = 1),
                3000,
                false,
            ),
            SettleRow(
                "two frames but the hold is not over",
                navigated(frames = 2),
                2800,
                false,
            ),
            SettleRow(
                "two frames and the hold is over",
                navigated(frames = 2),
                2810,
                true,
            ),
            SettleRow(
                "two frames and a tap, so no hold",
                navigated(frames = 2, skipped = true),
                2520,
                true,
            ),
            SettleRow(
                "a tap, but no frames drawn yet",
                navigated(frames = 0, skipped = true),
                2520,
                false,
            ),
        ).forEach { row -> assertEquals(row.settled, isSettled(row.state, row.nowMs), row.describe) }
    }

    @Test
    fun `an app that never says it navigated is revealed anyway, 1500 milliseconds after it was asked`() {
        val run = simulate(navigateAfterMs = null)

        val asked = run.navigateAskedAt.single()
        val fadeStart = assertNotNull(run.fadeStartedAt)
        assertTrue(fadeStart - asked in SETTLE_MAX_MS until SETTLE_MAX_MS + FRAME_MS, "waited ${fadeStart - asked} ms")
        assertNotNull(run.finishedAt, "the colour can never hang")
    }

    @Test
    fun `a slow navigation is waited for, up to the maximum`() {
        val quick = simulate(navigateAfterMs = 0L).fadeStartedAt
        val slow = simulate(navigateAfterMs = 800L).fadeStartedAt

        assertTrue(assertNotNull(slow) > assertNotNull(quick) + 700, "quick $quick, slow $slow")
    }

    @Test
    fun `the colour only ever fades once it has lifted, and ends at nothing`() {
        val run = simulate()

        val fade = run.frames.map { it.frame.overlayAlpha }
        assertEquals(fade.sortedDescending(), fade)
        assertEquals(0f, fade.last())
        assertTrue(run.frames.last().finished)
    }

    @Test
    fun `the whole finale with an instant navigation takes about three and a half seconds`() {
        val finishedAt = assertNotNull(simulate().finishedAt)

        assertTrue(finishedAt in 3400L..3700L, "took $finishedAt ms")
    }

    // endregion

    // region a tap

    @Test
    fun `a tap during the entrance covers the screen at once, asks to navigate, and fades out in 300 milliseconds`() {
        val run = simulate(tapAtMs = 800)

        val asked = run.navigateAskedAt.single()
        assertTrue(asked in 800L until 800L + 2 * FRAME_MS, "asked at $asked")
        val tapped = run.frames.first { it.navigateNow }.frame
        assertEquals(1f, tapped.overlayAlpha)
        assertEquals(1f, tapped.floodRadius)
        val fadeStart = assertNotNull(run.fadeStartedAt)
        val finishedAt = assertNotNull(run.finishedAt)
        val faded = finishedAt - fadeStart
        assertTrue(faded in FINALE_SKIP_FADE_MS until FINALE_SKIP_FADE_MS + FRAME_MS, "faded $faded ms")
        assertTrue(finishedAt < 1500L, "done at $finishedAt ms")
    }

    @Test
    fun `a tap still waits for the destination to be drawn for two frames before the colour lifts`() {
        val run = simulate(tapAtMs = 100, navigateAfterMs = 100L)

        val asked = run.navigateAskedAt.single()
        val fadeStart = assertNotNull(run.fadeStartedAt)
        assertTrue(fadeStart >= asked + 100L + 2 * FRAME_MS, "fade at $fadeStart, asked $asked")
    }

    @Test
    fun `a tap after the colour has lifted changes nothing about the fade`() {
        val steady = simulate(navigateAfterMs = 0L)
        val tapped = simulate(navigateAfterMs = 0L, tapAtMs = assertNotNull(steady.fadeStartedAt) + 100L)

        assertEquals(steady.fadeStartedAt, tapped.fadeStartedAt)
        assertEquals(steady.finishedAt, tapped.finishedAt)
    }

    @Test
    fun `a tap never makes the app navigate twice`() {
        val run = simulate(tapAtMs = 3000)

        assertEquals(1, run.navigateAskedAt.size)
        assertEquals(1, run.frames.count { it.navigateNow })
    }

    // endregion

    // region reduced motion

    @Test
    fun `reduced motion asks to navigate on the first frame, with no wordmark, and cross fades in 200 milliseconds`() {
        val run = simulate(reduceMotion = true)

        assertEquals(listOf(0L), run.navigateAskedAt)
        run.frames.forEach {
            assertEquals(0f, it.frame.wordmarkAlpha)
            assertEquals(0f, it.frame.glow)
        }
        val fadeStart = assertNotNull(run.fadeStartedAt)
        val faded = assertNotNull(run.finishedAt) - fadeStart
        assertTrue(fadeStart >= SETTLE_MIN_HOLD_MS, "held $fadeStart ms")
        assertTrue(faded in FINALE_REDUCED_FADE_MS until FINALE_REDUCED_FADE_MS + FRAME_MS, "faded $faded ms")
    }

    @Test
    fun `reduced motion is revealed even if the app never says it navigated`() {
        val run = simulate(reduceMotion = true, navigateAfterMs = null)

        assertNotNull(run.finishedAt)
        val waited = assertNotNull(run.fadeStartedAt) - run.navigateAskedAt.single()
        assertTrue(waited in SETTLE_MAX_MS until SETTLE_MAX_MS + FRAME_MS, "waited $waited ms")
    }

    // endregion

    // region the clock

    @Test
    fun `the clock is the real time until the covered moment, then waits there, then runs on from the lift`() {
        val waiting = FinaleRunState()
        val fading = FinaleRunState(fadeStartedAtMs = 3000L)

        assertEquals(800L, clockMs(waiting, elapsedMs = 800, skipped = false, coverMs = FINALE_COVER_MS))
        assertEquals(FINALE_COVER_MS, clockMs(waiting, elapsedMs = 2500, skipped = false, coverMs = FINALE_COVER_MS))
        assertEquals(FINALE_COVER_MS, clockMs(waiting, elapsedMs = 2900, skipped = false, coverMs = FINALE_COVER_MS))
        assertEquals(FINALE_COVER_MS, clockMs(waiting, elapsedMs = 800, skipped = true, coverMs = FINALE_COVER_MS))
        assertEquals(FINALE_COVER_MS, clockMs(fading, elapsedMs = 3000, skipped = false, coverMs = FINALE_COVER_MS))
        val running = clockMs(fading, elapsedMs = 3350, skipped = false, coverMs = FINALE_COVER_MS)
        assertEquals(FINALE_COVER_MS + 350, running)
    }

    // endregion

    private companion object {
        const val FRAME_MS = 16L
        const val LIMIT_MS = 8000L
    }
}
