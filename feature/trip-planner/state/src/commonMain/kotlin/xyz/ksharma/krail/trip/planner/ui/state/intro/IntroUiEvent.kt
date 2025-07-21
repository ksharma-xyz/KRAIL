package xyz.ksharma.krail.trip.planner.ui.state.intro

import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.ReferFriend.EntryPoint

sealed interface IntroUiEvent {
    data class ReferFriend(val analyticsEntryPoint: EntryPoint) : IntroUiEvent

    /**
     * When the user completes the intro screen.
     * @param pageType - The page type of the intro screen, where Complete action was performed.
     */
    data class Complete(
        val pageType: IntroState.IntroPageType,
        val pageNumber: Int,
    ) : IntroUiEvent
}
