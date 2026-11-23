package xyz.ksharma.krail.trip.planner.ui.searchstop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.provider.Settings
import android.view.View
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.FROM_PLACEHOLDER
import xyz.ksharma.krail.trip.planner.ui.components.MapMotion
import xyz.ksharma.krail.trip.planner.ui.components.TO_PLACEHOLDER
import xyz.ksharma.krail.trip.planner.ui.components.mapCardColors
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The bare swap of the Search Stop card, read from the real card (`docs/MAP_HOME_UX.md`, The box): a 24 dp glyph with no circle in
 * an invisible 48 dp box 36 dp from the card's right edge, centred on the divider level while at most one stop is chosen and on the
 * From row's centre line once both are, gliding between the two in 200 ms on the arrival curve, fading in over 150 ms, present only
 * with a stop chosen, and a target that beats the map under it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-notnight-xxhdpi", manifest = Config.NONE)
class SearchStopCardSwapTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val gaps = searchStopGaps()

    private var fromStop by mutableStateOf<StopItem?>(null)
    private var toStop by mutableStateOf<StopItem?>(null)
    private var fontScaleState by mutableStateOf(1f)
    private var swaps = 0
    private var mapTaps = 0
    private var composeView: View? = null
    private var tint = Color.Unspecified

    private fun setCard(reduceMotion: Boolean = false, paused: Boolean = false) {
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, if (reduceMotion) 0f else 1f)
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScaleState)) {
                PreviewTheme {
                    composeView = LocalView.current
                    tint = mapCardColors().tint
                    // The map under the card: a tap that reaches it is counted.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(KrailTheme.colors.surface)
                            .clickable { mapTaps++ }
                            .padding(gaps.edge),
                    ) {
                        SearchStopGroupCard(
                            form = SearchStopFormModel(
                                shape = FormShape.ToAndFrom,
                                editing = null,
                                toPlaceholder = toStop?.stopName ?: TO_PLACEHOLDER,
                                fromPlaceholder = fromStop?.stopName ?: FROM_PLACEHOLDER,
                                toStop = toStop,
                                fromStop = fromStop,
                                fieldState = rememberTextFieldState(),
                                focusRequester = remember { FocusRequester() },
                                onFlowEvent = {},
                                onGoClick = {},
                                onTextChange = {},
                                onSwap = if (fromStop != null || toStop != null) ({ swaps++ }) else null,
                            ),
                            onBack = {},
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        if (paused) composeRule.mainClock.autoAdvance = false
    }

    private fun boundsOf(tag: String): DpRect = composeRule.onNodeWithTag(
        tag,
        useUnmergedTree = true,
    ).getUnclippedBoundsInRoot()

    /** Where a node is drawn, from its bounds in the root, which include the layer move of the swap's glide. */
    private fun drawnBoundsOf(tag: String): DpRect = with(composeRule.density) {
        val b = composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        DpRect(b.left.toDp(), b.top.toDp(), b.right.toDp(), b.bottom.toDp())
    }

    private fun DpRect.cy() = top + (bottom - top) / 2f
    private fun DpRect.cx() = left + (right - left) / 2f

    private fun assertNear(what: String, expected: Dp, actual: Dp, within: Float = 0.75f) {
        assertTrue(abs((expected - actual).value) <= within, "$what: expected $expected, got $actual")
    }

    private fun exists(tag: String) = composeRule.onAllNodes(hasTestTag(tag), useUnmergedTree = true)
        .fetchSemanticsNodes(atLeastOneRootRequired = false)
        .isNotEmpty()

    private fun change(block: () -> Unit) = composeRule.runOnUiThread {
        block()
        Snapshot.sendApplyNotifications()
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

    /** The swap's centre, from the card's top, with [card] and the divider read from the real nodes. */
    private fun swapCentreFromCardTop(): Dp = drawnBoundsOf(TripPlannerTestTags.SEARCH_ROW_REVERSE).cy() - boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD).top

    private fun dividerLevel(): Dp = boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM).let {
        it.bottom - boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD).top
    } + 0.5.dp

    private fun fromCentre(): Dp = boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM).let {
        it.cy() - boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD).top
    }

    // region geometry

    @Test
    fun `the swap is a bare 24 dp glyph in an invisible 48 dp box, 36 dp from the right edge, with no circle and no fill`() {
        fromStop = CENTRAL
        setCard()

        val card = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        val box = drawnBoundsOf(TripPlannerTestTags.SEARCH_ROW_REVERSE)
        val glyph = drawnBoundsOf(TripPlannerTestTags.SEARCH_ROW_SWAP_GLYPH)
        assertNear("the box is 48 dp wide", 48.dp, box.right - box.left)
        assertNear("and 48 dp tall", 48.dp, box.bottom - box.top)
        assertNear("the glyph is 24 dp", 24.dp, glyph.right - glyph.left)
        assertNear("the glyph's centre is 36 dp from the card's right edge", 36.dp, card.right - glyph.cx())
        assertNear("the box and the glyph share a centre", box.cx(), glyph.cx())
        assertNear("and a centre line", box.cy(), glyph.cy())

        // Nothing is drawn in the box but the glyph: the corner of the box is the card's own tint, so no circle, ring or fill.
        val image = drawn()
        val density = composeRule.density
        val corner =
            Color(
                image.getPixel(
                    with(density) { (box.left + 2.dp).roundToPx() },
                    with(density) { (box.cy()).roundToPx() },
                ),
            )
        assertTrue(
            abs(corner.red - tint.red) <= 0.03f && abs(corner.green - tint.green) <= 0.03f && abs(corner.blue - tint.blue) <= 0.03f,
            "the box has no fill of its own: $corner against the card's $tint",
        )
    }

    @Test
    fun `with one stop chosen the swap is centred on the divider level, 28_5 dp below the From row's centre at 100 percent`() {
        fromStop = CENTRAL
        setCard()

        assertNear("the swap is on the divider level", dividerLevel(), swapCentreFromCardTop())
        assertNear("28.5 dp below the From row's centre in 56 dp rows", fromCentre() + 28.5.dp, swapCentreFromCardTop())
    }

    @Test
    fun `with both stops chosen the swap is on the From row's centre line, top right`() {
        fromStop = CENTRAL
        toStop = TOWN_HALL
        setCard()

        assertNear("the swap is on the From row's centre line", fromCentre(), swapCentreFromCardTop())
    }

    @Test
    fun `at 150 percent text the divider level and the From row's centre keep their meaning, a longer glide`() {
        fromStop = LONG_NAME
        fontScaleState = 1.5f
        setCard()
        val oneStop = swapCentreFromCardTop()
        assertNear("one stop, on the divider level", dividerLevel(), oneStop)

        composeRule.runOnIdle { toStop = TOWN_HALL }
        composeRule.mainClock.advanceTimeBy(MapMotion.SWAP_GLIDE_MS.toLong() + 300)
        composeRule.waitForIdle()
        assertNear("both stops, on the From row's centre line", fromCentre(), swapCentreFromCardTop())
        assertTrue(
            oneStop - swapCentreFromCardTop() > 28.dp,
            "The glide is half the From row plus half the divider, longer than at 100 percent.",
        )
    }

    @Test
    fun `the divider runs from the text start at 44 dp to 64 dp before the card's right edge`() {
        fromStop = CENTRAL
        toStop = TOWN_HALL
        setCard()

        val card = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        val divider = boundsOf(TripPlannerTestTags.SEARCH_ROW_DIVIDER)
        assertNear("the divider starts at the text, x44", 44.dp, divider.left - card.left)
        assertNear("and ends 64 dp from the right edge, 8 dp before the controls", 64.dp, card.right - divider.right)
    }

    // endregion

    // region present or absent

    @Test
    fun `the swap is absent with no stop chosen, and present with one or two`() {
        setCard()
        assertTrue(!exists(TripPlannerTestTags.SEARCH_ROW_REVERSE), "No stop chosen, nothing to swap.")

        composeRule.runOnIdle { fromStop = CENTRAL }
        composeRule.waitForIdle()
        assertTrue(exists(TripPlannerTestTags.SEARCH_ROW_REVERSE), "One stop chosen.")

        composeRule.runOnIdle { toStop = TOWN_HALL }
        composeRule.waitForIdle()
        assertTrue(exists(TripPlannerTestTags.SEARCH_ROW_REVERSE), "Two stops chosen.")

        composeRule.runOnIdle {
            fromStop = null
            toStop = null
        }
        composeRule.mainClock.advanceTimeBy(MapMotion.SWAP_FADE_MS.toLong() + 200)
        composeRule.waitForIdle()
        assertTrue(!exists(TripPlannerTestTags.SEARCH_ROW_REVERSE), "Cleared, it fades away and leaves the tree.")
    }

    @Test
    fun `the card is exactly as tall with none, one or two stops chosen, so nothing moves`() {
        setCard()
        val none = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD).height()
        composeRule.runOnIdle { fromStop = CENTRAL }
        composeRule.waitForIdle()
        val one = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD).height()
        composeRule.runOnIdle { toStop = TOWN_HALL }
        composeRule.waitForIdle()
        val two = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD).height()

        assertEquals(none, one)
        assertEquals(none, two)
        assertNear("and it is 113 dp", 113.dp, none)
    }

    private fun DpRect.height() = bottom - top

    // endregion

    // region the glide

    /** The swap's drawn centre y at [elapsedMs] into a glide that goes from [from] to [to] on the arrival curve. */
    private fun glideY(from: Dp, to: Dp, elapsedMs: Float): Dp =
        from + (to - from) * FastOutSlowInEasing.transform((elapsedMs / MapMotion.SWAP_GLIDE_MS).coerceIn(0f, 1f))

    private fun assertGlideAt(what: String, from: Dp, to: Dp, atMs: Long) {
        val measured = swapCentreFromCardTop()
        val early = glideY(from, to, atMs - FRAME_MS)
        val late = glideY(from, to, atMs + FRAME_MS)
        assertTrue(
            measured >= minOf(early, late) - 0.75.dp && measured <= maxOf(early, late) + 0.75.dp,
            "$what at $atMs ms: $measured is not between $early and $late",
        )
    }

    @Test
    fun `the swap glides up from the divider level to the From row's centre line in 200 ms on the arrival curve`() {
        fromStop = CENTRAL
        setCard(paused = true)
        val start = swapCentreFromCardTop()
        val end = fromCentre()
        assertNear("it starts at the divider level", dividerLevel(), start)
        assertEquals(200, MapMotion.SWAP_GLIDE_MS)

        change { toStop = TOWN_HALL }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        assertGlideAt("glide up", start, end, 0)
        composeRule.mainClock.advanceTimeBy(100)
        assertGlideAt("glide up", start, end, 100)
        composeRule.mainClock.advanceTimeBy(100)
        assertNear("and it has landed by 200 ms plus a frame or two", end, swapCentreFromCardTop(), within = 3f)
        composeRule.mainClock.advanceTimeBy(100)
        assertNear("on the From row's centre line", end, swapCentreFromCardTop())
    }

    @Test
    fun `clearing a stop glides the swap back down to the divider level in the same 200 ms`() {
        fromStop = CENTRAL
        toStop = TOWN_HALL
        setCard(paused = true)
        val start = swapCentreFromCardTop()
        assertNear("it starts on the From row's centre line", fromCentre(), start)
        val end = dividerLevel()

        change { toStop = null }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        assertGlideAt("glide down", start, end, 0)
        composeRule.mainClock.advanceTimeBy(100)
        assertGlideAt("glide down", start, end, 100)
        composeRule.mainClock.advanceTimeBy(300)
        assertNear("it has landed on the divider level", end, swapCentreFromCardTop())
    }

    @Test
    fun `with reduced motion the swap jumps to its new place in one frame, with nothing between`() {
        fromStop = CENTRAL
        setCard(reduceMotion = true, paused = true)
        assertNear("on the divider level", dividerLevel(), swapCentreFromCardTop())

        change { toStop = TOWN_HALL }
        repeat(2) { composeRule.mainClock.advanceTimeByFrame() }
        assertNear("on the From row's centre line two frames later", fromCentre(), swapCentreFromCardTop())

        change { toStop = null }
        repeat(2) { composeRule.mainClock.advanceTimeByFrame() }
        assertNear("and back on the divider level", dividerLevel(), swapCentreFromCardTop())
    }

    // endregion

    // region fade in and taps

    @Test
    fun `the swap takes taps from alpha 0_5 while it fades in over 150 ms, none before`() {
        setCard(paused = true)
        change { fromStop = CENTRAL }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(30)

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_REVERSE, useUnmergedTree = true).performTouchInput {
            click(center)
        }
        assertEquals(0, swaps, "Early in the fade, below alpha 0.5, a tap is not a swap.")
        assertEquals(0, mapTaps, "and it does not reach the map under it either.")
        assertEquals(150, MapMotion.SWAP_FADE_MS)
        assertEquals(0.5f, MapMotion.SWAP_TAP_ALPHA)

        composeRule.mainClock.advanceTimeBy(100)
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_REVERSE, useUnmergedTree = true).performTouchInput {
            click(center)
        }
        assertEquals(1, swaps, "Past alpha 0.5 it is a swap.")
    }

    @Test
    fun `a tap in the corner of the swap box reaches the swap and not the map, at both resting heights`() {
        fromStop = CENTRAL
        setCard()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_REVERSE, useUnmergedTree = true)
            .performTouchInput { click(Offset(3f, 3f)) }
        assertEquals(1, swaps, "At the divider level, a tap in the box's corner is the swap.")
        assertEquals(0, mapTaps, "and never the map's.")

        composeRule.runOnIdle { toStop = TOWN_HALL }
        composeRule.mainClock.advanceTimeBy(MapMotion.SWAP_GLIDE_MS.toLong() + 300)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_REVERSE, useUnmergedTree = true)
            .performTouchInput { click(Offset(width - 3f, height - 3f)) }
        assertEquals(2, swaps, "On the From row's centre line too.")
        assertEquals(0, mapTaps)
    }

    // endregion

    private companion object {
        const val FRAME_MS = 17f
        val TOWN_HALL = StopItem(stopName = "Town Hall Station", stopId = "200070")
        val CENTRAL = StopItem(stopName = "Central Station", stopId = "200060")
        val LONG_NAME = StopItem(stopName = "Blacktown Rd before Metella Rd", stopId = "200100")
    }
}
