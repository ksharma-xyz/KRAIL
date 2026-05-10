package xyz.ksharma.krail.feature.debug.settings.state

/**
 * Logical groupings of network calls that the debug-settings runtime selector
 * can target independently.
 *
 * Mirrors the production Firebase Remote Config flag scheme
 * (`bff_use_for_<endpoint>`) so cohort rollout in production reuses the same
 * shape.
 *
 * Note: `STOP_FINDER` is deliberately omitted. The BFF has no `/stop_finder`
 * endpoint; stop search will move to local-search-on-device in Phase D rather
 * than gaining a third NSW/BFF/local target.
 */
enum class EndpointScope {
    TRIP_RESULTS,
    DEPARTURES,
    PARK_RIDE,
    TRACK,
}
