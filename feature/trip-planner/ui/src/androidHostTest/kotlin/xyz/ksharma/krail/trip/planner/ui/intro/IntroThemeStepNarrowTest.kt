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
import xyz.ksharma.krail.taj.theme.KrailDimensions
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
                            onEvent = { _, _ -> },
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
    fun `every badge is whole inside the window and a full 48 dp to tap, in two columns centred across it`() {
        show(none)

        introThemeChoices().forEach { choice ->
            composeRule.onNodeWithTag(choice.tag).assertIsDisplayed().assertInsideWindow(choice.name)
        }
        val bounds = badgeBounds()
        bounds.forEach { assertTrue(it.width >= TOUCH_TARGET - ROUNDING, "a badge is only ${it.width} wide") }
        bounds.forEach { assertTrue(it.height >= TOUCH_TARGET - ROUNDING, "a badge is only ${it.height} high") }
        // Two to a row of the grid: the right column is Metro and Ferry, in line with each other and clear of the left.
        assertEquals(bounds[1].left.value, bounds[3].left.value, ROUNDING.value, "the right column is not in line: $bounds")
        assertTrue(bounds[1].left > bounds[0].right && bounds[1].left > bounds[2].right, "the columns touch: $bounds")
        // The grid as a whole is centred across the window.
        val leftmost = bounds.minOf { it.left }
        val rightmost = bounds.maxOf { it.right }
        assertEquals(leftmost.value, (windowWidth() - rightmost).value, ROUNDING.value, "the grid is not centred: $bounds")
    }

    @Test
    fun `the grid is loose, the right column half a badge lower and the second row moved in, in reading order`() {
        show(none)
        val b = badgeBounds()
        val badge = b[0].width

        assertEquals((badge / 2).value, (b[1].top - b[0].top).value, ROUNDING.value, "Metro is not half a badge below Train")
        assertEquals((badge / 2).value, (b[3].top - b[2].top).value, ROUNDING.value, "Ferry is not half a badge below Bus")
        assertTrue(b[2].top >= b[0].bottom, "Bus is not under Train: $b")
        assertEquals(SHIFT.value, (b[2].left - b[0].left).value, ROUNDING.value, "Bus is not moved in from Train: $b")
    }

    @Test
    fun `at 200 percent a badge is still a plain 64 dp, whole at 320 dp, in the same two columns as on a phone`() {
        show(none)

        // The badge does not follow the text size, so it is the 64 dp it is at 100 percent.
        badgeBounds().forEach { assertEquals(64f, it.width.value, ROUNDING.value, "a badge is $it") }
        badgeBounds().forEach { assertEquals(64f, it.height.value, ROUNDING.value, "a badge is $it") }
        val b = badgeBounds()
        assertEquals(168f, b.maxOf { it.right }.value - b.minOf { it.left }.value, ROUNDING.value, "the grid is not the phone's: $b")
    }

    @Test
    fun `no two badges touch, and each is at least 24 dp from every other, so the drift and the dance cannot reach one`() {
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
        assertTrue(
            badgeBounds().all { it.top >= headlineBottom + introRhythm(KrailDimensions()).titleToMiddle - ROUNDING },
            "a badge is closer than the gap under the headline: $headlineBottom ${badgeBounds()}",
        )
        val badgesBottom = badgeBounds().maxOf { it.bottom }

        // The button is not shown until a theme is picked, and then it is in the room that was kept for it.
        show(none.copy(selectedThemeId = KrailThemeStyle.Train.id))
        val primary = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION)
        primary.assertIsDisplayed().assertInsideWindow(THEME_CONFIRM)
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
        val MIN_GAP = 24.dp
        val SHIFT = 16.dp
        val ROUNDING = 1.dp
    }
}
