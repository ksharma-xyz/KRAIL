package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Each of the five Intro screens has its own name and is reported once, the first time it is on screen in a run.
 * The two older names, `Intro` and `IntroFirstTrip`, are sent by no step.
 */
class IntroScreenViewTest {

    @Test
    fun `every step has its own screen name, in the order a rider sees them`() {
        assertEquals(
            listOf(
                AnalyticsScreen.IntroParkRide,
                AnalyticsScreen.IntroDestination,
                AnalyticsScreen.IntroOrigin,
                AnalyticsScreen.IntroConfirm,
                AnalyticsScreen.IntroTheme,
            ),
            IntroStep.entries.map { it.toAnalyticsScreen() },
        )
        assertEquals(
            listOf("IntroParkRide", "IntroDestination", "IntroOrigin", "IntroConfirm", "IntroTheme"),
            IntroStep.entries.map { it.toAnalyticsScreen().name },
        )
        assertEquals(IntroStep.entries.size, IntroStep.entries.map { it.toAnalyticsScreen() }.toSet().size)
    }

    @Test
    fun `no step reports the older Intro names`() {
        val names = IntroStep.entries.map { it.toAnalyticsScreen() }.toSet()

        assertEquals(false, AnalyticsScreen.Intro in names)
        assertEquals(false, AnalyticsScreen.IntroFirstTrip in names)
    }

    @Test
    fun `a screen is reported the first time and never again in a run`() {
        IntroStep.entries.forEach { step ->
            assertEquals(true, shouldTrackScreenView(emptySet(), step), "$step first time")
            assertEquals(false, shouldTrackScreenView(setOf(step), step), "$step second time")
            assertEquals(
                false,
                shouldTrackScreenView(IntroStep.entries.toSet(), step),
                "$step after every step has been shown",
            )
        }
    }

    @Test
    fun `a screen is still reported when other screens have been`() {
        assertEquals(true, shouldTrackScreenView(setOf(IntroStep.PARK_RIDE), IntroStep.DESTINATION))
        assertEquals(
            true,
            shouldTrackScreenView(setOf(IntroStep.PARK_RIDE, IntroStep.DESTINATION), IntroStep.CONFIRM),
            "a screen passed over, the origin, does not stop later ones",
        )
    }

    @Test
    fun `a resume mid flow reports the step it resumes on, as the first of a new run`() {
        assertEquals(true, shouldTrackScreenView(emptySet(), IntroStep.CONFIRM))
    }
}
