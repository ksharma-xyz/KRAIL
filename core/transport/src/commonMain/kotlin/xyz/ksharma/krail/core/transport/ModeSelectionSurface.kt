package xyz.ksharma.krail.core.transport

/**
 * A surface that lets the user pick transport modes.
 *
 * Each mode declares which surfaces it belongs to via the city config (see
 * [xyz.ksharma.krail.core.transport.nsw.NswTransportConfig.modesFor]). This keeps
 * "which modes show where" a single, exhaustive policy decision rather than scattered
 * `.filter` hacks at each call site.
 */
enum class ModeSelectionSurface {
    /** Map "Show stops for" filter — which nearby stop types appear on the map. */
    NEARBY_STOPS,

    /** Timetable trip-planner mode picker — real modes passed to the trip API. */
    TRIP_PLANNER,
}
