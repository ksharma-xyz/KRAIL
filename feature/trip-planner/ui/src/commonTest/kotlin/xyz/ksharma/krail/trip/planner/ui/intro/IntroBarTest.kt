package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.collections.immutable.persistentListOf
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

    // The two pairs of words the Park & Ride bar can show, which it is sized by as one.
    private val parkRidePairs = listOf("No thanks" to "Yes please", "Not now" to "Add to home")

    private val ask = IntroBar(
        left = IntroBarAction("No thanks", IntroUiEvent.AnswerParkRide(yes = false), spokenLabel = "No thanks"),
        right = IntroBarAction(
            "Yes please",
            IntroUiEvent.AnswerParkRide(yes = true),
            spokenLabel = "Yes please, show me parking spots",
        ),
        labelPairs = parkRidePairs,
    )

    private val parkRideRows = listOf(
        // Asking. The stops never matter on this screen.
        Row("unanswered", parkRide(ParkRideAnswer.ASK, ParkRideCardPhase.None), ask),
        Row("answered no, then back", parkRide(ParkRideAnswer.NO, ParkRideCardPhase.None), ask),
        // Looking. The bar keeps its shape with the right button not shown, so nothing jumps, and the left one is live.
        Row("locating", yesWith(ParkRideCardPhase.Locating()), decide(addShown = false)),
        Row(
            "a saved card being worked out again",
            yesWith(ParkRideCardPhase.Locating(stationId = "1")),
            decide(addShown = false),
        ),
        // A card still waiting for its numbers is live, because adding needs the station and not the readings.
        Row("card loading", yesWith(card(ParkRideAvailabilityState.Loading)), decide(addShown = true)),
        Row("card with numbers", yesWith(card(ParkRideAvailabilityState.Content)), decide(addShown = true)),
        Row("card unavailable", yesWith(card(ParkRideAvailabilityState.Unavailable)), decide(addShown = true)),
        // No station near, in the first look or the wider one: the card's room says so and the one button, OK, moves on.
        Row("no car park near", yesWith(ParkRideCardPhase.NoStationNear), noStation()),
        // Nothing to decide and nothing to say, so no bar at all and the flow has moved on.
        Row("denied or no fix", yesWith(ParkRideCardPhase.None), null),
    )

    private fun card(availability: ParkRideAvailabilityState) = ParkRideCardPhase.Card(
        ParkRideCardStation(stationId = "1", stationName = "Bella Vista", availability = availability),
    )

    private fun parkRide(answer: ParkRideAnswer, card: ParkRideCardPhase) =
        IntroState(step = IntroStep.PARK_RIDE, parkRideAnswer = answer, parkRideCard = card)

    private fun yesWith(card: ParkRideCardPhase) = parkRide(ParkRideAnswer.YES, card)

    private fun decide(addShown: Boolean) = IntroBar(
        left = IntroBarAction("Not now", IntroUiEvent.DeclineParkRide),
        right = IntroBarAction("Add to home", IntroUiEvent.AddParkRideAndContinue),
        rightShown = addShown,
        labelPairs = parkRidePairs,
    )

    private fun noStation() = IntroBar(
        left = null,
        right = IntroBarAction("OK", IntroUiEvent.AcknowledgeNoStation),
    )

    private fun theme(shown: Boolean) = IntroBar(
        left = null,
        right = IntroBarAction("Let's KRAIL", IntroUiEvent.ConfirmTheme, spokenLabel = "Let's KRAIL, use this colour"),
        shown = shown,
    )

    private fun trip() = IntroBar(
        left = IntroBarAction("Not now", IntroUiEvent.AnswerSaveTrip(yes = false)),
        right = IntroBarAction("Yes, save it", IntroUiEvent.AnswerSaveTrip(yes = true)),
    )

    private class Row(val name: String, val state: IntroState, val bar: IntroBar?)

    @Test
    fun `the Park and Ride screen has the bar the table says in every state`() {
        parkRideRows.forEach { row ->
            assertEquals(row.bar, introBar(row.state), row.name)
        }
    }

    @Test
    fun `the answers are the exact words, No thanks and Yes please shown and the longer phrase of Yes spoken, and every other button says what it shows`() {
        assertEquals("No thanks", PARK_RIDE_NO)
        assertEquals("No thanks", PARK_RIDE_NO_SPOKEN)
        assertEquals("Yes please", PARK_RIDE_YES)
        assertEquals("Yes please, show me parking spots", PARK_RIDE_YES_SPOKEN)
        assertEquals("Not now", PARK_RIDE_NOT_NOW)
        assertEquals("Add to home", PARK_RIDE_ADD)
        assertEquals(PARK_RIDE_NOT_NOW, decide(addShown = true).negative.spokenLabel)
        assertEquals(PARK_RIDE_ADD, decide(addShown = true).right.spokenLabel)
    }

    @Test
    fun `no bar answers both ways with one event, so the two buttons are always different answers`() {
        // The single button of the no station message has no other answer to differ from.
        val bars = parkRideRows.mapNotNull { it.bar }.filter { it.left != null }

        assertTrue(bars.isNotEmpty())
        bars.forEach { bar -> assertNotEquals(bar.negative.event, bar.right.event) }
    }

    @Test
    fun `the message that no station is near is one live OK button, shown at once, that says the same aloud and is not a way past a question`() {
        val bar = requireNotNull(introBar(yesWith(ParkRideCardPhase.NoStationNear)))

        assertEquals("OK", PARK_RIDE_NO_STATION_OK)
        assertNull(bar.left, "there is no left button, so nothing else can move the rider on")
        assertEquals("OK", bar.right.label)
        assertEquals(bar.right.label, bar.right.spokenLabel, "it says what it shows")
        assertEquals(IntroUiEvent.AcknowledgeNoStation, bar.right.event)
        assertTrue(bar.shown && bar.rightShown, "it is live as soon as the message shows, and never disabled")
    }

    private fun stops(shown: Boolean) = IntroBar(
        left = null,
        right = IntroBarAction("Yes, this one", IntroUiEvent.ConfirmStop),
        shown = shown,
    )

    @Test
    fun `the destination and origin screens are one button, not shown until a stop is selected, whatever the search`() {
        val phases = listOf(
            IntroSearchPhase.Idle,
            IntroSearchPhase.Results(persistentListOf(CENTRAL_STOP)),
            IntroSearchPhase.NoMatch,
        )

        listOf(IntroStep.DESTINATION, IntroStep.ORIGIN).forEach { step ->
            phases.forEach { phase ->
                val none = IntroState(step = step, search = phase)
                val selected = none.copy(selectedStop = CENTRAL_STOP)

                assertEquals(stops(shown = false), introBar(none), "$step $phase")
                assertEquals(stops(shown = true), introBar(selected), "$step $phase")
            }
        }
    }

    @Test
    fun `the stop bar's words are exact, whole at every text size, with nothing to its left and never a disabled button`() {
        val bar = requireNotNull(introBar(IntroState(step = IntroStep.DESTINATION)))

        assertEquals("Yes, this one", STOP_CHOOSE)
        assertNull(bar.left, "there is no No, search")
        assertEquals("Yes, this one", bar.right.label)
        assertEquals(bar.right.label, bar.right.spokenLabel, "it says what it shows")
        assertEquals(IntroUiEvent.ConfirmStop, bar.right.event)
    }

    @Test
    fun `the confirm screen has two buttons that only record the answer, so neither waits for anything`() {
        listOf(false, true).forEach { saved ->
            val state = IntroState(step = IntroStep.CONFIRM, saved = saved)

            assertEquals(trip(), introBar(state), "saved=$saved")
        }
        assertEquals("Not now", CONFIRM_NOT_NOW)
        assertEquals("Yes, save it", CONFIRM_SAVE)
    }

    @Test
    fun `the theme screen is one button, not shown until a theme is selected and live from then on, whatever was answered`() {
        val none = IntroState(step = IntroStep.THEME)
        val selected = none.copy(selectedThemeId = KrailThemeStyle.Metro.id)

        assertEquals(theme(shown = false), introBar(none), "nothing selected")
        assertEquals(theme(shown = true), introBar(selected), "a theme selected")
        TripDecision.entries.forEach { decision ->
            listOf(false, true).forEach { saved ->
                val state = selected.copy(tripDecision = decision, saved = saved)
                assertEquals(theme(shown = true), introBar(state), "$decision saved=$saved")
            }
        }
        assertEquals(theme(shown = true), introBar(selected.copy(tripDecision = null)), "no answer recorded")
    }

    @Test
    fun `the theme button is the same whether the rider said yes to saving the trip or not, it never waits for the stops`() {
        val saving = IntroState(
            step = IntroStep.THEME,
            selectedThemeId = KrailThemeStyle.Metro.id,
            tripDecision = TripDecision.SAVED,
        )

        assertEquals(theme(shown = true), introBar(saving))
        assertEquals(theme(shown = true), introBar(saving.copy(saved = true)))
        assertEquals(false, introBar(saving.copy(selectedThemeId = null))?.shown, "hidden, with its room kept")
    }

    @Test
    fun `a bar that is not shown still has its button, so the room for it is kept`() {
        val none = requireNotNull(introBar(IntroState(step = IntroStep.THEME)))

        assertEquals(false, none.shown)
        assertEquals("Let's KRAIL", none.right.label)
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
            val bar = introBar(state)
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
    fun `the theme bar is the one button, Let's KRAIL, with the longer phrase spoken and nothing to its left`() {
        val bar = requireNotNull(introBar(IntroState(step = IntroStep.THEME)))

        assertNull(bar.left, "the theme screen has no way to say no")
        assertEquals("Let's KRAIL", bar.right.label)
        assertEquals("Let's KRAIL", THEME_CONFIRM)
        assertEquals("Let's KRAIL, use this colour", bar.right.spokenLabel)
        assertEquals(THEME_CONFIRM_SPOKEN, bar.right.spokenLabel)
        assertTrue(
            bar.right.spokenLabel.startsWith(bar.right.label),
            "voice control that says the label finds the button",
        )
        assertEquals(IntroUiEvent.ConfirmTheme, bar.right.event)
    }

    @Test
    fun `the stop screens keep Yes, this one, and only the theme screen says Let's KRAIL`() {
        assertEquals("Yes, this one", STOP_CHOOSE)
        listOf(IntroStep.DESTINATION, IntroStep.ORIGIN).forEach { step ->
            assertEquals("Yes, this one", introBar(IntroState(step = step))?.right?.label, "$step")
        }
        IntroStep.entries.filter { it != IntroStep.THEME }.forEach { step ->
            assertTrue(introBar(IntroState(step = step))?.right?.label != "Let's KRAIL", "$step")
        }
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
            val bars = states.mapNotNull { introBar(it) }
            bars.any { it.left == null }
        }

        assertEquals(listOf(IntroStep.DESTINATION, IntroStep.ORIGIN, IntroStep.THEME), oneButton)
    }

    @Test
    fun `which theme is selected does not change the bar, only whether it is shown`() {
        introThemeChoices().forEach { choice ->
            val bar = introBar(IntroState(step = IntroStep.THEME, selectedThemeId = choice.style.id))

            assertEquals(theme(shown = true), bar, choice.name)
        }
        // An id the Intro does not offer is still a selection as far as the bar goes: the view model never stores one.
        assertEquals(theme(shown = true), introBar(IntroState(step = IntroStep.THEME, selectedThemeId = 99)))
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
            assertEquals(hasBar, introBar(IntroState(step = step)) != null, "$step")
        }
    }

    @Test
    fun `the Park and Ride answers show two words and say the longer phrase, and the words start the phrase`() {
        assertEquals("Yes please", ask.right.label)
        assertEquals("Yes please, show me parking spots", ask.right.spokenLabel)
        assertEquals("No thanks", ask.negative.label)
        assertEquals("No thanks", ask.negative.spokenLabel)
        assertTrue(ask.right.spokenLabel.startsWith(ask.right.label))
        assertTrue(ask.negative.spokenLabel.startsWith(ask.negative.label))
    }

    @Test
    fun `a button with no longer phrase says what it shows, at every text size, since there is no compact form any more`() {
        assertEquals("Add to home", decide(addShown = true).right.spokenLabel)
        assertEquals("Not now", decide(addShown = true).negative.spokenLabel)
        assertEquals("Yes, save it", trip().right.spokenLabel)
        assertEquals("Not now", trip().negative.spokenLabel)
    }
}
