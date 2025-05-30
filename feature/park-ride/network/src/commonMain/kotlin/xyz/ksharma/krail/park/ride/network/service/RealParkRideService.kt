package xyz.ksharma.krail.park.ride.network.service

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import io.ktor.client.call.body
import xyz.ksharma.krail.core.network.NSW_TRANSPORT_BASE_URL
import xyz.ksharma.krail.park.ride.network.model.CarParkFacilitiesResponse
import xyz.ksharma.krail.park.ride.network.model.CarParkFacilityDetailResponse

class RealParkRideService(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher
) : ParkRideService {

    override suspend fun getCarParkFacilities(
        facilityId: String
    ): CarParkFacilityDetailResponse = withContext(ioDispatcher) {
        httpClient.get("$NSW_TRANSPORT_BASE_URL/carpark") {
            url {
                parameters.append("facility", facilityId)
            }
        }.body()
    }

    override suspend fun getCarParkFacilities(): CarParkFacilitiesResponse =
        withContext(ioDispatcher) {
            httpClient.get("$NSW_TRANSPORT_BASE_URL/carpark") {}.body()
        }
}
