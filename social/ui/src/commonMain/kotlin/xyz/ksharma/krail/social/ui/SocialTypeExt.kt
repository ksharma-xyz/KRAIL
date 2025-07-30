package xyz.ksharma.krail.social.ui

import krail.social.ui.generated.resources.Res
import krail.social.ui.generated.resources.ic_facebook
import krail.social.ui.generated.resources.ic_instagram
import krail.social.ui.generated.resources.ic_linkedin
import krail.social.ui.generated.resources.ic_reddit
import org.jetbrains.compose.resources.DrawableResource
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.SocialConnectionLinkClickEvent
import xyz.ksharma.krail.social.state.KrailSocialType
import xyz.ksharma.krail.social.state.SocialType

internal fun KrailSocialType.resource(): DrawableResource = when (this) {
    KrailSocialType.LinkedIn -> Res.drawable.ic_linkedin
    KrailSocialType.Reddit -> Res.drawable.ic_reddit
    KrailSocialType.Instagram -> Res.drawable.ic_instagram
    KrailSocialType.Facebook -> Res.drawable.ic_facebook
}

internal fun SocialType.resource(): DrawableResource = when (this) {
    SocialType.LinkedIn -> Res.drawable.ic_linkedin
    SocialType.Reddit -> Res.drawable.ic_reddit
    SocialType.Instagram -> Res.drawable.ic_instagram
    SocialType.Facebook -> Res.drawable.ic_facebook
}

fun SocialType.toAnalyticsEventPlatform(): SocialConnectionLinkClickEvent.SocialPlatform =
    when (this) {
        SocialType.LinkedIn -> SocialConnectionLinkClickEvent.SocialPlatform.LINKEDIN
        SocialType.Reddit -> SocialConnectionLinkClickEvent.SocialPlatform.REDDIT
        SocialType.Instagram -> SocialConnectionLinkClickEvent.SocialPlatform.INSTAGRAM
        SocialType.Facebook -> SocialConnectionLinkClickEvent.SocialPlatform.FACEBOOK
    }

fun KrailSocialType.toAnalyticsEventPlatform(): SocialConnectionLinkClickEvent.SocialPlatform =
    when (this) {
        KrailSocialType.LinkedIn -> SocialConnectionLinkClickEvent.SocialPlatform.LINKEDIN
        KrailSocialType.Reddit -> SocialConnectionLinkClickEvent.SocialPlatform.REDDIT
        KrailSocialType.Instagram -> SocialConnectionLinkClickEvent.SocialPlatform.INSTAGRAM
        KrailSocialType.Facebook -> SocialConnectionLinkClickEvent.SocialPlatform.FACEBOOK
    }

