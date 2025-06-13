package xyz.ksharma.krail.park.ride.network

import kotlinx.coroutines.flow.Flow
import xyz.ksharma.krail.park.ride.network.model.NswParkRideFacility

interface NswParkRideRepository {

    fun getParkRideFacilities(): Flow<List<NswParkRideFacility>>

    suspend fun fetchParkRideFacilities()
}
