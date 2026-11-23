package xyz.ksharma.krail.trip.planner.ui.intro

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The theme change sequence as a table: every element's progress at a time, on the one clock of about 700 ms. Nothing here draws
 * and nothing waits.
 */
class IntroThemeChangeTimelineTest {

    private val tolerance = 0.0005f

    private fun assertNear(expected: Float, actual: Float, what: String) =
        assertEquals(expected, actual, tolerance, what)

    private data class Row(val timeMs: Float, val expected: Float)

    private fun table(name: String, rows: List<Row>, actual: (Float) -> Float) =
        rows.forEach { assertNear(it.expected, actual(it.timeMs), "$name at ${it.timeMs} ms") }

    private fun assertMonotonic(name: String, increasing: Boolean, samples: List<Float>) =
        samples.zipWithNext().forEach { (a, b) ->
            assertTrue(if (increasing) b >= a - 1e-6f else b <= a + 1e-6f, "$name goes the wrong way: $a then $b")
        }

    private fun clock(to: Float): List<Float> = (0..to.toInt()).map { it.toFloat() }

    @Test
    fun `the whole sequence is about 700 ms on one clock, and a plain 200 ms for reduced motion`() {
        assertEquals(700f, THEME_CHANGE_MS)
        assertEquals(200f, THEME_CHANGE_REDUCED_MS)
        assertEquals(700f, themeChangeDurationMs(reduceMotion = false))
        assertEquals(200f, themeChangeDurationMs(reduceMotion = true))
        assertFalse(themeChangeFrameAt(699f, reduceMotion = false).done)
        assertTrue(themeChangeFrameAt(700f, reduceMotion = false).done)
        assertFalse(themeChangeFrameAt(199f, reduceMotion = true).done)
        assertTrue(themeChangeFrameAt(200f, reduceMotion = true).done)
    }

    @Test
    fun `the badge presses softly, dips by a twelfth at 90 ms, comes back with no overshoot and is exactly 1 again`() {
        table(
            "press",
            listOf(Row(-20f, 1f), Row(0f, 1f), Row(45f, 0.93f), Row(90f, 0.92f), Row(540f, 1f), Row(700f, 1f)),
            ::themePress,
        )
        val samples = clock(700f).map(::themePress)
        assertTrue(samples.all { it <= 1f }, "the press goes past rest, so it overshoots")
        assertTrue(samples.min() >= 0.92f - tolerance, "the press dips more than ${THEME_PRESS_DEPTH}")
        assertMonotonic("the dip", increasing = false, samples = (0..90).map { themePress(it.toFloat()) })
        assertMonotonic("the return", increasing = true, samples = (90..700).map { themePress(it.toFloat()) })
    }

    @Test
    fun `the wash grows from 0 to covering the window over 520 ms and starts and ends slowly`() {
        table("wash", listOf(Row(0f, 0f), Row(260f, 0.5f), Row(520f, 1f), Row(700f, 1f)), ::themeWash)
        assertMonotonic("the wash", increasing = true, samples = clock(700f).map(::themeWash))
        assertTrue(themeWash(10f) < 0.01f, "it starts with a jump")
        assertTrue(themeWash(510f) > 0.99f, "it ends with a jump")
    }

    @Test
    fun `the colours move on one blend, 0 to 1 over 600 ms with motion and over the whole 200 ms with reduced motion`() {
        table("colour", listOf(Row(0f, 0f), Row(300f, 0.5f), Row(600f, 1f), Row(700f, 1f)), { themeColour(it, false) })
        table(
            "reduced colour",
            listOf(Row(0f, 0f), Row(100f, 0.5f), Row(200f, 1f), Row(500f, 1f)),
            { themeColour(it, true) },
        )
        assertMonotonic("the colour", increasing = true, samples = clock(700f).map { themeColour(it, false) })
    }

    @Test
    fun `the old headline eases up and fades out over the first third, 233 ms`() {
        table(
            "outgoing",
            listOf(Row(0f, 0f), Row(116.5f, 0.875f), Row(233f, 1f), Row(700f, 1f)),
            { themeOutgoing(it, false) },
        )
        assertNear(1f, themeOutgoingAlpha(1f, 0f), "opaque at the start")
        assertNear(0f, themeOutgoingAlpha(1f, 1f), "gone at the end")
        assertNear(0.25f, themeOutgoingAlpha(0.5f, 0.5f), "a line that was half shown leaves from there")
        assertNear(0f, themeOutgoingRiseDp(0f, 0f, false), "in place at the start")
        assertNear(-8f, themeOutgoingRiseDp(0f, 1f, false), "8 dp up at the end")
        assertNear(-4f, themeOutgoingRiseDp(0f, 0.5f, false), "half way")
        assertNear(-8f, themeOutgoingRiseDp(-3f, 1f, false), "a line that was already up carries on to the same place")
    }

    @Test
    fun `the new tagline comes in one line after another, 60 ms apart, each rising 8 dp into its place, after the old is gone`() {
        val lineStarts = listOf(233f, 293f, 353f)
        lineStarts.forEachIndexed { index, start ->
            assertNear(0f, themeIncomingLine(start, index, false), "line $index at its start")
            assertNear(1f, themeIncomingLine(start + 280f, index, false), "line $index 280 ms later")
            assertNear(0f, themeIncomingLine(start - 50f, index, false), "line $index before its start")
            assertMonotonic(
                "line $index",
                increasing = true,
                samples = clock(700f).map { themeIncomingLine(it, index, false) },
            )
        }
        assertNear(60f, lineStarts[1] - lineStarts[0], "the stagger")
        // No line shows anything while the old headline is still leaving, so the two are never on screen together.
        (0..232).forEach { assertNear(0f, themeIncomingLine(it.toFloat(), 0, false), "line 0 at $it ms") }
        assertNear(8f, themeIncomingRiseDp(0f, false), "8 dp below its place at the start")
        assertNear(0f, themeIncomingRiseDp(1f, false), "in place at the end")
        assertNear(4f, themeIncomingRiseDp(0.5f, false), "half way")
        assertTrue(themeIncomingLine(700f, 3, false) == 1f, "a fourth line is in too by the end")
    }

    @Test
    fun `the emoji shrinks to 0 6 in 120 ms, is the new one from there and springs back to exactly 1`() {
        table(
            "emoji scale",
            listOf(Row(0f, 1f), Row(60f, 0.8f), Row(120f, 0.6f), Row(620f, 1f), Row(700f, 1f)),
            { themeEmojiScale(it, 1f, false) },
        )
        assertFalse(themeEmojiIsNew(119f, false))
        assertTrue(themeEmojiIsNew(120f, false))
        val spring = (120..700).map { themeEmojiScale(it.toFloat(), 1f, false) }
        assertTrue(spring.max() > 1f, "a short spring pops past 1")
        assertTrue(spring.max() < 1.1f, "the pop is small: ${spring.max()}")
        assertTrue(spring.all { it >= 0.6f - tolerance }, "it never goes below the low point")
        assertMonotonic(
            "the shrink",
            increasing = false,
            samples = (0..120).map { themeEmojiScale(it.toFloat(), 1f, false) },
        )
    }

    @Test
    fun `an emoji that was cut short while small shrinks from there and never grows on the way down`() {
        assertNear(0.5f, themeEmojiScale(0f, 0.5f, false), "starts where it was")
        (0..120).forEach { assertNear(0.5f, themeEmojiScale(it.toFloat(), 0.5f, false), "held at $it ms") }
        assertNear(1f, themeEmojiScale(700f, 0.5f, false), "and still ends at 1")
        val frame = themeChangeFrameAt(0f, reduceMotion = false, emojiScaleAtStart = 0.8f)
        assertNear(0.8f, frame.emojiScale, "the frame starts at the scale it was given")
    }

    @Test
    fun `the button shows last, from 450 ms on the first pick, and it is already there on a later one`() {
        assertEquals(450f, themeButtonAtMs(reduceMotion = false))
        assertEquals(0f, themeButtonAtMs(reduceMotion = true))
        assertFalse(themeButtonShown(449f, themeButtonAtMs(false)))
        assertTrue(themeButtonShown(450f, themeButtonAtMs(false)))
        assertTrue(themeButtonShown(0f, 0f))
        // The 450 ms and the 250 ms the bar takes to appear end together, with the sequence.
        assertEquals(THEME_CHANGE_MS, THEME_BUTTON_AT_MS + BAR_REVEAL_MS)
    }

    @Test
    fun `every element starts at its first value and ends exactly settled, so a frame at 0 and a frame at the end are the same state`() {
        val start = themeChangeFrameAt(0f, reduceMotion = false)
        val end = themeChangeFrameAt(THEME_CHANGE_MS, reduceMotion = false)

        assertEquals(ThemeChangeFrame(0f, false, 1f, 0f, 0f, 0f, 1f, false, false), start)
        assertEquals(ThemeChangeFrame(700f, false, 1f, 1f, 1f, 1f, 1f, true, true), end)
        assertEquals(0f, start.incomingLine(0))
        assertEquals(1f, end.incomingLine(0))
        assertEquals(1f, end.incomingLine(1))
    }

    @Test
    fun `the frame at a time before the tap is the tap`() {
        assertEquals(themeChangeFrameAt(0f, false).copy(timeMs = 0f), themeChangeFrameAt(-100f, false))
    }

    @Test
    fun `reduced motion is a plain 200 ms cross-fade, with no press, no wash, no rise and no pop`() {
        val rows = listOf(0f, 50f, 100f, 150f, 200f, 400f)
        rows.forEach { time ->
            val frame = themeChangeFrameAt(time, reduceMotion = true)

            assertEquals(1f, frame.press, "press at $time")
            assertEquals(1f, frame.wash, "there is no growing circle at $time")
            assertEquals(1f, frame.emojiScale, "no pop at $time")
            assertNear(
                themeColour(time, true),
                frame.outgoing,
                "the old headline fades on the same line as the colours at $time",
            )
            assertNear(frame.colour, frame.incomingLine(0), "the new headline comes in on the same line at $time")
            assertNear(frame.colour, frame.incomingLine(3), "with no stagger at $time")
            assertEquals(0f, themeIncomingRiseDp(frame.incomingLine(0), reduceMotion = true), "no rise at $time")
            assertEquals(0f, themeOutgoingRiseDp(0f, frame.outgoing, reduceMotion = true), "no rise leaving at $time")
        }
        assertNear(0.5f, themeChangeFrameAt(100f, true).colour, "half way")
        assertFalse(themeEmojiIsNew(99f, true))
        assertTrue(themeEmojiIsNew(100f, true))
    }

    @Test
    fun `the looks of a headline are what a second tap carries on from`() {
        val settled = LineLooks.settled(2)
        assertEquals(LineLooks(listOf(1f, 1f), listOf(0f, 0f)), settled)
        assertEquals(2f, settled.visibility)

        val half = outgoingLooks(settled, 116.5f, reduceMotion = false)
        assertNear(0.125f, half.alphas[0], "alpha half way through leaving")
        assertNear(-7f, half.risesDp[0], "rise half way through leaving")

        val arriving = incomingLooks(2, 293f, reduceMotion = false)
        assertNear(0.0f, arriving.alphas[1], "line 1 has not started at 293 ms")
        assertTrue(arriving.alphas[0] > 0f, "line 0 has")
        assertEquals(LineLooks.settled(2), incomingLooks(2, 700f, reduceMotion = false))
        assertTrue(incomingLooks(2, 0f, false).visibility < 1e-6f, "nothing of the new headline at the start")
    }
}
