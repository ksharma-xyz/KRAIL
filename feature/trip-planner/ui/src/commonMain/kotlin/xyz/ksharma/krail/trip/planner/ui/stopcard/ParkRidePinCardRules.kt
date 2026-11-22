package xyz.ksharma.krail.trip.planner.ui.stopcard

import kotlinx.collections.immutable.toImmutableSet
import xyz.ksharma.krail.trip.planner.ui.savedtrips.ParkRideTile
import xyz.ksharma.krail.trip.planner.ui.savedtrips.toTile
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState.ParkRideFacilityDetail

private const val UNREAD_COUNT = -1

/**
 * The Park & Ride pin card's content for a tapped station, from the picker's station and the
 * readings the local store holds for it.
 *
 * Every car park of the station is listed, whether or not it has a reading yet (assumption A2b),
 * stacked at equal weight in the fixed order the home tile uses, by name and then id, so a number
 * landing never moves a block. A car park with no reading shows its name alone and never an error.
 * Directions go to each car park's own position and to nothing else (decision G3): a car park whose
 * reading carried none has no Directions, and the station's position is never borrowed.
 *
 * It is the same [ParkRideTile] the home row draws, so the numbers, the total and the time read the
 * same on both surfaces.
 */
internal fun ParkRideStationPickerItem.toPinCardTile(readings: Collection<ParkRideFacilityDetail>): ParkRideTile {
    val byFacility = readings.associateBy { it.facilityId }
    val carParks = mappings
        .distinctBy { it.facilityId }
        .map { mapping ->
            byFacility[mapping.facilityId] ?: ParkRideFacilityDetail(
                spotsAvailable = UNREAD_COUNT,
                totalSpots = UNREAD_COUNT,
                facilityName = mapping.facilityName,
                percentageFull = UNREAD_COUNT,
                stopId = mapping.stopId,
                timeText = "",
                facilityId = mapping.facilityId,
            )
        }
    return ParkRideUiState(
        stopId = stationId,
        stopName = stationName,
        facilities = carParks.toImmutableSet(),
    ).toTile()
}

/**
 * What the card's one action reads for a station: the words of rule 38, "Remove from home" when the
 * station is on home and "Add to home" when it is not.
 */
internal fun parkRidePinActionLabel(isOnHome: Boolean): String =
    if (isOnHome) "Remove from home" else "Add to home"
