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

    /**
     * Kill switch for the address/POI search section on SearchStopScreen
     * (`stop_finder` with `type_sf=any`, filtered to non-`stop` results). `false` by
     * default — when off, the remote search job is never launched, so local stop
     * search is completely unaffected regardless of this flag's value.
     */
    SEARCH_STOP_ADDRESS_SEARCH_ENABLED("search_stop_address_search_enabled"),

    /**
     * Minimum trimmed-query length (characters) before [SEARCH_STOP_ADDRESS_SEARCH_ENABLED]
     * is allowed to fire an address/POI request. Bounded integer `2..12`; a missing,
     * malformed, or out-of-range value falls back to `6` client-side rather than being
     * clamped. Independent of the boolean kill switch above — this only tunes *when* an
     * already-enabled search fires, it cannot enable the feature by itself. See
     * feature/trip-planner/ui/ADDRESS_SEARCH_ELIGIBILITY.md.
     */
    SEARCH_STOP_ADDRESS_MIN_QUERY_LENGTH("search_stop_address_min_query_length"),

    /**
     * Kill switch for requesting the platform review sheet (Play In-App Review / StoreKit).
     * `false` by default so the trigger stays dormant until it is deliberately switched on.
     * When off, saved-trip opens are still counted, so flipping it on later sees each user's
     * real history rather than restarting everyone from zero.
     *
     * This gates the *automatic* request only. A user-initiated "Rate KRAIL" row in Settings
     * deep-links to the store listing and is unaffected.
     */
    IN_APP_REVIEW_ENABLED("in_app_review_enabled"),

    /**
     * Saved-trip opens required before a review sheet is requested. Defaults to
     * `DEFAULT_MIN_SAVED_TRIP_OPENS` in `:core:app-review` when missing or malformed.
     */
    IN_APP_REVIEW_MIN_SAVED_TRIP_OPENS("in_app_review_min_saved_trip_opens"),

    /**
     * Minimum install age in days before a review sheet is requested, read from the
     * user-lifecycle store (`docs/USER_LIFECYCLE_STORE.md`). Defaults to
     * `DEFAULT_MIN_ACCOUNT_AGE_DAYS` in `:core:app-review` when missing or malformed.
     */
    IN_APP_REVIEW_MIN_ACCOUNT_AGE_DAYS("in_app_review_min_account_age_days"),

    /**
     * Days between review requests. Both platforms throttle silently, so without a
     * client-side cooldown the platform quota gets spent on back-to-back requests with no
     * way to observe it. Defaults to `DEFAULT_COOLDOWN_DAYS` in `:core:app-review`.
     */
    IN_APP_REVIEW_COOLDOWN_DAYS("in_app_review_cooldown_days"),
}
