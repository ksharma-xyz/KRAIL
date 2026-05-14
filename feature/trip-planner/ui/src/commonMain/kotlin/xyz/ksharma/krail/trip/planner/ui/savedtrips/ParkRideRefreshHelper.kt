package xyz.ksharma.krail.trip.planner.ui.savedtrips

import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.park.ride.network.model.CarParkFacilityDetailResponse
import xyz.ksharma.krail.park.ride.network.model.ParkingStopBatchResponse
import xyz.ksharma.krail.park.ride.network.service.ParkRideService
import xyz.ksharma.krail.sandook.NswParkRideSandook
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Pure helpers for the SavedTrips Park&Ride refresh flow. Extracted out of
 * [SavedTripsViewModel] to keep the ViewModel under detekt's `LargeClass`
 * threshold and to make the BFF-batch vs NSW-fan-out branching independently
 * readable.
 *
 * These functions intentionally take their collaborators as parameters
 * rather than reading them from a holder object, so the unit-testable
 * surface stays a plain function call.
 */

/**
 * Returns the subset of [facilityIds] that are off-cooldown and therefore
 * eligible to be refreshed now. Cooldown timestamps are read from
 * [parkRideSandook]. Anything still on cooldown is logged and skipped.
 */
@OptIn(ExperimentalTime::class)
internal suspend fun filterRefreshableFacilities(
    parkRideSandook: NswParkRideSandook,
    facilityIds: Set<String>,
    cooldown: Duration,
    nowInstant: Instant,
): List<String> {
    val refreshable = mutableListOf<String>()
    facilityIds.forEach { facilityId ->
        val lastCallEpoch = parkRideSandook.getLastApiCallTimestamp(facilityId)
            ?: Instant.DISTANT_PAST.epochSeconds
        val lastCall = Instant.fromEpochSeconds(lastCallEpoch)
        if (nowInstant - lastCall >= cooldown) {
            refreshable += facilityId
        } else {
            val timeLeft = cooldown - (nowInstant - lastCall)
            log("Facility $facilityId is on cooldown for another ${timeLeft.inWholeSeconds} seconds")
        }
    }
    return refreshable
}

/**
 * Applies a successful BFF batch response to the local store. Facilities the
 * batch did not return are reset to `DISTANT_PAST` so the cooldown does not
 * suppress a retry.
 */
@OptIn(ExperimentalTime::class)
internal suspend fun applyBatchResults(
    parkRideSandook: NswParkRideSandook,
    stopId: String,
    refreshable: List<String>,
    batched: ParkingStopBatchResponse,
    now: Long,
) {
    // Per the BFF handover doc, partial failures live on `stops[id].errors`
    // and unmapped stops on `unknownStops`. Just log them; surfacing
    // partial-failure UI is out of scope for this change.
    if (batched.unknownStops.isNotEmpty()) {
        log("park-ride batch unknownStops: ${batched.unknownStops}")
    }
    batched.stops.forEach { (_, stopFacilities) ->
        if (stopFacilities.errors.isNotEmpty()) {
            log("park-ride batch errors: ${stopFacilities.errors}")
        }
    }

    // Flatten facilities across the (single) requested stop. The home card
    // UI groups by facility ID, not by the BFF's stop key.
    val byFacilityId: Map<String, CarParkFacilityDetailResponse> =
        batched.stops.values.fold(emptyMap()) { acc, stopFacilities ->
            acc + stopFacilities.facilities
        }

    refreshable.forEach { facilityId ->
        val apiResult = byFacilityId[facilityId]
        if (apiResult != null) {
            persistFacilityDetail(parkRideSandook, facilityId, stopId, now, apiResult)
        } else {
            // Reset timestamp to DISTANT_PAST so we can retry sooner than the
            // cooldown would otherwise allow.
            parkRideSandook.updateApiCallTimestamp(facilityId, Instant.DISTANT_PAST.epochSeconds)
            logError("Batch result missing facility $facilityId for stop $stopId")
        }
    }
}

/**
 * Existing NSW per-facility fan-out, used when the BFF override is off and
 * [ParkRideService.fetchAvailabilityForStops] returns `null`. Identical to
 * the previous in-place implementation; extracted unchanged.
 */
@OptIn(ExperimentalTime::class)
internal suspend fun fanOutPerFacility(
    parkRideSandook: NswParkRideSandook,
    parkRideService: ParkRideService,
    stopId: String,
    refreshable: List<String>,
    now: Long,
) {
    refreshable.forEach { facilityId ->
        log("Fetching facility $facilityId for stop $stopId")
        val apiResult = parkRideService.fetchCarParkFacilities(facilityId).getOrNull()
        if (apiResult != null) {
            persistFacilityDetail(parkRideSandook, facilityId, stopId, now, apiResult)
        } else {
            // reset timestamp to DISTANT_PAST as API call failed, so we can retry
            // earlier than cooldown would end.
            parkRideSandook.updateApiCallTimestamp(facilityId, Instant.DISTANT_PAST.epochSeconds)
            logError("API call failed for facility $facilityId")
        }
    }
}

@OptIn(ExperimentalTime::class)
private suspend fun persistFacilityDetail(
    parkRideSandook: NswParkRideSandook,
    facilityId: String,
    stopId: String,
    nowEpoch: Long,
    apiResult: CarParkFacilityDetailResponse,
) {
    parkRideSandook.updateApiCallTimestamp(facilityId, nowEpoch)

    // Get the stop name from the saved park ride mapping for this facility
    val stopName =
        parkRideSandook.getSavedParkRideByFacilityId(facilityId)?.stopName
            ?: stopId // Fall back to stopId if mapping is somehow missing

    val detail = apiResult.toNSWParkRideFacilityDetail(
        stopName = stopName,
        stopId = stopId,
    )

    parkRideSandook.insertOrReplaceAll(listOf(detail))
    log("Fetched and saved facility $facilityId for stop $stopId - $detail")
}
