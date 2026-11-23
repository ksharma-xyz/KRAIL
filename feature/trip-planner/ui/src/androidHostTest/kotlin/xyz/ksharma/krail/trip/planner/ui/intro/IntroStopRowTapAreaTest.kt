package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.ThemeController
import xyz.ksharma.krail.taj.theme.ThemeMode
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The tap area, the ripple and the tint of a stop row are one shape: the whole row, from the room above its name to the room under
 * its icons. The tint was a background with 16 dp of room under the icons that the click did not cover, so the ripple stopped 16 dp
 * short of the bottom of the tint and a tap in that strip did nothing. The test draws real rows, one after the other as the list
 * does, and reads the tint out of the drawn pixels.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroStopRowTapAreaTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val stops = listOf(
        IntroStop("200060", "Central Station"),
        IntroStop("200080", "Wynyard Station"),
        IntroStop("200070", "Town Hall Station"),
    )
    private val selected = mutableStateOf<String?>(stops[1].stopId)
    private val taps = mutableListOf<String>()
    private var surface = Color.Unspecified

    private fun show(select: String? = stops[1].stopId) {
        selected.value = select
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                KrailTheme(themeController = ThemeController(currentMode = ThemeMode.LIGHT, setThemeMode = {})) {
                    // The Intro draws a screen in its own colour, and the app's theme is another one, so a tint of the wrong one fails.
                    val themeColour = remember { mutableStateOf(KrailThemeStyle.Train.hexColorCode) }
                    CompositionLocalProvider(LocalThemeColor provides themeColour) {
                        surface = KrailTheme.colors.surface
                        IntroScreenColours(IntroStep.DESTINATION) {
                            Column(modifier = Modifier.testTag(LIST).fillMaxWidth().background(KrailTheme.colors.surface)) {
                                stops.forEachIndexed { index, stop ->
                                    IntroStopRow(
                                        stop = stop,
                                        modes = persistentListOf(TransportMode.Train, TransportMode.LightRail),
                                        selected = stop.stopId == selected.value,
                                        onClick = {
                                            taps += stop.stopId
                                            selected.value = stop.stopId
                                        },
                                        modifier = if (index == 0) Modifier.testTag(TripPlannerTestTags.INTRO_STOP_FIRST) else Modifier,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun row(stop: IntroStop) = composeRule.onNodeWithTag(TripPlannerTestTags.introStop(stop.stopId))

    private fun bounds(stop: IntroStop): Rect = row(stop).fetchSemanticsNode().boundsInRoot

    /**
     * Where the row of [stop] is in the list, from the list and not from the row's own node: the three rows have the same content,
     * so each is a third of the list's height, one after another. A tap is placed by this, so it lands where the rider sees the row.
     */
    private fun tile(stop: IntroStop): Rect {
        val list = composeRule.onNodeWithTag(LIST).fetchSemanticsNode().boundsInRoot
        val height = list.height / stops.size
        val top = list.top + stops.indexOf(stop) * height
        return Rect(list.left, top, list.right, top + height)
    }

    private fun px(dp: Float) = with(composeRule.density) { dp.dp.toPx() }

    private fun close(a: Color, b: Color, tolerance: Float = 0.012f) =
        abs(a.red - b.red) <= tolerance && abs(a.green - b.green) <= tolerance && abs(a.blue - b.blue) <= tolerance

    private fun picture() = IntroPixels(composeRule).of(LIST)

    /** The top and bottom of the run of pixels in [tint] down the column at [x] that crosses [y], so how far the tint reaches. */
    private fun tintExtent(x: Float, y: Float, tint: Color): Pair<Int, Int> {
        val image = picture()
        fun at(row: Int) = Color(image.getRGB(x.toInt(), row))
        var top = y.toInt()
        while (top > 0 && close(at(top - 1), tint)) top--
        var bottom = y.toInt()
        while (bottom < image.height - 1 && close(at(bottom + 1), tint)) bottom++
        return top to (bottom + 1)
    }

    // A column to the right of everything in the row, past the rounded corner, where only the tint is drawn.
    private fun emptyColumn(stop: IntroStop) = bounds(stop).right - px(24f)

    private fun tintOfSelected(): Color {
        val box = bounds(stops[1])
        return Color(picture().getRGB((box.right - px(24f)).toInt(), box.center.y.toInt()))
    }

    @Test
    fun `the node with the click is the whole tinted row, so the tint and the tap area have the same top and bottom`() {
        show()
        val tint = tintOfSelected()
        assertTrue(!close(tint, surface), "the selected row is not tinted: $tint on $surface")
        val box = bounds(stops[1])

        val (top, bottom) = tintExtent(emptyColumn(stops[1]), box.center.y, tint)

        assertEquals(box.top.toInt(), top, "the tint starts at $top and the tap area at ${box.top}")
        assertEquals(box.bottom.toInt(), bottom, "the tint ends at $bottom and the tap area at ${box.bottom}")
        row(stops[1]).assertHasClickAction()
    }

    @Test
    fun `the tap area is as wide as the tint, which is edge to edge`() {
        show()
        val box = bounds(stops[1])
        val root = composeRule.onNodeWithTag(LIST).fetchSemanticsNode().boundsInRoot

        assertEquals(root.left, box.left, "the tap area starts inside the list's edge")
        assertEquals(root.right, box.right, "the tap area ends inside the list's edge")
    }

    @Test
    fun `a tap in the bottom 16 dp of a row selects it, and so does a tap in its top margin`() {
        show(select = null)
        val first = tile(stops[0])

        composeRule.onRoot().performTouchInput { click(Offset(first.center.x, first.bottom - px(4f))) }
        composeRule.waitForIdle()
        assertEquals(listOf(stops[0].stopId), taps, "a tap 4 dp above the bottom of the first row did nothing")
        row(stops[0]).assertIsSelected()

        val second = tile(stops[1])
        composeRule.onRoot().performTouchInput { click(Offset(second.center.x, second.top + px(3f))) }
        composeRule.waitForIdle()
        assertEquals(listOf(stops[0].stopId, stops[1].stopId), taps, "a tap 3 dp below the top of the second row did nothing")
        row(stops[1]).assertIsSelected()
    }

    @Test
    fun `every point down the row, from its first pixel to its last, selects that row and no other, outside the rounded corners`() {
        show(select = null)
        stops.forEach { stop ->
            val box = tile(stop)
            var y = box.top + 1f
            while (y < box.bottom - 1f) {
                taps.clear()
                composeRule.onRoot().performTouchInput { click(Offset(box.right - px(16f), y)) }
                composeRule.waitForIdle()
                assertEquals(listOf(stop.stopId), taps, "a tap at ${y - box.top} px down ${stop.name} selected $taps")
                y += px(2f)
            }
        }
    }

    @Test
    fun `two rows share no pixel and leave no gap between them, so a finger between two rows is on one of them`() {
        show()
        val boxes = stops.map { bounds(it) }

        boxes.zipWithNext().forEach { (above, below) ->
            assertEquals(above.bottom, below.top, "there is a strip between two rows, or they overlap")
        }
        assertTrue(boxes.all { it.height > 0f })
        val strip = (boxes[0].bottom - px(0.5f))
        taps.clear()
        composeRule.onRoot().performTouchInput { click(Offset(boxes[0].center.x, strip)) }
        composeRule.waitForIdle()
        assertEquals(listOf(stops[0].stopId), taps)
    }

    @Test
    fun `one node announces the row, with its name, that it is a button and whether it is selected`() {
        show()

        val selectedNodes = composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected))
            .fetchSemanticsNodes()
        assertEquals(stops.size, selectedNodes.size, "one node a row says whether it is selected")
        assertEquals(1, selectedNodes.count { it.config[SemanticsProperties.Selected] })
        row(stops[1]).assertIsSelected()
        val clickable = composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        assertEquals(stops.size, clickable.size, "one button a row")
        stops.forEach { stop ->
            val node = row(stop).fetchSemanticsNode()
            assertTrue(
                node.config[SemanticsProperties.Text].any { it.text == stop.name },
                "the node of ${stop.name} does not carry the name",
            )
        }
        // The tag of the first row is on a node with no click of its own, as it always was, so it is the row's tag and not a second button.
        composeRule.onAllNodes(hasTestTag(TripPlannerTestTags.INTRO_STOP_FIRST)).fetchSemanticsNodes().forEach {
            assertTrue(SemanticsProperties.Selected !in it.config)
        }
    }

    @Test
    fun `the first row's tag selects it, as the Maestro flow taps it`() {
        show(select = null)

        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_STOP_FIRST).performTouchInput { click() }
        composeRule.waitForIdle()

        assertEquals(listOf(stops[0].stopId), taps)
        row(stops[0]).assertIsSelected()
    }

    @Test
    fun `an unselected row is as tall as the same row selected, so the list does not move when a row is picked`() {
        show(select = null)
        val before = stops.map { bounds(it) }

        composeRule.onRoot().performTouchInput { click(Offset(before[1].center.x, before[1].center.y)) }
        composeRule.waitForIdle()

        assertEquals(before, stops.map { bounds(it) })
    }


    private companion object {
        const val LIST = "intro.rows.list"
    }
}
