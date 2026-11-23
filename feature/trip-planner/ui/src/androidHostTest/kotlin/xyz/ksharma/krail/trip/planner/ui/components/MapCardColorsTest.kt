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
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.taj.themeDecorColor

/**
 * The colours of every card that floats over the map, from the one accessor ([mapCardColors]): the soft tint family the
 * Timetable's cards are in, never a near black slab (the stop card was one in both modes) and never pure white. The table
 * of every theme in both modes is printed, so the real figures are on record, and held to the floors the brief set: the
 * text on the body and on the header 4.5 to 1 or better, the header a visibly different step from the body at 1.15 to 1
 * or better, and a chip's text on its tint 4.5 to 1 or better.
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
     * The Search Stop card is the Timetable's own tint ([MapCardColors.tint]) with the page's text colour on it, the hairline
     * `softLabel` as its edge in light mode, and no edge on the dark map where the card stands off the ground by itself. The
     * numbers are printed for every theme, and held to the floors of the approved picture: text 4.5, the hairline and the dark
     * card 3 against the map ground.
     */
    private fun assertSearchStopCard(mode: String, dark: Boolean) {
        readTokens()
        assertEquals(timetableTint, accessor.tint, "$mode: the card's tint is the Timetable's own")
        for (style in allThemes) {
            val tint = mapCardColorsFor(style.hexColorCode.hexToComposeColor(), darkMode = dark, surface = surface, onSurface = onSurface).chip
            val text = onSurface.contrastRatio(tint)
            val cardOnMap = tint.contrastRatio(MAP_GROUND)
            val hairlineOnMap = softLabel.contrastRatio(MAP_GROUND)
            println("SEARCHCARD $mode ${style.name} textOnTint=$text cardOnMap=$cardOnMap hairlineOnMap=$hairlineOnMap")
            assertTrue(text >= TEXT, "$mode ${style.name}: the stop name, prompt, dot and pin on the card are $text to 1")
            if (dark) {
                assertTrue(cardOnMap >= EDGE, "$mode ${style.name}: the dark card needs no edge, it is $cardOnMap to 1 against the map")
            } else {
                assertTrue(hairlineOnMap >= EDGE, "$mode ${style.name}: the hairline is $hairlineOnMap to 1 against the map")
            }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight")
    fun `in light mode the Search Stop card reads at 4_5 to 1 on every theme and its hairline holds 3 to 1 on the map`() =
        assertSearchStopCard("light", dark = false)

    @Test
    @Config(qualifiers = "w411dp-h891dp-night")
    fun `in dark mode the Search Stop card reads at 4_5 to 1 on every theme and stands off the map by itself`() =
        assertSearchStopCard("dark", dark = true)

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
    fun `the accessor follows the Timetable's own tint function and the pure family agrees with it`() {
        readTokens()

        assertEquals(timetableTint, accessor.chip.containerColor, "A chip is exactly the Timetable's tint over the surface")
        val pure = mapCardColorsFor(themeColor, darkMode = false, surface = surface, onSurface = onSurface)
        assertEquals(timetableTint, pure.chip, "and the pure family has not drifted from taj's themeBackgroundColor")
        assertEquals(pure.body, accessor.body)
        assertEquals(pure.header, accessor.header)
        assertEquals(onSurface, accessor.onCard)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night")
    fun `in dark mode the chip is the Timetable's tint too`() {
        readTokens()

        assertEquals(timetableTint, accessor.chip.containerColor)
        assertEquals(timetableTint, accessor.header, "and the dark header is that same tint")
    }

    private companion object {
        const val TEXT = 4.5f
        const val EDGE = 3f
        const val HEADER_STEP = 1.15f
        const val DARK_STEP_OVER_PAGE = 1.3f
        val MAP_GROUND = Color(0xFFEDEBE4)
    }
}
