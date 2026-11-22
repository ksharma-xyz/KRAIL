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
import xyz.ksharma.krail.trip.planner.ui.state.intro.TripDecision
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntroBarTest {

    // The Park & Ride and confirm screens have a left button, so reading it is an assertion that there is one.
    private val IntroBar.negative: IntroBarAction get() = requireNotNull(left) { "no left button on $this" }

    private val unknown = StopsReadiness.Unknown
    private val loading = StopsReadiness.Loading
    private val ready = StopsReadiness.Ready(stopCount = 100)
    private val failed = StopsReadiness.Failed(attempt = 1, reason = "disk full")

    private val ask = IntroBar(
        left = IntroBarAction("No", IntroUiEvent.AnswerParkRide(yes = false), spokenLabel = "No thanks"),
        right = IntroBarAction(
            "Yes",
            IntroUiEvent.AnswerParkRide(yes = true),
            spokenLabel = "Yes, show me free spots",
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

    private fun theme(shown: Boolean, enabled: Boolean = true) = IntroBar(
        left = null,
        right = IntroBarAction("Yes, this one", IntroUiEvent.ConfirmTheme),
        rightEnabled = enabled,
        shown = shown,
    )

    private fun trip() = IntroBar(
        left = IntroBarAction("Not now", IntroUiEvent.AnswerSaveTrip(yes = false)),
        right = IntroBarAction("Yes, save it", IntroUiEvent.AnswerSaveTrip(yes = true)),
    )

    private class Row(val name: String, val state: IntroState, val readiness: StopsReadiness, val bar: IntroBar?)

    @Test
    fun `the Park and Ride screen has the bar the table says in every state`() {
        parkRideRows.forEach { row ->
            assertEquals(row.bar, introBar(row.state, row.readiness), row.name)
        }
    }

    @Test
    fun `the answers are the exact words, Yes and No shown and the longer phrases spoken, and every other button says what it shows`() {
        assertEquals("No", PARK_RIDE_NO)
        assertEquals("No thanks", PARK_RIDE_NO_SPOKEN)
        assertEquals("Yes", PARK_RIDE_YES)
        assertEquals("Yes, show me free spots", PARK_RIDE_YES_SPOKEN)
        assertEquals("Not now", PARK_RIDE_NOT_NOW)
        assertEquals("Add to home", PARK_RIDE_ADD)
        assertEquals(PARK_RIDE_NOT_NOW, decide(true).negative.spokenLabel)
        assertEquals(PARK_RIDE_ADD, decide(true).right.spokenLabel)
    }

    @Test
    fun `no bar answers both ways with one event, so the two buttons are always different answers`() {
        val bars = parkRideRows.mapNotNull { it.bar }

        assertTrue(bars.isNotEmpty())
        bars.forEach { bar -> assertNotEquals(bar.negative.event, bar.right.event) }
    }

    private fun stops(shown: Boolean) = IntroBar(
        left = null,
        right = IntroBarAction("Yes, this one", IntroUiEvent.ConfirmStop),
        shown = shown,
    )

    @Test
    fun `the destination and origin screens are one button, not shown until a stop is selected, whatever the search or the stops data`() {
        val phases = listOf(
            IntroSearchPhase.Idle,
            IntroSearchPhase.Results(persistentListOf(CENTRAL_STOP)),
            IntroSearchPhase.NoMatch,
        )

        listOf(IntroStep.DESTINATION, IntroStep.ORIGIN).forEach { step ->
            listOf(unknown, loading, ready, failed).forEach { readiness ->
                phases.forEach { phase ->
                    val none = IntroState(step = step, search = phase)
                    val selected = none.copy(selectedStop = CENTRAL_STOP)

                    assertEquals(stops(shown = false), introBar(none, readiness), "$step $readiness $phase")
                    assertEquals(stops(shown = true), introBar(selected, readiness), "$step $readiness $phase")
                }
            }
        }
    }

    @Test
    fun `the stop bar's words are exact, whole at every text size, with nothing to its left and never a disabled button`() {
        val bar = requireNotNull(introBar(IntroState(step = IntroStep.DESTINATION), ready))

        assertEquals("Yes, this one", STOP_CHOOSE)
        assertNull(bar.left, "there is no No, search")
        assertEquals("Yes, this one", bar.right.label)
        assertEquals(bar.right.label, bar.right.spokenLabel, "it says what it shows")
        assertTrue(bar.rightEnabled)
        assertEquals(IntroUiEvent.ConfirmStop, bar.right.event)
    }

    @Test
    fun `the confirm screen has two buttons that only record the answer, so neither waits for the stops`() {
        listOf(unknown, loading, ready, failed).forEach { readiness ->
            listOf(false, true).forEach { saved ->
                val state = IntroState(step = IntroStep.CONFIRM, saved = saved)

                assertEquals(trip(), introBar(state, readiness), "$readiness saved=$saved")
            }
        }
        assertTrue(trip().rightEnabled)
        assertEquals("Not now", CONFIRM_NOT_NOW)
        assertEquals("Yes, save it", CONFIRM_SAVE)
    }

    @Test
    fun `the theme screen is one button, not shown until a theme is selected, and not held back with nothing to write`() {
        val none = IntroState(step = IntroStep.THEME)
        val selected = none.copy(selectedThemeId = KrailThemeStyle.Metro.id)
        val declined = selected.copy(tripDecision = TripDecision.DECLINED)

        listOf(unknown, loading, ready, failed).forEach { readiness ->
            assertEquals(theme(shown = false), introBar(none, readiness), "nothing selected, $readiness")
            assertEquals(theme(shown = true), introBar(selected, readiness), "a theme selected, $readiness")
            assertEquals(theme(shown = true), introBar(declined, readiness), "declined at confirm, $readiness")
        }
    }

    @Test
    fun `the theme button waits for stops that are still loading only when the rider said yes to saving the trip`() {
        val saving = IntroState(
            step = IntroStep.THEME,
            selectedThemeId = KrailThemeStyle.Metro.id,
            tripDecision = TripDecision.SAVED,
        )

        assertEquals(theme(shown = true, enabled = false), introBar(saving, unknown))
        assertEquals(theme(shown = true, enabled = false), introBar(saving, loading))
        assertEquals(theme(shown = true), introBar(saving, ready))
        assertEquals(theme(shown = true), introBar(saving, failed), "a failed load is no different from a ready one")
        // A trip already written needs no wait.
        assertEquals(theme(shown = true), introBar(saving.copy(saved = true), loading))
        // Not shown yet is still a kept room, and never a disabled looking one: it is hidden, not off.
        assertEquals(false, introBar(saving.copy(selectedThemeId = null), loading)?.shown)
    }

    @Test
    fun `a bar that is not shown still has its button, so the room for it is kept`() {
        val none = requireNotNull(introBar(IntroState(step = IntroStep.THEME), ready))

        assertEquals(false, none.shown)
        assertEquals("Yes, this one", none.right.label)
        assertNull(none.left)
    }

    @Test
    fun `every bar is shown, except the theme and stop screens before anything is picked`() {
        val states = parkRideRows.map { it.state } +
            IntroState(step = IntroStep.CONFIRM) +
            IntroState(step = IntroStep.THEME, selectedThemeId = KrailThemeStyle.Bus.id) +
            IntroState(step = IntroStep.DESTINATION, selectedStop = CENTRAL_STOP) +
            IntroState(step = IntroStep.ORIGIN, selectedStop = CENTRAL_STOP)

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
        assertEquals(bar.right.label, bar.right.spokenLabel, "it says what it shows")
    }

    @Test
    fun `only the screens that ask for a pick have one button, so no other step can lose its left one`() {
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

        assertEquals(listOf(IntroStep.DESTINATION, IntroStep.ORIGIN, IntroStep.THEME), oneButton)
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
            IntroStep.PARK_RIDE to true,
            IntroStep.DESTINATION to true,
            IntroStep.ORIGIN to true,
            IntroStep.CONFIRM to true,
            IntroStep.THEME to true,
        )

        assertEquals(IntroStep.entries.toSet(), covered.keys)
        covered.forEach { (step, hasBar) ->
            assertEquals(hasBar, introBar(IntroState(step = step), ready) != null, "$step")
        }
    }

    @Test
    fun `the Park and Ride answers show one word and say the longer phrase, and the word starts the phrase`() {
        assertEquals("Yes", ask.right.label)
        assertEquals("Yes, show me free spots", ask.right.spokenLabel)
        assertEquals("No", ask.negative.label)
        assertEquals("No thanks", ask.negative.spokenLabel)
        assertTrue(ask.right.spokenLabel.startsWith(ask.right.label))
        assertTrue(ask.negative.spokenLabel.startsWith(ask.negative.label))
    }

    @Test
    fun `a button with no longer phrase says what it shows, at every text size, since there is no compact form any more`() {
        assertEquals("Add to home", decide(true).right.spokenLabel)
        assertEquals("Not now", decide(true).negative.spokenLabel)
        assertEquals("Yes, save it", trip().right.spokenLabel)
        assertEquals("Not now", trip().negative.spokenLabel)
    }
}
