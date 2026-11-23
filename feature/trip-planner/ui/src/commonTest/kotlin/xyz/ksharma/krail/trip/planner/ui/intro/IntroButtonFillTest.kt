package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import xyz.ksharma.krail.taj.theme.contrastRatio
import xyz.ksharma.krail.taj.theme.md_theme_dark_surface
import xyz.ksharma.krail.taj.theme.md_theme_light_surface
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The fill of the primary button of each screen, a deeper shade of the screen's accent family so a white label has at least 4.5 to 1,
 * held to the numbers of the approved mock: green 5.59, blue 5.42, teal 5.45 and Barbie Pink 5.38. It is taj's own ink ladder
 * ([introButtonFill]) in light mode for both modes, and the label is always white.
 */
class IntroButtonFillTest {

    private val families = listOf(
        IntroStep.PARK_RIDE to 5.59f,
        IntroStep.DESTINATION to 5.42f,
        IntroStep.ORIGIN to 5.45f,
        IntroStep.CONFIRM to 5.38f,
    )

    private fun fill(step: IntroStep): Color = introButtonFill(introAccentTheme(step))

    @Test
    fun `a white label has the mock's contrast on each fill, green 5_59, blue 5_42, teal 5_45 and pink 5_38`() {
        families.forEach { (step, want) ->
            assertEquals(want, Color.White.contrastRatio(fill(step)), 0.1f, "$step")
        }
    }

    @Test
    fun `a white label has at least 4_5 to 1 on every fill`() {
        listOf(IntroStep.PARK_RIDE, IntroStep.DESTINATION, IntroStep.ORIGIN, IntroStep.CONFIRM, IntroStep.THEME).forEach { step ->
            assertTrue(Color.White.contrastRatio(fill(step)) >= BUTTON_LABEL_MIN_CONTRAST, "$step: ${Color.White.contrastRatio(fill(step))}")
        }
    }

    @Test
    fun `each fill is a deeper shade of its own theme colour, never the colour itself, and each is different`() {
        families.forEach { (step, _) ->
            val theme = introAccentTheme(step)
            assertNotEquals(theme, fill(step), "$step is the theme colour as it is")
            assertTrue(fill(step).luminance() < theme.luminance(), "$step is not deeper than its theme colour")
        }
        assertEquals(families.size, families.map { fill(it.first) }.toSet().size)
    }

    @Test
    fun `each fill is clearly a button on the page in both modes, at least 3 to 1 on the white and on the dark page`() {
        families.forEach { (step, _) ->
            assertTrue(fill(step).contrastRatio(md_theme_light_surface) >= 3.0f, "$step on the light page")
            assertTrue(fill(step).contrastRatio(md_theme_dark_surface) >= 3.0f, "$step on the dark page")
        }
    }

}
