package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class IntroScreenViewTest {

    private class Row(val name: String, val previous: IntroStep?, val next: IntroStep, val tracked: Boolean)

    private val rows = listOf(
        // Entering the Intro sends the name once, on whichever screen it opens on.
        Row("the first screen is the theme screen", null, IntroStep.THEME, true),
        Row("the first screen is Park and Ride", null, IntroStep.PARK_RIDE, true),
        Row("the first screen after a killed process", null, IntroStep.CONFIRM, true),
        // The theme and Park and Ride screens are both the Intro, in either direction.
        Row("theme to Park and Ride", IntroStep.THEME, IntroStep.PARK_RIDE, false),
        Row("Park and Ride back to theme", IntroStep.PARK_RIDE, IntroStep.THEME, false),
        // The trip screens are one screen name between them.
        Row("destination to origin", IntroStep.DESTINATION, IntroStep.ORIGIN, false),
        Row("origin to confirm", IntroStep.ORIGIN, IntroStep.CONFIRM, false),
        Row("confirm back to destination", IntroStep.CONFIRM, IntroStep.DESTINATION, false),
        // A change of name is a new screen view.
        Row("Park and Ride to destination", IntroStep.PARK_RIDE, IntroStep.DESTINATION, true),
        Row("theme to destination, Park and Ride passed over", IntroStep.THEME, IntroStep.DESTINATION, true),
        Row("destination back to Park and Ride", IntroStep.DESTINATION, IntroStep.PARK_RIDE, true),
        Row("destination back to theme", IntroStep.DESTINATION, IntroStep.THEME, true),
    )

    @Test
    fun `a screen view is sent on entering the Intro and when the screen name changes, and never twice for one name`() {
        rows.forEach { row ->
            assertEquals(row.tracked, shouldTrackScreenView(row.previous, row.next), row.name)
        }
    }

    @Test
    fun `walking the whole flow forward sends the Intro name once and the trip name once`() {
        val order = IntroStep.entries
        val sent = order.indices.filter { shouldTrackScreenView(order.getOrNull(it - 1), order[it]) }

        assertEquals(
            listOf(IntroStep.THEME, IntroStep.DESTINATION),
            sent.map { order[it] },
        )
        assertNotEquals(IntroStep.THEME.toAnalyticsScreen(), IntroStep.DESTINATION.toAnalyticsScreen())
    }
}
