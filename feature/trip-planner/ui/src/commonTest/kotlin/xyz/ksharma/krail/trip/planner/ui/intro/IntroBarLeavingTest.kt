package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.graphics.Color
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Which bar is leaving, as a table over every ordered pair of steps: a bar that was shown and whose step the Intro has left, while
 * the bar of the new step is not shown yet, leaves as it was, and nothing else is held. `IntroBarHoldTest` draws it.
 */
class IntroBarLeavingTest {

    private val shownStates = mapOf(
        IntroStep.PARK_RIDE to PreviewAskState,
        IntroStep.DESTINATION to PreviewDestinationSelected,
        IntroStep.ORIGIN to PreviewOriginNearby.copy(selectedStop = IntroStop("201510", "Redfern Station")),
        IntroStep.CONFIRM to PreviewConfirm,
        IntroStep.THEME to IntroState(step = IntroStep.THEME, selectedThemeId = KrailThemeStyle.Bus.id),
    )
    private val openStates = mapOf(
        IntroStep.PARK_RIDE to PreviewAskState,
        IntroStep.DESTINATION to PreviewDestinationSuggestions,
        IntroStep.ORIGIN to PreviewOriginNearby,
        IntroStep.CONFIRM to PreviewConfirm,
        IntroStep.THEME to PreviewThemeNoneState,
    )

    private val steps = IntroStep.entries.toList()

    private fun heldOn(step: IntroStep, fill: Color? = Color(step.ordinal + 1)): IntroHeldBar =
        IntroHeldBar(step, requireNotNull(introBar(shownStates.getValue(step))), fill)

    private fun newBarShown(to: IntroStep) = requireNotNull(introBar(openStates.getValue(to))).shown

    @Test
    fun `every step opens with its bar shown or hidden, as the table here says`() {
        assertEquals(
            mapOf(
                IntroStep.PARK_RIDE to true,
                IntroStep.DESTINATION to false,
                IntroStep.ORIGIN to false,
                IntroStep.CONFIRM to true,
                IntroStep.THEME to false,
            ),
            steps.associateWith { newBarShown(it) },
        )
        steps.forEach { assertEquals(true, requireNotNull(introBar(shownStates.getValue(it))).shown, "$it") }
    }

    @Test
    fun `for every ordered pair, the old bar leaves as it was when the new bar is not shown, and never otherwise`() {
        steps.forEach { from ->
            steps.filter { it != from }.forEach { to ->
                val held = heldOn(from)
                val leaving = introBarLeaving(held, to, currentShown = newBarShown(to))
                if (newBarShown(to)) {
                    assertNull(leaving, "a bar is held behind the shown bar of $to after $from")
                } else {
                    // The very bar that was shown, with its words, its events and its label pairs, and the fill it wore.
                    val kept = requireNotNull(leaving) { "no bar is leaving in $from to $to" }
                    assertSame(held, kept, "$from to $to")
                    assertEquals(from, kept.step)
                    assertEquals(shownStates.getValue(from).let(::introBar), kept.bar)
                    assertEquals(Color(from.ordinal + 1), kept.fill)
                }
            }
        }
    }

    @Test
    fun `the words held are the old step's and not the new step's`() {
        val toDestination = introBarLeaving(heldOn(IntroStep.PARK_RIDE), IntroStep.DESTINATION, currentShown = false)
        assertNotNull(toDestination)
        assertEquals(PARK_RIDE_NO, toDestination.bar.left?.label)
        assertEquals(PARK_RIDE_YES, toDestination.bar.right.label)

        val toTheme = introBarLeaving(heldOn(IntroStep.CONFIRM), IntroStep.THEME, currentShown = false)
        assertNotNull(toTheme)
        assertEquals(CONFIRM_NOT_NOW, toTheme.bar.left?.label)
        assertEquals(CONFIRM_SAVE, toTheme.bar.right.label)
        assertEquals(Color(IntroStep.CONFIRM.ordinal + 1), toTheme.fill)
    }

    @Test
    fun `on the same step nothing is held, because a bar there fades in and out in its own clothes`() {
        steps.forEach { step -> assertNull(introBarLeaving(heldOn(step), step, currentShown = false), "$step") }
        steps.forEach { step -> assertNull(introBarLeaving(heldOn(step), step, currentShown = true), "$step") }
    }

    @Test
    fun `with nothing held there is nothing leaving`() {
        steps.forEach { step -> assertNull(introBarLeaving(null, step, currentShown = false), "$step") }
    }

    @Test
    fun `the colour screen's bar has no fill of its own, so a bar leaving it is not given one`() {
        val held = heldOn(IntroStep.THEME, fill = null)

        val leaving = introBarLeaving(held, IntroStep.DESTINATION, currentShown = false)

        assertSame(held, leaving)
        assertNull(requireNotNull(leaving).fill)
    }

    @Test
    fun `the fade is the length the bar has always had`() {
        assertEquals(250, BAR_REVEAL_MS)
        assertEquals(0, introBarRevealMs(reduceMotion = true))
        assertEquals(BAR_REVEAL_MS, introBarRevealMs(reduceMotion = false))
    }
}
