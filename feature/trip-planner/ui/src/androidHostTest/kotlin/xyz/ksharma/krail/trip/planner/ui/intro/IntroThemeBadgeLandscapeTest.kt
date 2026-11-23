package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The theme screen on a phone turned on its side, 640 by 360 dp, at 100 percent text and at 200 percent. The content is the 480 dp
 * column and the wave of badges is 112 dp tall, but the headline and the badges together are still taller than the room above the
 * bar, so the middle zone scrolls and every badge can be brought fully into view above the bar, and none is ever cut off behind the
 * button. The badges keep their wave at this width, spread over the 448 dp room.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = "w640dp-h360dp-xxhdpi",
    manifest = Config.NONE,
)
class IntroThemeBadgeLandscapeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val shown = mutableStateOf(IntroState(step = IntroStep.THEME, selectedThemeId = KrailThemeStyle.Train.id))

    private fun showAt(textScale: Float) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = textScale),
                LocalInspectionMode provides true,
            ) {
                PreviewTheme {
                    IntroFlowScreen(
                        state = shown.value,
                        suggestedStops = DEFAULT_SUGGESTED_STOPS,
                        onEvent = { _, _ -> },
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `the window is wide and short, and the badges are the wave of one row, 112 dp tall`() {
        showAt(LARGE_TEXT)

        val window = composeRule.onRoot().getUnclippedBoundsInRoot()
        val bounds = introThemeChoices().map { composeRule.onNodeWithTag(it.tag).getUnclippedBoundsInRoot() }

        assertTrue(window.width == 640.dp && window.height == 360.dp, "the window is $window")
        // The chosen badge, Train here, sits lifted 4 dp, so that is the slack.
        assertEquals(
            112f,
            (
                bounds.maxOf {
                    it.bottom
                } - bounds.minOf { it.top }
                ).value,
            LIFT.value + ROUNDING.value,
            "the wave is not 112 dp tall: $bounds",
        )
        assertEquals(
            bounds[0].top.value,
            bounds[2].top.value,
            LIFT.value + ROUNDING.value,
            "Train and Bus are not in line: $bounds",
        )
        assertEquals(
            bounds[1].top.value,
            bounds[3].top.value,
            ROUNDING.value,
            "Metro and Ferry are not in line: $bounds",
        )
    }

    // The bar is over the bottom of the content now, so a badge scrolled into view can be under it. The content has room
    // at its end for the bar, so scrolling on by what is under the bar always brings the badge clear of it.
    private fun scrollClearOfBar(tag: String) {
        composeRule.onNodeWithTag(tag).performScrollTo()
        val barTop = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).getUnclippedBoundsInRoot().top
        val under = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot().bottom - barTop
        if (under > 0.dp) {
            val px = with(composeRule.density) { under.toPx() }
            composeRule.onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, px) }
            composeRule.waitForIdle()
        }
    }

    @Test
    fun `at 200 percent every badge scrolls fully into view above the bar, whole inside the window and never behind the button`() {
        showAt(LARGE_TEXT)
        everyBadgeScrollsClearOfTheBar()
    }

    @Test
    fun `at 100 percent every badge is whole inside the window and clear of the bar once scrolled into view`() {
        showAt(1f)
        everyBadgeScrollsClearOfTheBar()
    }

    private fun everyBadgeScrollsClearOfTheBar() {
        val window = composeRule.onRoot().getUnclippedBoundsInRoot()
        introThemeChoices().forEach { choice ->
            scrollClearOfBar(choice.tag)
            composeRule.onNodeWithTag(choice.tag).assertIsDisplayed()

            val bounds = composeRule.onNodeWithTag(choice.tag).getUnclippedBoundsInRoot()
            val barTop = composeRule.onNodeWithTag(
                TripPlannerTestTags.INTRO_PRIMARY_ACTION,
            ).getUnclippedBoundsInRoot().top
            assertTrue(
                bounds.left >= 0.dp && bounds.right <= window.width,
                "${choice.name} is outside the window: $bounds",
            )
            assertTrue(bounds.top >= 0.dp, "${choice.name} is above the top of the window: $bounds")
            assertTrue(
                bounds.bottom <= barTop + ROUNDING,
                "${choice.name} ends at ${bounds.bottom}, behind the bar at $barTop",
            )
        }
    }

    private companion object {
        const val LARGE_TEXT = 2.0f
        val ROUNDING = 1.dp
        val LIFT = 4.dp
    }
}
