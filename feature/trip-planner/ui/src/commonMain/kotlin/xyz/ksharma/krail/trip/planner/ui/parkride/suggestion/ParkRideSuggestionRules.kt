package xyz.ksharma.krail.trip.planner.ui.parkride.suggestion

import kotlinx.collections.immutable.toImmutableSet
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.sandook.db.NSWParkRideFacilityDetail
import xyz.ksharma.krail.trip.planner.ui.intro.nearestParkRideStation
import xyz.ksharma.krail.trip.planner.ui.parkride.savedStopName
import xyz.ksharma.krail.trip.planner.ui.savedtrips.ParkRideTile
import xyz.ksharma.krail.trip.planner.ui.savedtrips.directionsTarget
import xyz.ksharma.krail.trip.planner.ui.savedtrips.toParkRideState
import xyz.ksharma.krail.trip.planner.ui.savedtrips.toTile
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState.ParkRideFacilityDetail
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel

// The one-time Park & Ride suggestion on the map home, `docs/MAP_HOME_UX.md` rule 47 and assumption A10
// in `docs/features/map-home/ERD.md`. Everything that decides something lives here as a pure function, so
// each rule is driven at its boundary without a layout, a location fix or a database.

/** The right button of the card. The primary action, so it reads as one. */
internal const val ADD_TO_HOME_TEXT = "Add to home"

/** The left button of the card. It only dismisses. */
internal const val NOT_NOW_TEXT = "Not now"

private const val SPOKEN_CARD_NAME = "Park & Ride suggestion"

/**
 * Whether the suggestion can be offered at all, before any station is looked for.
 *
 * It needs the map home (the card is part of it, so a build with the maps flag off keeps today's home),
 * a rider who has not been asked yet (the one boolean the Intro also writes, so a rider the Intro asked
 * is never asked again on home and a rider who has seen the card never sees it twice), and no saved
 * station (a rider who already has one does not need the first one suggested).
 */
internal fun canSuggest(mapHomeOn: Boolean, alreadyAsked: Boolean, hasSavedStation: Boolean): Boolean =
    mapHomeOn && !alreadyAsked && !hasSavedStation

/**
 * Where the nearest station is measured from: Home when the rider has one with a position, else the
 * rider's current fix, else nowhere. A rider whose Home is set is asked about the stations near their
 * home and not near wherever they happen to be standing, so a set Home with no station near gives no
 * suggestion even when the rider is at a station.
 */
internal fun suggestionAnchor(home: LatLng?, currentFix: LatLng?): LatLng? = home ?: currentFix

/**
 * The station to suggest: the nearest to [anchor] within [radiusKm], or null. Stations are tried in
 * name order, so two at one distance (the same stop listed twice under different names) always give
 * the same one. A station with no coordinates is never offered, and no anchor means no suggestion.
 */
internal fun stationToSuggest(
    anchor: LatLng?,
    stations: List<ParkRideStationPickerItem>,
    radiusKm: Double,
): ParkRideStationPickerItem? {
    if (anchor == null) return null
    val byName = stations.sortedWith(compareBy({ it.stationName.lowercase() }, { it.stationId }))
    return nearestParkRideStation(location = anchor, stations = byName, radiusKm = radiusKm)
}

/** The stop the rider's Home label holds, or null while Home has none. */
internal fun homeStopId(labels: List<StopLabel>): String? =
    labels.firstOrNull { it.isProtected && it.isSet }?.stopId

/**
 * Where Home is, from the coordinates of its stop, or null when Home has no stop, the stop is not in
 * the local table, or its position is not a real place (the table stores 0.0 and 0.0 for none).
 */
internal fun homePosition(labels: List<StopLabel>, coordinates: Map<String, Pair<Double, Double>>): LatLng? =
    homeStopId(labels)
        ?.let { stopId -> coordinates[stopId] }
        ?.let { (latitude, longitude) -> directionsTarget(latitude, longitude) }

/**
 * The tile the card draws for [station], from the readings already stored on the phone.
 *
 * A car park with no stored reading shows no number, so the card is the station's name alone at the
 * same height (`docs/MAP_HOME_UX.md` rule 22), and nothing is fetched to fill it. The name is the one the
 * add path stores, the stop's own name before the catalogue's, so the card and the tile it becomes read
 * alike. Every car park of the station is listed once, whichever stops it sits behind.
 */
internal fun suggestionTile(
    station: ParkRideStationPickerItem,
    cachedReadings: List<NSWParkRideFacilityDetail>,
): ParkRideTile {
    val readings = cachedReadings.associateBy { it.facilityId }
    val carParks = station.mappings.distinctBy { it.facilityId }.map { mapping ->
        readings[mapping.facilityId]?.toParkRideState() ?: ParkRideFacilityDetail(
            spotsAvailable = NO_READING,
            totalSpots = NO_READING,
            facilityName = mapping.facilityName,
            percentageFull = NO_READING,
            stopId = mapping.stopId,
            timeText = "",
            facilityId = mapping.facilityId,
        )
    }
    return ParkRideUiState(
        stopId = station.stationId,
        stopName = station.savedStopName(),
        facilities = carParks.toImmutableSet(),
    ).toTile()
}

// The sentinel a car park that was never fetched is stored with, which is never a real count.
private const val NO_READING = -1

/**
 * What a screen reader says for the card: its name, the station, the free spots when there is a number
 * and the time of the reading when there is one. Commas only, and the same numbers as the face.
 */
internal fun suggestionSpokenLabel(tile: ParkRideTile): String = buildList {
    add(SPOKEN_CARD_NAME)
    add(tile.stationName)
    tile.totalFreeSpots?.let { spots -> add(if (spots == 1) "1 spot" else "$spots spots") }
    tile.timeText?.let { time -> add("last updated at $time") }
}.joinToString(separator = ", ")

/** What a screen reader says for the primary button, so it names the station it adds. */
internal fun addToHomeSpokenLabel(tile: ParkRideTile): String = "Add ${tile.stationName} to home"
