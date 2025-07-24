package xyz.ksharma.krail.trip.planner.ui.state.settings

import xyz.ksharma.krail.core.social.model.SocialType

sealed interface SettingsEvent {
    data class SocialLinkClick(val socialType: SocialType): SettingsEvent
}
