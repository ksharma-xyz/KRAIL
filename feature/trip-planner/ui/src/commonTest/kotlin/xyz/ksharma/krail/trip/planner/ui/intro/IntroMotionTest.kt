package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntroMotionTest {

    @Test
    fun `the direction is decided by the order of the two screens for every pair`() {
        IntroStep.entries.forEach { from ->
            IntroStep.entries.forEach { to ->
                val expected = when {
                    to.ordinal > from.ordinal -> IntroSlide.Forward
                    to.ordinal < from.ordinal -> IntroSlide.Back
                    else -> IntroSlide.None
                }

                assertEquals(expected, introSlideDirection(from, to), "$from to $to")
            }
        }
    }

    @Test
    fun `forward goes in from the right, back is the mirror image, and a change of nothing does not move`() {
        assertEquals(IntroSlide.Forward, introSlideDirection(IntroStep.PARK_RIDE, IntroStep.DESTINATION))
        assertEquals(IntroSlide.Back, introSlideDirection(IntroStep.DESTINATION, IntroStep.PARK_RIDE))
        assertEquals(IntroSlide.None, introSlideDirection(IntroStep.PARK_RIDE, IntroStep.PARK_RIDE))
    }

    @Test
    fun `the transition spec is the one the table says`() {
        val forward = IntroTransitionSpec(slides = true, enterFrom = 1, exitTo = -1, exitMs = 150)
        val back = IntroTransitionSpec(slides = true, enterFrom = -1, exitTo = 1, exitMs = 150)
        val fade = IntroTransitionSpec(slides = false, enterFrom = 0, exitTo = 0, exitMs = 90)
        val rows = listOf(
            Triple(IntroSlide.Forward, false, forward),
            Triple(IntroSlide.Back, false, back),
            Triple(IntroSlide.None, false, fade),
            // Reduced motion is the plain cross-fade, however the screens are ordered.
            Triple(IntroSlide.Forward, true, fade),
            Triple(IntroSlide.Back, true, fade),
            Triple(IntroSlide.None, true, fade),
        )

        rows.forEach { (slide, reduceMotion, spec) ->
            assertEquals(spec, introTransitionSpec(slide, reduceMotion), "$slide reduceMotion=$reduceMotion")
        }
    }

    @Test
    fun `back enters from where forward leaves, and the old screen always leaves quickly`() {
        val forward = introTransitionSpec(IntroSlide.Forward, reduceMotion = false)
        val back = introTransitionSpec(IntroSlide.Back, reduceMotion = false)

        assertEquals(forward.enterFrom, -back.enterFrom)
        assertEquals(forward.exitTo, -back.exitTo)
        assertTrue(forward.exitMs <= MAX_EXIT_MS && back.exitMs <= MAX_EXIT_MS)
    }

    @Test
    fun `a slide enters and exits with a transition and moves nothing in the plain fade`() {
        val slide = introScreenTransition(IntroStep.PARK_RIDE, IntroStep.DESTINATION, reduceMotion = false)
        assertNotEquals(EnterTransition.None, slide.targetContentEnter)
        assertNotEquals(ExitTransition.None, slide.initialContentExit)

        val fade = introScreenTransition(IntroStep.PARK_RIDE, IntroStep.DESTINATION, reduceMotion = true)
        assertNotEquals(EnterTransition.None, fade.targetContentEnter, "a short cross-fade, not a swap")
        assertNotEquals(ExitTransition.None, fade.initialContentExit)
        assertNull(fade.sizeTransform)
    }

    private companion object {
        const val MAX_EXIT_MS = 200
    }
}
