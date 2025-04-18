package xyz.ksharma.krail.trip.planner.ui.state.intro

sealed interface IntroUiEvent {
    data object ReferFriend : IntroUiEvent
    data object Complete : IntroUiEvent
}
