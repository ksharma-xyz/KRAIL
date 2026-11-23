package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import org.junit.Rule
import org.junit.Test
import xyz.ksharma.krail.taj.theme.KrailDimensions
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.math.max
import kotlin.math.min
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The checks every window size shares for where the theme badges sit: centred on the middle of the window, clear of the
 * headline above and of the bar below, and the same whichever headline is shown. A subclass gives the window, in its
 * own Robolectric config, and the text size. The screen is drawn as a preview is, with no motion, so every badge is at its
 * settled place. The window has no system bars here, so the window middle is the middle of the root.
 */
abstract class IntroThemeCentreChecks(private val textScale: Float) {

    @get:Rule
    val composeRule = createComposeRule()

    private val none = IntroState(step = IntroStep.THEME)
    private val shown = mutableStateOf(none)
    private var contentSet = false
    private val rhythm = introRhythm(KrailDimensions())
    private val barPadding = KrailDimensions().spacingL

    protected fun show(state: IntroState) {
        shown.value = state
        if (!contentSet) {
            contentSet = true
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
                            onEvent = {},
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun badgeBounds() = introThemeChoices().map { composeRule.onNodeWithTag(it.tag).getUnclippedBoundsInRoot() }

    protected fun gridTop(): Dp = badgeBounds().minOf { it.top }

    protected fun gridBottom(): Dp = badgeBounds().maxOf { it.bottom }

    private fun gridHeight(): Dp = gridBottom() - gridTop()

    private fun windowMiddle(): Dp = composeRule.onRoot().getUnclippedBoundsInRoot().height / 2

    // The bottom of the headline zone: its text and the one gap under it.
    private fun headlineBottom(): Dp =
        composeRule.onNodeWithText(PICK_THEME_HEADING).getUnclippedBoundsInRoot().bottom + rhythm.titleToMiddle

    // The top of the bar's slot, which is there whether or not the bar is shown. The one button is in it with room above.
    private fun barSlotTop(): Dp {
        val keep = shown.value
        show(none.copy(selectedThemeId = KrailThemeStyle.Train.id))
        val top = composeRule.onNodeWithTag(TripPlannerTestTags.INTRO_PRIMARY_ACTION).getUnclippedBoundsInRoot().top - barPadding
        show(keep)
        return top
    }

    protected fun expectedGridTop(): Dp {
        val ideal = windowMiddle() - gridHeight() / 2
        val lowest = barSlotTop() - rhythm.middleBottom - gridHeight()
        return max(headlineBottom().value, min(ideal.value, lowest.value)).dp
    }

    protected fun fitsBetweenHeadlineAndBar(): Boolean {
        val ideal = windowMiddle() - gridHeight() / 2
        return ideal >= headlineBottom() && ideal <= barSlotTop() - rhythm.middleBottom - gridHeight()
    }

    @Test
    fun `the grid is where the placement puts it, centred on the window middle and moved only to clear the headline and the bar`() {
        show(none)

        assertEquals(expectedGridTop().value, gridTop().value, TOLERANCE.value, "the grid top, with the window ${windowMiddle()} tall in half")
    }

    @Test
    fun `the grid centre is the middle of the window when it fits, and the grid starts a gap under the headline when it does not`() {
        show(none)

        if (fitsBetweenHeadlineAndBar()) {
            val centre = (gridTop() + gridBottom()) / 2
            assertEquals(windowMiddle().value, centre.value, TOLERANCE.value, "the grid centre is $centre, the window middle ${windowMiddle()}")
        } else {
            assertEquals(headlineBottom().value, gridTop().value, TOLERANCE.value, "the grid is not right under the headline")
        }
    }

    /** True when the window is tall enough to hold the grid on its middle with the headline and the bar clear. */
    protected fun assertWindowHoldsGridOnItsMiddle() {
        show(none)
        assertTrue(fitsBetweenHeadlineAndBar(), "the grid does not fit between the headline and the bar at this size")
    }

    @Test
    fun `the grid is no nearer the headline than the one gap under it`() {
        show(none)

        assertTrue(gridTop() >= headlineBottom() - TOLERANCE, "the grid starts at ${gridTop()}, the headline zone ends at ${headlineBottom()}")
    }

    @Test
    fun `the grid does not move when the headline changes from the question to a tagline`() {
        show(none)
        val question = badgeBounds()

        introThemeChoices().forEachIndexed { picked, choice ->
            show(none.copy(selectedThemeId = choice.style.id))

            // The badge that is picked lifts 4 dp, which is its own pose and not the grid's place.
            badgeBounds().zip(question).filterIndexed { index, _ -> index != picked }.forEach { (now, before) ->
                assertEquals(before.top.value, now.top.value, SAME.value, "${choice.name} moved a badge vertically")
                assertEquals(before.left.value, now.left.value, SAME.value, "${choice.name} moved a badge sideways")
            }
        }
    }

    @Test
    fun `scrolled to the end the grid is one gap above the bar and never behind it, and no two badges overlap`() {
        show(none)
        val slotTop = barSlotTop()
        composeRule.onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, SCROLL_FAR_PX) }
        composeRule.waitForIdle()

        assertTrue(gridBottom() <= slotTop - rhythm.middleBottom + TOLERANCE, "the grid ends at ${gridBottom()}, the bar starts at $slotTop")
        val bounds = badgeBounds()
        bounds.forEachIndexed { i, a ->
            bounds.drop(i + 1).forEach { b ->
                val apart = maxOf(a.left - b.right, b.left - a.right, a.top - b.bottom, b.top - a.bottom)
                assertTrue(apart > 0.dp, "two badges overlap: $a $b")
            }
        }
    }

    private companion object {
        val TOLERANCE = 2.dp
        val SAME = 0.5.dp
        const val SCROLL_FAR_PX = 100_000f
    }
}
