package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class IntroFlowChromeTest {

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
}
