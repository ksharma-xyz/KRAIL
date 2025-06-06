package xyz.ksharma.krail.park.ride.network

import xyz.ksharma.krail.park.ride.network.model.NswParkRideFacility

interface NswParkRideFacilityManager {

    fun getParkRideFacilities(): List<NswParkRideFacility>
}
