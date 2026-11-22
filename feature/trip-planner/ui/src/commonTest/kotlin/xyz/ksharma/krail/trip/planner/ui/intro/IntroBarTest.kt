package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroSearchPhase
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

    // Every screen but the theme screen has a left button, so reading it is an assertion that there is one.
    private val IntroBar.negative: IntroBarAction get() = requireNotNull(left) { "no left button on $this" }

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

    private fun theme(shown: Boolean) = IntroBar(
        left = null,
        right = IntroBarAction("Yes, this one", IntroUiEvent.ConfirmTheme),
        shown = shown,
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
        assertNull(decide(true).negative.compactLabel)
        assertNull(decide(true).right.compactLabel)
    }

    @Test
    fun `no bar answers both ways with one event, so the two buttons are always different answers`() {
        val bars = parkRideRows.mapNotNull { it.bar }

        assertTrue(bars.isNotEmpty())
        bars.forEach { bar -> assertNotEquals(bar.negative.event, bar.right.event) }
    }

    private fun stops(enabled: Boolean) = IntroBar(
        left = IntroBarAction("No, search", IntroUiEvent.FocusSearch),
        right = IntroBarAction("Yes, this one", IntroUiEvent.UseHighlightedStop),
        rightEnabled = enabled,
    )

    @Test
    fun `the destination and origin screens ask No search and Yes this one, and the stops data never matters`() {
        val typedNothing = listOf(IntroSearchPhase.Idle, IntroSearchPhase.Results(persistentListOf(CENTRAL_STOP)))

        listOf(IntroStep.DESTINATION, IntroStep.ORIGIN).forEach { step ->
            listOf(unknown, loading, ready, failed).forEach { readiness ->
                typedNothing.forEach { phase ->
                    val state = IntroState(step = step, search = phase)

                    assertEquals(stops(enabled = true), introBar(state, readiness), "$step $readiness $phase")
                }
                // Nothing is highlighted when the search found nothing, so the right button has nothing to take.
                val noMatch = IntroState(step = step, search = IntroSearchPhase.NoMatch)
                assertEquals(stops(enabled = false), introBar(noMatch, readiness), "$step $readiness no match")
            }
        }
    }

    @Test
    fun `the stop bar's words are exact, whole at every text size, and the left button never moves on`() {
        val bar = requireNotNull(introBar(IntroState(step = IntroStep.DESTINATION), ready))

        assertEquals("No, search", STOP_SEARCH)
        assertEquals("Yes, this one", STOP_CHOOSE)
        assertNull(bar.negative.compactLabel)
        assertNull(bar.right.compactLabel)
        listOf(1.0f, 1.31f, 2.0f).forEach { scale ->
            assertEquals("No, search", bar.negative.visibleLabel(scale), "left at $scale")
            assertEquals("Yes, this one", bar.right.visibleLabel(scale), "right at $scale")
        }
        assertEquals(IntroUiEvent.FocusSearch, bar.negative.event)
    }

    @Test
    fun `the confirm screen has a bar that waits only for stops that are still loading`() {
        val rows = listOf(
            Triple(unknown, false, false),
            Triple(loading, false, false),
            Triple(ready, false, true),
            Triple(failed, false, true),
            // A trip already written needs no wait.
            Triple(unknown, true, true),
            Triple(loading, true, true),
        )

        rows.forEach { (readiness, saved, enabled) ->
            val state = IntroState(step = IntroStep.CONFIRM, saved = saved)

            assertEquals(trip(enabled), introBar(state, readiness), "$readiness saved=$saved")
        }
    }

    @Test
    fun `the theme screen is one button, not shown until a theme is selected, and never disabled`() {
        val none = IntroState(step = IntroStep.THEME)
        val selected = none.copy(selectedThemeId = KrailThemeStyle.Metro.id)

        listOf(unknown, loading, ready, failed).forEach { readiness ->
            assertEquals(theme(shown = false), introBar(none, readiness), "nothing selected, $readiness")
            assertEquals(theme(shown = true), introBar(selected, readiness), "a theme selected, $readiness")
            assertTrue(introBar(none, readiness)?.rightEnabled == true, "never a disabled button, $readiness")
        }
    }

    @Test
    fun `a bar that is not shown still has its button, so the room for it is kept`() {
        val none = requireNotNull(introBar(IntroState(step = IntroStep.THEME), ready))

        assertEquals(false, none.shown)
        assertEquals("Yes, this one", none.right.label)
        assertNull(none.left)
    }

    @Test
    fun `every bar is shown, except the theme screen before a theme is selected`() {
        val states = parkRideRows.map { it.state } +
            IntroState(step = IntroStep.CONFIRM) +
            IntroState(step = IntroStep.THEME, selectedThemeId = KrailThemeStyle.Bus.id)

        states.forEach { state ->
            val bar = introBar(state, ready)
            assertTrue(bar == null || bar.shown, "${state.step} $state")
        }
    }

    @Test
    fun `the bar fades in over 250 ms, and appears at once with reduced motion`() {
        assertEquals(250, BAR_REVEAL_MS)
        assertEquals(250, introBarRevealMs(reduceMotion = false))
        assertEquals(0, introBarRevealMs(reduceMotion = true))
    }

    @Test
    fun `the theme bar is the one button, in the exact words, with no short form and nothing to its left`() {
        val bar = requireNotNull(introBar(IntroState(step = IntroStep.THEME), ready))

        assertNull(bar.left, "the theme screen has no way to say no")
        assertEquals("Yes, this one", bar.right.label)
        assertEquals(IntroUiEvent.ConfirmTheme, bar.right.event)
        assertNull(bar.right.compactLabel)
        listOf(1.0f, 1.31f, 2.0f).forEach { scale ->
            assertEquals("Yes, this one", bar.right.visibleLabel(scale), "right at $scale")
        }
    }

    @Test
    fun `the theme screen is the only screen with one button, so no other step can lose its left one`() {
        val oneButton = IntroStep.entries.filter { step ->
            val states = listOf(
                IntroState(step = step),
                IntroState(step = step, saved = true),
                IntroState(step = step, parkRideAnswer = ParkRideAnswer.NO),
                IntroState(
                    step = step,
                    parkRideAnswer = ParkRideAnswer.YES,
                    parkRideCard = card(ParkRideAvailabilityState.Loading),
                ),
                IntroState(
                    step = step,
                    parkRideAnswer = ParkRideAnswer.YES,
                    parkRideCard = ParkRideCardPhase.Locating(),
                ),
                IntroState(step = step, search = IntroSearchPhase.NoMatch),
            )
            val readinesses = listOf(unknown, loading, ready, failed)
            val bars = states.flatMap { state ->
                readinesses.mapNotNull { introBar(state, it) }
            }
            bars.any { it.left == null }
        }

        assertEquals(listOf(IntroStep.THEME), oneButton)
    }

    @Test
    fun `which theme is selected does not change the bar, only whether it is shown`() {
        introThemeChoices().forEach { choice ->
            val bar = introBar(IntroState(step = IntroStep.THEME, selectedThemeId = choice.style.id), ready)

            assertEquals(theme(shown = true), bar, choice.name)
        }
        // An id the Intro does not offer is still a selection as far as the bar goes: the view model never stores one.
        assertEquals(theme(shown = true), introBar(IntroState(step = IntroStep.THEME, selectedThemeId = 99), ready))
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
        val no = ask.negative
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
            assertEquals("Not now", decide(true).negative.visibleLabel(scale), "at $scale")
        }
    }

    @Test
    fun `the whole phrase is kept as the label whatever is shown, for a screen reader`() {
        assertEquals("Yes, show me free spots", ask.right.label)
        assertEquals("No thanks", ask.negative.label)
    }
}
