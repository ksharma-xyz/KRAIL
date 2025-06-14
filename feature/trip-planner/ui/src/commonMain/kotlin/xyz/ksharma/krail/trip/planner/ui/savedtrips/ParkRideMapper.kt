package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.Companion.PACKAGE_PRIVATE
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toSimple12HourTime
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.park.ride.network.model.CarParkFacilityDetailResponse
import xyz.ksharma.krail.sandook.NSWParkRide
import xyz.ksharma.krail.trip.planner.ui.state.parkride.ParkRideState

/**
 * Converts a [CarParkFacilityDetailResponse] to a [ParkRideState] for UI display.
 *
 * This method calculates the number of available spots, total spots, and percentage full
 * for a Park&Ride facility using the occupancy data from all zones.
 *
 * Occupied spots are determined by summing the `transients` field from each zone's occupancy.
 * If `transients` is `null` or missing, it is treated as 0.
 *
 * @return [ParkRideState] containing available spots, total spots, facility name, percentage
 * full, and stop ID
 **/
@VisibleForTesting(otherwise = PACKAGE_PRIVATE)
fun CarParkFacilityDetailResponse.toParkRideState(
    displayParkRideIcon: Boolean = true,
): ParkRideState {
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

    val time = messageDate.toSimple12HourTime().replace(" ", "\u00A0") // Non-breaking space for better display

    return ParkRideState(
        spotsAvailable = spotsAvailable,
        totalSpots = totalSpots,
        facilityName = facilityName.removePrefix("Park&Ride - ").trim(),
        percentageFull = percentFull,
        stopId = tsn,
        timeText = time,
        displayParkRideIcon = displayParkRideIcon,
    )
}

fun List<CarParkFacilityDetailResponse>.toParkRideStates(): List<ParkRideState> {
    val seenStopIds = mutableSetOf<String>()
    return this.map { response ->
        val isFirst = seenStopIds.add(response.tsn)
        response.toParkRideState(displayParkRideIcon = isFirst)
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

    log("[$facilityName - $facilityId - $tsn] " +
        "Total spots: $totalSpots, Occupied spots: $occupiedSpots, " +
        "Spots available: $spotsAvailable, Percentage full: $percentageFull%")

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
