package xyz.ksharma.krail.trip.planner.ui.searchstop

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.isAppInDarkMode
import xyz.ksharma.krail.taj.theme.md_theme_dark_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_dark_surface
import xyz.ksharma.krail.taj.theme.md_theme_light_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_light_surface
import xyz.ksharma.krail.taj.themeDecorColor
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapDisplay
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import kotlin.math.abs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The card look of each state of the real Search Stop screen. The box and the location notice follow the app mode in the map state
 * and in the list state alike: the same light look in light mode and the Timetable's dark tint with cream text in dark mode, in both
 * states, so a rider who taps the box and leaves the map sees no change of colour. The Options card follows the app mode the same
 * way, and the white Options pill is a control and keeps its own look. Each is read from the pixels the screen draws, against the
 * expected tint built from the taj tokens, and the screen is taken from the map state to the list state and back in dark mode.
 * This replaces the S7 rule that the map state is fixed light in both modes, and the S9 rule that the list state alone follows
 * the app mode.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel6, manifest = Config.NONE)
class SearchStopScreenCardLookTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val quietKeyboard = object : SoftwareKeyboardController {
        override fun show() = Unit
        override fun hide() = Unit
    }
    private var composeView: View? = null
    private var lightTint = Color.Unspecified
    private var listTint = Color.Unspecified
    private var cream = Color.Unspecified
    private var pageSurface = Color.Unspecified

    private fun setScreen(fontScale: Float = 1f, locationAllowed: Boolean = true) {
        val display = if (locationAllowed) MapDisplay(userLocation = LatLng(-33.8688, 151.2093)) else MapDisplay()
        val state: SearchStopState = SearchStopFixtures.recentWithHomeSet()
            .copy(isMapsAvailable = true, mapUiState = MapUiState.Ready(mapDisplay = display))
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale),
                LocalInspectionMode provides true,
                LocalSoftwareKeyboardController provides quietKeyboard,
            ) {
                PreviewTheme {
                    composeView = LocalView.current
                    // The light tint, built from the taj tokens: the Timetable's 15 percent over white, the colour light mode always was.
                    lightTint = themeDecorColor().copy(alpha = 0.15f).compositeOver(md_theme_light_surface)
                    // The list state's tint, built from the taj tokens: the Timetable's 45 percent over the dark surface in dark mode and
                    // 15 percent over white in light mode.
                    listTint = if (isAppInDarkMode()) {
                        themeDecorColor().copy(alpha = 0.45f).compositeOver(md_theme_dark_surface)
                    } else {
                        themeDecorColor().copy(alpha = 0.15f).compositeOver(md_theme_light_surface)
                    }
                    cream = if (isAppInDarkMode()) md_theme_dark_onSurface else md_theme_light_onSurface
                    pageSurface = KrailTheme.colors.surface
                    SearchStopScreen(searchStopState = state, onEvent = {})
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun boundsOf(tag: String): DpRect = composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

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

    private fun DpRect.cy() = top + (bottom - top) / 2f

    /** The box's and the notice's fill, on the map and on the gradient: the tint of the app mode, the light tint in light mode. */
    private fun assertFilledWithListTint(what: String, tag: String, image: Bitmap) {
        val block = boundsOf(tag)
        val at = pixel(image, block.left + 3.dp, block.cy())
        assertTrue(close(listTint, at), "$what is the app mode's own tint $listTint, drawn as $at")
    }

    /** Every text the box holds is the app mode's own text colour, cream in dark mode. */
    private fun assertTextIsModeInk(what: String, tag: String) {
        val node = composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        val colors = mutableListOf<TextLayoutResult>()
        fun collect(n: SemanticsNode) {
            n.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(colors)
            n.children.forEach { collect(it) }
        }
        collect(node)
        assertTrue(colors.isNotEmpty(), "$what holds text")
        for (layout in colors) {
            assertTrue(close(cream, layout.layoutInput.style.color.copy(alpha = 1f)), "$what text is $cream, drawn as ${layout.layoutInput.style.color}")
        }
    }

    // region the map state

    private fun assertMapState(mode: String) {
        setScreen()
        val image = drawn()
        assertFilledWithListTint("$mode, the box", TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, image)
        val pill = boundsOf(TripPlannerTestTags.SEARCH_STOP_MODE_PILL)
        assertTrue(close(md_theme_light_surface, pixel(image, pill.left + 7.dp, pill.cy())), "$mode, the Options pill is white")

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON).performClick()
        composeRule.waitForIdle()
        assertFilledWithListTint("$mode, the Options card", TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS, drawn())
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight-xxhdpi")
    fun `in light mode the map state's box and Options card are the light tint and the Options pill is white`() = assertMapState("light")

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun `in dark mode the map state's box and Options card are the dark tint and the Options pill is still white`() {
        assertMapState("dark")
        assertNotEquals(pageSurface, listTint, "the box is not the page's own surface")
        assertTrue(!close(lightTint, listTint), "the box on the map is the dark tint $listTint, not the light tint $lightTint")
    }

    private fun assertNotice(mode: String) {
        setScreen(locationAllowed = false)
        val image = drawn()
        assertFilledWithListTint("$mode, the box", TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, image)
        assertFilledWithListTint("$mode, the location notice", TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL, image)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight-xxhdpi")
    fun `in light mode the location notice is the box's own tint`() = assertNotice("light")

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun `in dark mode the location notice is the box's own dark tint`() = assertNotice("dark")

    @Test
    @Config(qualifiers = "w320dp-h900dp-night-xxhdpi")
    fun `at 320 dp and 200 percent text in dark mode the box, notice and map controls all stay in the window`() {
        setScreen(fontScale = 2f, locationAllowed = false)

        val image = drawn()
        assertFilledWithListTint("the box", TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, image)
        assertFilledWithListTint("the notice", TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL, image)
        for (tag in listOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL, TripPlannerTestTags.SEARCH_STOP_LOCATION_BUTTON)) {
            val block = boundsOf(tag)
            assertTrue(block.left >= 0.dp && block.right <= 320.dp, "$tag $block stays inside the 320 dp window")
        }
    }

    // endregion

    // region the list state and the wide window

    private fun assertListState(mode: String) {
        setScreen()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_QUERY_FIELD).performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON).assertCountEquals(0)
        assertFilledWithListTint("$mode, the list state's box", TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, drawn())
        assertTextIsModeInk("$mode, the list state's box", TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight-xxhdpi")
    fun `in light mode the list state's box is the light tint it was`() {
        assertListState("light")
        assertTrue(close(lightTint, listTint), "in light mode the list state's tint is the light tint it was")
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun `in dark mode the list state's box is the Timetable's dark tint with cream text, not the light tint`() {
        assertListState("dark")
        assertTrue(!close(lightTint, listTint), "the dark tint $listTint is not the light tint $lightTint")
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun `in dark mode going from the map state to the list state and back keeps the box the same dark tint`() {
        setScreen()
        assertFilledWithListTint("dark, the map state's box", TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, drawn())
        assertTextIsModeInk("dark, the map state's box", TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_QUERY_FIELD).performClick()
        composeRule.waitForIdle()
        assertFilledWithListTint("dark, the list state's box", TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, drawn())
        assertTextIsModeInk("dark, the list state's box", TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON).assertCountEquals(1)
        assertFilledWithListTint("dark, back on the map state, the box", TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, drawn())
        assertTextIsModeInk("dark, back on the map state, the box", TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
    }

    private fun assertWideWindow(mode: String) {
        setScreen()

        assertFilledWithListTint("$mode, the box in the wide window's left pane", TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, drawn())
        assertTextIsModeInk("$mode, the box in the wide window's left pane", TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
    }

    @Test
    @Config(qualifiers = "w900dp-h700dp-notnight-xxhdpi")
    fun `in light mode the box in a wide window is the light tint`() = assertWideWindow("light")

    @Test
    @Config(qualifiers = "w900dp-h700dp-night-xxhdpi")
    fun `in dark mode the box in a wide window, which stands on the gradient, is the dark tint with cream text`() {
        assertWideWindow("dark")
        assertTrue(!close(lightTint, listTint), "the dark tint $listTint is not the light tint $lightTint")
    }

    // endregion
}
