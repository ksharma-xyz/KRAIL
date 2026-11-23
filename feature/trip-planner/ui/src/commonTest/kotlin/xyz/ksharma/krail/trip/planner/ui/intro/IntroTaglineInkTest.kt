package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.asThemeInk
import xyz.ksharma.krail.taj.theme.contrastRatio
import xyz.ksharma.krail.taj.theme.ensureContrastWith
import xyz.ksharma.krail.taj.theme.md_theme_dark_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_dark_surface
import xyz.ksharma.krail.taj.theme.md_theme_light_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_light_surface
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The tagline that replaces the question once a badge is picked, against the ground it is really drawn on, the page tinted with the
 * theme colour ([introGroundColour]), for the four themes the Intro offers and for Purple Drip too, in light and in dark mode.
 *
 * - Dark mode is the page's near white ink as it is, at least 6 to 1 on every ground.
 * - Light mode is the theme colour made deeper, keeping its hue, at least 7 to 1.
 *
 * It was taj's ink, worked out against the app's plain surfaces, which in dark mode are darker than the tint, so it measured 2.75 to
 * 2.98 to 1 there. Then it was worked out against the tint at 4.5 to 1, which read as pale. The numbers are printed.
 */
class IntroTaglineInkTest {

    private val offered = introThemeChoices().map { it.style }
    private val themes = offered + KrailThemeStyle.PurpleDrip

    private fun surface(dark: Boolean) = if (dark) md_theme_dark_surface else md_theme_light_surface

    private fun ink(dark: Boolean) = if (dark) md_theme_dark_onSurface else md_theme_light_onSurface

    private fun ground(theme: KrailThemeStyle, dark: Boolean): Color =
        introGroundColour(theme.hexColorCode.hexToComposeColor(), surface(dark), dark)

    private fun after(theme: KrailThemeStyle, dark: Boolean): Color = introTaglineInk(
        theme = theme.hexColorCode.hexToComposeColor(),
        ground = ground(theme, dark),
        onSurface = ink(dark),
        dark = dark,
    )

    // What it was at 4.5 to 1, the last change: the same ladder on the same ground at the old minimum.
    private fun at45(theme: KrailThemeStyle, dark: Boolean): Color = theme.hexColorCode.hexToComposeColor()
        .ensureContrastWith(background = ground(theme, dark), toward = ink(dark), minContrast = 4.5f)

    // What it was before that: taj's ink for the theme, drawn on the tint.
    private fun before(theme: KrailThemeStyle, dark: Boolean): Color =
        theme.hexColorCode.hexToComposeColor().asThemeInk(dark)

    @Test
    fun `in dark mode the tagline is the near white ink and has at least 6 to 1 on every theme's real tinted ground`() {
        themes.forEach { theme ->
            val contrast = after(theme, dark = true).contrastRatio(ground(theme, dark = true))
            println("tagline, dark, ${theme.name}: ${after(theme, true)} $contrast")

            assertEquals(md_theme_dark_onSurface, after(theme, dark = true), "${theme.name} is the page's ink")
            assertTrue(contrast >= MIN_DARK, "${theme.name}: $contrast is under 6")
        }
    }

    @Test
    fun `in light mode the tagline keeps the theme's hue and has at least 7 to 1 on every theme's real tinted ground`() {
        themes.forEach { theme ->
            val ink = after(theme, dark = false)
            val contrast = ink.contrastRatio(ground(theme, dark = false))
            println("tagline, light, ${theme.name}: $ink $contrast")

            assertTrue(contrast >= TAGLINE_MIN_CONTRAST_LIGHT, "${theme.name}: $contrast is under 7")
            assertNotEquals(
                md_theme_light_onSurface,
                ink,
                "${theme.name} went all the way to the plain ink, so the hue is lost",
            )
        }
    }

    @Test
    fun `the numbers are the ones of the approved mock for the four offered themes`() {
        // v13 index.md, the colour screen tagline: dark is the plain ink, light is the ink at 7 to 1.
        val dark = mapOf(
            KrailThemeStyle.Train to 6.61f,
            KrailThemeStyle.Metro to 8.15f,
            KrailThemeStyle.Bus to 6.59f,
            KrailThemeStyle.Ferry to 7.01f,
        )
        val light = mapOf(
            KrailThemeStyle.Train to 7.58f,
            KrailThemeStyle.Metro to 7.16f,
            KrailThemeStyle.Bus to 7.37f,
            KrailThemeStyle.Ferry to 7.33f,
        )

        dark.forEach { (theme, mock) ->
            val actual = after(theme, true).contrastRatio(ground(theme, true))
            assertTrue(abs(actual - mock) < MOCK_TOLERANCE, "${theme.name} dark: $actual against the mock's $mock")
        }
        light.forEach { (theme, mock) ->
            val actual = after(theme, false).contrastRatio(ground(theme, false))
            assertTrue(abs(actual - mock) < MOCK_TOLERANCE, "${theme.name} light: $actual against the mock's $mock")
        }
    }

    @Test
    fun `the new ink is never lower than the 4_5 one or the original one on the real ground`() {
        listOf(false, true).forEach { dark ->
            themes.forEach { theme ->
                val now = after(theme, dark).contrastRatio(ground(theme, dark))

                assertTrue(
                    now >= at45(theme, dark).contrastRatio(ground(theme, dark)) - 0.001f,
                    "${theme.name}, dark $dark, against 4.5",
                )
                assertTrue(
                    now >= before(theme, dark).contrastRatio(ground(theme, dark)) - 0.001f,
                    "${theme.name}, dark $dark, against taj's",
                )
            }
        }
    }

    @Test
    fun `before the original fix the dark tagline was under 3 to 1 on its ground, which is what the first fix was for`() {
        val measured = themes.map { it.name to before(it, dark = true).contrastRatio(ground(it, dark = true)) }

        assertTrue(measured.any { it.second < 3.0f }, "nothing was under 3 to 1 before: $measured")
        assertTrue(measured.all { it.second > 2.0f }, "$measured")
    }

    @Test
    fun `in light mode a colour that already reads is used as it is, and one that does not is moved only as far as it needs`() {
        val white = Color.White
        val black = Color.Black

        assertEquals(black, introTaglineInk(black, ground = white, onSurface = black, dark = false))
        val moved = introTaglineInk(white, ground = white, onSurface = black, dark = false)
        assertTrue(moved.contrastRatio(white) >= TAGLINE_MIN_CONTRAST_LIGHT, "${moved.contrastRatio(white)}")
        assertTrue(moved != black, "it went all the way to the surface text, further than it needed")
    }

    private companion object {
        const val MIN_DARK = 6.0f
        const val MOCK_TOLERANCE = 0.05f
    }
}
