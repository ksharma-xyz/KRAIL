package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.math.abs
import kotlin.test.assertTrue

/**
 * Rule 13 of `docs/MAP_HOME_UX.md` on the real search row: when width is short the action button
 * (Reverse, or Ask KRAIL) goes in its own row under the From bar, in line with the go circle, so
 * the bars take the whole width and not what a 64 dp button column leaves. Where there is room it
 * stays beside the bars, centred on the two of them.
 *
 * Each test runs in a window of its own width (density 1, so a dp is a pixel) at its own font
 * scale, and reads where the real nodes landed.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [ACTION_TEST_SDK], manifest = Config.NONE)
class SearchStopRowActionPlacementTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w320dp-h800dp")
    fun `320 dp at 200 percent puts Reverse under the From bar and widens both bars`() =
        assertUnder(fontScale = 2f, ai = false, window = 320.dp)

    @Test
    @Config(qualifiers = "w320dp-h800dp")
    fun `320 dp at 200 percent puts Ask KRAIL under the From bar and widens both bars`() =
        assertUnder(fontScale = 2f, ai = true, window = 320.dp)

    @Test
    @Config(qualifiers = "w320dp-h800dp")
    fun `320 dp at 100 percent puts the action under the bars`() =
        assertUnder(fontScale = 1f, ai = false, window = 320.dp)

    @Test
    @Config(qualifiers = "w320dp-h800dp")
    fun `320 dp at 130 percent puts the action under the bars`() =
        assertUnder(fontScale = 1.3f, ai = false, window = 320.dp)

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `411 dp at 200 percent puts the action under the bars`() =
        assertUnder(fontScale = 2f, ai = false, window = 411.dp)

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `a phone at 100 percent keeps Reverse beside the bars`() =
        assertBeside(fontScale = 1f, ai = false, window = 411.dp)

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `a phone at 100 percent keeps Ask KRAIL beside the bars`() =
        assertBeside(fontScale = 1f, ai = true, window = 411.dp)

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun `a phone at 130 percent keeps the action beside the bars`() =
        assertBeside(fontScale = 1.3f, ai = false, window = 411.dp)

    @Test
    @Config(qualifiers = "w600dp-h900dp")
    fun `a wide window at 200 percent keeps the action beside the bars`() =
        assertBeside(fontScale = 2f, ai = false, window = 600.dp)

    private fun assertUnder(fontScale: Float, ai: Boolean, window: Dp) {
        setRow(fontScale, ai)
        val (to, from, action) = parts(ai)
        val content = window - PAGE_PADDING * 2

        assertNear("gap under the From bar", 8.dp, action.top - from.bottom)
        assertNear("action in line with the go circle", from.right, action.right)
        assertNear("action width", 48.dp, action.width)
        assertNear("action height", 48.dp, action.height)
        assertNear("To bar fills the content width, not what a 64 dp column leaves", content, to.width)
        assertNear("From bar fills the content width", content, from.width)
        assertTrue(to.width > content - BESIDE_COST, "The bars at ${to.width} are no wider than they were beside it.")
        assertNear("left edge", to.left, from.left)
    }

    private fun assertBeside(fontScale: Float, ai: Boolean, window: Dp) {
        setRow(fontScale, ai)
        val (to, from, action) = parts(ai)
        val content = window - PAGE_PADDING * 2

        assertNear("gap beside the bars", 16.dp, action.left - to.right)
        assertNear("action centred on the two bars", (to.top + from.bottom) / 2, (action.top + action.bottom) / 2)
        assertNear("bars leave the 64 dp column", content - BESIDE_COST, to.width)
        assertNear("action right edge", content + PAGE_PADDING, action.right)
        assertNear("action width", 48.dp, action.width)
    }

    private fun parts(ai: Boolean): Triple<DpRect, DpRect, DpRect> = Triple(
        boundsOf(TripPlannerTestTags.SEARCH_ROW_TO),
        boundsOf(TripPlannerTestTags.SEARCH_ROW_FROM),
        boundsOf(if (ai) TripPlannerTestTags.SEARCH_ROW_ASK_KRAIL else TripPlannerTestTags.SEARCH_ROW_REVERSE),
    )

    private fun setRow(fontScale: Float, ai: Boolean) {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                PreviewTheme {
                    Box(Modifier.fillMaxSize()) {
                        SearchStopRow(
                            fromButtonClick = {},
                            toButtonClick = {},
                            modifier = Modifier.align(Alignment.BottomCenter),
                            fromStopItem = StopItem(stopName = "Central Station", stopId = "200060"),
                            toStopItem = StopItem(stopName = "Town Hall Station", stopId = "200070"),
                            isExpanded = true,
                            isAiSearchAvailable = ai,
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun boundsOf(tag: String): DpRect =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun assertNear(what: String, expected: Dp, actual: Dp) {
        assertTrue(abs(expected.value - actual.value) <= TOLERANCE_DP, "$what should be $expected but is $actual.")
    }

    private companion object {
        val PAGE_PADDING = 16.dp

        /** The button column beside the bars: the 48 dp button and the 16 dp gap before it. */
        val BESIDE_COST = 64.dp
        const val TOLERANCE_DP = 0.5f
    }
}

private const val ACTION_TEST_SDK = 34
