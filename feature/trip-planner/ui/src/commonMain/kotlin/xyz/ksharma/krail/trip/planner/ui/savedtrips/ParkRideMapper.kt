package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.Companion.PACKAGE_PRIVATE
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toSimple12HourTime
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.park.ride.network.model.CarParkFacilityDetailResponse
import xyz.ksharma.krail.sandook.NSWParkRideFacilityDetail
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState.ParkRideFacilityDetail

/**
 * Converts a [CarParkFacilityDetailResponse] to a [NSWParkRideFacilityDetail] for database storage.
 *
 * This method calculates the number of available spots, total spots, and percentage full
 * for a Park&Ride facility using the occupancy data from all zones.
 *
 * Occupied spots are determined by summing the `transients` field from each zone's occupancy.
 * If `transients` is `null` or missing, it is treated as 0.
 *
 * @return [NSWParkRideFacilityDetail] containing facility details for database storage
 **/
@VisibleForTesting(otherwise = PACKAGE_PRIVATE)
fun CarParkFacilityDetailResponse.toNSWParkRideFacilityDetail(stopName: String): NSWParkRideFacilityDetail {
    val totalSpots = spots.toIntOrNull() ?: 0
    val occupiedSpots = zones.sumOf {
        it.occupancy.total?.toIntOrNull() ?: it.occupancy.transients?.toIntOrNull() ?: 0
    }
    val spotsAvailable = totalSpots - occupiedSpots
    val percentageFull = if (totalSpots > 0) (occupiedSpots * 100) / totalSpots else 0
    val timeText = messageDate.toSimple12HourTime().replace(" ", "\u00A0")

    log(
        "[$facilityName - $facilityId - $tsn] " +
                "Total spots: $totalSpots, Occupied spots: $occupiedSpots, " +
                "Spots available: $spotsAvailable, Percentage full: $percentageFull%, time: $timeText"
    )

    return NSWParkRideFacilityDetail(
        facilityId = facilityId,
        spotsAvailable = spotsAvailable.toLong(),
        totalSpots = totalSpots.toLong(),
        facilityName = facilityName.toDisplayFacilityName(),
        percentageFull = percentageFull.toLong(),
        stopId = tsn,
        timeText = timeText,
        suburb = location.suburb ?: "",
        address = location.address ?: "",
        latitude = location.latitude?.toDoubleOrNull() ?: 0.0,
        longitude = location.longitude?.toDoubleOrNull() ?: 0.0,
        stopName = stopName,
        timestamp = Clock.System.now().epochSeconds,
    )
}

internal fun NSWParkRideFacilityDetail.toParkRideState(): ParkRideFacilityDetail =
    ParkRideFacilityDetail(
        spotsAvailable = spotsAvailable.toInt(),
        totalSpots = totalSpots.toInt(),
        facilityName = facilityName,
        percentageFull = percentageFull.toInt(),
        timeText = timeText,
        stopId = stopId,
        facilityId = facilityId,
    )

/**
 * Maps a list of [NSWParkRideFacilityDetail] entities to a list of [ParkRideUiState]s for UI display.
 *
 * Each [ParkRideUiState] represents a stop and contains unique facilities for that stop.
 * Each facility (by `facilityId`) appears only once across all stops (first occurrence is used).
 *
 * @receiver List of [NSWParkRideFacilityDetail] entities, possibly with duplicate facilities across stops.
 * @return List of [ParkRideUiState], one per stop, each with unique facilities.
 */
fun List<NSWParkRideFacilityDetail>.toParkRideUiState(): List<ParkRideUiState> {
    val seenFacilityIds = mutableSetOf<String>()
    return groupBy { it.stopId }
        .map { (stopId, facilitiesForStop) ->
            val stopName = facilitiesForStop.firstOrNull()?.stopName ?: ""

            val uniqueFacilities = facilitiesForStop
                .filter { seenFacilityIds.add(it.facilityId) }
                .map {
                    ParkRideFacilityDetail(
                        spotsAvailable = it.spotsAvailable.toInt(),
                        totalSpots = it.totalSpots.toInt(),
                        facilityName = it.facilityName,
                        percentageFull = it.percentageFull.toInt(),
                        stopId = it.stopId,
                        timeText = it.timeText,
                        facilityId = it.facilityId,
                    )
                }
                .toImmutableSet()

            if (uniqueFacilities.isNotEmpty()) {
                ParkRideUiState(
                    stopId = stopId,
                    stopName = stopName,
                    facilities = uniqueFacilities,
                    error = null,
                    isLoading = false,
                )
            } else null
        }
        .filterNotNull()
}

internal fun String.toDisplayFacilityName(): String = removePrefix("Park&Ride - ").trim()
