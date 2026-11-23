package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.contrastRatio
import xyz.ksharma.krail.trip.planner.ui.components.FloatingDark
import xyz.ksharma.krail.trip.planner.ui.components.softFieldColors
import kotlin.test.assertTrue

/**
 * The contrast of Search Stop's colours, in light and dark app mode, read from the tokens the screen draws with.
 * Text and a placeholder need 4.5 to 1 on the pill's fill and the Options button's label on its fill. The pill's
 * fill against the box, and the box against the map, are what shows the shapes and are held to a floor and printed
 * so the real figure is on record: the pills are soft on purpose and read because they are rounded and hold text.
 *
 * The map ground is a stand in. The real tiles are not in a host test, so [MAP_GROUND] is the flat ground of the
 * approved mock, not a value read from the style.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel6, manifest = Config.NONE)
class SearchStopContrastTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class Read(
        val pillFill: Color,
        val pillText: Color,
        val pillPlaceholder: Color,
        val boxFill: Color,
        val optionsFill: Color,
        val optionsInk: Color,
        val activeFill: Color,
        val activeInk: Color,
        val labelFill: Color,
        val labelInk: Color,
        val onSurface: Color,
    )

    private lateinit var read: Read

    private fun readTokens() {
        composeRule.setContent {
            PreviewTheme {
                val soft = softFieldColors()
                val options = FloatingDark.buttonColors()
                val active = FloatingDark.activeButtonColors()
                read = Read(
                    pillFill = soft.containerColor,
                    pillText = soft.contentColor,
                    pillPlaceholder = soft.placeholderColor,
                    boxFill = KrailTheme.colors.surface,
                    optionsFill = options.containerColor,
                    optionsInk = options.contentColor,
                    activeFill = active.containerColor,
                    activeInk = active.contentColor,
                    labelFill = KrailTheme.colors.stopLabelSurface,
                    labelInk = KrailTheme.colors.onStopLabelSurface,
                    onSurface = KrailTheme.colors.onSurface,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun report(mode: String) {
        val r = read
        println(
            "CONTRAST $mode " +
                "pillText=${r.pillText.contrastRatio(r.pillFill)} " +
                "pillPlaceholder=${r.pillPlaceholder.contrastRatio(r.pillFill)} " +
                "pillFillOnBox=${r.pillFill.contrastRatio(r.boxFill)} " +
                "boxOnMapGround=${r.boxFill.contrastRatio(MAP_GROUND)} " +
                "optionsLabel=${r.optionsInk.contrastRatio(r.optionsFill)} " +
                "optionsActiveLabel=${r.activeInk.contrastRatio(r.activeFill)} " +
                "optionsOnMapGround=${r.optionsFill.contrastRatio(MAP_GROUND)} " +
                "labelPillText=${r.labelInk.contrastRatio(r.labelFill)}",
        )
    }

    private fun assertReads(mode: String) {
        readTokens()
        report(mode)
        val r = read
        assertTrue(r.pillText.contrastRatio(r.pillFill) >= TEXT, "$mode: the pill text on its fill")
        assertTrue(r.pillPlaceholder.contrastRatio(r.pillFill) >= TEXT, "$mode: the pill placeholder on its fill")
        assertTrue(r.onSurface.contrastRatio(r.pillFill) >= TEXT, "$mode: back arrow and dot on the pill")
        assertTrue(r.optionsInk.contrastRatio(r.optionsFill) >= TEXT, "$mode: the Options label on its fill")
        assertTrue(r.activeInk.contrastRatio(r.activeFill) >= TEXT, "$mode: the Options label in its active look")
        assertTrue(r.labelInk.contrastRatio(r.labelFill) >= TEXT, "$mode: the label pill text on its fill")
        assertTrue(r.pillFill.contrastRatio(r.boxFill) >= PILL_SHAPE_FLOOR, "$mode: the pill is a visible step from the box")
        assertTrue(r.optionsFill.contrastRatio(MAP_GROUND) >= EDGE, "$mode: the Options button against the map")
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight")
    fun `in light mode the text, placeholder and Options label all read at 4_5 to 1`() = assertReads("light")

    @Test
    @Config(qualifiers = "w411dp-h891dp-night")
    fun `in dark mode the text, placeholder and Options label all read at 4_5 to 1`() = assertReads("dark")

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight")
    fun `in light mode the white box is lifted off the map by its glow, not by its fill alone`() {
        readTokens()

        // Fill against the map is about 1.2 to 1, which is why the box wears the info tile's glow and border.
        assertTrue(read.boxFill.contrastRatio(MAP_GROUND) < EDGE, "The fill alone is not the edge, so the glow is needed.")
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight")
    fun `in light mode the lone pill's text and placeholder read on the surface fill`() = assertLonePill("light")

    @Test
    @Config(qualifiers = "w411dp-h891dp-night")
    fun `in dark mode the lone pill's text and placeholder read on the surface fill`() = assertLonePill("dark")

    /**
     * The lone pill is the soft field's text, placeholder and caret on the card's own surface fill. The numbers are the
     * test's message so a run prints them: the text and the placeholder both clear 4.5 to 1 on the fill, and the fill
     * against the map is under 3 to 1 in light mode (white on a light map), which is why the pill wears the same edge
     * as the card.
     */
    private fun assertLonePill(mode: String) {
        readTokens()

        val text = read.pillText.contrastRatio(read.boxFill)
        val placeholder = read.pillPlaceholder.contrastRatio(read.boxFill)
        val againstMap = read.boxFill.contrastRatio(MAP_GROUND)
        assertTrue(text >= TEXT, "$mode: the lone pill's text on the surface fill is $text to 1")
        assertTrue(placeholder >= TEXT, "$mode: the lone pill's placeholder on the surface fill is $placeholder to 1 (map $againstMap)")
    }

    private companion object {
        const val TEXT = 4.5f
        const val EDGE = 3f
        const val PILL_SHAPE_FLOOR = 1.1f

        /** The flat ground of the approved mock's map, a stand in for the real style. */
        val MAP_GROUND = Color(0xFFEDEBE4)
    }
}
