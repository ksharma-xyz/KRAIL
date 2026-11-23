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
import kotlin.test.assertTrue

/**
 * The theme screen in a short, narrow window, 320 by 568 dp with text at 200 percent, where the headline and the badges
 * together are taller than the room above the bar. The middle zone scrolls, so every badge can be brought fully into view
 * above the bar, and none is ever cut off behind the button.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = "w320dp-h568dp-xxhdpi",
    manifest = Config.NONE,
)
class IntroThemeBadgeGridShortTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val shown = mutableStateOf(IntroState(step = IntroStep.THEME, selectedThemeId = KrailThemeStyle.Train.id))

    private fun show() {
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
        composeRule.waitForIdle()
    }

    @Test
    fun `the window is short and narrow, and the headline and badges together are taller than the room above the bar`() {
        show()

        val window = composeRule.onRoot().getUnclippedBoundsInRoot()
        val barTop = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).getUnclippedBoundsInRoot().top
        val lastBadgeBottom = introThemeChoices().maxOf {
            composeRule.onNodeWithTag(it.tag).getUnclippedBoundsInRoot().bottom
        }

        assertTrue(window.width == 320.dp && window.height == 568.dp, "the window is $window")
        assertTrue(lastBadgeBottom > barTop, "everything fits above the bar at $barTop, so the scroll is not being tested")
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
    fun `every badge scrolls fully into view above the bar, whole inside the window and never behind the button`() {
        show()

        val window = composeRule.onRoot().getUnclippedBoundsInRoot()
        introThemeChoices().forEach { choice ->
            scrollClearOfBar(choice.tag)
            composeRule.onNodeWithTag(choice.tag).assertIsDisplayed()

            val bounds = composeRule.onNodeWithTag(choice.tag).getUnclippedBoundsInRoot()
            val barTop = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).getUnclippedBoundsInRoot().top
            assertTrue(bounds.left >= 0.dp && bounds.right <= window.width, "${choice.name} is outside the window: $bounds")
            assertTrue(bounds.top >= 0.dp, "${choice.name} is above the top of the window: $bounds")
            assertTrue(bounds.bottom <= barTop + ROUNDING, "${choice.name} ends at ${bounds.bottom}, behind the bar at $barTop")
        }
    }

    private companion object {
        const val LARGE_TEXT = 2.0f
        val ROUNDING = 1.dp
    }
}
