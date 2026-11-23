package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The confirm screen at 320 dp, the smallest window the app supports, with text at 200 percent, drawn as a preview is
 * with no motion. The tile holds the two names whole, wrapping them inside itself, the headline stays above it and the
 * bar below it, and the bar keeps both whole buttons inside the window, stacked or in one row.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = "w320dp-h640dp-xxhdpi",
    manifest = Config.NONE,
)
class IntroConfirmStepNarrowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val shown = mutableStateOf(PreviewConfirm)
    private var contentSet = false

    private fun show(state: IntroState) {
        shown.value = state
        if (!contentSet) {
            contentSet = true
            composeRule.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, fontScale = LARGE_TEXT),
                    LocalInspectionMode provides true,
                ) {
                    PreviewTheme {
                        IntroFlowScreen(
                            state = shown.value,
                            suggestedStops = DEFAULT_SUGGESTED_STOPS,
                            onEvent = {},
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun windowWidth(): Dp = composeRule.onRoot().getUnclippedBoundsInRoot().width

    private fun SemanticsNodeInteraction.assertInsideWindowWidth(what: String) {
        val bounds = getUnclippedBoundsInRoot()
        assertTrue(bounds.left >= 0.dp, "$what starts left of the window at ${bounds.left}")
        assertTrue(bounds.right <= windowWidth(), "$what ends at ${bounds.right}, past the ${windowWidth()} window")
    }

    @Test
    fun `the window is 320 dp wide, so the checks below mean something`() {
        show(PreviewConfirm)

        assertEquals(320.dp, windowWidth())
    }

    @Test
    fun `the headline and the tile are whole across the width, with short and with long names`() {
        listOf(PreviewConfirm, PreviewConfirmLongNames).forEach { state ->
            show(state)

            composeRule.onNodeWithText(CONFIRM_HEADING).assertInsideWindowWidth("headline")
            composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_CONFIRM_TILE).assertInsideWindowWidth("tile")
        }
    }

    @Test
    fun `the headline is above the tile and the tile is above the bar`() {
        show(PreviewConfirmLongNames)

        val headline = composeRule.onNodeWithText(CONFIRM_HEADING).getUnclippedBoundsInRoot()
        val tile = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_CONFIRM_TILE).getUnclippedBoundsInRoot()
        val bar = listOf(TripPlannerTestTags.INTRO_PRIMARY_ACTION, TripPlannerTestTags.INTRO_SECONDARY_ACTION)
            .map { composeRule.onNodeWithTag(it).getUnclippedBoundsInRoot() }

        assertTrue(tile.top >= headline.bottom - ROUNDING, "the tile starts above the headline ends: $headline $tile")
        assertTrue(bar.all { it.top >= tile.bottom - ROUNDING }, "the bar is over the tile: $tile $bar")
    }

    @Test
    fun `at 200 percent the bar keeps both whole labels, stacked or in one row`() {
        show(PreviewConfirm)

        val primary = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION)
        val secondary = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_SECONDARY_ACTION)
        primary.assertInsideWindowWidth(CONFIRM_SAVE)
        secondary.assertInsideWindowWidth(CONFIRM_NOT_NOW)

        val p = primary.getUnclippedBoundsInRoot()
        val s = secondary.getUnclippedBoundsInRoot()
        val stacked = p.bottom <= s.top + ROUNDING
        val inOneRow = s.right <= p.left + ROUNDING && p.top == s.top
        assertTrue(stacked || inOneRow, "neither stacked nor in one row: $p $s")
    }

    private companion object {
        const val LARGE_TEXT = 2.0f
        val ROUNDING = 1.dp
    }
}
