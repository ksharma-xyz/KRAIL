package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
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
import xyz.ksharma.krail.taj.theme.KrailDimensions
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The destination and origin screens at 320 dp, the smallest window the app supports, with text at 200 percent.
 * It is drawn as a preview is, with no motion. The field and every row are whole inside the window, the screen
 * scrolls so the last row can be reached, and the bar stays pinned below the list with both whole buttons inside
 * the window, stacked or in one row, and never squeezed.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = "w320dp-h640dp-xxhdpi",
    manifest = Config.NONE,
)
class IntroStopStepNarrowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val shown = mutableStateOf(PreviewDestinationSuggestions)
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

    private fun SemanticsNodeInteraction.assertInsideWindowWidth(what: String) {
        val bounds = getUnclippedBoundsInRoot()
        assertTrue(bounds.left >= 0.dp, "$what starts left of the window at ${bounds.left}")
        assertTrue(bounds.right <= windowWidth(), "$what ends at ${bounds.right}, past the ${windowWidth()} window")
    }

    private fun row(stopId: String) = composeRule.onNodeWithTag(TripPlannerTestTags.introStop(stopId))

    @Test
    fun `the window is 320 dp wide, so the checks below mean something`() {
        show(PreviewDestinationSuggestions)

        assertEquals(320.dp, windowWidth())
    }

    @Test
    fun `the headline, the field and every row are whole across the width at 200 percent text`() {
        show(PreviewDestinationSuggestions)

        composeRule.onNodeWithText(DESTINATION_HEADING).assertInsideWindowWidth("headline")
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_STOP_FIELD).assertInsideWindowWidth("field")
        DEFAULT_SUGGESTED_STOPS.end.forEach { stop ->
            row(stop.stopId).performScrollTo().assertInsideWindowWidth(stop.name)
        }
    }

    @Test
    fun `the screen scrolls so the last row can be reached, and the bar stays where it was`() {
        show(PreviewDestinationSelected)
        val primary = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION)
        val before = primary.getUnclippedBoundsInRoot()

        row(DEFAULT_SUGGESTED_STOPS.end.last().stopId).performScrollTo().assertIsDisplayed()

        assertEquals(before, primary.getUnclippedBoundsInRoot(), "the bar moved when the list scrolled")
    }

    @Test
    fun `the list reads in order below the field and above the bar`() {
        show(PreviewOriginNearby.copy(selectedStop = PreviewOriginNearby.nearbyStops.first()))

        val field = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_STOP_FIELD).getUnclippedBoundsInRoot()
        val first = row(PreviewOriginNearby.nearbyStops.first().stopId).getUnclippedBoundsInRoot()
        val bar = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).getUnclippedBoundsInRoot()

        assertTrue(first.top >= field.bottom - ROUNDING, "the first row starts above the field ends: $field $first")
        assertTrue(bar.top >= field.bottom - ROUNDING, "the bar is above the field: $bar $field")
    }

    @Test
    fun `at 200 percent the bar is the one whole button across the window, once a stop is selected`() {
        show(PreviewDestinationSuggestions)
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).assertDoesNotExist()

        show(PreviewDestinationSelected)

        val primary = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION)
        primary.assertInsideWindowWidth(STOP_CHOOSE)
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_SECONDARY_ACTION).assertDoesNotExist()
        val p = primary.getUnclippedBoundsInRoot()
        assertTrue(p.width > composeRule.onRoot().getUnclippedBoundsInRoot().width / 2, "not across the window: $p")
    }

    @Test
    fun `the field is clear of the title and the first row is clear of the field, at 200 percent on 320 dp`() {
        show(PreviewDestinationSuggestions)
        val rhythm = introRhythm(KrailDimensions())

        val title = composeRule.onNodeWithText(DESTINATION_HEADING).getUnclippedBoundsInRoot()
        val field = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_STOP_FIELD).getUnclippedBoundsInRoot()
        val first = row(DEFAULT_SUGGESTED_STOPS.end.first().stopId).getUnclippedBoundsInRoot()

        assertTrue(field.top - title.bottom >= rhythm.titleToMiddle - ROUNDING, "the field is ${field.top - title.bottom} under the title")
        assertTrue(first.top - field.bottom >= rhythm.fieldToList - ROUNDING, "the first row is ${first.top - field.bottom} under the field")
    }

    @Test
    fun `the rows keep a clear gap between their names, selected or not`() {
        show(PreviewDestinationSelected)

        val rows = DEFAULT_SUGGESTED_STOPS.end.take(3).map { row(it.stopId).getUnclippedBoundsInRoot() }

        rows.zipWithNext().forEach { (above, below) ->
            assertTrue(below.top >= above.bottom - ROUNDING, "two rows overlap: $above $below")
        }
    }

    @Test
    fun `the no match line is whole inside the window`() {
        show(PreviewDestinationNoMatch)

        composeRule.onNodeWithText(STOP_NO_MATCH).assertInsideWindowWidth("no match line")
    }

    private companion object {
        const val LARGE_TEXT = 2.0f
        val ROUNDING = 1.dp
    }
}
