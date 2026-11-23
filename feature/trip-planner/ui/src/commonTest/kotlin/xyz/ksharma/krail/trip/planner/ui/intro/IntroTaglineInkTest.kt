package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.asThemeInk
import xyz.ksharma.krail.taj.theme.contrastRatio
import xyz.ksharma.krail.taj.theme.md_theme_dark_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_dark_surface
import xyz.ksharma.krail.taj.theme.md_theme_light_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_light_surface
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The tagline that replaces the question once a badge is picked, against the ground it is really drawn on, the page tinted with the
 * theme colour ([introGroundColour]), for the four themes the Intro offers and for Purple Drip too, in light and in dark mode.
 * It was taj's ink, worked out against the app's plain surfaces, which in dark mode are darker than the tint, so it measured 2.75 to
 * 2.98 to 1 there. It is now worked out against the tint itself.
 */
class IntroTaglineInkTest {

    private val themes = introThemeChoices().map { it.style } + KrailThemeStyle.PurpleDrip

    private fun ground(theme: KrailThemeStyle, dark: Boolean): Color =
        introGroundColour(theme.hexColorCode.hexToComposeColor(), if (dark) md_theme_dark_surface else md_theme_light_surface, dark)

    private fun after(theme: KrailThemeStyle, dark: Boolean): Color = introTaglineInk(
        theme = theme.hexColorCode.hexToComposeColor(),
        ground = ground(theme, dark),
        onSurface = if (dark) md_theme_dark_onSurface else md_theme_light_onSurface,
    )

    // What it was: taj's ink for the theme, drawn on the tint.
    private fun before(theme: KrailThemeStyle, dark: Boolean): Color =
        theme.hexColorCode.hexToComposeColor().asThemeInk(dark)

    @Test
    fun `the tagline's ink has at least 3 to 1 on the real tinted ground for every theme in both modes, and in fact 4_5`() {
        listOf(false, true).forEach { dark ->
            themes.forEach { theme ->
                val contrast = after(theme, dark).contrastRatio(ground(theme, dark))

                assertTrue(contrast >= 3.0f, "${theme.name}, dark $dark: $contrast")
                assertTrue(contrast >= TAGLINE_MIN_CONTRAST, "${theme.name}, dark $dark: $contrast is under 4.5")
            }
        }
    }

    @Test
    fun `before the fix the dark tagline was under 3 to 1 on its ground, which is what this fixes`() {
        val measured = themes.map { it.name to before(it, dark = true).contrastRatio(ground(it, dark = true)) }

        // The numbers the brief gave were 2.75 to 2.98. At least one theme is under 3 to 1 on its tint without the fix.
        assertTrue(measured.any { it.second < 3.0f }, "nothing was under 3 to 1 before: $measured")
        assertTrue(measured.all { it.second > 2.0f }, "$measured")
    }

    @Test
    fun `the new ink is never lower than the old one on the real ground`() {
        listOf(false, true).forEach { dark ->
            themes.forEach { theme ->
                assertTrue(
                    after(theme, dark).contrastRatio(ground(theme, dark)) >= before(theme, dark).contrastRatio(ground(theme, dark)) - 0.001f,
                    "${theme.name}, dark $dark",
                )
            }
        }
    }

    @Test
    fun `a colour that already reads on its ground is used as it is, and one that does not is moved towards the surface text only as far as it needs`() {
        val white = Color.White
        val black = Color.Black

        assertEquals(black, introTaglineInk(black, ground = white, onSurface = black))
        val moved = introTaglineInk(white, ground = white, onSurface = black)
        assertTrue(moved.contrastRatio(white) >= TAGLINE_MIN_CONTRAST, "${moved.contrastRatio(white)}")
        assertTrue(moved != black, "it went all the way to the surface text, further than it needed")
    }
}
