package xyz.ksharma.krail.trip.planner.ui.parkride

import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.asNumber
import xyz.ksharma.krail.park.ride.network.service.ParkRideService
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.trip.planner.ui.savedtrips.applyBatchResults
import xyz.ksharma.krail.trip.planner.ui.savedtrips.fanOutPerFacility
import xyz.ksharma.krail.trip.planner.ui.savedtrips.filterRefreshableFacilities
import xyz.ksharma.krail.trip.planner.ui.savedtrips.parkRideApiCooldown
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideMapping
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Fetches Park & Ride availability on demand for the picker's map sheet.
 *
 * Deliberately a collaborator rather than more methods on [AddParkRideViewModel]: it keeps the
 * ViewModel's constructor and surface small, and it makes the rate-limiting reusable by any
 * other surface that later wants live parking numbers.
 *
 * Reuses the exact refresh path and cooldown the home cards use
 * ([filterRefreshableFacilities], [applyBatchResults], [fanOutPerFacility],
 * [parkRideApiCooldown]), so a new entry point cannot become a second, unthrottled way to hit
 * the API. Anything still on cooldown falls through to whatever is already cached.
 *
 * This is on-demand only, driven by an explicit tap — never a background poll. See
 * `docs/POLLING_LIFECYCLE.md`.
 */
interface ParkRideAvailabilityLoader {

    /**
     * Refreshes any of [mappings] that are off cooldown. Returns without a network call when
     * everything is still fresh, leaving the cached rows to be displayed.
     *
     * [mappings] may span more than one stop; each stop is refreshed on its own terms, so a
     * station whose facilities are still warm costs nothing while its neighbour reloads.
     */
    suspend fun refreshIfNeeded(mappings: List<ParkRideMapping>)
}

internal class RealParkRideAvailabilityLoader(
    private val parkRideSandook: NswParkRideSandook,
    private val parkRideService: ParkRideService,
    private val flag: Flag,
) : ParkRideAvailabilityLoader {

    private val nonPeakTimeCooldownSeconds: Long by lazy {
        flag.getFlagValue(FlagKeys.NSW_PARK_RIDE_NON_PEAK_TIME_COOLDOWN.key)
            .asNumber(DEFAULT_NON_PEAK_COOLDOWN_SECONDS)
    }

    private val peakTimeCooldownSeconds: Long by lazy {
        flag.getFlagValue(FlagKeys.NSW_PARK_RIDE_PEAK_TIME_COOLDOWN.key)
            .asNumber(DEFAULT_PEAK_COOLDOWN_SECONDS)
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun refreshIfNeeded(mappings: List<ParkRideMapping>) {
        if (mappings.isEmpty()) return

        // One pass per stop, because everything downstream of the fetch is written against a
        // single stop: the batch call asks about stop ids, `applyBatchResults` resets whatever
        // it was told to refresh and did not find, and both paths file each facility's detail
        // under the stop id they were handed. Computing "what is refreshable" across every
        // mapping and then asking about only the first stop meant the other stops' facilities
        // were declared refreshable, never requested, and then reset to DISTANT_PAST as
        // missing — which defeats their cooldown for good, so every later refresh spends an
        // API call on them.
        mappings.groupBy { it.stopId }.forEach { (stopId, stopMappings) ->
            refreshStop(stopId = stopId, mappings = stopMappings)
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun refreshStop(stopId: String, mappings: List<ParkRideMapping>) {
        val now = Clock.System.now().epochSeconds
        val refreshable = filterRefreshableFacilities(
            parkRideSandook = parkRideSandook,
            facilityIds = mappings.map { it.facilityId }.toSet(),
            cooldown = parkRideApiCooldown(
                peakTimeCooldownSeconds = peakTimeCooldownSeconds,
                nonPeakTimeCooldownSeconds = nonPeakTimeCooldownSeconds,
            ),
            nowInstant = Instant.fromEpochSeconds(now),
        )
        if (refreshable.isEmpty()) {
            log("Park & Ride facilities for stop $stopId all on cooldown; showing cached availability")
            return
        }

        val batched = parkRideService.fetchAvailabilityForStops(stopIds = listOf(stopId))
        if (batched != null) {
            applyBatchResults(parkRideSandook, stopId, refreshable, batched, now)
        } else {
            fanOutPerFacility(parkRideSandook, parkRideService, stopId, refreshable, now)
        }
    }

    private companion object {
        const val DEFAULT_PEAK_COOLDOWN_SECONDS = 120L
        const val DEFAULT_NON_PEAK_COOLDOWN_SECONDS = 600L
    }
}
