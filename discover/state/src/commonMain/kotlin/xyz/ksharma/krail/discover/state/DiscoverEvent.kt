package xyz.ksharma.krail.discover.state

import xyz.ksharma.krail.social.state.KrailSocialType
import  xyz.ksharma.krail.discover.state.Button.Social.PartnerSocial.PartnerSocialLink

sealed interface DiscoverEvent {

    data class AppSocialLinkClicked(val krailSocialType: KrailSocialType) : DiscoverEvent

    data class PartnerSocialLinkClicked(
        val partnerSocialLink: PartnerSocialLink,
        val cardId: String,
        val cardType: DiscoverCardType,
    ) : DiscoverEvent

    data class ShareButtonClicked(
        val url: String,
        val cardId: String,
        val cardType: DiscoverCardType,
    ) : DiscoverEvent

    data class CtaButtonClicked(
        val url: String,
        val cardId: String,
        val cardType: DiscoverCardType,
    ) : DiscoverEvent

    /**
     * Event triggered when the user clicks on the feedback thumbs up/down button.
     * @param isPositive
     *          true if the user clicked the thumbs up button,
     *          false if they clicked the thumbs down button.
     */
    data class FeedbackThumbButtonClicked(
        val isPositive: Boolean,
        val cardId: String,
        val cardType: DiscoverCardType,
    ) : DiscoverEvent

    data class FeedbackCtaButtonClicked(
        val isPositive: Boolean,
        val cardId: String,
        val cardType: DiscoverCardType,
    ) : DiscoverEvent
}
