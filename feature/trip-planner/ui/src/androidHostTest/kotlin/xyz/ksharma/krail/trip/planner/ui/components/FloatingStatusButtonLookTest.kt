package xyz.ksharma.krail.trip.planner.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.contrastRatio
import xyz.ksharma.krail.taj.theme.md_theme_dark_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_dark_softLabel
import xyz.ksharma.krail.taj.theme.md_theme_dark_surface
import xyz.ksharma.krail.taj.theme.md_theme_light_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_light_softLabel
import xyz.ksharma.krail.taj.theme.md_theme_light_surface
import xyz.ksharma.krail.taj.themeDecorColor
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The home map's notices (the location status and Map offline) and the location chip on the saved trips map are one
 * [FloatingStatusButton]. They follow the app mode like the Search Stop box: in light mode the light tint, the light text and a
 * light `softLabel` hairline, exactly as they were, and in dark mode the Timetable's dark tint with cream text and no hairline on
 * the map. Read from the real button on a light map ground, with the contrast of the disabled Map offline notice printed.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-notnight-xxhdpi", manifest = Config.NONE)
class FloatingStatusButtonLookTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var composeView: View? = null
    private var themeColor = Color.Unspecified

    private fun setButtons() {
        composeRule.setContent {
            PreviewTheme(backgroundColor = MAP_GROUND) {
                composeView = LocalView.current
                themeColor = themeDecorColor()
                Column(
                    modifier = Modifier.fillMaxSize().background(MAP_GROUND).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    FloatingStatusButton(text = ASK, tag = ASK_TAG, onClick = {})
                    FloatingStatusButton(text = OFFLINE, tag = OFFLINE_TAG, onClick = {}, enabled = false)
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun drawn(): Bitmap {
        var bitmap: Bitmap? = null
        composeRule.runOnUiThread {
            val view = checkNotNull(composeView)
            bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(checkNotNull(bitmap)))
        }
        return checkNotNull(bitmap)
    }

    private fun Bitmap.at(x: Dp, y: Dp): Color =
        Color(getPixel(with(composeRule.density) { x.roundToPx() }, with(composeRule.density) { y.roundToPx() }))

    private fun boundsOf(tag: String): DpRect = composeRule.onNodeWithTag(
        tag,
        useUnmergedTree = true,
    ).getUnclippedBoundsInRoot()

    private fun close(expected: Color, actual: Color, within: Float = 0.03f) =
        abs(expected.red - actual.red) <= within && abs(expected.green - actual.green) <= within &&
            abs(expected.blue - actual.blue) <= within

    private fun hex(c: Color) = "%06X".format(c.toArgb() and 0xFFFFFF)

    private fun textColorOf(text: String): Color {
        val results = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(text, useUnmergedTree = true)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.GetTextLayoutResult)
            ?.action
            ?.invoke(results)
        return results.first().layoutInput.style.color
    }

    /** The pixels across the pill's start edge, on its centre line, where a 1 dp stroke is 2 or 3 pixels wide. */
    private fun edgeStrip(tag: String, image: Bitmap): List<Color> {
        val box = boundsOf(tag)
        val density = composeRule.density
        val y = with(density) { (box.top + (box.bottom - box.top) / 2f).roundToPx() }
        val firstX = with(density) { (box.left - 2.dp).roundToPx() }
        val lastX = with(density) { (box.left + 2.dp).roundToPx() }
        return (firstX..lastX).map { Color(image.getPixel(it, y)) }
    }

    private fun assertLook(mode: String, dark: Boolean) {
        setButtons()
        val ink = if (dark) md_theme_dark_onSurface else md_theme_light_onSurface
        val edge = if (dark) md_theme_dark_softLabel else md_theme_light_softLabel
        val expectedFill = themeColor.copy(alpha = if (dark) 0.45f else 0.15f)
            .compositeOver(if (dark) md_theme_dark_surface else md_theme_light_surface)
        val image = drawn()

        val ask = boundsOf(ASK_TAG)
        val fill = image.at(ask.left + 6.dp, ask.top + (ask.bottom - ask.top) / 2f)
        val text = textColorOf(ASK).copy(alpha = 1f)
        val strip = edgeStrip(ASK_TAG, image)
        val hairline = strip.minBy { abs(it.red - edge.red) + abs(it.green - edge.green) + abs(it.blue - edge.blue) }
        println(
            "STATUSCHIP $mode fill=${hex(fill)} expected=${hex(expectedFill)} text=${hex(text)} " +
                "textOnFill=${ink.contrastRatio(expectedFill)} fillOnMap=${expectedFill.contrastRatio(MAP_GROUND)} " +
                "hairline=${hex(hairline)} wanted=${hex(edge)}",
        )
        assertTrue(
            close(expectedFill, fill),
            "$mode: the notice is the box's tint ${hex(expectedFill)}, drawn as ${hex(fill)}",
        )
        assertEquals(ink.toArgb(), text.toArgb(), "$mode: the words are the mode's ink")
        assertTrue(
            ink.contrastRatio(expectedFill) >= TEXT,
            "$mode: the words read at ${ink.contrastRatio(expectedFill)} to 1",
        )
        if (dark) {
            assertTrue(!close(edge, hairline, 0.02f), "$mode: no hairline on the map: nearest pixel ${hex(hairline)}")
            assertTrue(
                expectedFill.contrastRatio(MAP_GROUND) >= EDGE,
                "$mode: the dark notice is ${expectedFill.contrastRatio(MAP_GROUND)} to 1 off the map on its own",
            )
        } else {
            assertTrue(
                close(edge, hairline),
                "$mode: the light softLabel hairline is drawn: nearest pixel ${hex(hairline)}",
            )
        }

        // Map offline is the disabled look of the same button: the tint and the ink at 38 percent, as taj's disabled colours are.
        val container = expectedFill.copy(alpha = DISABLED_ALPHA).compositeOver(MAP_GROUND)
        val label = ink.copy(alpha = DISABLED_ALPHA).compositeOver(container)
        println(
            "STATUSCHIP $mode offline container=${hex(container)} label=${hex(label)} labelOnContainer=${label.contrastRatio(container)} " +
                "containerOnMap=${container.contrastRatio(MAP_GROUND)}",
        )
    }

    @Test
    fun `in light mode the notices are the light tint with the light text and hairline, as they were`() = assertLook(
        "light",
        dark = false,
    )

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun `in dark mode the notices are the dark tint with cream text and no hairline on the map`() = assertLook(
        "dark",
        dark = true,
    )

    private companion object {
        const val ASK = "Location permission required"
        const val OFFLINE = "Map offline"
        const val ASK_TAG = "status.ask"
        const val OFFLINE_TAG = "status.offline"
        const val TEXT = 4.5f
        const val EDGE = 3f
        const val DISABLED_ALPHA = 0.38f
        val MAP_GROUND = Color(0xFFEDEBE4)
    }
}
