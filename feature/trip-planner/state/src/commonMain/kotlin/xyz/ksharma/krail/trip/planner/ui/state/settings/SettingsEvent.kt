package xyz.ksharma.krail.trip.planner.ui.state.settings

sealed interface SettingsEvent {
    data object LinkedInLogoClick : SettingsEvent
}
