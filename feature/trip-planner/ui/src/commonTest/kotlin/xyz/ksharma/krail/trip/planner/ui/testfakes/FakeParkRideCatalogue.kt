package xyz.ksharma.krail.trip.planner.ui.testfakes

import xyz.ksharma.krail.trip.planner.ui.parkride.ParkRideCatalogue
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.LoadingEmoji
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem

/**
 * Lets a ViewModel test state the stations it wants directly, instead of working backwards
 * through Remote Config JSON and a stops table to produce them.
 */
internal class FakeParkRideCatalogue(
    var stations: List<ParkRideStationPickerItem> = emptyList(),
    var loadingEmoji: LoadingEmoji = LoadingEmoji(emoji = "🅿️", greeting = "Loading"),
) : ParkRideCatalogue {

    /** How many times the list was read, so a test can hold that a look at the stations reads it once. */
    var stationReads: Int = 0
        private set

    override fun stations(): List<ParkRideStationPickerItem> {
        stationReads++
        return stations
    }

    override fun loadingEmoji(): LoadingEmoji = loadingEmoji
}
