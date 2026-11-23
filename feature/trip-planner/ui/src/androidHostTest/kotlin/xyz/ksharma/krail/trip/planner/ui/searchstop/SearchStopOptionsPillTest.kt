package xyz.ksharma.krail.trip.planner.ui.searchstop

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.transport.ModeSelectionSurface
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.md_theme_light_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_light_softLabel
import xyz.ksharma.krail.taj.theme.md_theme_light_surface
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.mapActiveChipColors
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.MapControlsLayer
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The Options pill, read from the real control line (`docs/MAP_HOME_UX.md`, rule 39): taj's medium button as shipped, white with
 * the light palette's text and a 1 dp light `softLabel` hairline in both app modes, on one centre line with the 48 dp location
 * control at every text size, and the solid theme colour while a transport mode is switched off.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-notnight-xxhdpi", manifest = Config.NONE)
class SearchStopOptionsPillTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val gaps = searchStopGaps()
    private val allModes: ImmutableSet<Int> = NswTransportConfig.modesFor(ModeSelectionSurface.NEARBY_STOPS)
        .map { NswTransportConfig.productClassFor(it) }.toImmutableSet()

    private var measuredPill by mutableStateOf(MAP_OPTIONS_PILL_REST_HEIGHT)
    private var selected by mutableStateOf(allModes)
    private var composeView: View? = null
    private var pageSurface = Color.Unspecified
    private var modeText = Color.Unspecified
    private var activeFill = Color.Unspecified

    private fun setControls(fontScale: Float = 1f) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                PreviewTheme {
                    composeView = LocalView.current
                    pageSurface = KrailTheme.colors.surface
                    modeText = KrailTheme.colors.onSurface
                    activeFill = mapActiveChipColors().containerColor
                    Box(modifier = Modifier.fillMaxSize()) {
                        MapControlsLayer(
                            showsLocationButton = true,
                            isLocationActive = true,
                            onLocationButtonClick = {},
                            cardOpen = false,
                            cardHeight = 0.dp,
                            hidesControlsForCard = true,
                            locationBottomPadding = { clearance -> locationControlBottomPadding(gaps, measuredPill, clearance) },
                            bottomControls = { clearance, controlsHidden ->
                                SearchStopModeControls(
                                    open = false,
                                    selectedModes = selected,
                                    onButtonClick = {},
                                    onToggleMode = {},
                                    pillHeight = measuredPill,
                                    onPillHeight = { measuredPill = it },
                                    clearance = clearance,
                                    visible = !controlsHidden,
                                )
                            },
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun boundsOf(tag: String): DpRect = composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()
    private fun DpRect.cy() = top + (bottom - top) / 2f
    private fun DpRect.h() = bottom - top

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

    private fun assertNear(what: String, expected: Dp, actual: Dp, within: Float = 0.75f) {
        assertTrue(abs((expected - actual).value) <= within, "$what: expected $expected, got $actual")
    }

    private fun SemanticsNode.textColors(): List<Color> {
        val own = mutableListOf<TextLayoutResult>().also { config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(it) }
        return own.map { it.layoutInput.style.color } + children.flatMap { it.textColors() }
    }

    // region height

    private fun pillHeightAt(scale: Float): Dp {
        setControls(fontScale = scale)
        val height = boundsOf(TripPlannerTestTags.SEARCH_STOP_MODE_PILL).h()
        println("OPTIONSPILL height at $scale = $height (the control line centre uses the measured height, $measuredPill)")
        return height
    }

    @Test
    fun `at 100 percent text the pill is taj's medium button, 32 dp tall, taller than the extra small one it replaces`() {
        val height = pillHeightAt(1f)

        assertNear("medium is 32 dp at 100 percent text", 32.dp, height)
        assertEquals(32.dp, MAP_OPTIONS_PILL_REST_HEIGHT, "The rest height is what is measured, so the first frame is already where it ends up.")
        assertTrue(height > 20.dp, "It was 20 dp as an extra small button.")
    }

    @Test
    fun `at 130 and 150 percent text the pill grows with the text and is never shorter than at 100 percent`() {
        setControls(fontScale = 1.3f)
        val at130 = boundsOf(TripPlannerTestTags.SEARCH_STOP_MODE_PILL).h()
        println("OPTIONSPILL height at 1.3 = $at130")
        assertTrue(at130 > 32.dp, "At 130 percent the pill is taller than 32 dp: $at130")
    }

    @Test
    fun `at 150 percent text the pill is taller still`() {
        setControls(fontScale = 1.5f)
        val at150 = boundsOf(TripPlannerTestTags.SEARCH_STOP_MODE_PILL).h()
        println("OPTIONSPILL height at 1.5 = $at150")
        assertTrue(at150 > 34.dp, "At 150 percent the pill is taller than at 130: $at150")
    }

    // endregion

    // region one centre line

    private fun assertOneCentreLine(scale: Float) {
        setControls(fontScale = scale)
        val pill = boundsOf(TripPlannerTestTags.SEARCH_STOP_MODE_PILL)
        val location = boundsOf(TripPlannerTestTags.SEARCH_STOP_LOCATION_BUTTON)
        assertNear("at $scale the Options pill and the 48 dp location control share one centre line", pill.cy(), location.cy())
        assertNear("the location control is 48 dp", 48.dp, location.h())
        assertNear("the measured pill height is the one the line is built from", pill.h(), measuredPill)
        val target = boundsOf(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON)
        assertTrue(target.h() >= 48.dp - 0.5.dp, "Options keeps a 48 dp finger target: ${target.h()}")
        println("OPTIONSPILL scale=$scale pill=${pill.h()} centre=${pill.cy()} location=${location.cy()} target=${target.h()}")
    }

    @Test
    fun `at 100 percent text the pill and the location control are on one centre line`() = assertOneCentreLine(1f)

    @Test
    fun `at 130 percent text the pill and the location control are on one centre line`() = assertOneCentreLine(1.3f)

    @Test
    fun `at 150 percent text the pill and the location control are on one centre line`() = assertOneCentreLine(1.5f)

    // endregion

    // region colours

    private fun assertWhitePill(mode: String) {
        setControls()
        val pill = boundsOf(TripPlannerTestTags.SEARCH_STOP_MODE_PILL)
        val image = drawn()
        // Left of the icon, inside the rounded end and clear of the hairline.
        assertTrue(close(md_theme_light_surface, pixel(image, pill.left + 7.dp, pill.cy())), "$mode: the resting pill is the light palette's white")
        val colors = composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_PILL, useUnmergedTree = true).fetchSemanticsNode().textColors()
        assertTrue(colors.isNotEmpty(), "$mode: the pill's label can be read")
        for (color in colors) assertEquals(md_theme_light_onSurface, color.copy(alpha = 1f), "$mode: the label is the light palette's onSurface")
        // The hairline: a pixel across the pill's left edge is the light softLabel.
        val density = composeRule.density
        val y = with(density) { pill.cy().roundToPx() }
        val across = (with(density) { (pill.left - 1.dp).roundToPx() }..with(density) { (pill.left + 2.dp).roundToPx() }).map { Color(image.getPixel(it, y)) }
        assertTrue(across.any { close(md_theme_light_softLabel, it) }, "$mode: the 1 dp light softLabel hairline is drawn across the pill's edge")
    }

    @Test
    fun `in light mode the resting pill is white with the light text and a light softLabel hairline`() = assertWhitePill("light")

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun `in dark mode the resting pill is the very same white, not the dark surface`() {
        assertWhitePill("dark")
        assertNotEquals(md_theme_light_surface, pageSurface, "the page behind it is the dark surface")
        assertNotEquals(md_theme_light_onSurface, modeText, "and the page's text is not the light palette's")
    }

    @Test
    fun `while a mode is switched off the pill is the solid theme colour, the active filter look, in both app modes`() {
        selected = persistentSetOf()
        setControls()

        val pill = boundsOf(TripPlannerTestTags.SEARCH_STOP_MODE_PILL)
        assertTrue(close(activeFill, pixel(drawn(), pill.left + 7.dp, pill.cy())), "the active filter look is the theme colour")
        assertNotEquals(md_theme_light_surface, activeFill)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun `in dark mode too the active filter look is the solid theme colour`() {
        selected = persistentSetOf()
        setControls()

        val pill = boundsOf(TripPlannerTestTags.SEARCH_STOP_MODE_PILL)
        assertTrue(close(activeFill, pixel(drawn(), pill.left + 7.dp, pill.cy())))
    }

    // endregion
}
