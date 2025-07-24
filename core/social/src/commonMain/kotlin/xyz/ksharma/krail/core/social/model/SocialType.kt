package xyz.ksharma.krail.core.social.model

import krail.core.social.generated.resources.Res
import krail.core.social.generated.resources.ic_reddit
import krail.core.social.generated.resources.ic_linkedin
import krail.core.social.generated.resources.ic_instagram
import krail.core.social.generated.resources.ic_facebook
import org.jetbrains.compose.resources.DrawableResource
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent

enum class SocialType(val displayName: String, val httpLink: String) {
    LinkedIn(displayName = "LinkedIn", httpLink = "https://www.linkedin.com/company/krail/"),

    Reddit(displayName = "Reddit", httpLink = "https://www.reddit.com/r/krailapp/"),

    Instagram(displayName = "Instagram", httpLink = "https://www.instagram.com/krailapp/"),

    Facebook(displayName = "Facebook", httpLink = "https://www.facebook.com/krailapp")
    ;
}

fun SocialType.resource(): DrawableResource = when (this) {
    SocialType.LinkedIn -> Res.drawable.ic_linkedin
    SocialType.Reddit -> Res.drawable.ic_reddit
    SocialType.Instagram -> Res.drawable.ic_instagram
    SocialType.Facebook -> Res.drawable.ic_facebook
}

fun SocialType.toAnalyticsEventPlatform(): AnalyticsEvent.SocialConnectionLinkClickEvent.SocialPlatform = when (this) {
    SocialType.LinkedIn -> AnalyticsEvent.SocialConnectionLinkClickEvent.SocialPlatform.LINKEDIN
    SocialType.Reddit -> AnalyticsEvent.SocialConnectionLinkClickEvent.SocialPlatform.REDDIT
    SocialType.Instagram -> AnalyticsEvent.SocialConnectionLinkClickEvent.SocialPlatform.INSTAGRAM
    SocialType.Facebook -> AnalyticsEvent.SocialConnectionLinkClickEvent.SocialPlatform.FACEBOOK
}