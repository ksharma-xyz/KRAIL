package xyz.ksharma.krail.trip.planner.ui.savedtrips

import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.park.ride.network.NswParkRideFacilityManager
import xyz.ksharma.krail.park.ride.network.model.NswParkRideFacility
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource.SavedTrips
import xyz.ksharma.krail.sandook.db.SavedParkRide
import xyz.ksharma.krail.sandook.db.SavedTrip
import xyz.ksharma.krail.trip.planner.ui.searchstop.StopResultsManager

/*
 * The Park & Ride rows a saved trip holds, kept in step with the trips. Moved out of
 * SavedTripsViewModel unchanged, so the ViewModel stays under detekt's LargeClass limit.
 */

/**
 * Handles the logic for updating Park & Ride stop IDs in the database based on saved trips.
 * Updates the database with (stopId, facilityId) pairs for all saved trips.
 *
 * For each unique stop ID found in the list of saved trips (from both `fromStopId` and `toStopId`),
 * this method finds all associated Park & Ride facility IDs using the facility manager.
 *
 * Only stop IDs that have at least one facility are included.
 * The resulting set of (stopId, facilityId) pairs is then inserted or replaced in the database.
 *
 * In short:
 *      StopId -> FacilityId Mapping for StopIds in SavedTrips
 *
 * edge case solved, where multiple stop Ids have same facility Ids.
 * {"stopId":"209325","parkRideFacilityId":"489","parkRideName":"Park&Ride - Manly Vale"},
 * {"stopId":"209324","parkRideFacilityId":"489","parkRideName":"Park&Ride - Manly Vale"},
 *
 * It sends no analytics. These rows follow the saved trips and no rider asked for any of them, so they are not
 * counted as `park_ride_user_facility`.
 *
 * @param savedTrips List of saved trips to extract stop IDs from.
 */
internal suspend fun syncSavedTripParkRides(
    savedTrips: List<SavedTrip>,
    facilityManager: NswParkRideFacilityManager,
    stopResultsManager: StopResultsManager,
    parkRideSandook: NswParkRideSandook,
) {
    // 1. Collect all unique stop IDs from the saved trips (both fromStopId and toStopId)
    val uniqueStopIds: Set<String> = uniqueSavedTripStopIds(savedTrips)
    log("Unique Stop IDs from Saved Trips: $uniqueStopIds")

    // 2. Build a map from stopId to a list of associated park & ride facilities
    val facilityDetailMap: Map<String, List<NswParkRideFacility>> = facilityManager
        .getParkRideFacilities()
        .groupBy { it.stopId }
    log("Facility Detail Map: $facilityDetailMap")

    // 3. For each unique stopId, create SavedParkRide objects with all required data
    val savedParkRideList: Set<SavedParkRide> = uniqueStopIds
        .flatMap { stopId ->
            // Try to resolve stopName for each stopId in priority order:
            // 1. Use local DB (should always succeed for saved trips).
            // 2. If not found, use Park & Ride facility name from facility mapping - remote config.
            //    This can happen if the stopId in the saved trip is different from the one in
            //    the park ride api call response. In that case, the remote config mapping
            //    will provide the correct facility name for those stopId's.
            // 3. If still not found, fallback to stopId itself (should rarely happen).
            val stopName = stopResultsManager.fetchLocalStopName(stopId)
                ?: facilityDetailMap[stopId]?.firstOrNull()?.parkRideName
                ?: stopId

            facilityDetailMap[stopId]?.map { facility ->
                SavedParkRide(
                    stopId = stopId,
                    facilityId = facility.parkRideFacilityId,
                    stopName = stopName,
                    facilityName = facility.parkRideName,
                    source = SavedTrips.value,
                )
            } ?: emptyList()
        }
        .toSet()

    savedParkRideList.map {
        it.stopId to it.facilityId + " (${it.stopName})"
    }.let {
        log("StopId -> FacilityId Mapping: \n${it.joinToString("\n")}")
    }

    // Clear all existing saved park rides linked to saved trips for accuracy
    parkRideSandook.clearAllSavedParkRidesBySource(source = SavedTrips)

    if (savedParkRideList.isNotEmpty()) {
        parkRideSandook.insertOrReplaceSavedParkRides(
            parkRideInfoList = savedParkRideList,
            source = SavedTrips,
        )
    }
}

private fun uniqueSavedTripStopIds(savedTrips: List<SavedTrip>): Set<String> {
    val uniqueSavedTripStopIds: Set<String> = savedTrips
        .flatMap { listOf(it.fromStopId, it.toStopId) }
        .toSet()
    return uniqueSavedTripStopIds
}
