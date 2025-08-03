package xyz.ksharma.krail.discover.state

import xyz.ksharma.krail.discover.state.Button.Social.PartnerSocial.PartnerSocialLink
import xyz.ksharma.krail.social.state.KrailSocialType

sealed interface DiscoverEvent {

    data class AppSocialLinkClicked(val krailSocialType: KrailSocialType) : DiscoverEvent

    data class PartnerSocialLinkClicked(
        val partnerSocialLink: PartnerSocialLink,
        val cardId: String,
        val cardType: DiscoverCardType,
    ) : DiscoverEvent

    data class ShareButtonClicked(
        val cardTitle: String,
        val url: String,
        val cardId: String,
        val cardType: DiscoverCardType,
    ) : DiscoverEvent

    data class CtaButtonClicked(
        val url: String,
        val cardId: String,
        val cardType: DiscoverCardType,
    ) : DiscoverEvent

    data class CardSeen(val cardId: String) : DiscoverEvent

    data object ResetAllSeenCards : DiscoverEvent
}
