package xyz.ksharma.krail.trip.planner.ui.testfakes

import xyz.ksharma.krail.trip.planner.ui.parkride.ParkRideCatalogue
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.LoadingEmoji
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem

/**
 * Lets a ViewModel test state the stations it wants directly, instead of working backwards
 * through Remote Config JSON and a stops table to produce them.
 */
class FakeParkRideCatalogue(
    var stations: List<ParkRideStationPickerItem> = emptyList(),
    var loadingEmoji: LoadingEmoji = LoadingEmoji(emoji = "🅿️", greeting = "Loading"),
) : ParkRideCatalogue {

    override fun stations(): List<ParkRideStationPickerItem> = stations

    override fun loadingEmoji(): LoadingEmoji = loadingEmoji
}
