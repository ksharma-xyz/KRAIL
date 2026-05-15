package xyz.ksharma.krail.park.ride.network.service

import app.krail.bff.proto.ParkingAvailabilityResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.network.IS_BFF_LOCAL_OVERRIDE_SET
import xyz.ksharma.krail.core.network.IS_BFF_PROTO_ENABLED
import xyz.ksharma.krail.core.network.KRAIL_BFF_BASE_URL
import xyz.ksharma.krail.core.network.NSW_TRANSPORT_BASE_URL
import xyz.ksharma.krail.core.network.NetworkTarget
import xyz.ksharma.krail.core.network.logNetworkCall
import xyz.ksharma.krail.coroutines.ext.suspendSafeResult
import xyz.ksharma.krail.park.ride.network.mapper.toStopBatchResponse
import xyz.ksharma.krail.park.ride.network.model.CarParkFacilityDetailResponse
import xyz.ksharma.krail.park.ride.network.model.ParkingStopBatchResponse

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

    override suspend fun fetchAvailabilityForStops(
        stopIds: List<String>,
    ): ParkingStopBatchResponse? {
        // NSW has no equivalent batch endpoint, so the override-off branch
        // returns null and the caller falls back to the per-facility path.
        if (!IS_BFF_LOCAL_OVERRIDE_SET) return null

        // Empty list is a no-op; do not fire a request.
        if (stopIds.isEmpty()) return ParkingStopBatchResponse()

        // Mirror the BFF's 20-ID cap client-side. Anything beyond the cap
        // is silently truncated rather than erroring, matching the server's
        // tolerant behaviour for the per-facility cap.
        val cappedStopIds = capStopIdsForBatch(stopIds)
        val joinedStopIds = cappedStopIds.joinToString(",")

        return withContext(ioDispatcher) {
            // Phase C: when both the BFF local-override and the proto flag
            // are on, hit /api/v1/parking/availability-proto and decode a
            // ParkingAvailabilityResponse via Wire, then map to the existing
            // ParkingStopBatchResponse so SavedTripsViewModel works
            // unchanged. Otherwise fall back to the JSON batch endpoint.
            if (IS_BFF_PROTO_ENABLED) {
                logNetworkCall(
                    target = NetworkTarget.BFF,
                    method = "GET",
                    path = "/api/v1/parking/availability-proto",
                )
                val bytes: ByteArray = httpClient.get(
                    "$KRAIL_BFF_BASE_URL/api/v1/parking/availability-proto",
                ) {
                    url {
                        parameters.append("stopIds", joinedStopIds)
                    }
                    accept(ContentType("application", "x-protobuf"))
                }.body()
                return@withContext ParkingAvailabilityResponse.ADAPTER.decode(bytes)
                    .toStopBatchResponse()
            }

            val requestUrl = buildParkRideBatchByStopsUrl(
                bffBaseUrl = KRAIL_BFF_BASE_URL,
            )
            logNetworkCall(
                target = NetworkTarget.BFF,
                method = "GET",
                path = "/v1/parking/availability",
            )
            httpClient.get(requestUrl) {
                url {
                    parameters.append("stopIds", joinedStopIds)
                }
            }.body()
        }
    }
}

/**
 * BFF cap on `?stopIds=` values per batch request, per
 * `KRAIL-BFF/docs/handover/PARK_RIDE_BATCH_HANDOVER.md` §2.
 */
internal const val MAX_STOP_IDS_PER_BATCH: Int = 20

/**
 * Truncates [stopIds] to the BFF's per-batch cap if needed, logging when a
 * truncation actually happens. Pure and side-effect-free apart from the log
 * line, so the cap behaviour can be unit-tested without a fake HTTP client.
 */
internal fun capStopIdsForBatch(stopIds: List<String>): List<String> {
    if (stopIds.size <= MAX_STOP_IDS_PER_BATCH) return stopIds
    log(
        "fetchAvailabilityForStops: ${stopIds.size} stop IDs exceeds BFF cap of " +
            "$MAX_STOP_IDS_PER_BATCH; truncating to first $MAX_STOP_IDS_PER_BATCH.",
    )
    return stopIds.take(MAX_STOP_IDS_PER_BATCH)
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

/**
 * Builds the BFF batch URL for the `?stopIds=` mode. The query parameter
 * itself is appended by the Ktor caller via `url { parameters.append(...) }`
 * so encoding is handled by the client; this helper exists purely so the
 * base path is testable in isolation alongside the other URL builders.
 */
internal fun buildParkRideBatchByStopsUrl(
    bffBaseUrl: String,
): String = "$bffBaseUrl/v1/parking/availability"
