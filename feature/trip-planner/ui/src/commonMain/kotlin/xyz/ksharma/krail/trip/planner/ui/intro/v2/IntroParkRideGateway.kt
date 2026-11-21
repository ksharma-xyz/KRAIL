package xyz.ksharma.krail.trip.planner.ui.intro.v2

import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.trip.planner.ui.intro.INTRO_PARK_RIDE_RADIUS_KM
import xyz.ksharma.krail.trip.planner.ui.intro.nearestParkRideStation
import xyz.ksharma.krail.trip.planner.ui.parkride.ParkRideAvailabilityLoader
import xyz.ksharma.krail.trip.planner.ui.parkride.ParkRideCatalogue
import xyz.ksharma.krail.trip.planner.ui.savedtrips.toParkRideState
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideCarPark
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import kotlin.coroutines.cancellation.CancellationException

/**
 * Everything the Park & Ride step reads, kept apart from the view model so the view model decides
 * the order and this class does the work, in the same way as [IntroCompletion].
 *
 * Every call runs on the IO dispatcher and none of them polls.
 */
internal class IntroParkRideGateway(
    private val catalogue: ParkRideCatalogue,
    private val availabilityLoader: ParkRideAvailabilityLoader,
    private val parkRideSandook: NswParkRideSandook,
    private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * The nearest station within [INTRO_PARK_RIDE_RADIUS_KM] of [location], or null. [location] is
     * used for this one computation and kept nowhere: not here, not in state, not saved.
     */
    suspend fun nearestStation(location: LatLng): ParkRideStationPickerItem? = withContext(ioDispatcher) {
        nearestParkRideStation(location, catalogue.stations(), INTRO_PARK_RIDE_RADIUS_KM)
    }

    /**
     * The car parks of [station] with their latest reading, after asking the loader once.
     *
     * The loader is the same one the Saved Trips card and the picker use, so its cooldown applies
     * here and an Intro that follows a fresh reading costs no call. Whatever the loader could not
     * fetch is read from what is stored, and a car park with nothing stored is a name alone. A
     * loader that fails is the same as one that found nothing.
     */
    suspend fun carParks(station: ParkRideStationPickerItem): List<ParkRideCarPark> = withContext(ioDispatcher) {
        attempt { availabilityLoader.refreshIfNeeded(station.mappings) }
        val readings = attempt { parkRideSandook.getAllParkRideFacilityDetail().first() }
            .orEmpty()
            .associateBy { it.facilityId }
        station.mappings.distinctBy { it.facilityId }.map { mapping ->
            val reading = readings[mapping.facilityId]?.toParkRideState()
            ParkRideCarPark(
                facilityId = mapping.facilityId,
                name = mapping.facilityName,
                spotsAvailable = reading?.spotsAvailable,
                percentageFull = reading?.percentageFull,
                timeText = reading?.timeText?.takeIf { it.isNotBlank() },
            )
        }
    }
}

/**
 * [block]'s result, or null when it failed. The Intro never shows an error, so a failure is only
 * logged. A cancelled caller is never swallowed.
 */
internal suspend fun <T> attempt(block: suspend () -> T): T? = runCatching { block() }
    .onFailure { failure ->
        if (failure is CancellationException) throw failure
        logError("Intro Park & Ride step failed", failure)
    }
    .getOrNull()

/** The car parks as the card first shows them, names with no readings yet. */
internal fun ParkRideStationPickerItem.carParksWithoutReadings() = mappings
    .distinctBy { it.facilityId }
    .map { ParkRideCarPark(facilityId = it.facilityId, name = it.facilityName) }
    .toImmutableList()
