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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
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
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.contrastRatio
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.taj.theme.md_theme_dark_softLabel
import xyz.ksharma.krail.taj.theme.md_theme_dark_surface
import xyz.ksharma.krail.taj.theme.md_theme_light_softLabel
import xyz.ksharma.krail.taj.theme.md_theme_light_surface
import xyz.ksharma.krail.taj.themeDecorColor
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Options card with its chips follows the app mode like the box does: in light mode the theme at 15 percent over white with a
 * light `softLabel` hairline and white chips that are off, exactly as it was, and in dark mode the Timetable's dark tint with no
 * hairline on the map and the app's own dark chips, the page surface with the mode colour as the outline. Drawn from the real
 * component and read from its pixels, once in light and once in dark app mode, with the contrast of the chips printed.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-notnight-xxhdpi", manifest = Config.NONE)
class SearchStopOptionsCardLookTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var composeView: View? = null
    private var themeColor = Color.Unspecified
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

    private fun hex(c: Color) = "%06X".format(c.toArgb() and 0xFFFFFF)

    private fun DpRect.width() = right - left

    private fun setOptionsCard(selected: Set<Int>) {
        composeRule.setContent {
            PreviewTheme {
                composeView = LocalView.current
                themeColor = themeDecorColor()
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

    /** The pixels across the card's left edge, where a 1 dp stroke is 2 or 3 pixels wide. */
    private fun edgeStrip(image: Bitmap): List<Color> {
        val box = boundsOf(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS)
        val density = composeRule.density
        val y = with(density) { (box.top + (box.bottom - box.top) / 2f).roundToPx() }
        return (with(density) { (box.left - 2.dp).roundToPx() }..with(density) { (box.left + 2.dp).roundToPx() })
            .map { Color(image.getPixel(it, y)) }
    }

    /** The lightest pixel across the edge: in dark mode a drawn hairline raises it well above the dark card and the dark page. */
    private fun edgeStrength(image: Bitmap): Float = edgeStrip(image).maxOf { it.luminance() }

    private fun assertOptionsCard(mode: String, dark: Boolean) {
        val ferry = NswTransportConfig.productClassFor(TransportMode.Ferry)
        setOptionsCard(selected = emptySet<Int>() + ferry)

        val block = boundsOf(TripPlannerTestTags.SEARCH_STOP_MODE_CHIPS)
        val image = drawn()
        val expectedCard = themeColor.copy(alpha = if (dark) 0.45f else 0.15f)
            .compositeOver(if (dark) md_theme_dark_surface else md_theme_light_surface)
        // The card's own padding, clear of every chip and of the hairline.
        val card = pixel(image, block.left + 24.dp, block.top + 6.dp)
        assertTrue(close(expectedCard, card), "$mode: the Options card is the box's tint ${hex(expectedCard)}, drawn as ${hex(card)}")
        // A chip that is off is the app's own surface with its mode colour as its outline: white in light mode, the page surface in dark.
        val unselected = boundsOf(TripPlannerTestTags.searchStopModeChip(NswTransportConfig.productClassFor(TransportMode.Train)))
        val offFill = pixel(image, unselected.right - 10.dp, unselected.top + (unselected.bottom - unselected.top) / 2f)
        val offFillTop = pixel(image, unselected.left + unselected.width() / 2f, unselected.top + 5.dp)
        assertTrue(close(pageSurface, offFill) || close(pageSurface, offFillTop), "$mode: an unselected chip is filled with the app's surface ${hex(pageSurface)}")
        // A chip that is on is filled with its mode colour.
        val selected = boundsOf(TripPlannerTestTags.searchStopModeChip(ferry))
        val ferryColor = NswTransportConfig.colorFor(TransportMode.Ferry).hexToComposeColor()
        val onFill = pixel(image, selected.right - 10.dp, selected.top + (selected.bottom - selected.top) / 2f)
        assertTrue(close(ferryColor, onFill), "$mode: a chip that is on is the mode colour ${hex(ferryColor)}, drawn as ${hex(onFill)}")

        val strength = edgeStrength(image)
        println(
            "OPTIONSCARD $mode card=${hex(card)} offChip=${hex(pageSurface)} onChip=${hex(ferryColor)} " +
                "offChipOnCard=${pageSurface.contrastRatio(expectedCard)} onChipOnCard=${ferryColor.contrastRatio(expectedCard)} " +
                "offLabelOnOffChip=${Color.Gray.contrastRatio(pageSurface)} onLabelOnOnChip=${getForegroundColor(ferryColor).contrastRatio(ferryColor)} " +
                "hairlineStrength=$strength",
        )
        if (dark) {
            assertTrue(strength < HAIRLINE_PRESENT, "$mode: no hairline on the map, strength $strength")
            assertTrue(md_theme_dark_softLabel.luminance() > HAIRLINE_PRESENT, "the dark softLabel would have been visible")
            assertTrue(Color.Gray.contrastRatio(pageSurface) >= TEXT, "$mode: the off label reads on the dark chip: ${Color.Gray.contrastRatio(pageSurface)} to 1")
            assertTrue(getForegroundColor(ferryColor).contrastRatio(ferryColor) >= TEXT, "$mode: the on label reads on its mode colour")
            assertEquals(md_theme_dark_surface.toArgb(), pageSurface.toArgb(), "the chips are filled with the dark page surface")
        } else {
            assertTrue(edgeStrip(image).any { close(md_theme_light_softLabel, it) }, "$mode: the light softLabel hairline is drawn across the edge")
            assertEquals(md_theme_light_surface.toArgb(), pageSurface.toArgb(), "the chips that are off are white, as they were")
        }
    }

    @Test
    fun `in light mode the Options card is the light tint with a hairline and white chips that are off, as it was`() = assertOptionsCard("light", dark = false)

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun `in dark mode the Options card is the dark tint with no hairline and the app's own dark chips`() = assertOptionsCard("dark", dark = true)

    private companion object {
        const val TEXT = 4.5f

        /** Luminance above which a pixel across the edge can only be a hairline: the dark box, the dark chips and the page are all far below it. */
        const val HAIRLINE_PRESENT = 0.3f
    }
}
