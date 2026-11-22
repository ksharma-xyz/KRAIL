package xyz.ksharma.krail.trip.planner.ui.intro.finale

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * How the wordmark is drawn for each fill, as values: the default has no animated glow, one static shadow and one layer for
 * the word's opacity, and the other two are the wordmark as it was.
 */
class FinaleWordmarkLookTest {

    private val website = finaleWordmarkLook(FinaleWordmarkFill.WebsiteModes)

    @Test
    fun `the default fill is the website's and its look is the website's`() {
        assertEquals(FinaleWordmarkFill.WebsiteModes, FINALE_WORDMARK_FILL)
        assertEquals(website, finaleWordmarkLook())
    }

    @Test
    fun `the default path has no glow, no slide, no opacity on a letter and one layer for the word's opacity`() {
        assertEquals(false, website.glowAnimated, "a glow that changes while the finale runs")
        assertEquals(false, website.gradientSlides, "a gradient that moves")
        assertEquals(false, website.letterAlpha, "a letter with an opacity of its own")
        assertEquals(1, website.wordAlphaLayers)
    }

    @Test
    fun `the default path has one static shadow, black at 16 percent, 14 px of blur and 4 px down at the design size`() {
        val shadow = assertNotNull(website.staticShadow)

        assertEquals(0.16f, shadow.alpha)
        assertEquals(14f, shadow.blurPx)
        assertEquals(4f, shadow.offsetYPx)
        assertEquals(WORDMARK_SHADOW_DESIGN_FONT_PX, shadow.designFontPx)
    }

    @Test
    fun `the shadow scales with the letter, is nothing but numbers and never changes with the frame`() {
        val shadow = assertNotNull(website.staticShadow)

        val half = shadow.scaledTo(shadow.designFontPx / 2f)
        val same = shadow.scaledTo(shadow.designFontPx)
        val double = shadow.scaledTo(shadow.designFontPx * 2f)

        assertEquals(7f, half.blurPx)
        assertEquals(2f, half.offsetYPx)
        assertEquals(shadow.copy(), same)
        assertEquals(28f, double.blurPx)
        assertEquals(8f, double.offsetYPx)
        assertEquals(0.16f, double.alpha, "the opacity does not scale")
        assertEquals(shadow.blurPx + shadow.offsetYPx, shadow.roomPx())
        // The look is a plain value: asking again gives the same one, so there is nothing that changes between two frames.
        assertEquals(finaleWordmarkLook(), finaleWordmarkLook())
    }

    @Test
    fun `the other two fills are the wordmark as it was, with the glow, the slide and an opacity on each letter, and no static shadow`() {
        listOf(FinaleWordmarkFill.Original, FinaleWordmarkFill.ThemePartner).forEach { fill ->
            val look = finaleWordmarkLook(fill)

            assertTrue(look.glowAnimated, "$fill")
            assertTrue(look.gradientSlides, "$fill")
            assertTrue(look.letterAlpha, "$fill")
            assertNull(look.staticShadow, "$fill")
            assertEquals(1, look.wordAlphaLayers, "$fill")
        }
    }

    @Test
    fun `the gradient is still across the whole word for the website fill, and slides only for the others`() {
        listOf(0f, 0.5f, 1f).forEach { travel ->
            assertEquals(0f to 500f, wordmarkGradientRangePx(website, widthPx = 500f, travel = travel), "travel $travel")
        }
        val old = finaleWordmarkLook(FinaleWordmarkFill.ThemePartner)

        assertEquals(gradientRangePx(500f, 0.5f), wordmarkGradientRangePx(old, widthPx = 500f, travel = 0.5f))
        val (start, end) = wordmarkGradientRangePx(old, widthPx = 500f, travel = 0f)
        assertEquals(0f, start, 0.0001f)
        assertEquals(800f, end)
    }

    private data class FadeRow(val ms: Long, val alpha: Float)

    @Test
    fun `the whole word fades in over 250 ms as one layer, from nothing to fully there and held`() {
        listOf(
            FadeRow(-50, 0f),
            FadeRow(0, 0f),
            FadeRow(125, 0.5f),
            FadeRow(250, 1f),
            FadeRow(2000, 1f),
        ).forEach { row -> assertEquals(row.alpha, wordmarkFadeInAt(row.ms), 0.0001f, "at ${row.ms} ms") }
        val samples = (0L..WORDMARK_FADE_IN_MS).map { wordmarkFadeInAt(it) }
        assertTrue(samples.zipWithNext().all { (a, b) -> b >= a }, "it never goes back")
        assertEquals(250L, WORDMARK_FADE_IN_MS)
    }
}
