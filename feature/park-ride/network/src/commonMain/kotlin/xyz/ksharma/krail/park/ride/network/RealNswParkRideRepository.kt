package xyz.ksharma.krail.park.ride.network

import kotlinx.coroutines.flow.Flow
import xyz.ksharma.krail.park.ride.network.model.NswParkRideFacility
import xyz.ksharma.krail.park.ride.network.service.ParkRideService
import xyz.ksharma.krail.sandook.Sandook

internal class RealNswParkRideRepository(
    private val parkRideService: ParkRideService,
    private val sandook: Sandook,
) : NswParkRideRepository {

    override fun getParkRideFacilities(): Flow<List<NswParkRideFacility>> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchParkRideFacilities() {
        TODO("Not yet implemented")
    }

    private fun insertParkRideFacilities(parkRideFacilities: List<NswParkRideFacility>): Flow<Unit> {
        TODO("Not yet implemented")
    }
}
