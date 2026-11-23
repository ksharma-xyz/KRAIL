package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.contrastRatio
import xyz.ksharma.krail.taj.theme.md_theme_dark_surface
import xyz.ksharma.krail.taj.theme.md_theme_light_surface
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The hero's colour of each screen, against the page it is drawn on, in both modes, held to the numbers of the approved mock: Park
 * and Ride Ferry green, destination Bus blue, origin Metro teal, confirm Barbie Pink and the colour question Purple Drip. A theme
 * colour that already has 3.2 to 1 on the page is used as it is, in light mode any other is the same hue and saturation made darker
 * only as far as it needs, and in dark mode any other takes taj's ladder. There is no colour typed into the Intro.
 */
class IntroAccentTest {

    private val steps = listOf(
        IntroStep.PARK_RIDE,
        IntroStep.DESTINATION,
        IntroStep.ORIGIN,
        IntroStep.CONFIRM,
        IntroStep.THEME,
    )

    private fun accent(step: IntroStep, dark: Boolean): Color =
        introAccentInk(introAccentTheme(step), if (dark) md_theme_dark_surface else md_theme_light_surface, dark)

    private fun contrast(step: IntroStep, dark: Boolean): Float =
        accent(step, dark).contrastRatio(if (dark) md_theme_dark_surface else md_theme_light_surface)

    @Test
    fun `each screen has its own theme colour, read from the theme and fixed`() {
        assertEquals(
            listOf(
                KrailThemeStyle.Ferry,
                KrailThemeStyle.Bus,
                KrailThemeStyle.Metro,
                KrailThemeStyle.BarbiePink,
                KrailThemeStyle.PurpleDrip,
            ),
            steps.map { introAccentStyle(it) },
        )
        steps.forEach { assertEquals(accent(it, dark = false), accent(it, dark = false), "$it is the same every time") }
    }

    @Test
    fun `in light mode the five accents have the mock's contrast on the white page, blue 3_24, green 3_27, teal 3_53, pink 4_42 and purple 5_84`() {
        val expected = mapOf(
            IntroStep.DESTINATION to 3.24f,
            IntroStep.PARK_RIDE to 3.27f,
            IntroStep.ORIGIN to 3.53f,
            IntroStep.CONFIRM to 4.42f,
            IntroStep.THEME to 5.84f,
        )

        expected.forEach { (step, want) ->
            assertEquals(
                want,
                contrast(step, dark = false),
                0.05f,
                "$step in light mode",
            )
        }
    }

    @Test
    fun `in dark mode the five accents have the mock's contrast on the dark page, blue 7_25, green 6_30, teal 4_88, pink 3_89 and purple 3_80`() {
        val expected = mapOf(
            IntroStep.DESTINATION to 7.25f,
            IntroStep.PARK_RIDE to 6.30f,
            IntroStep.ORIGIN to 4.88f,
            IntroStep.CONFIRM to 3.89f,
            IntroStep.THEME to 3.80f,
        )

        expected.forEach { (step, want) ->
            assertEquals(
                want,
                contrast(step, dark = true),
                0.05f,
                "$step in dark mode",
            )
        }
    }

    @Test
    fun `every accent has at least 3 to 1 on the real page in both modes, and in fact at least 3_2`() {
        listOf(false, true).forEach { dark ->
            steps.forEach { step ->
                assertTrue(contrast(step, dark) >= 3.0f, "$step, dark $dark: ${contrast(step, dark)}")
                assertTrue(contrast(step, dark) >= ACCENT_MIN_CONTRAST, "$step, dark $dark: ${contrast(step, dark)}")
            }
        }
    }

    @Test
    fun `the five accents are different from each other in each mode`() {
        listOf(false, true).forEach { dark ->
            val colours = steps.map { accent(it, dark) }
            assertEquals(steps.size, colours.toSet().size, "dark $dark: $colours")
        }
    }

    @Test
    fun `a theme colour with enough contrast is used as it is, and Bus blue and Ferry green are darkened in light mode only`() {
        listOf(IntroStep.ORIGIN, IntroStep.CONFIRM, IntroStep.THEME).forEach {
            assertEquals(introAccentTheme(it), accent(it, dark = false), "$it in light mode")
        }
        listOf(IntroStep.PARK_RIDE, IntroStep.DESTINATION, IntroStep.ORIGIN, IntroStep.CONFIRM).forEach {
            assertEquals(introAccentTheme(it), accent(it, dark = true), "$it in dark mode")
        }
        listOf(IntroStep.PARK_RIDE, IntroStep.DESTINATION).forEach {
            assertNotEquals(introAccentTheme(it), accent(it, dark = false), "$it in light mode")
        }
        assertNotEquals(introAccentTheme(IntroStep.THEME), accent(IntroStep.THEME, dark = true), "purple in dark mode")
    }

    @Test
    fun `a darkened accent keeps its hue and saturation and stops at the first step that reaches 3_2`() {
        listOf(IntroStep.PARK_RIDE, IntroStep.DESTINATION).forEach { step ->
            val theme = introAccentTheme(step)
            val darker = accent(step, dark = false)
            val (hue, saturation, lightness) = theme.toHsl()
            val (darkerHue, darkerSaturation, darkerLightness) = darker.toHsl()

            assertEquals(hue, darkerHue, 1.5f, "$step hue")
            assertEquals(saturation, darkerSaturation, 0.02f, "$step saturation")
            assertTrue(darkerLightness < lightness, "$step is darker")
            val oneStepLighter = hslColor(darkerHue, darkerSaturation, darkerLightness + 0.01f)
            assertTrue(
                oneStepLighter.contrastRatio(md_theme_light_surface) < ACCENT_MIN_CONTRAST,
                "$step is darkened no further than it needs",
            )
        }
    }

    @Test
    fun `the lighter purple of dark mode is the taj ladder's, which is a lighter purple with the same hue`() {
        val purple = introAccentTheme(IntroStep.THEME)
        val lifted = accent(IntroStep.THEME, dark = true)

        assertTrue(
            purple.contrastRatio(md_theme_dark_surface) < ACCENT_MIN_CONTRAST,
            "Purple Drip is under the bar as it is",
        )
        assertTrue(lifted.contrastRatio(md_theme_dark_surface) >= ACCENT_MIN_CONTRAST)
        assertTrue(abs(purple.toHsl().first - lifted.toHsl().first) < 12f, "still a purple")
    }

    @Test
    fun `the HSL conversion round trips`() {
        listOf(0xFF5AB031, 0xFF00B5EF, 0xFF009B77, 0xFFE0218A, 0xFFAC00C9, 0xFF808080).forEach { argb ->
            val colour = Color(argb)
            val (hue, saturation, lightness) = colour.toHsl()
            val back = hslColor(hue, saturation, lightness)

            assertEquals(colour.red, back.red, 0.004f)
            assertEquals(colour.green, back.green, 0.004f)
            assertEquals(colour.blue, back.blue, 0.004f)
        }
    }
}
