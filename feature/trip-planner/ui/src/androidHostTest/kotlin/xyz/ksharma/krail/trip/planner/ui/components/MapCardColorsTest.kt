package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createComposeRule
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.contrastRatio
import xyz.ksharma.krail.taj.theme.md_theme_light_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_light_softLabel
import xyz.ksharma.krail.taj.theme.md_theme_light_surface
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.taj.themeDecorColor

/**
 * The colours of every card that floats over the map, from the one accessor ([mapCardColors]). The Search Stop card, the bare
 * pill, the notice, the Options card and the Options pill are one fixed light look in both app modes: the theme colour at
 * 15 percent over white with the light palette's text and its `softLabel` hairline, never a dark tint and never the mode aware
 * `softLabel`. The stop card keeps the older mode aware family (a body and a header band), which is held here as it was: the text
 * on the body and on the header 4.5 to 1 or better, the header a visibly different step from the body at 1.15 to 1 or better.
 * The table of every theme in both modes is printed, so the real figures are on record.
 *
 * The map ground is a stand in. The real tiles are not in a host test, so [MAP_GROUND] is the flat ground of the approved
 * mock, not a value read from the style.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class MapCardColorsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var surface = Color.Unspecified
    private var onSurface = Color.Unspecified
    private var softLabel = Color.Unspecified
    private var darkSlab = Color.Unspecified
    private lateinit var accessor: MapCardColors
    private var timetableTint = Color.Unspecified
    private var themeColor = Color.Unspecified

    private fun readTokens() {
        composeRule.setContent {
            PreviewTheme(themeStyle = KrailThemeStyle.Train) {
                surface = KrailTheme.colors.surface
                onSurface = KrailTheme.colors.onSurface
                softLabel = KrailTheme.colors.softLabel
                darkSlab = FloatingDark.container
                accessor = mapCardColors()
                timetableTint = themeBackgroundColor().compositeOver(KrailTheme.colors.surface)
                themeColor = themeDecorColor()
            }
        }
        composeRule.waitForIdle()
    }

    private val themes = listOf(KrailThemeStyle.Train, KrailThemeStyle.Metro, KrailThemeStyle.Bus, KrailThemeStyle.Ferry)

    private fun assertFamily(mode: String, dark: Boolean) {
        readTokens()
        for (style in themes) {
            val theme = style.hexColorCode.hexToComposeColor()
            val tints = mapCardColorsFor(theme, darkMode = dark, surface = surface, onSurface = onSurface)
            val onBody = onSurface.contrastRatio(tints.body)
            val onHeader = onSurface.contrastRatio(tints.header)
            val onChip = onSurface.contrastRatio(tints.chip)
            val step = tints.header.contrastRatio(tints.body)
            println(
                "PALETTE $mode ${style.name} " +
                    "textOnBody=$onBody textOnHeader=$onHeader textOnChip=$onChip " +
                    "headerOverBody=$step bodyOnSurface=${tints.body.contrastRatio(surface)} " +
                    "bodyOnMap=${tints.body.contrastRatio(MAP_GROUND)} headerOnMap=${tints.header.contrastRatio(MAP_GROUND)} " +
                    "chipOnMap=${tints.chip.contrastRatio(MAP_GROUND)} hairlineOnMap=${softLabel.contrastRatio(MAP_GROUND)} " +
                    "hairlineOnChip=${softLabel.contrastRatio(tints.chip)}",
            )
            assertTrue(onBody >= TEXT, "$mode ${style.name}: text on the body is $onBody to 1")
            assertTrue(onHeader >= TEXT, "$mode ${style.name}: text on the header is $onHeader to 1")
            assertTrue(onChip >= TEXT, "$mode ${style.name}: text on a chip is $onChip to 1")
            assertTrue(step >= HEADER_STEP, "$mode ${style.name}: the header is only $step to 1 from the body")
        }
    }

    private val allThemes = KrailThemeStyle.entries

    /**
     * The Search Stop card is one fixed light look in both app modes: the Timetable's light tint over white with the light
     * palette's text, and the light palette's `softLabel` as its hairline. The numbers are printed for every theme and held to the
     * floors of the approved picture: the text 4.5 to 1 on every theme and 17.5 to 18.3 on the four standard ones, the hairline
     * 3.8 to 1 against the map ground, and the fill, the text and the edge the very same in light and dark app mode.
     */
    private fun assertSearchStopCard(mode: String) {
        readTokens()
        val lightSurface = md_theme_light_surface
        val lightText = md_theme_light_onSurface
        assertEquals(lightText, accessor.onTint, "$mode: the text is the light palette's onSurface")
        assertEquals(md_theme_light_softLabel, accessor.chipEdge, "$mode: the edge is the light palette's softLabel")
        assertEquals(lightText, accessor.chip.contentColor, "$mode: a chip carries the same text")
        val expected = themeColor.copy(alpha = TIMETABLE_ALPHA_LIGHT).compositeOver(lightSurface)
        assertEquals(expected, accessor.tint, "$mode: the card is the theme at 15 percent over white")
        assertEquals(expected, accessor.chip.containerColor, "$mode: a chip is the card's tint")
        assertTrue(accessor.chipEdge.contrastRatio(MAP_GROUND) >= HAIRLINE, "$mode: the hairline is ${accessor.chipEdge.contrastRatio(MAP_GROUND)} to 1 against the map")
        for (style in allThemes) {
            val tint = mapCardColorsFor(style.hexColorCode.hexToComposeColor(), darkMode = false, surface = lightSurface, onSurface = lightText).chip
            val text = lightText.contrastRatio(tint)
            println("SEARCHCARD $mode ${style.name} textOnTint=$text hairlineOnMap=${accessor.chipEdge.contrastRatio(MAP_GROUND)} hairlineOnTint=${accessor.chipEdge.contrastRatio(tint)}")
            assertTrue(text >= TEXT, "$mode ${style.name}: the stop name, prompt, dot and pin on the card are $text to 1")
        }
        for (style in themes) {
            val tint = mapCardColorsFor(style.hexColorCode.hexToComposeColor(), darkMode = false, surface = lightSurface, onSurface = lightText).chip
            val text = lightText.contrastRatio(tint)
            assertTrue(text in CARD_TEXT_LOW..CARD_TEXT_HIGH, "$mode ${style.name}: the card text is $text to 1, the picture's 17.51 to 18.26")
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight")
    fun `in light mode the Search Stop card is the fixed light look and reads at 17_5 to 18_3 on the standard themes`() =
        assertSearchStopCard("light")

    @Test
    @Config(qualifiers = "w411dp-h891dp-night")
    fun `in dark mode the Search Stop card is the very same fixed light look, not a dark tint`() = assertSearchStopCard("dark")

    @Test
    @Config(qualifiers = "w411dp-h891dp-night")
    fun `in dark mode the edge is not the mode aware softLabel, which is 1_82 to 1 against the map`() {
        readTokens()

        assertNotEquals(softLabel, accessor.chipEdge, "The mode aware softLabel in dark mode is ${softLabel.contrastRatio(MAP_GROUND)} to 1 against the map")
        assertTrue(softLabel.contrastRatio(MAP_GROUND) < EDGE)
        assertTrue(accessor.chipEdge.contrastRatio(MAP_GROUND) >= HAIRLINE)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight")
    fun `in light mode every theme reads at 4_5 to 1 and the header is a visible step from the body`() = assertFamily("light", dark = false)

    @Test
    @Config(qualifiers = "w411dp-h891dp-night")
    fun `in dark mode every theme reads at 4_5 to 1 and the header is a visible step from the body`() = assertFamily("dark", dark = true)

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight")
    fun `in light mode the card is a light tint, not the dark slab and not pure white`() {
        readTokens()

        assertNotEquals(darkSlab, accessor.body, "The body is not the fixed dark container")
        assertNotEquals(darkSlab, accessor.header)
        assertNotEquals(Color.White, accessor.body, "and not pure white")
        assertNotEquals(surface, accessor.body, "nor the plain surface")
        assertTrue(accessor.body.luminance() > 0.5f, "The body is light, ${accessor.body.luminance()}")
        assertTrue(accessor.header.luminance() > 0.4f, "and so is the header, ${accessor.header.luminance()}")
        assertTrue(accessor.body.contrastRatio(darkSlab) > 8f, "far from the near black it was")
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night")
    fun `in dark mode the card is a tint of the theme, a visible step above the page and not the near black slab`() {
        readTokens()

        assertNotEquals(darkSlab, accessor.body, "The body is not the fixed dark container")
        assertNotEquals(surface, accessor.body, "nor the page's own surface")
        assertNotEquals(darkSlab, accessor.header)
        assertTrue(accessor.body.contrastRatio(surface) >= DARK_STEP_OVER_PAGE, "The body stands ${accessor.body.contrastRatio(surface)} to 1 above the page")
        assertTrue(accessor.body.contrastRatio(darkSlab) >= DARK_STEP_OVER_PAGE, "and above the slab it replaces")
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight")
    fun `in light mode the fixed tint is exactly taj's themeBackgroundColor over the surface, so it has not drifted`() {
        readTokens()

        assertEquals(timetableTint, accessor.tint, "The card is exactly the Timetable's tint over the surface")
        assertEquals(timetableTint, accessor.chip.containerColor)
        val pure = mapCardColorsFor(themeColor, darkMode = false, surface = surface, onSurface = onSurface)
        assertEquals(timetableTint, pure.chip, "and the pure family has not drifted from taj's themeBackgroundColor")
        assertEquals(pure.body, accessor.body)
        assertEquals(pure.header, accessor.header)
        assertEquals(onSurface, accessor.onCard)
        assertEquals(onSurface, accessor.onTint)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night")
    fun `in dark mode the stop card keeps its mode aware family and the fixed tint is the light one`() {
        readTokens()

        assertEquals(timetableTint, accessor.header, "the stop card's dark header is still the Timetable's dark tint")
        assertNotEquals(timetableTint, accessor.tint, "and the fixed card is not that dark tint")
        assertTrue(accessor.tint.luminance() > 0.5f, "it is the light tint: ${accessor.tint.luminance()}")
        assertEquals(onSurface, accessor.onCard, "the stop card's text is the dark mode text")
        assertNotEquals(onSurface, accessor.onTint, "and the fixed card's is not")
    }

    private companion object {
        const val TEXT = 4.5f
        const val EDGE = 3f
        const val HAIRLINE = 3.8f
        const val CARD_TEXT_LOW = 17.5f
        const val CARD_TEXT_HIGH = 18.3f
        const val HEADER_STEP = 1.15f
        const val DARK_STEP_OVER_PAGE = 1.3f
        val MAP_GROUND = Color(0xFFEDEBE4)
    }
}
