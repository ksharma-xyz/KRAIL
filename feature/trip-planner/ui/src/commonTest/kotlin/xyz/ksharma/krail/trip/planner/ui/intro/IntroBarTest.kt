package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAvailabilityState
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardStation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntroBarTest {

    private val unknown = StopsReadiness.Unknown
    private val loading = StopsReadiness.Loading
    private val ready = StopsReadiness.Ready(stopCount = 100)
    private val failed = StopsReadiness.Failed(attempt = 1, reason = "disk full")

    private val ask = IntroBar(
        left = IntroBarAction("No thanks", IntroUiEvent.AnswerParkRide(yes = false), compactLabel = "No"),
        right = IntroBarAction(
            "Yes, show me free spots",
            IntroUiEvent.AnswerParkRide(yes = true),
            compactLabel = "Yes",
        ),
    )

    private val parkRideRows = listOf(
        // Asking. The stops never matter on this screen.
        Row("unanswered", parkRide(ParkRideAnswer.ASK, ParkRideCardPhase.None), loading, ask),
        Row("answered no, then back", parkRide(ParkRideAnswer.NO, ParkRideCardPhase.None), failed, ask),
        // Looking. The bar keeps its shape with the right button off, so nothing jumps.
        Row("locating", yesWith(ParkRideCardPhase.Locating()), ready, decide(addEnabled = false)),
        Row(
            "a saved card being worked out again",
            yesWith(ParkRideCardPhase.Locating(stationId = "1")),
            ready,
            decide(addEnabled = false),
        ),
        // A card still waiting for its numbers keeps the right button off, then it comes on.
        Row("card loading", yesWith(card(ParkRideAvailabilityState.Loading)), ready, decide(addEnabled = false)),
        Row("card with numbers", yesWith(card(ParkRideAvailabilityState.Content)), ready, decide(addEnabled = true)),
        Row("card unavailable", yesWith(card(ParkRideAvailabilityState.Unavailable)), ready, decide(addEnabled = true)),
        // Nothing to decide, so no bar at all and the flow has moved on.
        Row("no car park near", yesWith(ParkRideCardPhase.NoStationNear), ready, null),
        Row("denied or no fix", yesWith(ParkRideCardPhase.None), ready, null),
    )

    private fun card(availability: ParkRideAvailabilityState) = ParkRideCardPhase.Card(
        ParkRideCardStation(stationId = "1", stationName = "Bella Vista", availability = availability),
    )

    private fun parkRide(answer: ParkRideAnswer, card: ParkRideCardPhase) =
        IntroState(step = IntroStep.PARK_RIDE, parkRideAnswer = answer, parkRideCard = card)

    private fun yesWith(card: ParkRideCardPhase) = parkRide(ParkRideAnswer.YES, card)

    private fun decide(addEnabled: Boolean) = IntroBar(
        left = IntroBarAction("Not now", IntroUiEvent.DeclineParkRide),
        right = IntroBarAction("Add to home", IntroUiEvent.AddParkRideAndContinue),
        rightEnabled = addEnabled,
    )

    private fun theme(enabled: Boolean) = IntroBar(
        left = IntroBarAction("No, show me another", IntroUiEvent.ShowAnotherTheme),
        right = IntroBarAction("Yes, this one", IntroUiEvent.ConfirmTheme),
        rightEnabled = enabled,
    )

    private fun trip(enabled: Boolean) = IntroBar(
        left = IntroBarAction("Not now", IntroUiEvent.FinishWithoutTrip),
        right = IntroBarAction("Yes, save it", IntroUiEvent.FinishPressed),
        rightEnabled = enabled,
    )

    private class Row(val name: String, val state: IntroState, val readiness: StopsReadiness, val bar: IntroBar?)

    @Test
    fun `the Park and Ride screen has the bar the table says in every state`() {
        parkRideRows.forEach { row ->
            assertEquals(row.bar, introBar(row.state, row.readiness), row.name)
        }
    }

    @Test
    fun `the answers are the exact words, with a short form only for yes and no at large text`() {
        assertEquals("No thanks", PARK_RIDE_NO)
        assertEquals("No", PARK_RIDE_NO_COMPACT)
        assertEquals("Yes, show me free spots", PARK_RIDE_YES)
        assertEquals("Yes", PARK_RIDE_YES_COMPACT)
        assertEquals("Not now", PARK_RIDE_NOT_NOW)
        assertEquals("Add to home", PARK_RIDE_ADD)
        assertNull(decide(true).left.compactLabel)
        assertNull(decide(true).right.compactLabel)
    }

    @Test
    fun `no bar answers both ways with one event, so the two buttons are always different answers`() {
        val bars = parkRideRows.mapNotNull { it.bar }

        assertTrue(bars.isNotEmpty())
        bars.forEach { bar -> assertNotEquals(bar.left.event, bar.right.event) }
    }

    @Test
    fun `the trip screens share one bar that waits only for stops that are still loading`() {
        val rows = listOf(
            Triple(unknown, false, false),
            Triple(loading, false, false),
            Triple(ready, false, true),
            Triple(failed, false, true),
            // A trip already written needs no wait.
            Triple(unknown, true, true),
            Triple(loading, true, true),
        )

        listOf(IntroStep.DESTINATION, IntroStep.ORIGIN, IntroStep.CONFIRM).forEach { step ->
            rows.forEach { (readiness, saved, enabled) ->
                val state = IntroState(step = step, saved = saved)

                assertEquals(trip(enabled), introBar(state, readiness), "$step $readiness saved=$saved")
            }
        }
    }

    @Test
    fun `the theme screen asks for another theme on the left, and the right one waits for a theme to be selected`() {
        val none = IntroState(step = IntroStep.THEME)
        val selected = none.copy(selectedThemeId = KrailThemeStyle.Metro.id)

        listOf(unknown, loading, ready, failed).forEach { readiness ->
            assertEquals(theme(enabled = false), introBar(none, readiness), "nothing selected, $readiness")
            assertEquals(theme(enabled = true), introBar(selected, readiness), "a theme selected, $readiness")
        }
    }

    @Test
    fun `the theme bar keeps both buttons, in the exact words, with no short form`() {
        val bar = requireNotNull(introBar(IntroState(step = IntroStep.THEME), ready))

        assertEquals("No, show me another", bar.left.label)
        assertEquals("Yes, this one", bar.right.label)
        assertEquals(IntroUiEvent.ShowAnotherTheme, bar.left.event)
        assertEquals(IntroUiEvent.ConfirmTheme, bar.right.event)
        assertNull(bar.left.compactLabel)
        assertNull(bar.right.compactLabel)
        listOf(1.0f, 1.31f, 2.0f).forEach { scale ->
            assertEquals("No, show me another", bar.left.visibleLabel(scale), "left at $scale")
            assertEquals("Yes, this one", bar.right.visibleLabel(scale), "right at $scale")
        }
    }

    @Test
    fun `which theme is selected does not change the bar, only whether the right button is on`() {
        introThemeChoices().forEach { choice ->
            val bar = introBar(IntroState(step = IntroStep.THEME, selectedThemeId = choice.style.id), ready)

            assertEquals(theme(enabled = true), bar, choice.name)
        }
        // An id the Intro does not offer is still a selection as far as the bar goes: the view model never stores one.
        assertEquals(theme(enabled = true), introBar(IntroState(step = IntroStep.THEME, selectedThemeId = 99), ready))
    }

    @Test
    fun `every screen has a bar or none on purpose, so a new step is a row here`() {
        val covered = mapOf(
            IntroStep.THEME to true,
            IntroStep.PARK_RIDE to true,
            IntroStep.DESTINATION to true,
            IntroStep.ORIGIN to true,
            IntroStep.CONFIRM to true,
        )

        assertEquals(IntroStep.entries.toSet(), covered.keys)
        covered.forEach { (step, hasBar) ->
            assertEquals(hasBar, introBar(IntroState(step = step), ready) != null, "$step")
        }
    }

    @Test
    fun `the label is the whole phrase at or below 130 percent and the short form above it`() {
        val yes = ask.right
        val no = ask.left
        val rows = listOf(
            // font scale, yes, no
            Triple(1.0f, "Yes, show me free spots", "No thanks"),
            Triple(1.15f, "Yes, show me free spots", "No thanks"),
            Triple(1.3f, "Yes, show me free spots", "No thanks"),
            Triple(1.31f, "Yes", "No"),
            Triple(1.5f, "Yes", "No"),
            Triple(2.0f, "Yes", "No"),
        )

        rows.forEach { (scale, yesLabel, noLabel) ->
            assertEquals(yesLabel, yes.visibleLabel(scale), "yes at $scale")
            assertEquals(noLabel, no.visibleLabel(scale), "no at $scale")
        }
    }

    @Test
    fun `a button with no short form shows its whole label at every size`() {
        val add = decide(true).right

        listOf(1.0f, 1.3f, 1.31f, 2.0f).forEach { scale ->
            assertEquals("Add to home", add.visibleLabel(scale), "at $scale")
            assertEquals("Not now", decide(true).left.visibleLabel(scale), "at $scale")
        }
    }

    @Test
    fun `the whole phrase is kept as the label whatever is shown, for a screen reader`() {
        assertEquals("Yes, show me free spots", ask.right.label)
        assertEquals("No thanks", ask.left.label)
    }
}
