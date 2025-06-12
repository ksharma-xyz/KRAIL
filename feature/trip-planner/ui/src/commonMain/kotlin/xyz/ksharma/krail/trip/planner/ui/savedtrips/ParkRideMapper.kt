package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.Companion.PACKAGE_PRIVATE
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.park.ride.network.model.CarParkFacilityDetailResponse
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
fun CarParkFacilityDetailResponse.toParkRideState(): ParkRideState {
    val totalSpots = spots.toIntOrNull() ?: 0

    // Sum occupied spots from all zones (using loop sensor)
    val occupiedSpots = zones.sumOf { it.occupancy.transients?.toIntOrNull() ?: 0 }

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

    return ParkRideState(
        spotsAvailable = spotsAvailable,
        totalSpots = totalSpots,
        facilityName = facilityName,
        percentageFull = percentFull,
        stopId = tsn,
    )
}
