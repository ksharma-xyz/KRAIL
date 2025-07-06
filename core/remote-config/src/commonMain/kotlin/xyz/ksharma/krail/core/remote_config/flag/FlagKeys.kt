package xyz.ksharma.krail.core.remote_config.flag

enum class FlagKeys(val key: String) {
    HIGH_PRIORITY_STOP_IDS("high_priority_stop_ids"),
    OUR_STORY_TEXT("our_story_text"),
    DISCLAIMER_TEXT("disclaimer_text"),
    NSW_PARK_RIDE_FACILITIES("nsw_park_ride_facilities"),

    NSW_PARK_RIDE_PEAK_TIME_COOLDOWN("nsw_park_ride_peak_time_cooldown"),

    NSW_PARK_RIDE_NON_PEAK_TIME_COOLDOWN("nsw_park_ride_non_peak_time_cooldown"),

    NSW_PARK_RIDE_BETA("nsw_park_ride_beta"),

    NSW_PARK_RIDE_BETA_MESSAGE_DESC("nsw_park_ride_beta_message_desc"),

    LINKED_IN_KRAIL_APP_URL("linked_in_krail_app_url"),

    FESTIVALS("festivals"),
}
