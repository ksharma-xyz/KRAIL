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
 *         refreshIntervalMs = remoteConfig.getLong("departure_refresh_interval_ms")
 *                                 .takeIf { it > 0 } ?: DepartureBoardConfig.DEFAULT_REFRESH_INTERVAL_MS,
 *     )
 * }
 * ```
 *
 * @param refreshIntervalMs              Milliseconds between full API re-fetches (default 30 s).
 *                                       This is a per-stop budget, not a per-collector one: however
 *                                       many surfaces poll the same stop at once (the map sheet and
 *                                       the saved trips screen both can), exactly one API call is
 *                                       made per window. `DepartureBoardRepository.fetchIfWindowOpen`
 *                                       enforces it — the first session to claim the window fetches,
 *                                       the rest read the result from the shared cache entry.
 * @param previousDeparturesWindowMinutes Minutes into the past to fetch when the user taps
 *                                       "Show previous" (default 30 min). Can be driven from
 *                                       remote config or feature flags without changing call sites.
 */
data class DepartureBoardConfig(
    val refreshIntervalMs: Long = DEFAULT_REFRESH_INTERVAL_MS,
    val previousDeparturesWindowMinutes: Long = DEFAULT_PREVIOUS_WINDOW_MINUTES,
) {
    companion object {
        const val DEFAULT_REFRESH_INTERVAL_MS = 30_000L
        const val DEFAULT_PREVIOUS_WINDOW_MINUTES = 30L
    }
}
