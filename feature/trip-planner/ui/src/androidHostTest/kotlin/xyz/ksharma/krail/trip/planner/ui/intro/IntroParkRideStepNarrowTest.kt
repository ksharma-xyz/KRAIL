package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
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
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import kotlin.test.assertTrue

/**
 * The Park & Ride screen at 320 dp, the smallest window the app supports, with text at 200 percent.
 * Every item stays whole inside the window, so nothing is clipped, squeezed or pushed sideways.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = "w320dp-h1280dp-xxhdpi",
    manifest = Config.NONE,
)
class IntroParkRideStepNarrowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val shown = mutableStateOf(PreviewAskState)
    private var contentSet = false

    private fun show(state: IntroState) {
        shown.value = state
        if (!contentSet) {
            contentSet = true
            composeRule.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = LARGE_TEXT)) {
                    PreviewTheme {
                        IntroFlowScreen(
                            state = shown.value,
                            suggestedStops = DEFAULT_SUGGESTED_STOPS,
                            readiness = PreviewReady,
                            onEvent = {},
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun windowWidth(): Dp = composeRule.onRoot().getUnclippedBoundsInRoot().width

    private fun SemanticsNodeInteraction.assertInsideWindow(what: String) {
        val bounds = getUnclippedBoundsInRoot()
        val width = windowWidth()
        assertTrue(bounds.left >= 0.dp, "$what starts left of the window at ${bounds.left}")
        assertTrue(bounds.right <= width, "$what ends at ${bounds.right}, past the $width window")
    }

    @Test
    fun `the window is 320 dp wide, so the checks below mean something`() {
        show(PreviewAskState)

        assertTrue(windowWidth() == 320.dp, "The window is ${windowWidth()} wide")
    }

    @Test
    fun `asking keeps the headline and both answers whole inside the window`() {
        show(PreviewAskState)

        composeRule.onNodeWithText(PARK_RIDE_HEADING).assertIsDisplayed().assertInsideWindow(PARK_RIDE_HEADING)
        listOf(TripPlannerTestTags.INTRO_PRIMARY_ACTION, TripPlannerTestTags.INTRO_SECONDARY_ACTION).forEach { tag ->
            composeRule.onNodeWithTag(tag).assertIsDisplayed().assertInsideWindow(tag)
        }
    }

    @Test
    fun `at 200 percent every line of the headline is whole, left aligned and one under the next`() {
        show(PreviewAskState)

        val lines = PARK_RIDE_HEADLINE.map { line ->
            composeRule.onNodeWithText(line, useUnmergedTree = true).assertInsideWindow(line)
            composeRule.onNodeWithText(line, useUnmergedTree = true).getUnclippedBoundsInRoot()
        }
        lines.zipWithNext().forEach { (above, below) ->
            assertTrue(above.bottom <= below.top + ROUNDING, "a line starts at ${below.top}, over the one above")
        }
        assertTrue(lines.map { it.left }.distinct().size == 1, "the lines are not on one left edge: $lines")
    }

    @Test
    fun `at 200 percent text the answers are the short Yes and No, in one row, and read in full to a screen reader`() {
        show(PreviewAskState)

        val no = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_SECONDARY_ACTION)
        val yes = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION)
        no.assertTextEquals(PARK_RIDE_NO_COMPACT)
        yes.assertTextEquals(PARK_RIDE_YES_COMPACT)
        composeRule.onNodeWithContentDescription(PARK_RIDE_YES).assertExists()
        composeRule.onNodeWithContentDescription(PARK_RIDE_NO).assertExists()
        val noBounds = no.getUnclippedBoundsInRoot()
        val yesBounds = yes.getUnclippedBoundsInRoot()
        assertTrue(noBounds.top == yesBounds.top, "one row, but No is at ${noBounds.top} and Yes at ${yesBounds.top}")
        assertTrue(noBounds.right <= yesBounds.left, "No ends at ${noBounds.right}, past where Yes starts")
    }

    @Test
    fun `the card bar at 200 percent keeps its whole words and never exceeds two buttons`() {
        show(PreviewTallCardContentState)

        val primary = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION)
        val secondary = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_SECONDARY_ACTION)
        primary.assertTextEquals(PARK_RIDE_ADD).assertInsideWindow("Add to home")
        secondary.assertTextEquals(PARK_RIDE_NOT_NOW).assertInsideWindow("Not now")
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(2)
    }

    @Test
    fun `the tallest card stays whole inside the window with every car park and the row`() {
        show(PreviewTallCardContentState)

        // Unmerged, so each line of the card is measured on its own and not as the one box they merge into.
        // The middle of the screen scrolls, so a line below the fold is scrolled to, which is also the proof
        // it can be reached.
        listOf("Tallawong P1", "Tallawong P2", "Tallawong P3").forEach { text ->
            composeRule.onNodeWithText(text, useUnmergedTree = true)
                .performScrollTo()
                .assertIsDisplayed()
                .assertInsideWindow(text)
        }
        listOf("spots available", "full").forEach { text ->
            val nodes = composeRule.onAllNodesWithText(text, useUnmergedTree = true)
            repeat(3) { index -> nodes[index].assertInsideWindow("$text number ${index + 1}") }
        }
        composeRule.onNodeWithTag(INTRO_CARD_SLOT_TAG).assertInsideWindow("the card")
    }

    @Test
    fun `the card keeps one height from the skeleton to the numbers at large text too`() {
        val heights = listOf(
            PreviewLocatingState,
            PreviewCardLoadingState,
            PreviewCardContentState,
            PreviewCardUnavailableState,
            PreviewAddedState,
        ).map { state ->
            show(state)
            composeRule.onNodeWithTag(INTRO_CARD_SLOT_TAG).getUnclippedBoundsInRoot().height
        }

        assertTrue(
            heights.all { kotlin.math.abs(it.value - heights.first().value) < 0.5f },
            "The card changes height as its state changes: $heights",
        )
    }

    @Test
    fun `an added card stays inside the window`() {
        show(PreviewTallAddedState)

        composeRule.onNodeWithTag(INTRO_CARD_SLOT_TAG).assertInsideWindow("the added card")
    }

    private companion object {
        const val LARGE_TEXT = 2.0f
        val ROUNDING = 1.dp
    }
}
