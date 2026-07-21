package xyz.ksharma.krail.trip.planner.ui.parkride

import xyz.ksharma.krail.park.ride.network.model.NswParkRideFacility
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideMapping
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.StationPosition

/**
 * Collapses the raw Remote Config facility list into one entry per station.
 *
 * The NSW catalogue collides in both directions, and either shape produces duplicate rows if
 * the list is keyed on a single ID:
 *
 * - one stop, several car parks — Tallawong (`2155384`) has P1, P2 and P3; likewise Penrith,
 *   Campbelltown and Kellyville
 * - one car park, several stops — Mona Vale (facility `12`) is reachable from `210318` and
 *   `2103108`; likewise Sutherland, Hornsby and Manly Vale
 *
 * Grouping by `stopId` alone fixes the first and duplicates the second; grouping by
 * `facilityId` alone does the reverse. So entries are treated as edges in a bipartite graph
 * of stops and facilities, and each connected component becomes one station.
 *
 * Extracted to its own file so neither this nor `AddParkRideViewModel` grows past detekt's
 * per-file function limit.
 */
internal fun List<NswParkRideFacility>.groupIntoStations(
    stopNameLookup: (stopId: String) -> String?,
    positionLookup: (stopIds: List<String>) -> Map<String, StationPosition> = { emptyMap() },
): List<ParkRideStationPickerItem> {
    val unionFind = UnionFind()

    forEach { facility ->
        unionFind.union(stopNode(facility.stopId), facilityNode(facility.parkRideFacilityId))
    }

    // One batched lookup for the whole catalogue rather than a query per station.
    val positions = positionLookup(map { it.stopId }.distinct())

    return groupBy { unionFind.find(stopNode(it.stopId)) }
        .map { (_, facilities) -> facilities.toStation(stopNameLookup, positions) }
        .sortedBy { it.stationName.lowercase() }
}

private fun List<NswParkRideFacility>.toStation(
    stopNameLookup: (stopId: String) -> String?,
    positions: Map<String, StationPosition>,
): ParkRideStationPickerItem {
    val mappings = map { facility ->
        ParkRideMapping(
            stopId = facility.stopId,
            facilityId = facility.parkRideFacilityId,
            facilityName = facility.parkRideName.toDisplayName(),
        )
    }
    // Smallest stop ID keeps the identity stable regardless of Remote Config ordering.
    val stationId = minOf { it.stopId }
    val carParkNames = mappings.map { it.facilityName }.distinct()

    return ParkRideStationPickerItem(
        stationId = stationId,
        stationName = commonStationName(carParkNames),
        stopName = firstNotNullOfOrNull { stopNameLookup(it.stopId) }.orEmpty(),
        carParkNames = carParkNames,
        mappings = mappings,
        isUserAdded = false,
        isFromSavedTrip = false,
        // Any stop in the group works: they are platforms of the same station, metres apart.
        position = firstNotNullOfOrNull { positions[it.stopId] },
    )
}

/**
 * The name a rider would call the station, derived from its car park names.
 *
 * "Tallawong P1/P2/P3" becomes "Tallawong" and "Penrith (at-grade)/(multi-level)" becomes
 * "Penrith". The common prefix alone is not enough — it can stop mid-word ("Tallawong P") —
 * so a trailing partial word is dropped.
 */
internal fun commonStationName(names: List<String>): String {
    if (names.size == 1) return names.first()

    val prefix = names.reduce { acc, name -> acc.commonPrefixWith(name, ignoreCase = true) }
        .trim()
        .trimEnd { !it.isLetterOrDigit() }
    if (prefix.isEmpty()) return names.first()

    val endsAtWordBoundary = names.all { name ->
        name.length == prefix.length || !name[prefix.length].isLetterOrDigit()
    }
    val name = if (endsAtWordBoundary) prefix else prefix.substringBeforeLast(' ', prefix)

    return name.trim().ifEmpty { names.first() }
}

private fun String.toDisplayName(): String = removePrefix("Park&Ride - ").trim()

private fun stopNode(stopId: String) = "stop:$stopId"

private fun facilityNode(facilityId: String) = "facility:$facilityId"

/** Minimal union-find over string nodes; the catalogue is a few dozen entries. */
private class UnionFind {
    private val parent = mutableMapOf<String, String>()

    fun find(node: String): String {
        var root = parent.getOrPut(node) { node }
        while (root != parent.getValue(root)) {
            root = parent.getValue(root)
        }
        // Path compression keeps repeated lookups flat.
        var current = node
        while (current != root) {
            val next = parent.getValue(current)
            parent[current] = root
            current = next
        }
        return root
    }

    fun union(a: String, b: String) {
        val rootA = find(a)
        val rootB = find(b)
        if (rootA != rootB) parent[rootA] = rootB
    }
}
