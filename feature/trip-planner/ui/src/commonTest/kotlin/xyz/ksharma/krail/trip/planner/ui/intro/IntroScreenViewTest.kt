package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class IntroScreenViewTest {

    private class Row(val name: String, val previous: IntroStep?, val next: IntroStep, val tracked: Boolean)

    private val rows = listOf(
        // Entering the Intro sends the name once, on whichever screen it opens on.
        Row("the first screen is Park and Ride", null, IntroStep.PARK_RIDE, true),
        Row("the first screen after a killed process, the theme screen", null, IntroStep.THEME, true),
        Row("the first screen after a killed process, the confirm screen", null, IntroStep.CONFIRM, true),
        // The trip screens are one screen name between them.
        Row("destination to origin", IntroStep.DESTINATION, IntroStep.ORIGIN, false),
        Row("origin to confirm", IntroStep.ORIGIN, IntroStep.CONFIRM, false),
        Row("destination to confirm, origin passed over", IntroStep.DESTINATION, IntroStep.CONFIRM, false),
        Row("confirm back to destination", IntroStep.CONFIRM, IntroStep.DESTINATION, false),
        // A change of name is a new screen view, and the theme screen is the Intro name again after the trip screens.
        Row("Park and Ride to destination", IntroStep.PARK_RIDE, IntroStep.DESTINATION, true),
        Row("confirm to theme", IntroStep.CONFIRM, IntroStep.THEME, true),
        Row("destination back to Park and Ride", IntroStep.DESTINATION, IntroStep.PARK_RIDE, true),
        Row("theme back to confirm", IntroStep.THEME, IntroStep.CONFIRM, true),
        // The two Intro screens share a name, so one straight after the other sends nothing more.
        Row("Park and Ride to theme", IntroStep.PARK_RIDE, IntroStep.THEME, false),
        Row("theme to Park and Ride", IntroStep.THEME, IntroStep.PARK_RIDE, false),
    )

    @Test
    fun `a screen view is sent on entering the Intro and when the screen name changes, and never twice for one name`() {
        rows.forEach { row ->
            assertEquals(row.tracked, shouldTrackScreenView(row.previous, row.next), row.name)
        }
    }

    @Test
    fun `walking the whole flow forward sends the Intro name at each end and the trip name once between`() {
        val order = IntroStep.entries
        val sent = order.indices.filter { shouldTrackScreenView(order.getOrNull(it - 1), order[it]) }

        assertEquals(
            listOf(IntroStep.PARK_RIDE, IntroStep.DESTINATION, IntroStep.THEME),
            sent.map { order[it] },
        )
        assertNotEquals(IntroStep.PARK_RIDE.toAnalyticsScreen(), IntroStep.DESTINATION.toAnalyticsScreen())
        assertEquals(IntroStep.PARK_RIDE.toAnalyticsScreen(), IntroStep.THEME.toAnalyticsScreen())
        assertEquals(AnalyticsScreen.Intro, IntroStep.THEME.toAnalyticsScreen(), "no new screen name")
    }

    @Test
    fun `no step maps to a screen name other than the two the funnel already reads`() {
        assertEquals(
            setOf(AnalyticsScreen.Intro, AnalyticsScreen.IntroFirstTrip),
            IntroStep.entries.map { it.toAnalyticsScreen() }.toSet(),
        )
    }
}
