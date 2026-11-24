package xyz.ksharma.krail.trip.planner.ui.searchstop

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
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
import xyz.ksharma.krail.trip.planner.ui.components.ToFromField
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The right column of the Search Stop card, read from the real card: the bare swap glyph and the go circle on one axis
 * (`docs/MAP_HOME_UX.md`, The box), the go circle's appear and disappear on the paused clock, the swap's half turn, and the
 * finger targets that must beat the map under them. The swap's own positions and glide are in `SearchStopCardSwapTest`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-notnight", manifest = Config.NONE)
class SearchStopCardCirclesTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val gaps = searchStopGaps()
    private var surface = androidx.compose.ui.graphics.Color.Unspecified

    private var fromState by mutableStateOf<StopItem?>(CENTRAL)
    private var toState by mutableStateOf<StopItem?>(TOWN_HALL)
    private var swapEnabled by mutableStateOf(true)
    private var swaps = 0
    private var goes = 0

    private fun setCard(fontScale: Float = 1f, reduceMotion: Boolean = false, pausedClock: Boolean = false) {
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, if (reduceMotion) 0f else 1f)
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                PreviewTheme {
                    surface = KrailTheme.colors.surface
                    Box(modifier = Modifier.fillMaxSize().background(surface).padding(gaps.edge)) {
                        SearchStopGroupCard(
                            form = SearchStopFormModel(
                                shape = FormShape.ToAndFrom,
                                editing = null,
                                toPlaceholder = toState?.stopName ?: TO_PLACEHOLDER,
                                fromPlaceholder = fromState?.stopName ?: FROM_PLACEHOLDER,
                                toStop = toState,
                                fromStop = fromState,
                                fieldState = rememberTextFieldState(),
                                focusRequester = remember { FocusRequester() },
                                onFlowEvent = {},
                                onGoClick = { goes++ },
                                onTextChange = {},
                                onSwap = if (swapEnabled) ({ swaps++ }) else null,
                            ),
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        if (pausedClock) composeRule.mainClock.autoAdvance = false
    }

    private fun boundsOf(tag: String): DpRect = composeRule.onNodeWithTag(
        tag,
        useUnmergedTree = true,
    ).getUnclippedBoundsInRoot()

    /** Where a node is drawn, from its bounds in the root, which include a graphics layer's own move (the swap's glide). */
    private fun rootBoundsOf(tag: String): DpRect = with(composeRule.density) {
        val b = composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        DpRect(b.left.toDp(), b.top.toDp(), b.right.toDp(), b.bottom.toDp())
    }

    private fun DpRect.cx() = left + (right - left) / 2f
    private fun DpRect.cy() = top + height / 2f
    private fun DpRect.w() = right - left

    private fun assertNear(what: String, expected: Dp, actual: Dp, within: Float = 1f) {
        assertTrue(abs((expected - actual).value) <= within, "$what: expected $expected, got $actual")
    }

    /** The go circle's width on screen, from its bounds in the root, which include the layer's scale. Null when it has no node. */
    private fun goWidthDp(): Dp? {
        val node = composeRule.onAllNodes(hasTestTag(TripPlannerTestTags.SEARCH_ROW_GO_CIRCLE), useUnmergedTree = true)
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .firstOrNull()
            ?: return null
        return with(composeRule.density) { node.boundsInRoot.width.toDp() }
    }

    /** Changes state on the UI thread and tells the snapshot system, which the paused clock does not do by itself. */
    private fun change(block: () -> Unit) = composeRule.runOnUiThread {
        block()
        Snapshot.sendApplyNotifications()
    }

    // region the grid

    private fun assertRightColumn(label: String) {
        val card = boundsOf(TripPlannerTestTags.SEARCH_STOP_GROUP_CARD)
        val to = boundsOf(TripPlannerTestTags.SEARCH_ROW_TO)
        val go = boundsOf(TripPlannerTestTags.SEARCH_ROW_GO_CIRCLE)
        val swapTarget = rootBoundsOf(TripPlannerTestTags.SEARCH_ROW_REVERSE)
        val goTarget = boundsOf(TripPlannerTestTags.SEARCH_ROW_SEARCH)

        assertNear("$label, the go circle is 40 dp", 40.dp, go.w())
        assertNear("$label, the go circle's right edge is 16 dp from the card's", gaps.edge, card.right - go.right)
        assertNear(
            "$label, the swap and the go circle are on one vertical axis",
            swapTarget.cx(),
            go.cx(),
            within = 0.5f,
        )
        assertNear("$label, the axis is 36 dp from the card's right edge", 36.dp, card.right - swapTarget.cx())
        assertNear("$label, the go circle is centred on the To row's centre line", to.cy(), go.cy())
        assertNear("$label, the go circle's 48 dp target is centred on it", go.cy(), goTarget.cy())
        assertNear("$label, both targets are 48 dp", 48.dp, swapTarget.w())
        assertNear("$label, both targets are 48 dp", 48.dp, goTarget.w())
        assertTrue(
            swapTarget.bottom <= goTarget.top + 0.5.dp,
            "$label, the swap's target $swapTarget does not overlap the go circle's $goTarget",
        )
        val divider = boundsOf(TripPlannerTestTags.SEARCH_ROW_DIVIDER)
        assertNear("$label, the text stops 8 dp before the go circle", 8.dp, go.left - divider.right)
    }

    @Test
    fun `at 411 dp and 100 percent text the swap glyph and the go circle are on one axis, 36 dp from the right edge`() {
        setCard()
        assertRightColumn("411 dp, 100 percent")
        val from = boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM)
        val swap = rootBoundsOf(TripPlannerTestTags.SEARCH_ROW_SWAP_GLYPH)
        assertNear("the bare glyph is 24 dp", 24.dp, swap.w())
        assertNear("with both stops chosen the glyph is on the From row's centre line", from.cy(), swap.cy())
    }

    @Test
    fun `at 411 dp and 150 percent text the same grid holds`() {
        setCard(fontScale = 1.5f)
        assertRightColumn("411 dp, 150 percent")
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-notnight")
    fun `at 320 dp and 200 percent text the same grid holds, centred on rows that have grown`() {
        setCard(fontScale = 2f)
        assertRightColumn("320 dp, 200 percent")
    }

    @Test
    fun `the swap is not drawn until the rider has picked a stop, and the go circle only when both are chosen`() {
        swapEnabled = false
        setCard()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_REVERSE, useUnmergedTree = true).assertDoesNotExist()

        composeRule.runOnIdle {
            swapEnabled = true
            toState = null
        }
        composeRule.mainClock.advanceTimeBy(MapMotion.SWAP_FADE_MS.toLong() + 100)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_REVERSE, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_GO_CIRCLE, useUnmergedTree = true).assertDoesNotExist()
    }

    // endregion

    // region the go circle's motion

    @Test
    fun `the go circle arrives by growing past full size and settling, 0 to 1_2 by 180 ms and 1_0 by 300 ms`() {
        toState = null
        setCard(pausedClock = true)
        assertEquals(null, goWidthDp(), "There is no go circle while a stop is missing.")

        change { toState = TOWN_HALL }
        composeRule.mainClock.advanceTimeByFrame()
        val widths = mutableListOf<Pair<Long, Dp>>()
        var elapsed = 0L
        for (target in listOf(90L, 180L, 240L, 300L, 340L)) {
            composeRule.mainClock.advanceTimeBy(target - elapsed)
            elapsed = target
            widths += target to checkNotNull(goWidthDp()) { "the go circle is there at $target ms" }
        }
        val byTime = widths.toMap()
        assertTrue(checkNotNull(byTime[90L]) in 8.dp..44.dp, "Part way in it is growing: ${byTime[90L]}")
        assertNear("at the peak, 1.2 of 40 dp", 48.dp, checkNotNull(byTime[180L]), within = 2.5f)
        assertTrue(checkNotNull(byTime[240L]) in 40.dp..48.dp, "settling back from the peak: ${byTime[240L]}")
        assertNear("settled at full size by 300 ms", 40.dp, checkNotNull(byTime[300L]), within = 1f)
        assertNear("and it stays there", 40.dp, checkNotNull(byTime[340L]), within = 0.5f)
        assertTrue(widths.all { it.second <= 48.dp + 2.5.dp }, "It never grows past 1.2: $widths")
        assertEquals(1.2f, MapMotion.GO_PEAK_SCALE)
        assertEquals(180, MapMotion.GO_PEAK_MS)
        assertEquals(300, MapMotion.GO_ENTER_MS)
    }

    @Test
    fun `the go circle leaves by shrinking to nothing in 150 ms and takes no tap on its way out`() {
        setCard(pausedClock = true)
        assertNear("it is full size", 40.dp, checkNotNull(goWidthDp()))

        change { toState = null }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(60)
        val part = checkNotNull(goWidthDp()) { "the go circle is still drawn 60 ms into its exit" }
        assertTrue(part < 40.dp && part > 0.dp, "It is shrinking: $part")

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_SEARCH, useUnmergedTree = true).performClick()
        assertEquals(0, goes, "A tap on a circle that is on its way out does nothing.")

        composeRule.mainClock.advanceTimeBy(MapMotion.EXIT_MS.toLong() + 100)
        assertEquals(null, goWidthDp(), "It has gone, with no node left.")
    }

    @Test
    fun `with reduced motion the go circle is there at full size within a frame and gone within a frame`() {
        toState = null
        setCard(reduceMotion = true, pausedClock = true)

        change { toState = TOWN_HALL }
        repeat(3) { composeRule.mainClock.advanceTimeByFrame() }
        assertNear("at once at full size, no frame between", 40.dp, checkNotNull(goWidthDp()), within = 0.5f)

        change { toState = null }
        repeat(3) { composeRule.mainClock.advanceTimeByFrame() }
        assertEquals(null, goWidthDp(), "and gone with no frame between")
    }

    // endregion

    // region the swap

    /**
     * The glyph's width on screen, from its bounds in the root, which follow the layer's rotation: its 24 dp square is 24 dp
     * wide at rest and at a half turn, and about 34 dp wide at 45 degrees, so the width is how far through the turn it is.
     */
    private fun glyphWidthDp(): Dp = with(composeRule.density) {
        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_SWAP_GLYPH, useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
            .width
            .toDp()
    }

    @Test
    fun `a tap on the swap calls it once and the glyph makes a half turn in 300 ms while its box stays still`() {
        setCard(pausedClock = true)
        val boxBefore = boundsOf(TripPlannerTestTags.SEARCH_ROW_REVERSE)
        assertNear("at rest the glyph square is 24 dp", 24.dp, glyphWidthDp(), within = 0.5f)

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_REVERSE, useUnmergedTree = true).performClick()
        // Frame by frame, so the animation starts on the first frame it sees and the clock never jumps over the turn.
        var widest = 0.dp
        repeat(FRAMES_IN_TURN) {
            composeRule.mainClock.advanceTimeByFrame()
            widest = maxOf(widest, glyphWidthDp())
        }
        val end = glyphWidthDp()

        assertEquals(1, swaps, "One tap is one swap.")
        assertTrue(widest > 30.dp, "Part way through the turn the glyph is on a slant, at its widest $widest.")
        assertNear("A half turn ends square again", 24.dp, end, within = 0.5f)
        assertEquals(boxBefore, boundsOf(TripPlannerTestTags.SEARCH_ROW_REVERSE), "The 48 dp box itself stays still.")
        assertEquals(300, MapMotion.SWAP_TURN_MS)
    }

    @Test
    fun `with reduced motion the swap glyph is plain, never on a slant between`() {
        setCard(reduceMotion = true, pausedClock = true)

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_REVERSE, useUnmergedTree = true).performClick()
        repeat(3) {
            composeRule.mainClock.advanceTimeByFrame()
            assertNear("no frame between", 24.dp, glyphWidthDp(), within = 0.5f)
        }
        assertEquals(1, swaps)
    }

    // endregion

    // region the targets beat the map

    @Test
    fun `a tap in the corner of the 48 dp swap target, far from the 24 dp glyph, still reaches the swap`() {
        setCard()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_REVERSE, useUnmergedTree = true)
            .performTouchInput { click(Offset(2f, 2f)) }

        assertEquals(1, swaps)
    }

    @Test
    fun `a tap in the corner of the 48 dp go target, outside the 40 dp circle, still starts the trip`() {
        setCard()

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_SEARCH, useUnmergedTree = true)
            .performTouchInput { click(Offset(width - 2f, height - 2f)) }

        assertEquals(1, goes)
    }

    // endregion

    // region screen reader

    @Test
    fun `a screen reader hears Swap From and To and Show trips, each once`() {
        setCard()

        composeRule.onNodeWithContentDescription("Swap From and To").assertExists()
        composeRule.onNodeWithContentDescription("Show trips").assertExists()
    }

    // endregion

    // region the pure rules

    @Test
    fun `reduceTripSwap trades From and To and leaves nothing to edit when both are held`() {
        val step = reduceTripSwap(
            trip = SearchStopTrip(from = CENTRAL, to = TOWN_HALL),
            flow = SearchStopFlow(mode = SearchStopMode.List, editing = ToFromField.To),
            autoFrom = null,
        )

        assertEquals(SearchStopTrip(from = TOWN_HALL, to = CENTRAL), step.trip)
        assertEquals(null, step.flow.editing)
        assertEquals(SearchStopMode.Map, step.flow.mode, "With nothing to edit it is the map state, no keyboard.")
    }

    @Test
    fun `reduceTripSwap moves a lone stop across and edits the bar it leaves empty`() {
        val fromOnly = reduceTripSwap(
            SearchStopTrip(from = CENTRAL),
            SearchStopFlow(editing = ToFromField.To),
            autoFrom = null,
        )
        assertEquals(SearchStopTrip(from = null, to = CENTRAL), fromOnly.trip)
        assertEquals(ToFromField.From, fromOnly.flow.editing)

        val toOnly = reduceTripSwap(
            SearchStopTrip(to = TOWN_HALL),
            SearchStopFlow(editing = ToFromField.From),
            autoFrom = null,
        )
        assertEquals(SearchStopTrip(from = TOWN_HALL, to = null), toOnly.trip)
        assertEquals(ToFromField.To, toOnly.flow.editing)
    }

    @Test
    fun `reduceTripSwap counts a start that filled itself in as the From the rider sees`() {
        val step = reduceTripSwap(SearchStopTrip(to = TOWN_HALL), SearchStopFlow(editing = null), autoFrom = CENTRAL)

        assertEquals(SearchStopTrip(from = TOWN_HALL, to = CENTRAL), step.trip)
        assertEquals(null, step.flow.editing)
    }

    @Test
    fun `the swap is shown only once the rider has picked a stop`() {
        assertTrue(!swapShown(SearchStopTrip()))
        assertTrue(swapShown(SearchStopTrip(from = CENTRAL)))
        assertTrue(swapShown(SearchStopTrip(to = TOWN_HALL)))
    }

    // endregion

    private companion object {
        /** About 340 ms of 16 ms frames, a little past the 300 ms turn. */
        const val FRAMES_IN_TURN = 22
        val TOWN_HALL = StopItem(stopName = "Town Hall Station", stopId = "200070")
        val CENTRAL = StopItem(stopName = "Central Station", stopId = "200060")
    }
}
