package xyz.ksharma.krail.park.ride.network.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.core.network.NSW_TRANSPORT_BASE_URL
import xyz.ksharma.krail.park.ride.network.model.CarParkFacilityDetailResponse

internal class RealParkRideService(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher
) : ParkRideService {

    override suspend fun fetchCarParkFacilities(
        facilityId: String
    ): CarParkFacilityDetailResponse = withContext(ioDispatcher) {
        httpClient.get("$NSW_TRANSPORT_BASE_URL/v1/carpark") {
            url {
                parameters.append("facility", facilityId)
            }
        }.body()
    }

    override suspend fun fetchCarParkFacilities(): Map<String, String> =
        withContext(ioDispatcher) {
            httpClient.get("$NSW_TRANSPORT_BASE_URL/v1/carpark") {}.body()
        }
}
