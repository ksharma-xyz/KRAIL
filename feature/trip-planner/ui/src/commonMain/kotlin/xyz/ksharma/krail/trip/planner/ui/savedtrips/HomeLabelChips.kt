package xyz.ksharma.krail.trip.planner.ui.savedtrips

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.maps.data.model.NearbyStop
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.sandook.utils.GeoUtils
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.LabelChipTap
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.LabelTapOutcome
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

// The Home and Work chips above the home search row, `docs/MAP_HOME_UX.md` rule 41. Everything
// that decides something lives here as a pure function, so each rule is driven at its boundary
// without a layout, a location fix or a database.

/** How far from the rider a stop can be and still count as the nearest one, the same as Ask KRAIL's origin. */
internal const val LABEL_TAP_RADIUS_KM = 1.0

/** Rows asked of the stops query. A handful, so the nearest is chosen here with a tie rule, not by the query alone. */
internal const val LABEL_TAP_CANDIDATES = 5

/** One chip in the row. */
internal sealed interface HomeChip {

    /** Home or Work. Both stay once set: the chip reads "Set Home" until the label has a stop, then "Home". */
    data class Label(val label: StopLabel) : HomeChip {
        val text: String get() = if (label.isSet) label.label else "Set ${label.label}"
    }

    /** The way into the Park & Ride picker. It never changes. */
    data object ParkRide : HomeChip
}

/**
 * The chips for [stopLabels], in order: Home, Work, then Park & Ride. A label with no row (Work was
 * deleted in Manage) still gets its chip, as unset, so setting it is one tap away.
 *
 * Nothing until the labels have loaded, so the row appears whole and never grows a chip later.
 */
internal fun homeChipsFor(stopLabels: List<StopLabel>): ImmutableList<HomeChip> {
    if (stopLabels.isEmpty()) return persistentListOf()
    val labelChips = StopLabel.defaults.map { default ->
        HomeChip.Label(homeLabelFor(default.label, stopLabels) ?: default)
    }
    return (labelChips + HomeChip.ParkRide).toImmutableList()
}

/** The row for [labelKey] when the rider has one, else the built in Home or Work, else null. */
internal fun homeLabelFor(labelKey: String, stopLabels: List<StopLabel>): StopLabel? =
    stopLabels.firstOrNull { it.label.equals(labelKey, ignoreCase = true) }
        ?: StopLabel.defaults.firstOrNull { it.label.equals(labelKey, ignoreCase = true) }

/**
 * What a tap on [label]'s chip starts. A label with a stop needs one location fix before it can
 * plan, and one without a stop has nothing to plan to, so it opens the stop search to be set.
 */
internal fun startLabelChipTap(label: StopLabel): LabelChipTap =
    if (label.isSet) {
        LabelChipTap.Locating(label.label)
    } else {
        LabelChipTap.Decided(LabelTapOutcome.Assign(label.label))
    }

/**
 * The stop in [candidates] nearest to ([latitude], [longitude]) and no further than [radiusKm], or
 * null. Equal distances go to the smaller stop id, so the same rider position always gives the same
 * stop. The edge is inclusive, and a stop whose distance cannot be worked out is skipped.
 */
internal fun nearestStopTo(
    latitude: Double,
    longitude: Double,
    candidates: List<NearbyStop>,
    radiusKm: Double,
): NearbyStop? = candidates
    .mapNotNull { stop ->
        val distanceKm = GeoUtils.haversineDistance(
            lat1 = latitude,
            lon1 = longitude,
            lat2 = stop.latitude,
            lon2 = stop.longitude,
        ) ?: return@mapNotNull null
        stop to distanceKm
    }
    .filter { (_, distanceKm) -> distanceKm <= radiusKm }
    .minWithOrNull(compareBy<Pair<NearbyStop, Double>> { it.second }.thenBy { it.first.stopId })
    ?.first

/**
 * What a tap on a set chip does once the one location fix is known.
 *
 * Plan from the stop nearest the rider to [destination] when there is a [rider] position and a
 * stop within [radiusKm] of it. Everything else falls back to the search flow with [destination]
 * as the destination and From left empty: no permission, no fix, no stop near, or the nearest stop
 * is the destination itself, because a trip from a stop to itself is not a trip.
 */
internal fun decideLabelTap(
    destination: StopItem,
    rider: LatLng?,
    candidates: List<NearbyStop>,
    radiusKm: Double = LABEL_TAP_RADIUS_KM,
): LabelTapOutcome {
    val nearest = rider?.let { nearestStopTo(it.latitude, it.longitude, candidates, radiusKm) }
    return if (nearest == null || nearest.stopId == destination.stopId) {
        LabelTapOutcome.Search(to = destination)
    } else {
        LabelTapOutcome.Plan(
            from = StopItem(stopId = nearest.stopId, stopName = nearest.stopName),
            to = destination,
        )
    }
}
