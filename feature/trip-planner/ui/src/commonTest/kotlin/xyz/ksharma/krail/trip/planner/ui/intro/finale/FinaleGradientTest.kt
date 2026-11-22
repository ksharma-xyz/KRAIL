package xyz.ksharma.krail.trip.planner.ui.intro.finale

import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.tokens.AiGradientTokens
import xyz.ksharma.krail.taj.tokens.AiThemeGradientTokens
import xyz.ksharma.krail.trip.planner.ui.intro.introThemeChoices
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The wordmark's gradient built from the rider's theme: four stops, the theme colour first, and none darker than it.
 * The floor is stated here as a number as well as by the rule, so a theme added later cannot slip a dark stop in.
 */
class FinaleGradientTest {

    // The darkest of the four themes the Intro offers is Metro, at about 0.47. Nothing may be darker than this.
    private val absoluteFloor = 0.45f
    private val tolerance = 0.0001f

    // A colour holds half precision channels, so a lift is as exact as that allows.
    private val liftTolerance = 0.003f

    private val offered = introThemeChoices().map { it.style }

    private fun KrailThemeStyle.colour(): Color = hexColorCode.hexToComposeColor()

    @Test
    fun `the Intro offers four themes, which are the four this table is run on`() {
        assertEquals(
            listOf(KrailThemeStyle.Train, KrailThemeStyle.Metro, KrailThemeStyle.Bus, KrailThemeStyle.Ferry),
            offered,
        )
    }

    @Test
    fun `each theme has four stops and the first is the theme colour`() {
        KrailThemeStyle.entries.forEach { style ->
            val stops = themeGradientStops(style.colour())

            assertEquals(4, stops.size, "$style")
            assertEquals(style.colour(), stops.first(), "$style")
        }
    }

    @Test
    fun `no stop of any theme is darker than the theme colour, and none is darker than the stated floor`() {
        KrailThemeStyle.entries.forEach { style ->
            val themeLuma = lumaOf(style.colour())
            themeGradientStops(style.colour()).forEachIndexed { index, stop ->
                assertTrue(lumaOf(stop) >= themeLuma, "$style stop $index is darker than the theme colour")
            }
        }
        offered.forEach { style ->
            themeGradientStops(style.colour()).forEachIndexed { index, stop ->
                assertTrue(lumaOf(stop) >= absoluteFloor, "$style stop $index is below the floor: ${lumaOf(stop)}")
            }
        }
    }

    @Test
    fun `the blend to the theme colour never takes a stop below the floor either`() {
        offered.forEach { style ->
            val floor = lumaOf(style.colour()) - liftTolerance
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { blend ->
                val blended = finaleGradientColors(blend, style.colour(), themeGradientStops(style.colour()))

                blended.forEachIndexed { index, stop ->
                    assertTrue(lumaOf(stop) >= floor, "$style blend $blend stop $index")
                }
            }
        }
    }

    @Test
    fun `a partner that is as light as the theme is used as it is, and a darker one is lightened and not changed in kind`() {
        KrailThemeStyle.entries.forEach { style ->
            val partner = style.aiGradientPartner
            val third = themeGradientStops(style.colour())[2]

            if (lumaOf(partner) >= lumaOf(style.colour())) {
                assertEquals(partner, third, "$style keeps its partner")
            } else {
                assertTrue(third.red >= partner.red && third.green >= partner.green && third.blue >= partner.blue, "$style")
                assertEquals(lumaOf(style.colour()), lumaOf(third), liftTolerance, "$style is lightened only as far as it needs")
            }
        }
    }

    @Test
    fun `the gradient travels out to the partner and back to the point halfway`() {
        val train = themeGradientStops(KrailThemeStyle.Train.colour())

        assertEquals(train[1], train[3])
    }

    @Test
    fun `the halfway point is the app's own gradient for the theme, before it is lightened`() {
        KrailThemeStyle.entries.forEach { style ->
            val (_, halfway, _) = AiThemeGradientTokens.stopsFor(style)

            assertEquals(liftToLuma(halfway, lumaOf(style.colour())), themeGradientStops(style.colour())[1], "$style")
        }
    }

    @Test
    fun `a colour that is no theme gets the default theme pair and nothing fails`() {
        val stops = themeGradientStops(Color(0xFF123456))

        assertEquals(4, stops.size)
    }

    @Test
    fun `the old stops are still there behind the switch`() {
        KrailThemeStyle.entries.forEach { style ->
            assertEquals(AiGradientTokens.stops, finaleBrandStops(style.colour(), fromTheme = false))
            assertEquals(themeGradientStops(style.colour()), finaleBrandStops(style.colour(), fromTheme = true))
        }
        assertEquals(
            if (FINALE_GRADIENT_FROM_THEME) themeGradientStops(KrailThemeStyle.Bus.colour()) else AiGradientTokens.stops,
            finaleBrandStops(KrailThemeStyle.Bus.colour()),
        )
    }

    @Test
    fun `lifting is exact, leaves a light colour alone and never darkens`() {
        val dark = Color(0xFF200040)
        val light = Color(0xFFFFE080)

        assertEquals(light, liftToLuma(light, floor = 0.5f))
        assertEquals(Color.White, liftToLuma(Color.White, floor = 1f))
        val lifted = liftToLuma(dark, floor = 0.6f)
        assertTrue(lumaOf(lifted) >= 0.6f, "as light as the floor: ${lumaOf(lifted)}")
        assertEquals(0.6f, lumaOf(lifted), liftTolerance)
        assertTrue(lifted.red >= dark.red && lifted.green >= dark.green && lifted.blue >= dark.blue)
        assertEquals(dark.alpha, lifted.alpha)
    }

    @Test
    fun `luma runs from black at 0 to white at 1`() {
        assertEquals(0f, lumaOf(Color.Black), tolerance)
        assertEquals(1f, lumaOf(Color.White), tolerance)
        assertTrue(lumaOf(Color(0xFF000080)) < lumaOf(Color(0xFF008000)), "green is brighter than the same blue")
    }
}
