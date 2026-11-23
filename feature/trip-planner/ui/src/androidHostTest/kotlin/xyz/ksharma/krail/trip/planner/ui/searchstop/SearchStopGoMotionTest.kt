package xyz.ksharma.krail.trip.planner.ui.searchstop

import android.content.Context
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
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
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The go circle of the Search Stop card in Design Y, read from the real card on a paused clock (`docs/MAP_HOME_UX.md`, The box): it
 * stays in the card, in the To row's end, only while both stops are chosen. It arrives scaling from nothing to 1.2 by 180 ms and
 * settling to 1.0 by 300 ms on the arrival curve with an 80 ms fade, while the swap glides up. It leaves in 150 ms with its scale
 * 1 minus p squared. Its 48 dp box takes no tap until the scale reaches 0.9 and none from the first frame of leaving, the swap
 * wins any overlap, and the box keeps its size whatever the circle does.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-notnight-xxhdpi", manifest = Config.NONE)
class SearchStopGoMotionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val gaps = searchStopGaps()

    private var fromStop by mutableStateOf<StopItem?>(null)
    private var toStop by mutableStateOf<StopItem?>(null)
    private var fontScaleState by mutableStateOf(1f)
    private var swaps = 0
    private var goes = 0
    private var mapTaps = 0

    private fun setCard(reduceMotion: Boolean = false, paused: Boolean = false) {
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, if (reduceMotion) 0f else 1f)
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScaleState)) {
                PreviewTheme {
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
                                onGoClick = { goes++ },
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

    private fun rootBounds(tag: String): DpRect = with(composeRule.density) {
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

    /** The go circle's drawn width, which includes the layer's scale, or null when it has no node. */
    private fun goWidth(): Dp? = if (exists(TripPlannerTestTags.SEARCH_ROW_GO_CIRCLE)) {
        rootBounds(TripPlannerTestTags.SEARCH_ROW_GO_CIRCLE).let { it.right - it.left }
    } else {
        null
    }

    private fun clickGo(offset: (androidx.compose.ui.test.TouchInjectionScope) -> Offset = { it.center }) {
        composeRule.onNodeWithTag(
            TripPlannerTestTags.SEARCH_ROW_SEARCH,
            useUnmergedTree = true,
        ).performTouchInput { click(offset(this)) }
    }

    /** The appear curve: 0 to 1.2 by 180 ms, then 1.2 to 1.0 by 300 ms, each on the arrival curve. */
    private fun appearScale(ms: Float): Float = when {
        ms <= 0f -> 0f
        ms <= 180f -> 1.2f * FastOutSlowInEasing.transform(ms / 180f)
        ms <= 300f -> 1.2f - 0.2f * FastOutSlowInEasing.transform((ms - 180f) / 120f)
        else -> 1f
    }

    // region only with both stops

    @Test
    fun `the go circle is in the card, in the To row, only while both stops are chosen`() {
        fromStop = CENTRAL
        setCard()
        assertTrue(!exists(TripPlannerTestTags.SEARCH_ROW_GO_CIRCLE), "One stop chosen, no go circle and no node.")

        composeRule.runOnIdle { toStop = TOWN_HALL }
        composeRule.mainClock.advanceTimeBy(600)
        composeRule.waitForIdle()
        val go = rootBounds(TripPlannerTestTags.SEARCH_ROW_GO_CIRCLE)
        val to = composeRule.onNodeWithTag(
            TripPlannerTestTags.SEARCH_ROW_TO,
            useUnmergedTree = true,
        ).getUnclippedBoundsInRoot()
        val card = composeRule.onNodeWithTag(
            TripPlannerTestTags.SEARCH_STOP_GROUP_CARD,
            useUnmergedTree = true,
        ).getUnclippedBoundsInRoot()
        assertNear(
            "the go circle is in the card, on the To row's centre line",
            to.top + (to.bottom - to.top) / 2f,
            go.cy(),
        )
        assertNear("its centre is 36 dp from the card's right edge", 36.dp, card.right - go.cx())
        assertTrue(go.bottom <= card.bottom && go.top >= card.top, "inside the card, not floating on the map")

        composeRule.runOnIdle { fromStop = null }
        composeRule.mainClock.advanceTimeBy(600)
        composeRule.waitForIdle()
        assertTrue(!exists(TripPlannerTestTags.SEARCH_ROW_GO_CIRCLE), "A stop cleared, the go circle is gone.")
    }

    // endregion

    // region the appear and disappear specs, on the paused clock

    @Test
    fun `the go circle arrives 0 to 1_2 by 180 ms then 1_0 by 300 ms on the arrival curve, with the swap gliding at the same time`() {
        fromStop = CENTRAL
        setCard(paused = true)
        assertEquals(null, goWidth())
        val swapStart = rootBounds(TripPlannerTestTags.SEARCH_ROW_REVERSE).cy()

        change { toStop = TOWN_HALL }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        var elapsed = FRAME_MS_I * 2L
        val seen = mutableListOf<Pair<Long, Float>>()
        for (target in listOf(60L, 120L, 180L, 240L, 300L, 340L)) {
            composeRule.mainClock.advanceTimeBy(target - elapsed)
            elapsed = target
            seen += target to checkNotNull(goWidth()) { "the go circle is there at $target ms" }.value / 40f
        }
        for ((ms, scale) in seen) {
            val early = appearScale(ms - FRAME_MS * 2)
            val late = appearScale(ms + FRAME_MS * 2)
            assertTrue(
                scale >= minOf(early, late) - 0.05f && scale <= maxOf(early, late) + 0.05f,
                "at $ms ms the scale is $scale, between $early and $late",
            )
        }
        val peak = seen.first { it.first == 180L }.second
        assertTrue(peak in 1.1f..1.25f, "It peaks at 1.2 around 180 ms: $peak")
        assertNear("It has settled at 1.0 by 300 ms", 40.dp, checkNotNull(goWidth()), within = 1.5f)
        assertTrue(
            rootBounds(TripPlannerTestTags.SEARCH_ROW_REVERSE).cy() < swapStart - 20.dp,
            "The swap has glided up while the go arrived.",
        )
        assertEquals(1.2f, MapMotion.GO_PEAK_SCALE)
        assertEquals(180, MapMotion.GO_PEAK_MS)
        assertEquals(300, MapMotion.GO_ENTER_MS)
        assertEquals(80, MapMotion.GO_FADE_IN_MS)
    }

    @Test
    fun `the go circle leaves in 150 ms with its scale 1 minus p squared while the swap glides back down`() {
        fromStop = CENTRAL
        toStop = TOWN_HALL
        setCard(paused = true)
        assertNear("full size", 40.dp, checkNotNull(goWidth()))

        change { toStop = null }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(60 - FRAME_MS_I * 2L)
        val at60 = checkNotNull(goWidth()) { "still drawn 60 ms into its exit" }.value / 40f
        // The animation starts a frame or two after the change, so 60 ms of clock is 28 to 60 ms of it, p = 0.19 to 0.4: 1 minus p
        // squared is 0.84 to 0.96, where a straight line (0.6 to 0.81) or the arrival curve run backwards (0.38 to 0.8) is lower.
        assertTrue(at60 in 0.82f..0.98f, "60 ms in the scale is about 1 minus p squared: $at60")
        composeRule.mainClock.advanceTimeBy(60)
        val at120 = checkNotNull(goWidth()) { "still drawn 120 ms into its exit" }.value / 40f
        assertTrue(at120 in 0.3f..0.7f, "120 ms in, p = 0.59 to 0.8 gives 0.65 to 0.36: $at120")
        composeRule.mainClock.advanceTimeBy(MapMotion.GO_LEAVE_MS.toLong() + 100)
        assertEquals(null, goWidth(), "It has gone, with no node left.")
        assertEquals(150, MapMotion.GO_LEAVE_MS)
    }

    @Test
    fun `with reduced motion the go circle is at full size within a frame and gone within a frame`() {
        setCard(reduceMotion = true, paused = true)
        fromStop = CENTRAL
        change { toStop = TOWN_HALL }
        repeat(3) { composeRule.mainClock.advanceTimeByFrame() }
        assertNear("at full size, nothing between", 40.dp, checkNotNull(goWidth()), within = 0.5f)

        change { toStop = null }
        repeat(3) { composeRule.mainClock.advanceTimeByFrame() }
        assertEquals(null, goWidth(), "and gone within a frame")
    }

    // endregion

    // region the tap rule

    @Test
    fun `the go box is inert until the scale reaches 0_9 and takes taps after, and the box stays 48 dp throughout`() {
        fromStop = CENTRAL
        setCard(paused = true)
        change { toStop = TOWN_HALL }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(20)

        clickGo()
        assertEquals(0, goes, "Early in the arrival, below 0.9, a tap on the go box starts nothing.")
        assertEquals(0, mapTaps, "and it does not reach the map under it.")
        val box = rootBounds(TripPlannerTestTags.SEARCH_ROW_SEARCH)
        assertNear("The box is 48 dp whatever the circle's scale", 48.dp, box.right - box.left, within = 0.5f)
        assertTrue(checkNotNull(goWidth()) < 36.dp, "the circle is still small")

        composeRule.mainClock.advanceTimeBy(120)
        clickGo()
        assertEquals(1, goes, "Past 0.9 the go box takes the tap.")
        assertEquals(0.9f, MapMotion.GO_TAP_SCALE)
    }

    @Test
    fun `the go box is inert from the first frame of leaving, so a half shrunk circle never starts a search`() {
        fromStop = CENTRAL
        toStop = TOWN_HALL
        setCard(paused = true)

        change { toStop = null }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(30)
        clickGo()

        assertEquals(0, goes, "No tap on a circle on its way out.")
        assertEquals(0, mapTaps, "and none reaches the map either.")
    }

    @Test
    fun `a tap in the overlap of the swap and the go box during the glide is the swap's, never the go's`() {
        fromStop = CENTRAL
        setCard(paused = true)
        change { toStop = TOWN_HALL }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(30)

        val swap = rootBounds(TripPlannerTestTags.SEARCH_ROW_REVERSE)
        val go = rootBounds(TripPlannerTestTags.SEARCH_ROW_SEARCH)
        assertTrue(swap.bottom > go.top + 4.dp, "The two 48 dp boxes overlap early in the glide: swap $swap, go $go")
        val x = with(composeRule.density) { swap.cx().toPx() }
        val y = with(composeRule.density) { ((go.top + swap.bottom) / 2f).toPx() }
        composeRule.onRoot().performTouchInput { click(Offset(x, y)) }

        assertEquals(0, goes, "The go box is inert and under the swap.")
        assertEquals(0, mapTaps, "and the tap does not fall through to the map.")
    }

    @Test
    fun `a tap in the corner of the 48 dp go box, outside the circle, starts the trip and not the map`() {
        fromStop = CENTRAL
        toStop = TOWN_HALL
        setCard()

        clickGo { Offset(it.width - 2f, it.height - 2f) }

        assertEquals(1, goes)
        assertEquals(0, mapTaps)
    }

    @Test
    fun `a screen reader hears Show trips on the go circle and Swap From and To on the swap, each once`() {
        fromStop = CENTRAL
        toStop = TOWN_HALL
        setCard()

        composeRule.onNodeWithContentDescription("Show trips").assertExists()
        composeRule.onNodeWithContentDescription("Swap From and To").assertExists()
    }

    // endregion

    // region at rest the two boxes never overlap

    @Test
    fun `at rest with both stops chosen the swap and go boxes are 57 dp apart centre to centre with 9 dp between them at 100 percent`() {
        fromStop = CENTRAL
        toStop = TOWN_HALL
        setCard()

        val swap = rootBounds(TripPlannerTestTags.SEARCH_ROW_REVERSE)
        val go = rootBounds(TripPlannerTestTags.SEARCH_ROW_SEARCH)
        assertNear("57 dp apart, centre to centre", 57.dp, go.cy() - swap.cy(), within = 1f)
        assertNear("with 9 dp between the boxes", 9.dp, go.top - swap.bottom, within = 1f)
    }

    // endregion

    private companion object {
        const val FRAME_MS = 17f
        const val FRAME_MS_I = 16
        val TOWN_HALL = StopItem(stopName = "Town Hall Station", stopId = "200070")
        val CENTRAL = StopItem(stopName = "Central Station", stopId = "200060")
    }
}
