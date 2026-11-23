package xyz.ksharma.krail.trip.planner.ui.searchstop

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.md_theme_light_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_light_onSurface_placeholder
import xyz.ksharma.krail.taj.theme.md_theme_light_surface
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.FROM_PLACEHOLDER
import xyz.ksharma.krail.trip.planner.ui.components.TO_PLACEHOLDER
import xyz.ksharma.krail.trip.planner.ui.components.ToFromField
import xyz.ksharma.krail.trip.planner.ui.components.mapCardColors
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The surfaces that stand on the always light map are one fixed light look whatever the app mode is: the From and To card, the
 * Options card with its chips, and the bare single pill are the theme at 15 percent over white with the light palette's text,
 * drawn from the real components and read from their pixels and their text layouts, once in light and once in dark app mode.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-notnight", manifest = Config.NONE)
class SearchStopFixedLightCardsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var composeView: View? = null
    private var tint = Color.Unspecified
    private var pageSurface = Color.Unspecified
    private var modeText = Color.Unspecified
    private var pillStop by mutableStateOf<StopItem?>(null)

    private val gaps = searchStopGaps()

    private fun drawn(): Bitmap {
        var bitmap: Bitmap? = null
        composeRule.runOnUiThread {
            val view = checkNotNull(composeView)
            bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(checkNotNull(bitmap)))
        }
        return checkNotNull(bitmap)
    }

    private fun pixel(image: Bitmap, x: Dp, y: Dp): Color {
        val density = composeRule.density
        return Color(image.getPixel(with(density) { x.roundToPx() }, with(density) { y.roundToPx() }))
    }

    private fun close(expected: Color, actual: Color) =
        abs(expected.red - actual.red) <= 0.03f && abs(expected.green - actual.green) <= 0.03f && abs(expected.blue - actual.blue) <= 0.03f

    private fun boundsOf(tag: String): DpRect = composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun SemanticsNode.textColors(): List<Color> {
        val own = mutableListOf<TextLayoutResult>().also { config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(it) }
        return own.map { it.layoutInput.style.color } + children.flatMap { it.textColors() }
    }

    private fun setOptionsCard(selected: Set<Int>) {
        composeRule.setContent {
            PreviewTheme {
                composeView = LocalView.current
                tint = mapCardColors().tint
                pageSurface = KrailTheme.colors.surface
                modeText = KrailTheme.colors.onSurface
                Box(modifier = Modifier.fillMaxSize().padding(gaps.edge)) {
                    SearchStopModeBlock(
                        selectedModes = persistentSetOf<Int>().addAll(selected),
                        onToggle = {},
                        modifier = Modifier.width(300.dp),
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun assertOptionsCard(mode: String) {
        val off = NswTransportConfig.productClassFor(TransportMode.Ferry)
        setOptionsCard(selected = emptySet<Int>() + off)

        val block = boundsOf(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS)
        val image = drawn()
        // The card's own padding, clear of every chip and of the hairline.
        assertTrue(close(tint, pixel(image, block.left + 24.dp, block.top + 6.dp)), "$mode: the Options card is the fixed light tint")
        assertTrue(tint.red + tint.green + tint.blue > 2f, "$mode: and it is light, never a dark tint: $tint")
        // A chip that is off is the light surface with its mode colour as its outline, never the dark page surface.
        val unselected = boundsOf(TripPlannerTestTags.searchStopModeChip(NswTransportConfig.productClassFor(TransportMode.Train)))
        assertTrue(
            close(md_theme_light_surface, pixel(image, unselected.right - 10.dp, unselected.top + (unselected.bottom - unselected.top) / 2f)) ||
                close(md_theme_light_surface, pixel(image, unselected.left + unselected.width() / 2f, unselected.top + 5.dp)),
            "$mode: an unselected chip is filled with the light surface",
        )
    }

    private fun DpRect.width() = right - left

    @Test
    fun `in light mode the Options card is the fixed light tint with white chips that are off`() = assertOptionsCard("light")

    @Test
    @Config(qualifiers = "w411dp-h891dp-night")
    fun `in dark mode the Options card is the very same fixed light tint, not a dark one`() {
        assertOptionsCard("dark")
        assertNotEquals(pageSurface, tint, "the page behind it is the dark surface")
    }

    private fun setLonePill() {
        composeRule.setContent {
            PreviewTheme {
                composeView = LocalView.current
                tint = mapCardColors().tint
                pageSurface = KrailTheme.colors.surface
                modeText = KrailTheme.colors.onSurface
                Box(modifier = Modifier.fillMaxSize().padding(gaps.edge)) {
                    SearchStopGroupCard(
                        form = SearchStopFormModel(
                            shape = FormShape.Single,
                            editing = null,
                            toPlaceholder = labelPromptFor("Home"),
                            fromPlaceholder = FROM_PLACEHOLDER,
                            toStop = pillStop,
                            fromStop = null,
                            fieldState = rememberTextFieldState(),
                            focusRequester = remember { FocusRequester() },
                            onFlowEvent = {},
                            onGoClick = {},
                            onTextChange = {},
                        ),
                        onBack = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun assertLonePill(mode: String) {
        setLonePill()

        val pill = boundsOf(TripPlannerTestTags.SEARCH_ROW_TO)
        val image = drawn()
        assertTrue(close(tint, pixel(image, pill.right - 24.dp, pill.top + 24.dp)), "$mode: the lone pill is the fixed light tint")
        val colors = composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_TO, useUnmergedTree = true).fetchSemanticsNode().textColors()
        assertTrue(colors.isNotEmpty(), "$mode: the pill's prompt can be read")
        for (color in colors) {
            assertTrue(
                color.copy(alpha = 1f) in setOf(md_theme_light_onSurface, md_theme_light_onSurface_placeholder),
                "$mode: the prompt is drawn in the light palette, not the mode's text $modeText: $color",
            )
        }
    }

    @Test
    fun `in light mode the lone pill is the fixed light tint`() = assertLonePill("light")

    @Test
    @Config(qualifiers = "w411dp-h891dp-night")
    fun `in dark mode the lone pill is the very same fixed light tint`() = assertLonePill("dark")

    @Test
    @Config(qualifiers = "w411dp-h891dp-night")
    fun `in dark mode the From and To card text, markers and prompts are the light palette's text`() {
        composeRule.setContent {
            PreviewTheme {
                composeView = LocalView.current
                tint = mapCardColors().tint
                modeText = KrailTheme.colors.onSurface
                Box(modifier = Modifier.fillMaxSize().padding(gaps.edge)) {
                    SearchStopGroupCard(
                        form = SearchStopFormModel(
                            shape = FormShape.ToAndFrom,
                            editing = ToFromField.To,
                            toPlaceholder = TO_PLACEHOLDER,
                            fromPlaceholder = FROM_PLACEHOLDER,
                            toStop = null,
                            fromStop = StopItem(stopName = "Central Station", stopId = "200060"),
                            fieldState = rememberTextFieldState(),
                            focusRequester = remember { FocusRequester() },
                            onFlowEvent = {},
                            onGoClick = {},
                            onTextChange = {},
                        ),
                        onBack = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()

        assertNotEquals(md_theme_light_onSurface, modeText, "in dark mode the page's text is not the light palette's")
        val colors = composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, useUnmergedTree = true).fetchSemanticsNode().textColors()
        assertTrue(colors.isNotEmpty(), "the card holds text")
        for (color in colors) assertEquals(md_theme_light_onSurface, color.copy(alpha = 1f), "every text in the card is the light palette's onSurface")
    }
}
