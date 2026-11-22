package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import kotlin.test.Test
import kotlin.test.assertEquals

class IntroKeyboardActionTest {

    private val open = listOf<IntroUiEvent>(IntroUiEvent.FocusSearch)

    private val close = listOf(
        IntroUiEvent.ChooseStop(CENTRAL_STOP),
        IntroUiEvent.UseHighlightedStop,
        IntroUiEvent.Back,
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
    fun `No search is the one event that opens the keyboard`() {
        open.forEach { assertEquals(IntroKeyboardAction.Open, it.keyboardAction(), "$it") }
    }

    @Test
    fun `a choice and Back close the keyboard, so it never stays up over the next screen`() {
        close.forEach { assertEquals(IntroKeyboardAction.Close, it.keyboardAction(), "$it") }
    }

    @Test
    fun `typing and every other event leave the keyboard as it is`() {
        leaveAlone.forEach { assertEquals(IntroKeyboardAction.None, it.keyboardAction(), "$it") }
    }
}
