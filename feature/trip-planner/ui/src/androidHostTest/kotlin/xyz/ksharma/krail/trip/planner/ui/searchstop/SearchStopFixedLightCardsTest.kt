package xyz.ksharma.krail.trip.planner.ui.searchstop

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
import xyz.ksharma.krail.taj.theme.md_theme_light_surface
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.mapCardColors
import kotlin.math.abs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The Options card with its chips is still the fixed light look whatever the app mode is: the theme at 15 percent over white with
 * the light palette's text, drawn from the real component and read from its pixels, once in light and once in dark app mode. The
 * From and To card and the bare single pill follow the app mode and are read in `SearchStopCardLookTest`.
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

    private fun setOptionsCard(selected: Set<Int>) {
        composeRule.setContent {
            PreviewTheme {
                composeView = LocalView.current
                tint = mapCardColors().tint
                pageSurface = KrailTheme.colors.surface
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
}
