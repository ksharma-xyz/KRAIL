package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import kotlin.test.Test
import kotlin.test.assertEquals

class IntroKeyboardActionTest {

    private val close = listOf(
        IntroUiEvent.SelectStop(CENTRAL_STOP),
        IntroUiEvent.ConfirmStop,
    )

    private val leaveAlone = listOf(
        IntroUiEvent.QueryChanged("chat"),
        IntroUiEvent.QueryChanged(""),
        IntroUiEvent.ConfirmTheme,
        IntroUiEvent.PickTheme(1),
        IntroUiEvent.AnswerParkRide(yes = true),
        IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, LatLng(-33.88, 151.21)),
        IntroUiEvent.AddParkRideAndContinue,
        IntroUiEvent.DeclineParkRide,
        IntroUiEvent.FinishPressed,
        IntroUiEvent.FinishWithoutTrip,
    )

    @Test
    fun `no event opens the keyboard, because the search field opens it itself when it is tapped`() {
        assertEquals(setOf(IntroKeyboardAction.Close, IntroKeyboardAction.None), IntroKeyboardAction.entries.toSet())
    }

    @Test
    fun `selecting a stop and confirming one close the keyboard, so it never stays up over the next screen`() {
        close.forEach { assertEquals(IntroKeyboardAction.Close, it.keyboardAction(), "$it") }
    }

    @Test
    fun `typing and every other event leave the keyboard as it is`() {
        leaveAlone.forEach { assertEquals(IntroKeyboardAction.None, it.keyboardAction(), "$it") }
    }
}
