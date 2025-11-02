package xyz.ksharma.krail.core.analytics.event

import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

sealed class AnalyticsEvent(val name: String, val properties: Map<String, Any>? = null) {

    data class ScreenViewEvent(val screen: AnalyticsScreen) : AnalyticsEvent(
        name = "view_screen",
        properties = mapOf("name" to screen.name),
    )

    // region SavedTrips

    data class SavedTripCardClickEvent(val fromStopId: String, val toStopId: String) :
        AnalyticsEvent(
            name = "saved_trip_card_click",
            properties = mapOf("fromStopId" to fromStopId, "toStopId" to toStopId),
        )

    data class DeleteSavedTripClickEvent(val fromStopId: String, val toStopId: String) :
        AnalyticsEvent(
            name = "delete_saved_trip_card_click",
            properties = mapOf("fromStopId" to fromStopId, "toStopId" to toStopId),
        )

    data object ReverseStopClickEvent : AnalyticsEvent(name = "reverse_stop_click")

    data class LoadTimeTableClickEvent(val fromStopId: String, val toStopId: String) :
        AnalyticsEvent(
            name = "load_timetable_click",
            properties = mapOf("fromStopId" to fromStopId, "toStopId" to toStopId),
        )

    data object SettingsClickEvent : AnalyticsEvent(name = "settings_click")

    data object FromFieldClickEvent : AnalyticsEvent(name = "from_field_click")

    data object ToFieldClickEvent : AnalyticsEvent(name = "to_field_click")

    // endregion

    // region SearchStop

    data class StopSelectedEvent(
        val stopId: String,
        val isRecentSearch: Boolean = false,
    ) : AnalyticsEvent(
        name = "stop_selected",
        properties = mapOf(
            "stopId" to stopId,
            "isRecentSearch" to isRecentSearch,
        ),
    )

    data class SearchStopQuery(
        val query: String,
        val resultsCount: Int? = null,
        val isError: Boolean = false,
    ) : AnalyticsEvent(
        name = "search_stop_query",
        properties = mutableMapOf<String, Any>(
            "query" to query,
        ).apply {
            if (isError) {
                put("isError", isError)
            } else if (resultsCount != null) {
                put("resultsCount", resultsCount)
            }
        },
    )

    data class ClearRecentSearchClickEvent(
        val recentSearchCount: Int,
    ) : AnalyticsEvent(
        name = "clear_recent_search_stops",
        properties = mapOf(
            "recentSearchCount" to recentSearchCount,
        ),
    )

    // endregion

    // region Theme

    data class ThemeSelectedEvent(val themeId: String) : AnalyticsEvent(
        name = "theme_selected",
        properties = mapOf("themeId" to themeId),
    )

    // endregion

    // region PlanTripScreen / DateTimeSelection Screen

    data object ResetTimeClickEvent : AnalyticsEvent("reset_time_click")

    // endregion

    // region TimeTable Screen

    data class ReverseTimeTableClickEvent(val fromStopId: String, val toStopId: String) :
        AnalyticsEvent(
            name = "reverse_time_table_click",
            properties = mapOf("fromStopId" to fromStopId, "toStopId" to toStopId),
        )

    data class SaveTripClickEvent(val fromStopId: String, val toStopId: String) : AnalyticsEvent(
        name = "save_trip_click",
        properties = mapOf("fromStopId" to fromStopId, "toStopId" to toStopId),
    )

    data class PlanTripClickEvent(val fromStopId: String, val toStopId: String) : AnalyticsEvent(
        name = "plan_trip_click",
        properties = mapOf("fromStopId" to fromStopId, "toStopId" to toStopId),
    )

    data class ModeClickEvent(
        val fromStopId: String,
        val toStopId: String,
        val displayModeSelectionRow: Boolean,
    ) : AnalyticsEvent(
        name = "mode_click",
        properties = mapOf(
            "fromStopId" to fromStopId,
            "toStopId" to toStopId,
            "displayModeSelectionRow" to displayModeSelectionRow,
        ),
    )

    data class ModeSelectionDoneEvent(
        val fromStopId: String,
        val toStopId: String,
        val unselectedProductClasses: Set<Int>,
    ) : AnalyticsEvent(
        name = "mode_selection_done",
        properties = mapOf(
            "fromStopId" to fromStopId,
            "toStopId" to toStopId,
            "unselected" to unselectedProductClasses.toString(),
        ),
    )

    data class DateTimeSelectEvent(
        val dayOfWeek: String,
        val time: String,
        val journeyOption: String,
        // User clicked on reset button and then selected the time
        val isReset: Boolean = false,
    ) : AnalyticsEvent(
        name = "date_time_select",
        properties = mapOf(
            "dayOfWeek" to dayOfWeek,
            "time" to time,
            "journeyOption" to journeyOption,
            "isReset" to isReset,
        ),
    )

    data class JourneyCardExpandEvent(val hasStarted: Boolean) : AnalyticsEvent(
        name = "journey_card_expand",
        properties = mapOf("hasStarted" to hasStarted),
    )

    data class JourneyCardCollapseEvent(val hasStarted: Boolean) : AnalyticsEvent(
        name = "journey_card_collapse",
        properties = mapOf("hasStarted" to hasStarted),
    )

    data class JourneyLegClickEvent(val expanded: Boolean) : AnalyticsEvent(
        name = "journey_leg_click",
        properties = mapOf("expanded" to expanded),
    )

    data class JourneyAlertClickEvent(val fromStopId: String, val toStopId: String) :
        AnalyticsEvent(
            name = "journey_alert_click",
            properties = mapOf("fromStopId" to fromStopId, "toStopId" to toStopId),
        )

    // endregion

    // region Generic Events
    data class BackClickEvent(
        val fromScreen: AnalyticsScreen,
        val isPreviousBackStackEntryNull: Boolean,
    ) : AnalyticsEvent(
        name = "back_click",
        properties = mapOf(
            "fromScreen" to fromScreen.name,
            "isPreviousBackStackEntryNull" to isPreviousBackStackEntryNull,
        ),
    )

    @OptIn(ExperimentalTime::class)
    data class AppStart(
        val platformType: String,
        val appVersion: String,
        val osVersion: String,
        val deviceModel: String,
        val fontSize: String,
        val isDarkTheme: Boolean,
        val krailTheme: Int,
        val locale: String,
        val batteryLevel: Int,
        val timeZone: String,
    ) : AnalyticsEvent(
        name = "app_start",
        properties = mapOf(
            "platformType" to platformType.trim(),
            "appVersion" to appVersion.trim(),
            "osVersion" to osVersion.trim(),
            "deviceModel" to deviceModel.trim(),
            "fontSize" to fontSize.trim(),
            "isDarkTheme" to isDarkTheme,
            "krailTheme" to krailTheme,
            "timeStamp" to Clock.System.now().toString(),
            "locale" to locale.trim(),
            "batteryLevel" to batteryLevel,
            "timeZone" to timeZone.trim(),
        ),
    )
    // endregion

    // region Settings

    /**
     * Analytics event for the refer friend button click.
     *
     * @param entryPoint The entry point from which the user referred a friend. E.g. Setting / Intro
     * screen etc.
     */
    data class ReferFriend(val entryPoint: EntryPoint) : AnalyticsEvent(
        name = "refer_friend",
        properties = mapOf(
            "entryPoint" to entryPoint.from,
        ),
    ) {
        enum class EntryPoint(val from: String) {
            SETTINGS("settings"),
            INTRO_BUTTON("intro_button"),
            INTRO_CONTENT_BUTTON("intro_content_button"),
            SAVED_TRIPS("saved_trips_tile"),
        }
    }

    /**
     * Analytics event for the refer friend button click in the intro screen.
     *
     * @param pageType The page number of the intro screen from which the user clicked Let's KRAIL button.
     * @param interactionPages Represents pages list where interaction was performed. with the
     * content inside the intro pages.
     */
    data class IntroLetsKrailClickEvent(
        val pageType: InteractionPage,
        val pageNumber: Int,
    ) : AnalyticsEvent(
        name = "intro_lets_krail",
        properties = mapOf(
            "completedOnPage" to pageType.name,
            "completedOnPageNumber" to pageNumber,
        ),
    ) {
        enum class InteractionPage {
            SAVE_TRIPS, REAL_TIME_ROUTES, ALERTS, PLAN_TRIP, SELECT_MODE, INVITE_FRIENDS, PARK_RIDE,
        }
    }

    data object SettingsHowToUseClickEvent : AnalyticsEvent(
        name = "how_to_krail",
    )

    data object OurStoryClick : AnalyticsEvent(
        name = "our_story",
    )

    data class SocialConnectionLinkClickEvent(
        val socialPlatformType: SocialPlatformType,
        val source: SocialConnectionSource,
    ) : AnalyticsEvent(
        name = "social_connection_link_click",
        properties = mapOf(
            "socialPlatform" to socialPlatformType.platformName,
            "source" to source.source,
        ),
    ) {
        enum class SocialPlatformType(val platformName: String) {
            LINKEDIN("linkedin"), REDDIT("reddit"), INSTAGRAM("instagram"), FACEBOOK("facebook"),
        }

        enum class SocialConnectionSource(val source: String) {
            SETTINGS("settings"), DISCOVER_CARD("discover_card"),
        }
    }

    // endregion

    // region Park and Ride

    /**
     * Analytics event for the Park and Ride card click.
     * @param stopId The ID of the stop associated with the Park and Ride facility.
     * @param facilityId The ID of the Park and Ride facility.
     * @param expand Indicates whether the card is being expanded or collapsed.
     * @param time - when the card was clicked, format - epoch time in seconds.
     */
    data class ParkRideCardClickEvent
    @OptIn(ExperimentalTime::class)
    constructor(
        val stopId: String,
        val facilityId: String,
        val expand: Boolean,
        val time: Long = Clock.System.now().epochSeconds,
    ) : AnalyticsEvent(
        name = "park_ride_card_click",
        properties = mapOf(
            "stopId" to stopId.trim(),
            "facilityId" to facilityId.trim(),
            "expand" to expand.toString().trim(),
            "time" to time.toString().trim(),
        ),
    )
    // endregion

    // region Discover

    data object DiscoverButtonClick : AnalyticsEvent(
        name = "discover_button_click",
    )

    data class DiscoverCardClick(
        val location: String = "SYD",
        val source: Source,
        val cardId: String,
        val cardType: CardType,
        val partnerSocialLink: PartnerSocialLink? = null,
    ) : AnalyticsEvent(
        name = "discover_card_click",
        properties = mutableMapOf(
            // "location" to location,
            "source" to source.actionName,
            "cardId" to cardId,
            "cardType" to cardType.displayName,
        ).apply {
            partnerSocialLink?.let { socialLink ->
                put("partnerSocialPlatformName", socialLink.type.platformName)
                put("partnerSocialPlatformUrl", socialLink.url)
            }
        },
    ) {
        data class PartnerSocialLink(
            val type: SocialConnectionLinkClickEvent.SocialPlatformType,
            val url: String,
        )

        enum class CardType(val displayName: String) {
            TRAVEL(displayName = "Travel"),
            EVENTS(displayName = "Events"),
            FOOD(displayName = "Food"),
            SPORTS(displayName = "Sports"),
            UNKNOWN(displayName = "unknown"),
        }

        enum class Source(val actionName: String) {
            CTA_CLICK("cta_click"),
            SHARE_CLICK("share"),
            PARTNER_SOCIAL_LINK("partner_social_link"),
        }
    }

    data class DiscoverCardSessionComplete(
        val cardSeenCount: Int,
        val location: String = "SYD",
    ) : AnalyticsEvent(
        name = "discover_session_complete",
        properties = mapOf(
            "cardSeenCount" to cardSeenCount,
            "location" to location,
        ),
    )

    data class DiscoverFilterChipSelected(
        val cardType: DiscoverCardClick.CardType,
    ) : AnalyticsEvent(
        name = "discover_filter_chip_selected",
        properties = mapOf(
            "cardType" to cardType.displayName,
        ),
    )

    // endregion

    // region InfoTiles

    data class InfoTileInteraction(
        val key: String,
        val expand: Boolean? = null,
        val dismiss: Boolean? = null,
        val ctaUrl: String? = null,
    ) : AnalyticsEvent(
        name = "info_tile_interaction",
        properties = mutableMapOf<String, Any>(
            "key" to key,
        ).apply {
            dismiss?.let { put("dismiss", dismiss) }
            ctaUrl?.let { put("cta_click", ctaUrl) }
            expand?.let { put("expand", expand) }
        },
    )

    // endregion
}
