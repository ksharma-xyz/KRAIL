package xyz.ksharma.krail.trip.planner.ui.searchstop

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
import xyz.ksharma.krail.taj.theme.md_theme_light_surface
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.mapCardColors
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapDisplay
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import kotlin.math.abs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Every state of the real Search Stop screen wears the one fixed light card look, in light and dark app mode: the map state's box, the
 * location notice under it, the white Options pill and the Options card, the list state's box once the field is tapped, and the
 * box in a wide window's left pane. Each is read from the pixels the screen draws, against the tint of the card family read from
 * the same tokens, and the dark runs also prove the page behind is not that tint.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel6, manifest = Config.NONE)
class SearchStopScreenFixedLightTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val quietKeyboard = object : SoftwareKeyboardController {
        override fun show() = Unit
        override fun hide() = Unit
    }
    private var composeView: View? = null
    private var tint = Color.Unspecified
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
                    tint = mapCardColors().tint
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

    /** The card's own fill, 3 dp inside its left edge (clear of the 1 dp hairline and of the marker column), at its middle. */
    private fun assertFilledWithTint(what: String, tag: String, image: Bitmap) {
        val block = boundsOf(tag)
        val at = pixel(image, block.left + 3.dp, block.cy())
        assertTrue(close(tint, at), "$what is the card family's fixed light tint $tint, drawn as $at")
    }

    // region the map state

    private fun assertMapState(mode: String) {
        setScreen()
        val image = drawn()
        assertFilledWithTint("$mode, the box", TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, image)
        val pill = boundsOf(TripPlannerTestTags.SEARCH_STOP_MODE_PILL)
        assertTrue(close(md_theme_light_surface, pixel(image, pill.left + 7.dp, pill.cy())), "$mode, the Options pill is white")

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON).performClick()
        composeRule.waitForIdle()
        assertFilledWithTint("$mode, the Options card", TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS, drawn())
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight-xxhdpi")
    fun `in light mode the map state's box, Options pill and Options card are the fixed light look`() = assertMapState("light")

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun `in dark mode the map state's box, Options pill and Options card are the very same fixed light look`() {
        assertMapState("dark")
        assertNotEquals(pageSurface, tint, "the page behind them is the dark surface")
    }

    private fun assertNotice(mode: String) {
        setScreen(locationAllowed = false)
        val image = drawn()
        assertFilledWithTint("$mode, the box", TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, image)
        assertFilledWithTint("$mode, the location notice", TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL, image)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight-xxhdpi")
    fun `in light mode the location notice is the box's own tint`() = assertNotice("light")

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun `in dark mode the location notice is the box's own fixed light tint`() = assertNotice("dark")

    @Test
    @Config(qualifiers = "w320dp-h900dp-night-xxhdpi")
    fun `at 320 dp and 200 percent text in dark mode the box, notice and map controls all stay in the window`() {
        setScreen(fontScale = 2f, locationAllowed = false)

        val image = drawn()
        assertFilledWithTint("the box", TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, image)
        assertFilledWithTint("the notice", TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL, image)
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
        assertFilledWithTint("$mode, the list state's box", TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, drawn())
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight-xxhdpi")
    fun `in light mode the list state's box is the fixed light tint`() = assertListState("light")

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun `in dark mode the list state's box is the very same fixed light tint`() = assertListState("dark")

    private fun assertWideWindow(mode: String) {
        setScreen()

        assertFilledWithTint("$mode, the box in the wide window's left pane", TripPlannerTestTags.SEARCH_STOP_GROUP_CARD, drawn())
    }

    @Test
    @Config(qualifiers = "w900dp-h700dp-notnight-xxhdpi")
    fun `in light mode the box in a wide window is the fixed light tint`() = assertWideWindow("light")

    @Test
    @Config(qualifiers = "w900dp-h700dp-night-xxhdpi")
    fun `in dark mode the box in a wide window is the very same fixed light tint`() = assertWideWindow("dark")

    // endregion
}
