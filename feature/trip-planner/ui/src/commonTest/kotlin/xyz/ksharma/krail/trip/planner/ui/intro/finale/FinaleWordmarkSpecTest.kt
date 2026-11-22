package xyz.ksharma.krail.trip.planner.ui.intro.finale

import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.tokens.AiGradientTokens
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The wordmark's numbers are pure, so the lines, the label, the fit at a narrow width and a large text size, the
 * gradient and its blend into the theme colour, the glow's steps, and the circle that floods the screen are all
 * held here without drawing anything.
 */
class FinaleWordmarkSpecTest {

    // region the words

    @Test
    fun `the wordmark is the hash, let's and krail on two lines, eleven letters in all`() {
        assertEquals(listOf("#LET'S", "KRAIL"), WORDMARK_LINES)
        assertEquals(11, WORDMARK_LETTER_COUNT)
        assertEquals(0, firstLetterOfLine(0))
        assertEquals(6, firstLetterOfLine(1))
        assertEquals(11, firstLetterOfLine(2))
    }

    @Test
    fun `a screen reader hears one plain phrase, with the apostrophe and no hash`() {
        assertEquals("Let's KRAIL", FINALE_ACCESSIBILITY_LABEL)
        assertTrue('#' !in FINALE_ACCESSIBILITY_LABEL)
    }

    @Test
    fun `no emoji is in the wordmark or its label`() {
        (WORDMARK_LINES + FINALE_ACCESSIBILITY_LABEL).forEach { text ->
            assertTrue(text.all { it.code < 0x2000 }, "$text has a character above the plain range")
        }
    }

    // endregion

    // region fitting

    private data class FitRow(val describe: String, val measured: Float, val available: Float, val scale: Float)

    @Test
    fun `a line is scaled down to the width and never up, and never squeezed`() {
        listOf(
            FitRow("fits with room", measured = 200f, available = 300f, scale = 1f),
            FitRow("fits exactly", measured = 300f, available = 300f, scale = 1f),
            FitRow("a little too wide", measured = 400f, available = 300f, scale = 0.75f),
            FitRow("twice too wide, as at 200 percent text", measured = 600f, available = 300f, scale = 0.5f),
            FitRow("not measured yet", measured = 0f, available = 300f, scale = 1f),
            FitRow("no room at all", measured = 300f, available = 0f, scale = 0.1f),
            FitRow("far too wide, held to the least", measured = 100_000f, available = 300f, scale = 0.1f),
        ).forEach { row ->
            assertNear(row.scale, fitScale(row.measured, row.available), row.describe)
        }
    }

    @Test
    fun `the whole mark takes the one scale that the widest line needs`() {
        assertNear(0.5f, wordmarkFitScale(listOf(600f, 400f), availableWidthPx = 300f), "the wide line decides")
        assertNear(0.75f, wordmarkFitScale(listOf(200f, 400f), availableWidthPx = 300f), "the second line decides")
        assertNear(1f, wordmarkFitScale(listOf(200f, 250f), availableWidthPx = 300f), "both fit")
        assertNear(1f, wordmarkFitScale(emptyList(), availableWidthPx = 300f), "nothing measured")
    }

    @Test
    fun `a line that was scaled to fit is never wider than the room`() {
        listOf(100f, 250f, 320f, 640f, 1280f).forEach { measured ->
            val scaled = measured * fitScale(measured, availableWidthPx = 288f)
            assertTrue(scaled <= 288f + 0.01f, "$measured scaled to $scaled")
        }
    }

    // endregion

    // region the gradient

    @Test
    fun `the gradient is the app's own four stops, and at blend 0 it is exactly them`() {
        val colours = finaleGradientColors(blend = 0f, themeColour = THEME)

        assertEquals(AiGradientTokens.stops, colours)
        assertEquals(4, colours.size)
    }

    @Test
    fun `at blend 1 every stop is the theme colour, so the wordmark is one solid colour`() {
        assertEquals(List(4) { THEME }, finaleGradientColors(blend = 1f, themeColour = THEME))
    }

    @Test
    fun `halfway, each stop is halfway between its brand colour and the theme colour`() {
        val halfway = finaleGradientColors(blend = 0.5f, themeColour = THEME)

        AiGradientTokens.stops.zip(halfway).forEach { (brand, blended) ->
            assertNear((brand.red + THEME.red) / 2, blended.red, "red of $brand")
            assertNear((brand.green + THEME.green) / 2, blended.green, "green of $brand")
            assertNear((brand.blue + THEME.blue) / 2, blended.blue, "blue of $brand")
        }
    }

    @Test
    fun `a blend outside 0 to 1 is held to the ends`() {
        assertEquals(finaleGradientColors(0f, THEME), finaleGradientColors(-2f, THEME))
        assertEquals(finaleGradientColors(1f, THEME), finaleGradientColors(3f, THEME))
    }

    @Test
    fun `the gradient starts at the left edge and slides left over the wordmark`() {
        val (start0, end0) = gradientRangePx(widthPx = 500f, travel = 0f)
        val (start1, end1) = gradientRangePx(widthPx = 500f, travel = 1f)

        assertNear(0f, start0, "start at 0")
        assertNear(800f, end0, "wider than the word at 0")
        assertNear(-300f, start1, "slid to the left")
        assertNear(500f, end1, "and ends at the right edge")
    }

    // endregion

    // region the glow

    private data class GlowRow(val glow: Float, val step: Int)

    @Test
    fun `the glow is quantised to twelve steps so the text style changes only a few times`() {
        listOf(
            GlowRow(-1f, 0),
            GlowRow(0f, 0),
            GlowRow(0.04f, 0),
            GlowRow(0.05f, 1),
            GlowRow(0.5f, 6),
            GlowRow(0.6f, 7),
            GlowRow(1f, 12),
            GlowRow(2f, 12),
        ).forEach { row -> assertEquals(row.step, glowStep(row.glow), "glow ${row.glow}") }
        assertEquals(12, GLOW_STEPS)
    }

    @Test
    fun `the glow's blur and opacity grow with the step, are nothing at 0 and are held at the ends`() {
        assertEquals(0f, glowBlurPx(0, letterSizePx = 200f))
        assertEquals(0f, glowAlpha(0))
        assertNear(90f, glowBlurPx(GLOW_STEPS, letterSizePx = 200f), "full blur is 45 percent of the letter")
        assertNear(0.9f, glowAlpha(GLOW_STEPS), "full opacity")
        assertEquals(glowBlurPx(GLOW_STEPS, 200f), glowBlurPx(99, 200f))
        assertEquals(0f, glowAlpha(-4))
        val blurs = (0..GLOW_STEPS).map { glowBlurPx(it, 200f) }
        assertEquals(blurs.sorted(), blurs)
    }

    // endregion

    // region the flood

    @Test
    fun `the flood grows from nothing to the farthest corner from where it starts`() {
        assertEquals(0f, floodRadiusPx(0f, width = 400f, height = 800f, centerX = 200f, centerY = 400f))
        val full = floodRadiusPx(1f, width = 400f, height = 800f, centerX = 200f, centerY = 400f)
        assertNear(447.2f, full, "half the diagonal of 400 by 800, from the centre")
        assertNear(full / 2, floodRadiusPx(0.5f, 400f, 800f, 200f, 400f), "half grown is half the radius")
    }

    @Test
    fun `from a centre that is not the middle, the full radius still reaches the farthest corner`() {
        val full = floodRadiusPx(1f, width = 400f, height = 800f, centerX = 100f, centerY = 300f)

        assertNear(583.1f, full, "from 100,300 the farthest corner is 300,500 away")
        assertTrue(full >= corner(100f, 300f, 400f, 800f), "covers the far corner")
        assertTrue(full >= corner(100f, 300f, 0f, 0f), "covers the near corner")
    }

    @Test
    fun `the flood is held between nothing and everything`() {
        assertEquals(0f, floodRadiusPx(-1f, 400f, 800f, 200f, 400f))
        assertEquals(floodRadiusPx(1f, 400f, 800f, 200f, 400f), floodRadiusPx(2f, 400f, 800f, 200f, 400f))
    }

    // endregion

    private fun corner(fromX: Float, fromY: Float, toX: Float, toY: Float): Float {
        val dx = toX - fromX
        val dy = toY - fromY
        return sqrt(dx * dx + dy * dy)
    }

    private fun assertNear(expected: Float, actual: Float, where: String) {
        val allowed = TOLERANCE * maxOf(1f, abs(expected))
        assertTrue(abs(expected - actual) <= allowed, "$where: expected $expected, was $actual")
    }

    private companion object {
        val THEME = Color(red = 0f, green = 0.4f, blue = 0.8f)
        const val TOLERANCE = 0.005f
    }
}
