package xyz.ksharma.krail.trip.planner.ui.searchstop

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.sandook.utils.GeoUtils
import xyz.ksharma.krail.trip.planner.ui.intro.SuggestedStop
import xyz.ksharma.krail.trip.planner.ui.intro.SuggestedStops
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.ListState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState

// What the recents on Search Stop's list state hold, `docs/MAP_HOME_UX.md` rule 9. Every decision is a
// pure function, so the three sources and their order are pinned without a layout or a database.

/** The most stops the recents show. */
internal const val MAX_RECENT_ROWS = 5

/** Where the rows of the recents came from. */
internal enum class RecentsSource {
    /** The rider's own recent stops. */
    Recents,

    /** A rider with no recents and a location sees the stops nearest them instead. */
    Nearest,

    /** A rider with no recents and no location sees the Intro's suggested stops. */
    Curated,
}

/** The rows of the recents and where they came from. */
internal data class RecentsCard(val source: RecentsSource, val rows: List<SearchStopState.StopResult>)

/**
 * The recents for a rider with nothing typed, or null when there is nothing to show.
 *
 * - Any recents win, at most [MAX_RECENT_ROWS] of them.
 * - With no recents and a known location, the [nearest] stops. While they are still loading there is
 *   nothing to show yet, and the curated stops do not stand in for them, so the list never swaps
 *   one set for another a moment later.
 * - With no recents and no location, the [curated] stops.
 */
internal fun recentsCardFor(
    recents: List<SearchStopState.StopResult>,
    locationKnown: Boolean,
    nearest: List<SearchStopState.StopResult>,
    curated: List<SearchStopState.StopResult>,
): RecentsCard? = when {
    recents.isNotEmpty() -> RecentsCard(RecentsSource.Recents, recents.take(MAX_RECENT_ROWS))
    locationKnown -> nearest.takeIf { it.isNotEmpty() }
        ?.let { RecentsCard(RecentsSource.Nearest, it.take(MAX_RECENT_ROWS)) }
    else -> curated.takeIf { it.isNotEmpty() }
        ?.let { RecentsCard(RecentsSource.Curated, it.take(MAX_RECENT_ROWS)) }
}

/**
 * The stops in [stops] nearest [location], nearest first, at most [limit]. Equal distances go to the
 * smaller stop id so the same rider position always gives the same rows. With a [radiusKm] a stop
 * further away is left out, the edge is inclusive, and a stop whose distance cannot be worked out is
 * skipped.
 */
internal fun nearestStopRows(
    location: LatLng,
    stops: List<NearbyStopFeature>,
    limit: Int = MAX_RECENT_ROWS,
    radiusKm: Double? = null,
): List<SearchStopState.StopResult> = stops
    .mapNotNull { stop ->
        val distanceKm = GeoUtils.haversineDistance(
            lat1 = location.latitude,
            lon1 = location.longitude,
            lat2 = stop.position.latitude,
            lon2 = stop.position.longitude,
        ) ?: return@mapNotNull null
        stop to distanceKm
    }
    .filter { (_, distanceKm) -> radiusKm == null || distanceKm <= radiusKm }
    .sortedWith(compareBy<Pair<NearbyStopFeature, Double>> { it.second }.thenBy { it.first.stopId })
    .take(limit)
    .map { (stop, _) ->
        SearchStopState.StopResult(
            stopName = stop.stopName,
            stopId = stop.stopId,
            transportModeType = stop.transportModes,
        )
    }

/**
 * The Intro's suggested stops as rows: the start list when the rider is choosing a start, the end
 * list for everything else (a destination, a label, one leg). Each row carries its own id and name,
 * so a tap works before the stops table has finished loading. There are no mode badges, because the
 * list has none and the table may not be there to ask.
 */
internal fun curatedStopRows(
    fieldType: SearchStopFieldType,
    suggested: SuggestedStops,
): ImmutableList<SearchStopState.StopResult> =
    (if (fieldType == SearchStopFieldType.FROM) suggested.start else suggested.end)
        .map(SuggestedStop::toStopResult)
        .toImmutableList()

private fun SuggestedStop.toStopResult() = SearchStopState.StopResult(stopName = name, stopId = stopId)

/**
 * Whether the label row (Home, Work and any custom label that has a stop) sits at the top of the
 * card. It is the first row of the card whenever a label is set, with or without recents, and only
 * while a list the rider can pick from is showing.
 */
internal fun shouldShowLabelRow(listState: ListState, stopLabels: List<StopLabel>): Boolean {
    if (stopLabels.none { it.isSet }) return false
    return when (listState) {
        ListState.Recent -> true
        is ListState.Results -> listState.results.isNotEmpty()
        ListState.NoMatch, ListState.Error -> false
    }
}

/** The suggested stops to fall back on: starts for a start field, ends for everything else. */
internal fun SearchStopState.curatedStopsFor(fieldType: SearchStopFieldType): List<SearchStopState.StopResult> =
    if (fieldType == SearchStopFieldType.FROM) curatedStartStops else curatedEndStops
