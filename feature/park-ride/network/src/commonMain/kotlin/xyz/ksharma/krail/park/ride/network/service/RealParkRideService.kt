package xyz.ksharma.krail.park.ride.network.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineDispatcher
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.network.IS_BFF_LOCAL_OVERRIDE_SET
import xyz.ksharma.krail.core.network.KRAIL_BFF_BASE_URL
import xyz.ksharma.krail.core.network.NSW_TRANSPORT_BASE_URL
import xyz.ksharma.krail.core.network.NetworkTarget
import xyz.ksharma.krail.core.network.logNetworkCall
import xyz.ksharma.krail.coroutines.ext.suspendSafeResult
import xyz.ksharma.krail.park.ride.network.model.CarParkFacilityDetailResponse

internal class RealParkRideService(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher,
) : ParkRideService {

    override suspend fun fetchCarParkFacilities(
        facilityId: String,
    ): Result<CarParkFacilityDetailResponse> = suspendSafeResult(ioDispatcher) {
        require(facilityId.isNotBlank()) { "Facility ID must not be blank" }

        log("API Call: Fetching car park details for facility ID: $facilityId")

        val requestUrl = buildParkRideDetailUrl(
            isBffOverrideSet = IS_BFF_LOCAL_OVERRIDE_SET,
            bffBaseUrl = KRAIL_BFF_BASE_URL,
            nswBaseUrl = NSW_TRANSPORT_BASE_URL,
            facilityId = facilityId,
        )

        val response: CarParkFacilityDetailResponse = if (IS_BFF_LOCAL_OVERRIDE_SET) {
            // BFF embeds the facility id in the path; no query param.
            logNetworkCall(
                target = NetworkTarget.BFF,
                method = "GET",
                path = "/v1/parking/facilities/$facilityId/availability",
            )
            httpClient.get(requestUrl) {}.body()
        } else {
            // NSW takes a single carpark endpoint plus a `facility` query param.
            logNetworkCall(
                target = NetworkTarget.NSW,
                method = "GET",
                path = "/v1/carpark",
            )
            httpClient.get(requestUrl) {
                url {
                    parameters.append("facility", facilityId)
                }
            }.body()
        }

        response
    }

    override suspend fun fetchCarParkFacilities(): Result<Map<String, String>> =
        suspendSafeResult(ioDispatcher) {
            val requestUrl = buildParkRideListUrl(
                isBffOverrideSet = IS_BFF_LOCAL_OVERRIDE_SET,
                bffBaseUrl = KRAIL_BFF_BASE_URL,
                nswBaseUrl = NSW_TRANSPORT_BASE_URL,
            )
            logNetworkCall(
                target = if (IS_BFF_LOCAL_OVERRIDE_SET) NetworkTarget.BFF else NetworkTarget.NSW,
                method = "GET",
                path = if (IS_BFF_LOCAL_OVERRIDE_SET) "/v1/parking/facilities" else "/v1/carpark",
            )
            val response: Map<String, String> = httpClient.get(requestUrl) {}.body()
            response
        }
}

internal fun buildParkRideListUrl(
    isBffOverrideSet: Boolean,
    bffBaseUrl: String,
    nswBaseUrl: String,
): String = if (isBffOverrideSet) {
    "$bffBaseUrl/v1/parking/facilities"
} else {
    "$nswBaseUrl/v1/carpark"
}

internal fun buildParkRideDetailUrl(
    isBffOverrideSet: Boolean,
    bffBaseUrl: String,
    nswBaseUrl: String,
    facilityId: String,
): String = if (isBffOverrideSet) {
    "$bffBaseUrl/v1/parking/facilities/$facilityId/availability"
} else {
    "$nswBaseUrl/v1/carpark"
}
