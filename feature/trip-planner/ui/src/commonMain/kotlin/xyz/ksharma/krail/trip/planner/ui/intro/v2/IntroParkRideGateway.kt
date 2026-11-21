package xyz.ksharma.krail.trip.planner.ui.intro.v2

import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.intro.INTRO_PARK_RIDE_RADIUS_KM
import xyz.ksharma.krail.trip.planner.ui.intro.nearestParkRideStation
import xyz.ksharma.krail.trip.planner.ui.parkride.ParkRideCatalogue
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
    private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * The nearest station within [INTRO_PARK_RIDE_RADIUS_KM] of [location], or null. [location] is
     * used for this one computation and kept nowhere: not here, not in state, not saved.
     */
    suspend fun nearestStation(location: LatLng): ParkRideStationPickerItem? = withContext(ioDispatcher) {
        nearestParkRideStation(location, catalogue.stations(), INTRO_PARK_RIDE_RADIUS_KM)
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
