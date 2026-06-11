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

    TRIP_TRACKING_ENABLED("trip_tracking_enabled"),

    ENABLE_FUZZY_STOP_SEARCH("enable_fuzzy_stop_search"),

    /**
     * Single rollout flag for the KRAIL-BFF. When `true`, the four
     * BFF-eligible services (trip results, departures, park-ride,
     * GTFS-realtime) route through the BFF; when `false` they hit NSW
     * direct. Default `false` until cohort rollout begins.
     *
     * Resolved by `BffEndpointResolver` in `:core:network`. Debug builds
     * may override via the Debug Config UI (`NetworkSource.NSW_DIRECT` /
     * `BFF_LOCAL` / `BFF_PROD`); release builds always read the live RC
     * value.
     */
    ENABLE_PROTO_BFF("enable_proto_bff"),

    /**
     * Per-feature kill switch for the BFF live-tracking path
     * (`POST /api/v1/track/snapshot`). Only consulted when the endpoint
     * already resolves to the BFF (see [ENABLE_PROTO_BFF] + the arming
     * gate): `true`/unset → tracking polls the BFF snapshot endpoint;
     * `false` → tracking falls back to direct GTFS-RT polling even while
     * the rest of the app uses the BFF. Defaults to enabled so debug
     * `BFF_LOCAL` testing needs no RC change.
     */
    BFF_USE_FOR_TRACK("bff_use_for_track"),
}
