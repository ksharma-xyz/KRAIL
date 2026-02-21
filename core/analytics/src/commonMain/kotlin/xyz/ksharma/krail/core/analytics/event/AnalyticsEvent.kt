package xyz.ksharma.krail.core.analytics.event

import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val PROP_STOP_ID = "stopId"
private const val PROP_SOURCE = "source"

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
        val searchQuery: String? = null,
    ) : AnalyticsEvent(
        name = "stop_selected",
        properties = buildMap {
            put(PROP_STOP_ID, stopId)
            put("isRecentSearch", isRecentSearch)
            searchQuery?.let { put("searchQuery", it) }
        },
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
    ) : AnalyticsEvent(
        name = "back_click",
        properties = mapOf(
            "fromScreen" to fromScreen.name,
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
            PROP_SOURCE to source.source,
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
            PROP_STOP_ID to stopId.trim(),
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
            PROP_SOURCE to source.actionName,
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

    // region SearchStopMap

    /**
     * Fired when the user taps the Map toggle button in SearchTopBar.
     * Use to measure map feature adoption: how many users ever discover and open the map view.
     *
     * @param selected true = user opened map view, false = returned to list view.
     */
    data class MapToggleClickEvent(val selected: Boolean) : AnalyticsEvent(
        name = "search_stop_map_toggle_click",
        properties = mapOf("selected" to selected),
    )

    /**
     * Fired when the user taps "Save" on the MapOptionsBottomSheet.
     *
     * This single event is the source of truth for all map-options analysis. One rich snapshot
     * replaces what would otherwise be 4+ separate events. Use it to answer:
     *  - "What % of users prefer 5km radius?" → aggregate on [radiusKm]
     *  - "What transport mode filter combos are most common?" → aggregate on [transportModes]
     *  - "Do users actually change the radius or just leave it at default?" → filter [radiusChanged]
     *  - "Do map control toggles matter?" → look at [showDistanceScale] / [showCompass] distributions
     *
     * @param radiusKm          The saved search radius: 1.0, 3.0, or 5.0.
     * @param transportModes    Sorted, comma-separated product-class integers of all *enabled* modes
     *                          (e.g. "1,2,5,9").
     * @param showDistanceScale Whether the distance scale overlay is enabled after saving.
     * @param showCompass       Whether the compass overlay is enabled after saving.
     * @param radiusChanged     Whether [radiusKm] differs from the value before the sheet opened.
     * @param modesChanged      Whether any transport mode filter changed from the previous state.
     */
    data class MapOptionsSavedEvent(
        val radiusKm: Double,
        val transportModes: String,
        val showDistanceScale: Boolean,
        val showCompass: Boolean,
        val radiusChanged: Boolean,
        val modesChanged: Boolean,
    ) : AnalyticsEvent(
        name = "search_stop_map_options_saved",
        properties = mapOf(
            "radiusKm" to radiusKm,
            "transportModes" to transportModes,
            "showDistanceScale" to showDistanceScale,
            "showCompass" to showCompass,
            "radiusChanged" to radiusChanged,
            "modesChanged" to modesChanged,
        ),
    )

    /**
     * Fired when the user selects a stop via the map's StopDetailsBottomSheet.
     *
     * Captures the map configuration context at the moment of selection, so you can correlate
     * search settings with successful stop discovery:
     *  - Does a wider [searchRadiusKm] lead to more stop selections?
     *  - Do users with fewer [enabledModesCount] find stops faster?
     *  - Does having [hadUserLocation] improve selection rate?
     *
     * NOTE: GPS coordinates are intentionally NOT captured — neither user location nor map centre.
     * Logging lat/lon (even anonymously) requires declaring "Precise Location" data collection in
     * both the Apple App Privacy label and the Google Play Data Safety section. Use the boolean
     * [hadUserLocation] flag instead, which carries no privacy obligations.
     *
     * @param stopId             The ID of the stop the user selected.
     * @param searchRadiusKm     The active search radius at the moment of selection.
     * @param enabledModesCount  Number of transport modes currently enabled (not which ones —
     *                           use [MapOptionsSavedEvent.transportModes] for mode-level detail).
     * @param nearbyStopsCount   Number of stop markers visible on the map at time of selection.
     * @param hadUserLocation    Whether the device location was available (permission granted +
     *                           location resolved). Does NOT include coordinates.
     */
    data class StopSelectedFromMapEvent(
        val stopId: String,
        val searchRadiusKm: Double,
        val enabledModesCount: Int,
        val nearbyStopsCount: Int,
        val hadUserLocation: Boolean,
    ) : AnalyticsEvent(
        name = "stop_selected_from_map",
        properties = mapOf(
            PROP_STOP_ID to stopId,
            "searchRadiusKm" to searchRadiusKm,
            "enabledModesCount" to enabledModesCount,
            "nearbyStopsCount" to nearbyStopsCount,
            "hadUserLocation" to hadUserLocation,
        ),
    )

    /**
     * Fired when the user taps a stop marker on the SearchStopMap (the bottom sheet opens).
     *
     * This is the first step of the map selection funnel:
     *   nearby_stop_click → (user reviews sheet) → stop_selected_from_map
     *
     * Use the click→select ratio in BigQuery to understand drop-off:
     *   COUNT(stop_selected_from_map) / COUNT(nearby_stop_click) per user session.
     *
     * @param stopId             The stop that was tapped.
     * @param transportModesCount Number of transport modes served at this stop.
     *                            Stops with more modes may attract more exploratory taps.
     */
    data class NearbyStopClickEvent(
        val stopId: String,
        val transportModesCount: Int,
    ) : AnalyticsEvent(
        name = "nearby_stop_click",
        properties = mapOf(
            PROP_STOP_ID to stopId,
            "transportModesCount" to transportModesCount,
        ),
    )

    /**
     * Fired when the user taps the Options button on the map (SearchStopMap only).
     * Use to measure how often users discover and open map configuration.
     */
    data object MapOptionsOpenedEvent : AnalyticsEvent(name = "search_stop_map_options_opened")

    /**
     * Fired when the user taps "Go to Settings" on the location permission denied banner.
     *
     * Tells you how many users hit the permission wall and were motivated enough to open Settings.
     * Use this together with [MapLocationButtonClickEvent] (isLocationActive = false) to measure
     * the permission recovery funnel:
     *   location_button tap (no permission) → banner shown → settings click → (hopefully) grant
     *
     * [source] identifies which map screen the banner was shown on.
     */
    data class LocationPermissionSettingsClickEvent(
        val source: Source,
    ) : AnalyticsEvent(
        name = "location_permission_settings_click",
        properties = mapOf(PROP_SOURCE to source.value),
    ) {
        enum class Source(val value: String) {
            SEARCH_STOP_MAP("search_stop_map"),
            JOURNEY_MAP("journey_map"),
        }
    }

    /**
     * Fired when the user taps the User Location button on any map screen.
     *
     * [isLocationActive] tells you the state at tap time:
     *  - true  → user already had location and is re-centering the camera
     *  - false → user is attempting to start location (no permission yet, or was denied)
     *
     * [source] identifies which map screen the tap came from, so you can compare
     * location usage between the stop-search flow and the journey-viewing flow.
     *
     * In BigQuery, filter `isLocationActive = false` to identify users who want location
     * but can't get it. No coordinates are captured.
     */
    data class MapLocationButtonClickEvent(
        val isLocationActive: Boolean,
        val source: Source,
    ) : AnalyticsEvent(
        name = "user_location_button_click",
        properties = mapOf(
            "isLocationActive" to isLocationActive,
            PROP_SOURCE to source.value,
        ),
    ) {
        enum class Source(val value: String) {
            SEARCH_STOP_MAP("search_stop_map"),
            JOURNEY_MAP("journey_map"),
        }
    }

    // endregion

    // region JourneyMap

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
