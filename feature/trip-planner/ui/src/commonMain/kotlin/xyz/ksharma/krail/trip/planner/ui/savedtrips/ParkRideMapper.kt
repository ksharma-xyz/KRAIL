package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.Companion.PACKAGE_PRIVATE
import kotlinx.collections.immutable.toImmutableSet
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toSimple12HourTime
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.park.ride.network.model.CarParkFacilityDetailResponse
import xyz.ksharma.krail.sandook.db.NSWParkRideFacilityDetail
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState.ParkRideFacilityDetail
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val FULL_PERCENT = 100

/**
 * Caps a real occupancy reading at [max] while leaving the negative loading sentinel alone.
 *
 * [FULL_PERCENT] is the cap everywhere: a full car park is 100%, the feed can report more
 * than that, and the UI must never repeat it.
 *
 * A card that has never had data carries -1 for every number, which the UI reads as
 * "still loading". Clamping into a 0..100 range would turn that sentinel into a real
 * looking 0% and show an empty car park where there is simply no reading yet.
 */
private fun Int.coerceAtMostIfReal(max: Int): Int = if (this < 0) this else minOf(this, max)

/**
 * Converts a [CarParkFacilityDetailResponse] to a [NSWParkRideFacilityDetail] for database storage.
 *
 * This method calculates the number of available spots, total spots, and percentage full
 * for a Park&Ride facility using the occupancy data from all zones.
 *
 * Occupied spots are determined by summing the `transients` field from each zone's occupancy.
 * If `transients` is `null` or missing, it is treated as 0.
 *
 * @param stopId - needs to be passed because tsn provided by cark park is sometimes not from the
 * latest gtfs updates=d data.
 * @param stopName - similarly, stopName can also not be relied upon form the car park api, therefore,
 * using stop name provided separately using saved trips data.
 *
 * @return [NSWParkRideFacilityDetail] containing facility details for database storage
 **/
@OptIn(ExperimentalTime::class)
@VisibleForTesting(otherwise = PACKAGE_PRIVATE)
fun CarParkFacilityDetailResponse.toNSWParkRideFacilityDetail(
    stopName: String,
    stopId: String,
): NSWParkRideFacilityDetail {
    val totalSpots = spots.toIntOrNull() ?: 0
    val occupiedSpots = zones.sumOf {
        it.occupancy.total?.toIntOrNull() ?: it.occupancy.transients?.toIntOrNull() ?: 0
    }
    val spotsAvailable = totalSpots - occupiedSpots
    // Clamped for the same reason spotsAvailable is: the feed reports occupancy above
    // capacity often enough to matter (overflow parking, a stale `spots` total, bays
    // counted in occupancy but not in capacity), and a car park cannot be more than full.
    // Leaving this unbounded while clamping spotsAvailable produced "0 spots available,
    // 135% full" on the same card - two numbers from the same inputs disagreeing.
    val percentageFull = if (totalSpots > 0) {
        ((occupiedSpots * FULL_PERCENT) / totalSpots).coerceIn(0, FULL_PERCENT)
    } else {
        0
    }
    val timeText = messageDate.toSimple12HourTime().replace(" ", "\u00A0")

    log(
        "[$facilityName - $facilityId - $tsn] " +
            "Total spots: $totalSpots, Occupied spots: $occupiedSpots, " +
            "Spots available: $spotsAvailable, Percentage full: $percentageFull%, time: $timeText",
    )

    return NSWParkRideFacilityDetail(
        facilityId = facilityId,
        spotsAvailable = spotsAvailable.toLong().coerceAtLeast(0),
        totalSpots = totalSpots.toLong(),
        facilityName = facilityName.toDisplayFacilityName(),
        percentageFull = percentageFull.toLong(),
        stopId = stopId,
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
        // Clamped on the way out as well as on the way in: rows written before the write
        // path was fixed are already on disk with values above 100, and they would keep
        // rendering that way until the next successful poll replaced them.
        // The -1 loading sentinel is preserved - it is not an occupancy value.
        percentageFull = percentageFull.toInt().coerceAtMostIfReal(FULL_PERCENT),
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
                // Was an inline copy of toParkRideState(), which is the path the home
                // cards actually render from - so a fix applied only to that function
                // would have missed the screen showing the wrong number.
                .map { it.toParkRideState() }
                .toImmutableSet()

            if (uniqueFacilities.isNotEmpty()) {
                ParkRideUiState(
                    stopId = stopId,
                    stopName = stopName,
                    facilities = uniqueFacilities,
                    error = null,
                    isLoading = false,
                )
            } else {
                null
            }
        }
        .filterNotNull()
}

internal fun String.toDisplayFacilityName(): String = removePrefix("Park&Ride - ").trim()
