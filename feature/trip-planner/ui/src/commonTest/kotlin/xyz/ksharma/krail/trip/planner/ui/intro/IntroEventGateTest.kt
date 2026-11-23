package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Which events the Intro takes, with no view model: the gate in front of every event. */
class IntroEventGateTest {

    @Test
    fun `an event is taken on a step it is valid on`() {
        assertTrue(introEventAccepted(false, IntroStep.THEME, IntroUiEvent.ConfirmTheme))
        assertTrue(introEventAccepted(false, IntroStep.PARK_RIDE, IntroUiEvent.DeclineParkRide))
        assertTrue(introEventAccepted(false, IntroStep.CONFIRM, IntroUiEvent.AnswerSaveTrip(yes = true)))
    }

    @Test
    fun `an event is dropped on a step it is not valid on`() {
        assertFalse(introEventAccepted(false, IntroStep.THEME, IntroUiEvent.DeclineParkRide))
        assertFalse(introEventAccepted(false, IntroStep.PARK_RIDE, IntroUiEvent.ConfirmTheme))
        assertFalse(introEventAccepted(false, IntroStep.CONFIRM, IntroUiEvent.ConfirmStop))
    }

    @Test
    fun `the two stop screens share their events`() {
        assertTrue(introEventAccepted(false, IntroStep.DESTINATION, IntroUiEvent.ConfirmStop))
        assertTrue(introEventAccepted(false, IntroStep.ORIGIN, IntroUiEvent.ConfirmStop))
        assertTrue(introEventAccepted(false, IntroStep.ORIGIN, IntroUiEvent.QueryChanged("central")))
    }

    @Test
    fun `an event that names the step it was pressed on is dropped once the Intro has moved on`() {
        assertTrue(introEventAccepted(false, IntroStep.ORIGIN, IntroUiEvent.ConfirmStop, pressedOn = IntroStep.ORIGIN))
        assertFalse(
            introEventAccepted(false, IntroStep.ORIGIN, IntroUiEvent.ConfirmStop, pressedOn = IntroStep.DESTINATION),
        )
    }

    @Test
    fun `nothing is taken from the finish press on`() {
        IntroStep.entries.forEach { step ->
            assertFalse(introEventAccepted(true, step, IntroUiEvent.ConfirmTheme))
            assertFalse(introEventAccepted(true, step, IntroUiEvent.DeclineParkRide))
        }
    }

    @Test
    fun `an event with no step named is judged by the step alone`() {
        val noStep: IntroStep? = null
        assertEquals(true, introEventAccepted(false, IntroStep.THEME, IntroUiEvent.ConfirmTheme, noStep))
        assertEquals(false, introEventAccepted(false, IntroStep.ORIGIN, IntroUiEvent.ConfirmTheme, noStep))
    }
}
