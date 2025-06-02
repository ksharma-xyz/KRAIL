package xyz.ksharma.core.test.fakes

import xyz.ksharma.krail.park.ride.network.model.NswParkRideFacility
import xyz.ksharma.krail.park.ride.network.NswParkRideFacilityManager

class FakeParkRideFacilityManager : NswParkRideFacilityManager {

    var facilities: List<NswParkRideFacility> = listOf(
        NswParkRideFacility(
            stopId = "207210",
            parkRideFacilityId = "6",
            parkRideName = "Sample Park & Ride 1"
        ),
        NswParkRideFacility(
            stopId = "207211",
            parkRideFacilityId = "7",
            parkRideName = "Sample Park & Ride 2"
        )
    )

    override fun getParkRideFacilities(): List<NswParkRideFacility> {
        return facilities
    }
}