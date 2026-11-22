package xyz.ksharma.krail.trip.planner.ui.intro.v2

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
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
import xyz.ksharma.krail.trip.planner.ui.intro.SuggestedStop
import xyz.ksharma.krail.trip.planner.ui.intro.SuggestedStops
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroState
import kotlin.test.assertTrue

/**
 * Step 2 at 320 dp, the smallest window the app supports, with text at 200 percent. Every row stays
 * whole inside the window, a long stop name wraps inside its own row, every target is at least 48 dp
 * tall, and the sections stack, so nothing is clipped, squeezed or pushed sideways.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = "w320dp-h1280dp-xxhdpi",
    manifest = Config.NONE,
)
class IntroFirstTripStepNarrowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val shown = mutableStateOf(PreviewTripNothingChosen)
    private var contentSet = false

    private val longName = "Cherrybrook Station and Bus Interchange Long Name Test Stop"
    private val suggestions = SuggestedStops(
        start = listOf(SuggestedStop("1", longName), SuggestedStop("2", "Central Station")),
        end = listOf(SuggestedStop("3", longName), SuggestedStop("4", "Wynyard Station")),
    )

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
                            suggestedStops = suggestions,
                            readiness = StopsReadiness.Ready(stopCount = 100),
                            onEvent = {},
                            firstTripStep = introFirstTripStep(onSearchStop = {}),
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun windowWidth(): Dp = composeRule.onRoot().getUnclippedBoundsInRoot().width

    private fun SemanticsNodeInteraction.assertInsideWindow(what: String): SemanticsNodeInteraction {
        val bounds = getUnclippedBoundsInRoot()
        val width = windowWidth()
        assertTrue(bounds.left >= 0.dp, "$what starts left of the window at ${bounds.left}")
        assertTrue(bounds.right <= width, "$what ends at ${bounds.right}, past the $width window")
        return this
    }

    private fun SemanticsNodeInteraction.assertAtLeast48dpTall(what: String): SemanticsNodeInteraction {
        val height = getUnclippedBoundsInRoot().height
        assertTrue(height >= 48.dp, "$what is only $height tall")
        return this
    }

    @Test
    fun `the window is 320 dp wide, so the checks below mean something`() {
        show(PreviewTripNothingChosen)

        assertTrue(windowWidth() == 320.dp, "The window is ${windowWidth()} wide")
    }

    @Test
    fun `the start list stays whole, with a long name wrapped inside its own row`() {
        show(PreviewTripNothingChosen)

        composeRule.onNodeWithText(FIRST_TRIP_HEADING).assertIsDisplayed().assertInsideWindow("the heading")
        suggestions.start.forEach { stop ->
            composeRule.onNodeWithContentDescription(pickStopDescription(IntroStopRole.START, stop.name))
                .performScrollTo()
                .assertInsideWindow(stop.name)
                .assertAtLeast48dpTall(stop.name)
        }
        composeRule.onNodeWithContentDescription(searchStopDescription(IntroStopRole.START))
            .performScrollTo()
            .assertInsideWindow(SEARCH_ANOTHER_STOP)
            .assertAtLeast48dpTall(SEARCH_ANOTHER_STOP)
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).assertIsDisplayed()
            .assertInsideWindow("the bottom button")
    }

    @Test
    fun `a pre-filled start and the end list stack, one section under the other`() {
        show(PreviewTripPrefilled)

        val from = composeRule.onNodeWithText(FIRST_TRIP_FROM).getUnclippedBoundsInRoot()
        val chosen = composeRule
            .onNodeWithContentDescription(changeStopDescription(IntroStopRole.START, "Tallawong Station"))
            .assertInsideWindow("the collapsed start")
            .assertAtLeast48dpTall("the collapsed start")
            .getUnclippedBoundsInRoot()
        val to = composeRule.onNodeWithText(FIRST_TRIP_TO).getUnclippedBoundsInRoot()

        assertTrue(chosen.top >= from.bottom, "The row starts at ${chosen.top}, above From ends at ${from.bottom}")
        assertTrue(to.top >= chosen.bottom, "To starts at ${to.top}, above the row ends at ${chosen.bottom}")
        assertTrue(to.left == from.left, "To starts at ${to.left} and From at ${from.left}")
    }

    @Test
    fun `the tile keeps both names whole and stacked inside the window`() {
        show(PreviewTripBothChosen)

        val tile = composeRule.onNodeWithContentDescription(
            changeTripDescription("Tallawong Station", "Central Station"),
        ).assertInsideWindow("the tile").assertAtLeast48dpTall("the tile")
        tile.assertIsDisplayed()

        val start = composeRule.onNodeWithText("Tallawong Station", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val end = composeRule.onNodeWithText("Central Station", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(end.top >= start.bottom, "The end starts at ${end.top}, above the start ends at ${start.bottom}")
        assertTrue(end.left == start.left, "The end starts at ${end.left} and the start at ${start.left}")
    }

    @Test
    fun `the open lists keep every target whole at large text`() {
        show(PreviewTripBothChosen)
        composeRule.onNodeWithContentDescription(changeTripDescription("Tallawong Station", "Central Station"))
            .performClick()

        suggestions.end.forEach { stop ->
            composeRule.onNodeWithContentDescription(pickStopDescription(IntroStopRole.END, stop.name))
                .performScrollTo()
                .assertInsideWindow(stop.name)
                .assertAtLeast48dpTall(stop.name)
        }
    }

    private companion object {
        const val LARGE_TEXT = 2.0f
    }
}
