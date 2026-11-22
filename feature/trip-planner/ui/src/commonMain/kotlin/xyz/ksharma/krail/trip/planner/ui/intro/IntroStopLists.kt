package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop

/** How many rows a stop screen lists before the rider types anything. */
internal const val INTRO_STOP_LIST_SIZE = 5

/** The most nearby stations the origin list leads with. The curated starts fill the rest. */
internal const val INTRO_NEARBY_STATIONS = 3

/**
 * The rows of the origin screen: the nearest two or three stations first, then the curated starts, [size] in all.
 * [nearby] is the stops near the rider, nearest first, and may be empty (no fix, a denied location, stops not
 * ready), which gives the curated list alone. A station that is nearby and curated shows once, at its nearby
 * place, and fewer than three nearby stations are made up from the curated ones.
 */
internal fun fromSuggestions(
    nearby: List<IntroStop>,
    curatedStarts: List<SuggestedStop>,
    size: Int = INTRO_STOP_LIST_SIZE,
): List<IntroStop> = distinctStops(
    nearby.collapsedByStationName().take(INTRO_NEARBY_STATIONS) + curatedStarts.map { it.toIntroStop() },
).take(size)

/** The rows of the destination screen: the curated ends alone, because where a rider goes is not where they stand. */
internal fun destinationSuggestions(
    curatedEnds: List<SuggestedStop>,
    size: Int = INTRO_STOP_LIST_SIZE,
): List<IntroStop> = distinctStops(curatedEnds.map { it.toIntroStop() }).take(size)

// The first of each place, so the order given is the order kept.
private fun distinctStops(stops: List<IntroStop>): List<IntroStop> =
    stops.fold(emptyList()) { kept, stop -> if (kept.any { it.isSameStopAs(stop) }) kept else kept + stop }
