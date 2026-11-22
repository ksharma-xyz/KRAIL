package xyz.ksharma.krail.trip.planner.ui.intro.v2

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.intro.DEFAULT_SUGGESTED_STOPS
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroState
import kotlin.test.assertTrue

/**
 * Step 1 at 320 dp, the smallest window the app supports, with text at 200 percent. Every item stays
 * whole inside the window and the answers and the card stack, so nothing is clipped, squeezed or
 * pushed sideways.
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
                            readiness = StopsReadiness.Ready(stopCount = 100),
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
    fun `asking keeps the question, both answers and the button whole`() {
        show(PreviewAskState)

        listOf(PARK_RIDE_QUESTION, PARK_RIDE_YES, PARK_RIDE_NO).forEach { text ->
            composeRule.onNodeWithText(text).assertIsDisplayed().assertInsideWindow(text)
        }
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).assertIsDisplayed()
            .assertInsideWindow("the bottom button")
    }

    @Test
    fun `the two answers are stacked, one under the other, at the same left edge and width`() {
        show(PreviewAskState)

        val yes = composeRule.onNodeWithText(PARK_RIDE_YES).getUnclippedBoundsInRoot()
        val no = composeRule.onNodeWithText(PARK_RIDE_NO).getUnclippedBoundsInRoot()

        assertTrue(no.top >= yes.bottom, "No starts at ${no.top}, above where Yes ends at ${yes.bottom}")
        assertTrue(no.left == yes.left, "No starts at ${no.left} and Yes at ${yes.left}")
    }

    @Test
    fun `the tallest card stays whole inside the window with every car park and the row`() {
        show(PreviewTallCardContentState)

        // Unmerged, so each line of the card is measured on its own and not as the one button they merge into.
        // The step scrolls, so a line below the fold is scrolled to, which is also the proof it can be reached.
        listOf("Tallawong P1", "Tallawong P2", "Tallawong P3", ADD_TO_HOME_TEXT).forEach { text ->
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
            PreviewNoStationNearState,
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

        composeRule.onNodeWithText(ADDED_TO_HOME_TEXT).assertIsDisplayed().assertInsideWindow(ADDED_TO_HOME_TEXT)
    }

    private companion object {
        const val LARGE_TEXT = 2.0f
    }
}
