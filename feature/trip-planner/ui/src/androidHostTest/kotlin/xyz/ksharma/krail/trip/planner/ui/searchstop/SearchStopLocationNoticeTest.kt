package xyz.ksharma.krail.trip.planner.ui.searchstop

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.md_theme_dark_onSurface
import xyz.ksharma.krail.taj.theme.md_theme_dark_softLabel
import xyz.ksharma.krail.taj.theme.md_theme_dark_surface
import xyz.ksharma.krail.taj.theme.md_theme_light_softLabel
import xyz.ksharma.krail.taj.themeDecorColor
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.mapCardColors
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The location notice under the Search Stop box, read from the real tile (`docs/MAP_HOME_UX.md`, The box): the app's own info
 * tile in its Bare style, wearing the box's surface, expanded, with Allow or Settings and its own Not now. Copy, calls, the leave
 * and the return, the heights, the hairline and the touch it swallows. The notice against the box, pixel for pixel, is read in
 * `SearchStopCardLookTest`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-notnight", manifest = Config.NONE)
class SearchStopLocationNoticeTest {

    private val EM_DASH = Char(EM_DASH_CODE)

    @get:Rule
    val composeRule = createComposeRule()

    private var pillState by mutableStateOf(LocationPill.Ask)
    private var dismissedState by mutableStateOf(false)
    private var ctas = 0
    private var dismisses = 0
    private var mapTaps = 0
    private var composeView: View? = null
    private var surface = Color.Unspecified
    private var tint = Color.Unspecified
    private var edge = Color.Unspecified
    private var modeSoftLabel = Color.Unspecified
    private var themeColor = Color.Unspecified

    private fun setNotice(fontScale: Float = 1f, onGradient: Boolean = false) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                PreviewTheme {
                    composeView = LocalView.current
                    surface = KrailTheme.colors.surface
                    tint = mapCardColors().tint
                    edge = mapCardColors().chipEdge
                    modeSoftLabel = KrailTheme.colors.softLabel
                    themeColor = themeDecorColor()
                    // The map under the notice: a tap that reaches it is counted.
                    Box(modifier = Modifier.fillMaxSize().background(surface).clickable { mapTaps++ }.padding(16.dp)) {
                        LocationNotice(
                            pill = pillState,
                            dismissed = dismissedState,
                            onDismiss = {
                                dismissedState = true
                                dismisses++
                            },
                            onCtaClick = { ctas++ },
                            onGradient = onGradient,
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun boundsOf(tag: String): DpRect = composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun textBounds(text: String): DpRect = composeRule.onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun lineCountOf(text: String): Int {
        val results = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
            .config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        assertTrue(results.isNotEmpty(), "$text is text that can be measured.")
        return results.first().lineCount
    }

    private fun exists(tag: String) = composeRule.onAllNodes(androidx.compose.ui.test.hasTestTag(tag), useUnmergedTree = true)
        .fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()

    // region copy and calls

    @Test
    fun `the Ask notice reads Location is off, Turn on location to see stops near you, Not now and Allow`() {
        setNotice()

        composeRule.onNodeWithText("Location is off", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Turn on location to see stops near you", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Not now", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Allow", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Settings", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `the Settings notice has the same words and Settings as its call to action`() {
        pillState = LocationPill.OpenSettings
        setNotice()

        composeRule.onNodeWithText("Location is off", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Settings", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Allow", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `no word of the notice has a colon or an em dash`() {
        for (text in listOf(LOCATION_NOTICE_TITLE, LOCATION_NOTICE_BODY, LOCATION_NOTICE_DISMISS, LOCATION_NOTICE_CTA_ALLOW, LOCATION_NOTICE_CTA_SETTINGS)) {
            assertTrue(':' !in text && EM_DASH !in text, "$text")
        }
    }

    @Test
    fun `the call to action calls the existing callback once, and Not now dismisses and hides the notice`() {
        setNotice()

        composeRule.onNodeWithText("Allow", useUnmergedTree = true).performClick()
        assertEquals(1, ctas)
        assertEquals(0, dismisses)

        composeRule.onNodeWithText("Not now", useUnmergedTree = true).performClick()
        composeRule.waitForIdle()
        assertEquals(1, dismisses)
        assertEquals(1, ctas, "Not now is not the call to action.")
        assertTrue(!exists(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL), "The notice is gone for the visit.")
    }

    @Test
    fun `the notice leaves when the permission is granted and returns if it is lost again`() {
        setNotice()
        assertTrue(exists(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL))

        composeRule.runOnIdle { pillState = LocationPill.Hidden }
        composeRule.waitForIdle()
        assertTrue(!exists(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL), "Granted, it leaves.")

        composeRule.runOnIdle { pillState = LocationPill.OpenSettings }
        composeRule.waitForIdle()
        assertTrue(exists(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL))
    }

    @Test
    fun `the notice keeps its call to action's words while it is leaving`() {
        setNotice()
        composeRule.mainClock.autoAdvance = false

        composeRule.runOnUiThread {
            pillState = LocationPill.Hidden
            androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(60)

        assertTrue(exists(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL), "It is still drawn part way through leaving.")
        composeRule.onNodeWithText("Allow", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `a screen reader hears the tile's title and words as one node and reaches both buttons as buttons`() {
        setNotice()

        val node = composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL).fetchSemanticsNode()
        val said = node.config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString(" ") { it.text }
        for (word in listOf("Location is off", "Turn on location to see stops near you")) {
            assertTrue(word in said, "A screen reader hears $word in: $said")
        }
        composeRule.onNodeWithText("Allow").assertHasClickAction()
        composeRule.onNodeWithText("Not now").assertHasClickAction()
    }

    // endregion

    // region heights

    @Test
    fun `expanded at 411 dp and 100 percent the tile is its title, its words and one row of buttons`() {
        setNotice()

        val tile = boundsOf(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL)
        assertTrue(tile.height in 118.dp..145.dp, "about 132 dp in the picture, ${tile.height}")
        assertEquals(1, lineCountOf("Allow"))
        assertEquals(1, lineCountOf("Not now"))
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-notnight")
    fun `at 320 dp and 200 percent the buttons never break in a word, the call to action wraps whole under Not now`() {
        pillState = LocationPill.OpenSettings
        setNotice(fontScale = 2f)

        assertEquals(1, lineCountOf("Settings"), "Settings is one line, never Setti and ngs.")
        assertEquals(1, lineCountOf("Not now"))
        assertTrue(textBounds("Settings").top >= textBounds("Not now").bottom - 1.dp, "Settings drops under Not now as a whole item.")
        val tile = boundsOf(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL)
        assertTrue(tile.left >= 0.dp && tile.right <= 320.dp, "The tile stays inside the window: $tile")
    }

    // endregion

    // region fill, edge and touch

    private fun pixelAt(image: Bitmap, x: Dp, y: Dp): Color {
        val density = composeRule.density
        return Color(image.getPixel(with(density) { x.roundToPx() }, with(density) { y.roundToPx() }))
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

    private fun close(expected: Color, actual: Color) =
        abs(expected.red - actual.red) <= 0.03f && abs(expected.green - actual.green) <= 0.03f && abs(expected.blue - actual.blue) <= 0.03f

    private fun edgeAcross(expectedEdge: Color): Color {
        val tile = boundsOf(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL)
        val density = composeRule.density
        val image = drawn()
        val y = with(density) { (tile.top + tile.height / 2f).roundToPx() }
        val across = (with(density) { (tile.left - 2.dp).roundToPx() }..with(density) { (tile.left + 2.dp).roundToPx() })
            .map { Color(image.getPixel(it, y)) }
        fun distance(c: Color) = abs(c.red - expectedEdge.red) + abs(c.green - expectedEdge.green) + abs(c.blue - expectedEdge.blue)
        return across.minBy { distance(it) }
    }

    @Test
    fun `the tile is filled with the soft theme tint and has no glow`() {
        setNotice()

        val tile = boundsOf(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL)
        val image = drawn()
        assertTrue(close(tint, pixelAt(image, tile.right - 8.dp, tile.top + 8.dp)), "the fill is the Timetable's tint")
        assertNotEquals(surface, tint)
        assertTrue(close(surface, pixelAt(image, tile.left - 3.dp, tile.top + tile.height / 2f)), "nothing glows outside the tile")
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight-xxhdpi")
    fun `in light mode the tile wears the 1 dp light softLabel hairline`() {
        setNotice()

        assertTrue(close(edge, edgeAcross(edge)), "a pixel across the tile's edge is the hairline")
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun `in dark mode the tile wears no hairline on the map, and the dark softLabel one on the gradient`() {
        setNotice()

        assertTrue(!close(md_theme_dark_softLabel, edgeAcross(md_theme_dark_softLabel)), "no pixel across the tile's edge on the map is a hairline")
        assertNotEquals(modeSoftLabel, md_theme_light_softLabel, "the mode aware softLabel is the dark one")
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun `in dark mode on the gradient the tile wears the dark softLabel hairline`() {
        setNotice(onGradient = true)

        assertTrue(close(md_theme_dark_softLabel, edgeAcross(md_theme_dark_softLabel)), "a pixel across the tile's edge is the dark hairline")
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night")
    fun `in dark mode the tile is the Timetable's dark tint, with the box's cream text on it`() {
        setNotice()

        val dark = themeColor.copy(alpha = 0.45f).compositeOver(md_theme_dark_surface)
        val tile = boundsOf(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL)
        val image = drawn()
        assertTrue(close(dark, pixelAt(image, tile.right - 8.dp, tile.top + 8.dp)), "the fill is the theme at 45 percent over the dark surface")
        assertTrue(!close(tint, pixelAt(image, tile.right - 8.dp, tile.top + 8.dp)), "and it is not the light tint it was")
        assertNotEquals(surface, dark)
        for (text in listOf(LOCATION_NOTICE_TITLE, LOCATION_NOTICE_BODY, LOCATION_NOTICE_DISMISS)) {
            val results = mutableListOf<TextLayoutResult>()
            composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
                .config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
            assertEquals(md_theme_dark_onSurface, results.first().layoutInput.style.color.copy(alpha = 1f), "$text is the box's cream")
        }
    }

    @Test
    fun `a tap on the tile that misses its buttons never reaches the map under it`() {
        setNotice()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL, useUnmergedTree = true)
            .performTouchInput { click(Offset(width - 6f, 6f)) }

        assertEquals(0, mapTaps, "The tile takes the touch, so the map under it does not.")
        assertEquals(0, ctas)
    }

    // endregion

    // region the pure rules

    @Test
    fun `the notice is shown only while location is not allowed and not dismissed`() {
        assertTrue(locationNoticeShown(LocationPill.Ask, dismissed = false))
        assertTrue(locationNoticeShown(LocationPill.OpenSettings, dismissed = false))
        assertTrue(!locationNoticeShown(LocationPill.Hidden, dismissed = false))
        assertTrue(!locationNoticeShown(LocationPill.Ask, dismissed = true))
    }

    @Test
    fun `the call to action is Allow while the rider can be asked and Settings once they must go there`() {
        assertEquals("Allow", locationNoticeCta(LocationPill.Ask))
        assertEquals("Settings", locationNoticeCta(LocationPill.OpenSettings))
    }

    // endregion

    private companion object {
        /** The banned dash, spelled by its code so this file never holds the character. */
        const val EM_DASH_CODE = 0x2014
    }
}
