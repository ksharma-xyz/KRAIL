package xyz.ksharma.krail.trip.planner.ui.testfakes

import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideMapping
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem

/** A catalogue station with one car park under each of [stopIds]. */
internal fun parkRideStation(vararg stopIds: String) = ParkRideStationPickerItem(
    stationId = stopIds.first(),
    stationName = "Station ${stopIds.first()}",
    stopName = "",
    carParkNames = emptyList(),
    mappings = stopIds.map { ParkRideMapping(stopId = it, facilityId = "f$it", facilityName = "Car park $it") },
)
