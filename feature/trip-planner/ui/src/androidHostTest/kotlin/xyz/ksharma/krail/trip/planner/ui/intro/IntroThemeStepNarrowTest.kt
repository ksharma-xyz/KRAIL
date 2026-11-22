package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
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
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The theme screen at 320 dp, the smallest window the app supports, with text at 200 percent. It is drawn as a
 * preview is, with no motion, so every position is the settled one and a distance between two badges is the
 * least they are ever apart before they drift. The badges wrap whole, nothing is clipped or squeezed, and the bar
 * keeps both whole buttons inside the window.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = "w320dp-h1280dp-xxhdpi",
    manifest = Config.NONE,
)
class IntroThemeStepNarrowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val none = IntroState(step = IntroStep.THEME)
    private val shown = mutableStateOf(none)
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

    private fun SemanticsNodeInteraction.assertInsideWindow(what: String) {
        val bounds = getUnclippedBoundsInRoot()
        assertTrue(bounds.left >= 0.dp, "$what starts left of the window at ${bounds.left}")
        assertTrue(bounds.right <= windowWidth(), "$what ends at ${bounds.right}, past the ${windowWidth()} window")
    }

    private fun badgeBounds() = introThemeChoices().map { composeRule.onNodeWithTag(it.tag).getUnclippedBoundsInRoot() }

    @Test
    fun `the window is 320 dp wide, so the checks below mean something`() {
        show(none)

        assertEquals(320.dp, windowWidth())
    }

    @Test
    fun `every badge is whole inside the window, a full 48 dp to tap, and wraps whole if the row is full`() {
        show(none)

        introThemeChoices().forEach { choice ->
            composeRule.onNodeWithTag(choice.tag).assertIsDisplayed().assertInsideWindow(choice.name)
        }
        val bounds = badgeBounds()
        bounds.forEach { assertTrue(it.width >= TOUCH_TARGET - ROUNDING, "a badge is only ${it.width} wide") }
        bounds.forEach { assertTrue(it.height >= TOUCH_TARGET - ROUNDING, "a badge is only ${it.height} high") }
        val rows = bounds.groupBy { it.top }.values.map { it.size }
        assertTrue(rows.size in 1..2 && rows.sum() == 4, "the badges are in rows of $rows")
    }

    @Test
    fun `no two badges touch, and each is at least 16 dp from the next, in a row and between rows`() {
        show(none)
        val bounds = badgeBounds()

        bounds.forEachIndexed { i, a ->
            bounds.drop(i + 1).forEach { b ->
                val gapX = maxOf(a.left - b.right, b.left - a.right)
                val gapY = maxOf(a.top - b.bottom, b.top - a.bottom)
                assertTrue(maxOf(gapX, gapY) >= MIN_GAP - ROUNDING, "two badges are $gapX across and $gapY down: $a $b")
            }
        }
    }

    @Test
    fun `the headline, the badges and the bar read in that order down the screen, all inside the window`() {
        show(none)

        val headline = composeRule.onNodeWithText(PICK_THEME_HEADING)
        headline.assertInsideWindow("headline")
        val headlineBottom = headline.getUnclippedBoundsInRoot().bottom
        assertTrue(badgeBounds().all { it.top >= headlineBottom - ROUNDING })
        val badgesBottom = badgeBounds().maxOf { it.bottom }

        // The button is not shown until a theme is picked, and then it is in the room that was kept for it.
        show(none.copy(selectedThemeId = KrailThemeStyle.Train.id))
        val primary = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION)
        primary.assertIsDisplayed().assertInsideWindow("Yes, this one")
        assertTrue(badgesBottom <= primary.getUnclippedBoundsInRoot().top + ROUNDING)
    }

    @Test
    fun `at 200 percent the bar is the one whole button across the window, and nothing sits beside it`() {
        show(none.copy(selectedThemeId = KrailThemeStyle.Train.id))

        val primary = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION)
        primary.assertTextEquals(THEME_CONFIRM)
        composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_SECONDARY_ACTION).assertDoesNotExist()

        val p = primary.getUnclippedBoundsInRoot()
        assertTrue(p.width > windowWidth() / 2, "the one button is not across the window: $p in ${windowWidth()}")
        assertEquals(p.left, windowWidth() - p.right, "the button is not centred: $p in ${windowWidth()}")
    }

    @Test
    fun `the longest tagline stays whole inside the window`() {
        introThemeChoices().forEach { choice ->
            show(none.copy(selectedThemeId = choice.style.id))

            composeRule.onNodeWithText(choice.style.tagLine).assertInsideWindow(choice.style.tagLine)
        }
    }

    private companion object {
        const val LARGE_TEXT = 2.0f
        val TOUCH_TARGET = 48.dp
        val MIN_GAP = 16.dp
        val ROUNDING = 1.dp
    }
}
