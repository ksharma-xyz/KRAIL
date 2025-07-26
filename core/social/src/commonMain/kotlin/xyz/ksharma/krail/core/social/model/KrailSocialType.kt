package xyz.ksharma.krail.core.social.model

import krail.core.social.generated.resources.Res
import krail.core.social.generated.resources.ic_reddit
import krail.core.social.generated.resources.ic_linkedin
import krail.core.social.generated.resources.ic_instagram
import krail.core.social.generated.resources.ic_facebook
import org.jetbrains.compose.resources.DrawableResource
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent

enum class SocialType(val displayName: String) {
    LinkedIn(displayName = "linkedin"),
    Reddit(displayName = "reddit"),
    Instagram(displayName = "instagram"),
    Facebook(displayName = "facebook"),
}

// todo - move to social:network:api module
enum class KrailSocialType(val socialType: SocialType, val url: String) {
    LinkedIn(socialType = SocialType.LinkedIn, url = "https://www.linkedin.com/company/krail/"),

    Reddit(socialType = SocialType.Reddit, url = "https://www.reddit.com/r/krailapp/"),

    Instagram(socialType = SocialType.Instagram, url = "https://www.instagram.com/krailapp/"),

    Facebook(socialType = SocialType.Facebook, url = "https://www.facebook.com/krailapp")
    ;
}

fun KrailSocialType.resource(): DrawableResource = when (this) {
    KrailSocialType.LinkedIn -> Res.drawable.ic_linkedin
    KrailSocialType.Reddit -> Res.drawable.ic_reddit
    KrailSocialType.Instagram -> Res.drawable.ic_instagram
    KrailSocialType.Facebook -> Res.drawable.ic_facebook
}

fun KrailSocialType.toAnalyticsEventPlatform(): AnalyticsEvent.SocialConnectionLinkClickEvent.SocialPlatform =
    when (this) {
        KrailSocialType.LinkedIn -> AnalyticsEvent.SocialConnectionLinkClickEvent.SocialPlatform.LINKEDIN
        KrailSocialType.Reddit -> AnalyticsEvent.SocialConnectionLinkClickEvent.SocialPlatform.REDDIT
        KrailSocialType.Instagram -> AnalyticsEvent.SocialConnectionLinkClickEvent.SocialPlatform.INSTAGRAM
        KrailSocialType.Facebook -> AnalyticsEvent.SocialConnectionLinkClickEvent.SocialPlatform.FACEBOOK
    }