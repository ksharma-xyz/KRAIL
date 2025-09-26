package xyz.ksharma.krail.trip.planner.ui.state.settings

import xyz.ksharma.krail.social.state.KrailSocialType

sealed interface SettingsEvent {
    data class SocialLinkClick(val krailSocialType: KrailSocialType) : SettingsEvent
}
