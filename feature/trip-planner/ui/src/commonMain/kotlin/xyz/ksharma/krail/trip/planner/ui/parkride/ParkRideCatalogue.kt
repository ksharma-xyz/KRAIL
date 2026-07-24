package xyz.ksharma.krail.trip.planner.ui.parkride

import xyz.ksharma.krail.core.festival.FestivalManager
import xyz.ksharma.krail.core.festival.model.NoFestival
import xyz.ksharma.krail.core.festival.model.greetingAndEmoji
import xyz.ksharma.krail.park.ride.network.NswParkRideFacilityManager
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.trip.planner.ui.searchstop.StopResultsManager
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.LoadingEmoji
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.StationPosition

/**
 * Builds the list of Park & Ride stations the picker offers.
 *
 * Groups the three sources this needs — the Remote Config catalogue, local stop names, and
 * local stop coordinates — behind one call, so [AddParkRideViewModel] depends on a single
 * collaborator instead of carrying all three itself.
 */
interface ParkRideCatalogue {

    /**
     * One entry per station, collapsed from the raw (stopId, facilityId) pairs - see
     * [groupIntoStations] for why neither ID alone is a safe grouping key.
     */
    fun stations(): List<ParkRideStationPickerItem>

    /**
     * Emoji and greeting for the loading state, from the same festival source the timetable
     * uses - so a loading screen looks like the app rather than like this one screen.
     */
    fun loadingEmoji(): LoadingEmoji
}

internal class RealParkRideCatalogue(
    private val nswParkRideFacilityManager: NswParkRideFacilityManager,
    private val stopResultsManager: StopResultsManager,
    private val sandook: Sandook,
    private val festivalManager: FestivalManager,
) : ParkRideCatalogue {

    override fun loadingEmoji(): LoadingEmoji {
        val (greeting, emoji) = (festivalManager.festivalOnDate() ?: NoFestival()).greetingAndEmoji
        return LoadingEmoji(emoji = emoji, greeting = greeting)
    }

    override fun stations(): List<ParkRideStationPickerItem> =
        nswParkRideFacilityManager.getParkRideFacilities()
            .groupIntoStations(
                stopNameLookup = stopResultsManager::fetchLocalStopName,
                positionLookup = ::positions,
            )

    /**
     * Station coordinates come from the local GTFS stops table, never from the Park & Ride
     * availability API — so the map can plot every station immediately, with no network call
     * and no effect on the availability polling lifecycle.
     */
    private fun positions(stopIds: List<String>): Map<String, StationPosition> =
        sandook.selectStopCoordinatesBatch(stopIds)
            .mapValues { (_, latLon) -> StationPosition(latLon.first, latLon.second) }
}
