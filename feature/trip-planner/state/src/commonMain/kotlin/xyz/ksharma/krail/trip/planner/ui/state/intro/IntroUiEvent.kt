package xyz.ksharma.krail.trip.planner.ui.state.intro

sealed interface IntroUiEvent {
    data object OnNextClick : IntroUiEvent
    data object OnCompleteClick : IntroUiEvent
}