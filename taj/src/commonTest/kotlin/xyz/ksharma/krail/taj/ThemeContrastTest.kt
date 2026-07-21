package xyz.ksharma.krail.taj

import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.contrastRatio
import xyz.ksharma.krail.taj.theme.getForegroundColor
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Every theme's glyph-on-theme-colour pairing must clear WCAG AA.
 *
 * Components that fill a shape with the user's theme colour and draw on top of it (the
 * Park & Ride add toggle, the `P` icon, theme chips) derive their foreground from
 * `getForegroundColor`. That contract only holds if it actually produces a legible colour
 * for every theme we ship, including any added later — hence iterating the enum rather
 * than asserting a fixed list.
 */
class ThemeContrastTest {

    @Test
    fun `every theme colour yields a legible foreground`() {
        KrailThemeStyle.entries.forEach { style ->
            val background = style.hexColorCode.hexToComposeColor()
            val foreground = getForegroundColor(backgroundColor = background)
            val ratio = foreground.contrastRatio(background)

            assertTrue(
                ratio >= WCAG_AA_NORMAL_TEXT,
                "${style.name}: contrast $ratio is below WCAG AA ($WCAG_AA_NORMAL_TEXT)",
            )
        }
    }

    @Test
    fun `a low-contrast preferred foreground is replaced, not passed through`() {
        // The toggle passes LocalThemeContentColor as the preferred glyph colour. When that
        // local is unset (previews default it to opaque black) it can collide with a dark
        // theme colour, so the helper has to override it rather than honour the preference.
        KrailThemeStyle.entries.forEach { style ->
            val background = style.hexColorCode.hexToComposeColor()
            val preferred = unspecifiedColor.hexToComposeColor()
            val resolved = getForegroundColor(
                backgroundColor = background,
                foregroundColor = preferred,
            )

            assertTrue(
                resolved.contrastRatio(background) >= WCAG_AA_NORMAL_TEXT,
                "${style.name}: resolved glyph colour is below WCAG AA",
            )
        }
    }

    private companion object {
        const val WCAG_AA_NORMAL_TEXT = 4.5f
    }
}
