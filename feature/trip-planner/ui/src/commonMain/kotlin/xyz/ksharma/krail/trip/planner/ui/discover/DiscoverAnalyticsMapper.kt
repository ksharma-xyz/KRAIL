package xyz.ksharma.krail.trip.planner.ui.discover

import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.DiscoverCardClick
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.SocialConnectionLinkClickEvent
import xyz.ksharma.krail.discover.state.DiscoverCardType
import xyz.ksharma.krail.social.state.SocialType

internal fun DiscoverCardType.toAnalyticsCardType(): DiscoverCardClick.CardType {
    return when (this) {
        DiscoverCardType.Events -> DiscoverCardClick.CardType.EVENTS
        DiscoverCardType.Sports -> DiscoverCardClick.CardType.SPORTS
        DiscoverCardType.Krail -> DiscoverCardClick.CardType.KRAIL
        DiscoverCardType.Travel -> DiscoverCardClick.CardType.TRAVEL
        DiscoverCardType.Food -> DiscoverCardClick.CardType.FOOD
        DiscoverCardType.Unknown -> DiscoverCardClick.CardType.UNKNOWN
    }
}

internal fun SocialType.toAnalyticsSocialType(): SocialConnectionLinkClickEvent.SocialPlatformType =
    when (this) {
        SocialType.LinkedIn -> SocialConnectionLinkClickEvent.SocialPlatformType.LINKEDIN
        SocialType.Reddit -> SocialConnectionLinkClickEvent.SocialPlatformType.REDDIT
        SocialType.Instagram -> SocialConnectionLinkClickEvent.SocialPlatformType.INSTAGRAM
        SocialType.Facebook -> SocialConnectionLinkClickEvent.SocialPlatformType.FACEBOOK
    }
