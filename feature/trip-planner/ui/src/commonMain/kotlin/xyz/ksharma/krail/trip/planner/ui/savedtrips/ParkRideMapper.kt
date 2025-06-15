package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.Companion.PACKAGE_PRIVATE
import kotlinx.collections.immutable.toPersistentList
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toSimple12HourTime
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.park.ride.network.model.CarParkFacilityDetailResponse
import xyz.ksharma.krail.sandook.NSWParkRide
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState.ParkRideFacilityDetail

/**
 * Converts a [CarParkFacilityDetailResponse] to a [ParkRideFacilityDetail] for UI display.
 *
 * This method calculates the number of available spots, total spots, and percentage full
 * for a Park&Ride facility using the occupancy data from all zones.
 *
 * Occupied spots are determined by summing the `transients` field from each zone's occupancy.
 * If `transients` is `null` or missing, it is treated as 0.
 *
 * @return [ParkRideFacilityDetail] containing available spots, total spots, facility name, percentage
 * full, and stop ID
 **/
@VisibleForTesting(otherwise = PACKAGE_PRIVATE)
fun CarParkFacilityDetailResponse.toParkRideState(
): ParkRideFacilityDetail {
    val totalSpots = spots.toIntOrNull() ?: 0

    // Sum occupied spots from all zones (using loop sensor)
    val occupiedSpots = zones.sumOf {
        it.occupancy.total?.toIntOrNull() ?: it.occupancy.transients?.toIntOrNull() ?: 0
    }

    val spotsAvailable = totalSpots - occupiedSpots
    val percentFull = if (totalSpots > 0) {
        (occupiedSpots * 100) / totalSpots
    } else {
        0
    }

    log(
        "[$facilityName - $facilityId] \nTotal spots: $totalSpots, " +
                "Occupied spots: $occupiedSpots, Spots available: $spotsAvailable, " +
                "Percentage full: $percentFull%"
    )

    val time = messageDate.toSimple12HourTime()
        .replace(" ", "\u00A0") // Non-breaking space for better display

    return ParkRideFacilityDetail(
        spotsAvailable = spotsAvailable,
        totalSpots = totalSpots,
        facilityName = facilityName.removePrefix("Park&Ride - ").trim(),
        percentageFull = percentFull,
        stopId = tsn,
        timeText = time,
    )
}

fun List<CarParkFacilityDetailResponse>.toParkRideStates(): List<ParkRideFacilityDetail> {
    val seenStopIds = mutableSetOf<String>()
    return this.map { response ->
        val isFirst = seenStopIds.add(response.tsn)
        response.toParkRideState()
    }
}

/**
 * Converts a [CarParkFacilityDetailResponse] to a [NSWParkRide] for database storage.
 *
 * This method calculates the number of available spots, total spots, and percentage full
 * for a Park&Ride facility using the occupancy data from all zones.
 *
 * Occupied spots are determined by summing the `transients` field from each zone's occupancy.
 * If `transients` is `null` or missing, it is treated as 0.
 *
 * @return [NSWParkRide] containing facility details for database storage
 **/
@VisibleForTesting(otherwise = PACKAGE_PRIVATE)
fun CarParkFacilityDetailResponse.toDbNSWParkRide(): NSWParkRide {
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
                "Spots available: $spotsAvailable, Percentage full: $percentageFull%"
    )

    return NSWParkRide(
        facilityId = facilityId,
        spotsAvailable = spotsAvailable.toLong(),
        totalSpots = totalSpots.toLong(),
        facilityName = facilityName.removePrefix("Park&Ride - ").trim(),
        percentageFull = percentageFull.toLong(),
        stopId = tsn,
        timeText = timeText,
        suburb = location.suburb ?: "",
        address = location.address ?: "",
        latitude = location.latitude?.toDoubleOrNull() ?: 0.0,
        longitude = location.longitude?.toDoubleOrNull() ?: 0.0,
    )
}

internal fun NSWParkRide.toParkRideState(): ParkRideFacilityDetail =
    ParkRideFacilityDetail(
        spotsAvailable = spotsAvailable.toInt(),
        totalSpots = totalSpots.toInt(),
        facilityName = facilityName,
        percentageFull = percentageFull.toInt(),
        timeText = timeText,
        stopId = stopId,
    )

/**
 * Maps a list of [NSWParkRide] entities to a [ParkRideUiState] for UI display.
 *
 * Ensures that each facility (by `facilityId`) appears only once in the resulting UI state,
 * even if it is associated with multiple stops. Only the first occurrence of each facility is included.
 *
 * The returned [ParkRideUiState] uses the `stopId` and `suburb` from the first item in the list
 * as the main stop information. All unique facilities are mapped to [ParkRideFacilityDetail]s.
 *
 * @receiver List of [NSWParkRide] entities, possibly with duplicate facilities across stops.
 * @return [ParkRideUiState] containing unique facilities for display.
 */
fun List<NSWParkRide>.toParkRideUiState(): ParkRideUiState {
    val seenFacilityIds = mutableSetOf<String>()
    // Only keep first occurrence of each facilityId
    val facilities = filter { seenFacilityIds.add(it.facilityId) }
        .map {
            ParkRideFacilityDetail(
                spotsAvailable = it.spotsAvailable.toInt(),
                totalSpots = it.totalSpots.toInt(),
                facilityName = it.facilityName,
                percentageFull = it.percentageFull.toInt(),
                stopId = it.stopId,
                timeText = it.timeText
            )
        }
        .toPersistentList()

    // Use the first stop's info for stopId and stopName (adjust as needed)
    return ParkRideUiState(
        stopId = firstOrNull()?.stopId.orEmpty(),
        stopName = firstOrNull()?.suburb.orEmpty(),
        facilities = facilities
    )
}
