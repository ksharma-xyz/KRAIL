package xyz.ksharma.krail.trip.planner.ui.state.settings

sealed interface SettingsEvent {
    data class SocialLinkClick(val socialType: SocialType): SettingsEvent
}
