package xyz.ksharma.krail.departures.ui

/**
 * Runtime configuration for the departure board polling behaviour.
 *
 * Injected as a Koin singleton so it can be replaced at any injection site — e.g.
 * populated from Firebase Remote Config — without changing call sites. This mirrors
 * the same pattern used for Park & Ride remote config.
 *
 * ### Future remote-config wiring (sketch):
 * ```kotlin
 * single {
 *     DepartureBoardConfig(
 *         refreshIntervalMs  = remoteConfig.getLong("departure_refresh_interval_ms")
 *                                 .takeIf { it > 0 } ?: DepartureBoardConfig.DEFAULT_REFRESH_INTERVAL_MS,
 *         relativeTimeRefreshMs = DepartureBoardConfig.DEFAULT_RELATIVE_TIME_REFRESH_MS,
 *     )
 * }
 * ```
 *
 * @param refreshIntervalMs              Milliseconds between full API re-fetches (default 30 s).
 *                                       The repository will not call the API more often than this,
 *                                       even if the same stop is expanded across both the map sheet
 *                                       and the saved trips screen simultaneously.
 * @param relativeTimeRefreshMs          Milliseconds between "in X mins" text recomputes (default 10 s).
 * @param previousDeparturesWindowMinutes Minutes into the past to fetch when the user taps
 *                                       "Show previous" (default 30 min). Can be driven from
 *                                       remote config or feature flags without changing call sites.
 */
data class DepartureBoardConfig(
    val refreshIntervalMs: Long = DEFAULT_REFRESH_INTERVAL_MS,
    val relativeTimeRefreshMs: Long = DEFAULT_RELATIVE_TIME_REFRESH_MS,
    val previousDeparturesWindowMinutes: Long = DEFAULT_PREVIOUS_WINDOW_MINUTES,
) {
    companion object {
        const val DEFAULT_REFRESH_INTERVAL_MS = 30_000L
        const val DEFAULT_RELATIVE_TIME_REFRESH_MS = 10_000L
        const val DEFAULT_PREVIOUS_WINDOW_MINUTES = 30L
    }
}
