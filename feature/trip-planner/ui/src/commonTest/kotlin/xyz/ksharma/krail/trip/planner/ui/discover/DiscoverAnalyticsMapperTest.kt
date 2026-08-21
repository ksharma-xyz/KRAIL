package xyz.ksharma.krail.trip.planner.ui.discover

import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.DiscoverCardClick
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.SocialConnectionLinkClickEvent
import xyz.ksharma.krail.discover.state.DiscoverCardType
import xyz.ksharma.krail.social.state.SocialType
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The two Discover analytics mappers translate domain enums into the labels the dashboard groups
 * by. A wrong pairing does not fail anything at runtime — it silently files clicks under the wrong
 * category, and the mistake is only visible months later in a chart.
 *
 * Both tests assert the whole mapping at once, so adding a domain enum entry without an analytics
 * label fails here rather than compiling into a mis-labelled default.
 */
class DiscoverAnalyticsMapperTest {

    @Test
    fun `GIVEN every discover card type WHEN mapped THEN each gets its own analytics label`() {
        val expected = mapOf(
            DiscoverCardType.Travel to DiscoverCardClick.CardType.TRAVEL,
            DiscoverCardType.Events to DiscoverCardClick.CardType.EVENTS,
            DiscoverCardType.Food to DiscoverCardClick.CardType.FOOD,
            DiscoverCardType.Sports to DiscoverCardClick.CardType.SPORTS,
            DiscoverCardType.Unknown to DiscoverCardClick.CardType.UNKNOWN,
        )

        assertEquals(
            expected,
            DiscoverCardType.entries.associateWith { it.toAnalyticsCardType() },
        )
    }

    @Test
    fun `GIVEN every social platform WHEN mapped THEN each gets its own analytics platform`() {
        val expected = mapOf(
            SocialType.LinkedIn to SocialConnectionLinkClickEvent.SocialPlatformType.LINKEDIN,
            SocialType.Reddit to SocialConnectionLinkClickEvent.SocialPlatformType.REDDIT,
            SocialType.Instagram to SocialConnectionLinkClickEvent.SocialPlatformType.INSTAGRAM,
            SocialType.Facebook to SocialConnectionLinkClickEvent.SocialPlatformType.FACEBOOK,
        )

        assertEquals(
            expected,
            SocialType.entries.associateWith { it.toAnalyticsSocialType() },
        )
    }
}
