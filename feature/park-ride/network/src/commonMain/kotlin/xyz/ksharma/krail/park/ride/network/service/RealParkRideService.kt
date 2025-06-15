package xyz.ksharma.krail.park.ride.network.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineDispatcher
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.network.NSW_TRANSPORT_BASE_URL
import xyz.ksharma.krail.coroutines.ext.suspendSafeResult
import xyz.ksharma.krail.park.ride.network.model.CarParkFacilityDetailResponse

internal class RealParkRideService(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher
) : ParkRideService {

    override suspend fun fetchCarParkFacilities(
        facilityId: String
    ): Result<CarParkFacilityDetailResponse> = suspendSafeResult(ioDispatcher) {
        require(facilityId.isNotBlank()) { "Facility ID must not be blank" }

        log("API Call: Fetching car park details for facility ID: $facilityId")

        val response: CarParkFacilityDetailResponse =
            httpClient.get("$NSW_TRANSPORT_BASE_URL/v1/carpark") {
                url {
                    parameters.append("facility", facilityId)
                }
            }.body()

        response
    }

    override suspend fun fetchCarParkFacilities(): Result<Map<String, String>> =
        suspendSafeResult(ioDispatcher)
        {
            val response: Map<String, String> =
                httpClient.get("$NSW_TRANSPORT_BASE_URL/v1/carpark") {}.body()
            response
        }
}
