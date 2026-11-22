package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class IntroFlowChromeTest {

    @Test
    fun `the progress dots read one step of two to a screen reader`() {
        assertEquals("Step 1 of 2", introStepLabel(IntroStep.PARK_RIDE))
    }

    @Test
    fun `the progress dots read the second step of two on the trip step`() {
        assertEquals("Step 2 of 2", introStepLabel(IntroStep.FIRST_TRIP))
    }

    @Test
    fun `reduced motion swaps the steps with no fade and no size animation`() {
        val swap = introStepTransition(reduceMotion = true)

        assertEquals(EnterTransition.None, swap.targetContentEnter)
        assertEquals(ExitTransition.None, swap.initialContentExit)
        assertNull(swap.sizeTransform)
    }

    @Test
    fun `without reduced motion the step fades in and the old one fades out`() {
        val fade = introStepTransition(reduceMotion = false)

        assertNotEquals(EnterTransition.None, fade.targetContentEnter)
        assertNotEquals(ExitTransition.None, fade.initialContentExit)
        assertNull(fade.sizeTransform)
    }

    @Test
    fun `every action has its words and only the two waiting ones are disabled`() {
        val expected = mapOf(
            IntroFinishAction.Next to Pair("Next", true),
            IntroFinishAction.GettingReady to Pair("Getting ready", false),
            IntroFinishAction.PickBothStops to Pair("Save and start", false),
            IntroFinishAction.SaveAndStart to Pair("Save and start", true),
            IntroFinishAction.SkipForNow to Pair("Skip for now", true),
            IntroFinishAction.LetsKrail to Pair("Let's KRAIL", true),
        )

        assertEquals(IntroFinishAction.entries.toSet(), expected.keys, "A new action needs a row here")
        expected.forEach { (action, wording) ->
            assertEquals(wording.first, action.label(), "label of $action")
            assertEquals(wording.second, action.isEnabled(), "enabled of $action")
        }
    }
}
