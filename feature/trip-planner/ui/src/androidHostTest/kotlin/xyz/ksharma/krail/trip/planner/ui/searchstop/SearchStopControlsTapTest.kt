package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import kotlin.test.assertEquals

/**
 * Who gets a tap where Options stands over the map's info icon. The map is a stand in that counts the taps it is given,
 * as the native map and its info icon are the one thing under Options. A tap on the pill or in the rest of Options' 48 dp
 * target is Options', a tap on the visible i is the map's, and the target never reaches the i. The icon is placed from
 * the same numbers the screen's ornament padding gives the library: 21 dp square, 8 dp above the bottom.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp", manifest = Config.NONE)
class SearchStopControlsTapTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val gaps = searchStopGaps()
    private var mapTaps = 0
    private var optionsTaps = 0
    private var windowHeight: Dp = 0.dp

    private fun setControls() {
        composeRule.setContent {
            PreviewTheme {
                windowHeight = with(androidx.compose.ui.platform.LocalDensity.current) {
                    LocalWindowInfo.current.containerSize.height.toDp()
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { mapTaps++ } })
                    SearchStopModeControls(
                        open = false,
                        selectedModes = persistentSetOf(),
                        onButtonClick = { optionsTaps++ },
                        onToggleMode = {},
                        pillHeight = MAP_OPTIONS_PILL_REST_HEIGHT,
                        onPillHeight = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    /** Taps at [x] dp from the left and [aboveBottom] dp above the bottom of the window, and says who got it. */
    private fun tapAt(x: Dp, aboveBottom: Dp): String {
        mapTaps = 0
        optionsTaps = 0
        val density = composeRule.density
        val position = with(density) { Offset(x.toPx(), (windowHeight - aboveBottom).toPx()) }
        composeRule.onRoot().performTouchInput { click(position) }
        composeRule.waitForIdle()
        return when {
            optionsTaps == 1 && mapTaps == 0 -> "options"
            mapTaps == 1 && optionsTaps == 0 -> "map"
            else -> "options=$optionsTaps map=$mapTaps"
        }
    }

    @Test
    fun `a tap on the pill is Options' and a tap on the visible i is the map's`() {
        setControls()
        val glyphTop = MAP_ATTRIBUTION_GLYPH_TOP
        val pillCentre = glyphTop + gaps.aboveIcon + MAP_OPTIONS_PILL_REST_HEIGHT / 2
        val glyphCentre = glyphTop - 17.5.dp / 2
        val glyphX = gaps.edge + 17.5.dp / 2

        assertEquals("options", tapAt(gaps.edge + 20.dp, pillCentre), "the pill")
        assertEquals("map", tapAt(glyphX, glyphCentre), "the centre of the visible i")
        assertEquals("map", tapAt(glyphX, glyphTop - 3.dp), "the top of the visible i, which the target must not reach")
    }

    @Test
    fun `the rest of Options' 48 dp target, above the pill and in the gap under it, is Options' and ends at the i`() {
        setControls()
        val glyphTop = MAP_ATTRIBUTION_GLYPH_TOP
        val pillTop = glyphTop + gaps.aboveIcon + MAP_OPTIONS_PILL_REST_HEIGHT

        assertEquals("options", tapAt(gaps.edge + 20.dp, pillTop + 10.dp), "above the pill, still in the 48 dp target")
        assertEquals("options", tapAt(gaps.edge + 20.dp, glyphTop + 2.dp), "in the gap between the pill and the i")
        assertEquals("map", tapAt(gaps.edge + 20.dp, glyphTop - 1.dp), "just under the target, on the i's own line")
    }

    @Test
    fun `the target overlaps the icon's own empty top edge, which is the z order over the icon's slot`() {
        setControls()
        val iconTop = MAP_ORNAMENT_LIBRARY_MARGIN + MAP_ATTRIBUTION_ICON_SIZE
        val glyphTop = MAP_ATTRIBUTION_GLYPH_TOP

        // Between the icon's top edge and the i's top the icon has nothing drawn, and the target is there.
        assertEquals("options", tapAt(gaps.edge + 8.dp, (iconTop + glyphTop) / 2f), "the icon's empty top edge")
    }
}
