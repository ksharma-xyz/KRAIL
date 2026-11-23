package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
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
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.trip.planner.ui.components.DepartureBoardBody
import xyz.ksharma.krail.trip.planner.ui.components.FloatingDark
import xyz.ksharma.krail.trip.planner.ui.components.mapActiveChipColors
import xyz.ksharma.krail.trip.planner.ui.components.mapCardColors
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
        val chipEdge: Color,
    )

    private lateinit var read: Read

    private fun readTokens() {
        composeRule.setContent {
            PreviewTheme {
                val soft = softFieldColors()
                val cardColors = mapCardColors()
                val options = cardColors.chip
                val active = mapActiveChipColors()
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
                    chipEdge = cardColors.chipEdge,
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
                "optionsHairlineOnMapGround=${r.chipEdge.contrastRatio(MAP_GROUND)} " +
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
        // The Options chip is the soft theme tint, which can sit close to the light map, so its edge is the hairline round it:
        // either the fill or the hairline stands 3 to 1 off the map.
        assertTrue(
            maxOf(r.optionsFill.contrastRatio(MAP_GROUND), r.chipEdge.contrastRatio(MAP_GROUND)) >= EDGE,
            "$mode: the Options button against the map",
        )
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

    // region the stop card's secondary text

    private class BoardSecondary(val body: Color, val header: Color, val softLabel: Color, val onSurface: Color)

    private lateinit var board: BoardSecondary

    private fun readBoardSecondary() {
        composeRule.setContent {
            PreviewTheme {
                board = BoardSecondary(
                    body = mapCardColors().body,
                    header = mapCardColors().header,
                    softLabel = KrailTheme.colors.softLabel,
                    onSurface = KrailTheme.colors.onSurface,
                )
            }
        }
        composeRule.waitForIdle()
    }

    /**
     * softLabel on the stop card's tint, the old colour of the board's secondary text: on the body it is 3.98 to 1 in light mode
     * (under 4.5) and 5.31 in dark mode, and on the header, the stronger step the card is drawn in, 3.28 in dark mode. The card's
     * own text colour is what the board is given now, and it reads 4.5 to 1 or better on both bands in both modes.
     */
    private fun assertBoardSecondary(mode: String, softOnBody: Float, softOnHeader: Float) {
        readBoardSecondary()

        val onBody = board.softLabel.contrastRatio(board.body)
        val onHeader = board.softLabel.contrastRatio(board.header)
        val inkBody = board.onSurface.contrastRatio(board.body)
        val inkHeader = board.onSurface.contrastRatio(board.header)
        println("CONTRAST $mode stopCardSecondary softLabelOnBody=$onBody softLabelOnHeader=$onHeader onSurfaceOnBody=$inkBody onSurfaceOnHeader=$inkHeader")
        assertTrue(kotlin.math.abs(onBody - softOnBody) < 0.05f, "$mode: softLabel on the body is $onBody to 1, expected about $softOnBody")
        assertTrue(kotlin.math.abs(onHeader - softOnHeader) < 0.05f, "$mode: softLabel on the header is $onHeader to 1, expected about $softOnHeader")
        assertTrue(minOf(onBody, onHeader) < TEXT, "$mode: softLabel is under 4.5 on a band of the card, which is why the card passes its own text colour")
        assertTrue(inkBody >= TEXT, "$mode: the card's own text colour on the body is $inkBody to 1, 4.5 needed")
        assertTrue(inkHeader >= TEXT, "$mode: the card's own text colour on the header is $inkHeader to 1, 4.5 needed")
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight")
    fun `in light mode the board's secondary text on the stop card reads in the card's text colour at 4_5 to 1 or better, where softLabel was 3_98`() =
        assertBoardSecondary("light", softOnBody = 3.98f, softOnHeader = LIGHT_HEADER_SOFT)

    @Test
    @Config(qualifiers = "w411dp-h891dp-night")
    fun `in dark mode the board's secondary text on the stop card reads in the card's text colour at 4_5 to 1 or better, where softLabel was 3_28 on the header`() =
        assertBoardSecondary("dark", softOnBody = 5.31f, softOnHeader = 3.28f)

    /** The colour the empty board draws its message in, and the page's softLabel, for a host that passes [secondary]. */
    private fun emptyBoardTextColor(secondary: Color?): Pair<Color, Color> {
        var soft = Color.Unspecified
        composeRule.setContent {
            PreviewTheme {
                soft = KrailTheme.colors.softLabel
                DepartureBoardBody(
                    state = DeparturesState(isLoading = false),
                    onRetry = {},
                    onLoadPreviousDepartures = {},
                    secondaryTextColor = secondary,
                )
            }
        }
        composeRule.waitForIdle()
        val results = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText("No upcoming departures found.", useUnmergedTree = true).fetchSemanticsNode()
            .config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        return results.first().layoutInput.style.color to soft
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight")
    fun `the shared board draws its secondary text in the colour a host passes`() {
        val passed = Color(0xFF123456)
        assertTrue(emptyBoardTextColor(passed).first == passed, "A host's colour reaches the board's secondary text.")
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight")
    fun `with no colour passed the board's secondary text is softLabel exactly as before`() {
        val (drawn, soft) = emptyBoardTextColor(null)
        assertTrue(drawn == soft, "Other hosts are unchanged: $drawn against softLabel $soft.")
    }

    // endregion

    private companion object {
        const val TEXT = 4.5f
        /** softLabel on the light stop card header, the stronger step, as the run prints it. */
        const val LIGHT_HEADER_SOFT = 3.33f
        const val EDGE = 3f
        const val PILL_SHAPE_FLOOR = 1.1f

        /** The flat ground of the approved mock's map, a stand in for the real style. */
        val MAP_GROUND = Color(0xFFEDEBE4)
    }
}
