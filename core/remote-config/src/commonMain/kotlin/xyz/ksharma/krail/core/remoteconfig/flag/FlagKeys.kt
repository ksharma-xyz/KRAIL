package xyz.ksharma.krail.core.remoteconfig.flag

enum class FlagKeys(val key: String) {
    HIGH_PRIORITY_STOP_IDS("high_priority_stop_ids"),
    OUR_STORY_TEXT("our_story_text"),
    DISCLAIMER_TEXT("disclaimer_text"),
    NSW_PARK_RIDE_FACILITIES("nsw_park_ride_facilities"),

    NSW_PARK_RIDE_PEAK_TIME_COOLDOWN("nsw_park_ride_peak_time_cooldown"),

    NSW_PARK_RIDE_NON_PEAK_TIME_COOLDOWN("nsw_park_ride_non_peak_time_cooldown"),

    NSW_PARK_RIDE_BETA("nsw_park_ride_beta"),

    NSW_PARK_RIDE_BETA_MESSAGE_DESC("nsw_park_ride_beta_message_desc"),

    FESTIVALS("festivals"),

    DISCOVER_SYDNEY("discover_sydney"),

    DISCOVER_AVAILABLE("is_discover_available"),

    MIN_SUPPORTED_APP_VERSION("min_supported_app_version"),

    LATEST_APP_VERSION_ANDROID("latest_app_version_android"),

    LATEST_APP_VERSION_IOS("latest_app_version_ios"),

    INFO_TILES("info_tiles_json"),

    SEARCH_STOP_MAPS_AVAILABLE("maps_stop_search"),
    JOURNEY_MAPS_AVAILABLE("maps_journey"),
}
