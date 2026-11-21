package xyz.ksharma.krail.trip.planner.ui.intro.v2

import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.ParkRideUserFacilityEvent
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource.UserAdded
import xyz.ksharma.krail.sandook.db.SavedParkRide
import xyz.ksharma.krail.trip.planner.ui.intro.INTRO_PARK_RIDE_RADIUS_KM
import xyz.ksharma.krail.trip.planner.ui.intro.SuggestedStop
import xyz.ksharma.krail.trip.planner.ui.intro.nearestParkRideStation
import xyz.ksharma.krail.trip.planner.ui.intro.parkRideStartPrefill
import xyz.ksharma.krail.trip.planner.ui.parkride.ParkRideAvailabilityLoader
import xyz.ksharma.krail.trip.planner.ui.parkride.ParkRideCatalogue
import xyz.ksharma.krail.trip.planner.ui.savedtrips.toParkRideState
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideCarPark
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import kotlin.coroutines.cancellation.CancellationException

/**
 * Everything the Park & Ride step reads and writes, kept apart from the view model so the view model
 * decides the order and this class does the work, in the same way as [IntroCompletion].
 *
 * Every call runs on the IO dispatcher and none of them polls. [stopNameLookup] is the stops table,
 * for the one stop name the pre-fill needs.
 */
internal class IntroParkRideGateway(
    private val catalogue: ParkRideCatalogue,
    private val availabilityLoader: ParkRideAvailabilityLoader,
    private val parkRideSandook: NswParkRideSandook,
    private val ioDispatcher: CoroutineDispatcher,
    private val stopNameLookup: (stopId: String) -> String?,
    private val analytics: Analytics,
) {
    /**
     * The nearest station within [INTRO_PARK_RIDE_RADIUS_KM] of [location], or null. [location] is
     * used for this one computation and kept nowhere: not here, not in state, not saved.
     */
    suspend fun nearestStation(location: LatLng): ParkRideStationPickerItem? = withContext(ioDispatcher) {
        nearestParkRideStation(location, catalogue.stations(), INTRO_PARK_RIDE_RADIUS_KM)
    }

    /** The station with [stationId], to work the card out again after the process was killed. */
    suspend fun stationById(stationId: String): ParkRideStationPickerItem? = withContext(ioDispatcher) {
        catalogue.stations().firstOrNull { it.stationId == stationId }
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

    /**
     * Saves every car park of [station] as a user added row, the same rows and the same source as
     * the picker's Add, and tracks it with stop ids only. It never touches the app review manager:
     * the rider's first minute is not the moment to ask for a review.
     */
    suspend fun saveStation(station: ParkRideStationPickerItem) {
        withContext(ioDispatcher) {
            parkRideSandook.insertOrReplaceSavedParkRides(
                parkRideInfoList = station.mappings.map { mapping ->
                    SavedParkRide(
                        stopId = mapping.stopId,
                        facilityId = mapping.facilityId,
                        stopName = station.savedStopName(),
                        facilityName = mapping.facilityName,
                        source = UserAdded.value,
                    )
                }.toSet(),
                source = UserAdded,
            )
        }
        track(station, ParkRideUserFacilityEvent.Action.ADD)
    }

    /** Removes the user added rows of [station] and only those, so a row a saved trip holds stays. */
    suspend fun removeStation(station: ParkRideStationPickerItem) {
        withContext(ioDispatcher) {
            station.mappings.forEach { mapping ->
                parkRideSandook.deleteSavedParkRide(mapping.stopId, mapping.facilityId, UserAdded)
            }
        }
        track(station, ParkRideUserFacilityEvent.Action.REMOVE)
    }

    /**
     * The start to fill in once [station] is on home, or null: the smallest of its stop ids that is
     * in the stops table, with that stop's own name.
     */
    suspend fun startPrefill(station: ParkRideStationPickerItem): SuggestedStop? = withContext(ioDispatcher) {
        parkRideStartPrefill(station, stopNameLookup)
    }

    /**
     * Gives the stored readings of [station] its own name. The loader files a car park that is not
     * saved yet under its stop id, because it takes the name from the saved row, so a reading the
     * Intro fetched before Add shows the stop id on the Saved Trips card until the next refresh.
     * Only the name is rewritten: the time of the reading, and with it the cooldown, is kept.
     */
    suspend fun correctStopName(station: ParkRideStationPickerItem) = withContext(ioDispatcher) {
        val facilityIds = station.mappings.map { it.facilityId }.toSet()
        val misnamed = parkRideSandook.getAllParkRideFacilityDetail().first()
            .filter { it.facilityId in facilityIds && it.stopName != station.savedStopName() }
        if (misnamed.isNotEmpty()) {
            parkRideSandook.insertOrReplaceAll(misnamed.map { it.copy(stopName = station.savedStopName()) })
        }
    }

    // Stop ids only, never a name or a coordinate, as the picker's own event carries.
    private fun track(station: ParkRideStationPickerItem, action: ParkRideUserFacilityEvent.Action) {
        analytics.track(
            ParkRideUserFacilityEvent(
                facilityId = station.mappings.joinToString { it.facilityId },
                stopId = station.stationId,
                action = action,
                source = ParkRideUserFacilityEvent.Source.INTRO,
            ),
        )
    }
}

// The name the picker saves a station under, so a car park added here reads the same everywhere.
private fun ParkRideStationPickerItem.savedStopName(): String = stopName.ifBlank { stationName }

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
