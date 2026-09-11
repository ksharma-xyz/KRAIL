package xyz.ksharma.krail.park.ride.network.service

import app.krail.bff.proto.ParkingAvailabilityResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.network.BffEndpointResolver
import xyz.ksharma.krail.core.network.IS_BFF_PROTO_ENABLED
import xyz.ksharma.krail.core.network.NetworkUpstream
import xyz.ksharma.krail.core.network.error.NetworkCaller
import xyz.ksharma.krail.core.network.logNetworkCall
import xyz.ksharma.krail.core.network.toNetworkUpstream
import xyz.ksharma.krail.park.ride.network.mapper.toStopBatchResponse
import xyz.ksharma.krail.park.ride.network.model.CarParkFacilityDetailResponse
import xyz.ksharma.krail.park.ride.network.model.ParkingStopBatchResponse

internal class RealParkRideService(
    private val httpClient: HttpClient,
    private val networkCaller: NetworkCaller,
    private val resolver: BffEndpointResolver,
) : ParkRideService {

    override suspend fun fetchCarParkFacilities(
        facilityId: String,
    ): Result<CarParkFacilityDetailResponse> {
        require(facilityId.isNotBlank()) { "Facility ID must not be blank" }

        log("API Call: Fetching car park details for facility ID: $facilityId")

        // Resolved outside the call: reads the debug store and a Remote Config flag, not
        // the network, so a failure here is not a network failure to classify.
        val baseUrl = resolver.resolveBaseUrl()
        val upstream = baseUrl.toNetworkUpstream()
        val isBff = upstream == NetworkUpstream.BFF
        return networkCaller.call(endpoint = DETAIL_ENDPOINT, upstream = upstream.label) {
            val requestUrl = buildParkRideDetailUrl(
                isBffOverrideSet = isBff,
                bffBaseUrl = baseUrl,
                nswBaseUrl = baseUrl,
                facilityId = facilityId,
            )

            val response: CarParkFacilityDetailResponse = if (isBff) {
                // BFF embeds the facility id in the path; no query param.
                logNetworkCall(
                    target = NetworkUpstream.BFF,
                    method = "GET",
                    path = "/v1/parking/facilities/$facilityId/availability",
                )
                httpClient.get(requestUrl) {}.body()
            } else {
                // NSW takes a single carpark endpoint plus a `facility` query param.
                logNetworkCall(
                    target = NetworkUpstream.NSW,
                    method = "GET",
                    path = DETAIL_ENDPOINT,
                )
                httpClient.get(requestUrl) {
                    url {
                        parameters.append("facility", facilityId)
                    }
                }.body()
            }

            response
        }
    }

    override suspend fun fetchCarParkFacilities(): Result<Map<String, String>> {
        val baseUrl = resolver.resolveBaseUrl()
        val upstream = baseUrl.toNetworkUpstream()
        val isBff = upstream == NetworkUpstream.BFF
        return networkCaller.call(endpoint = LIST_ENDPOINT, upstream = upstream.label) {
            val requestUrl = buildParkRideListUrl(
                isBffOverrideSet = isBff,
                bffBaseUrl = baseUrl,
                nswBaseUrl = baseUrl,
            )
            logNetworkCall(
                target = if (isBff) NetworkUpstream.BFF else NetworkUpstream.NSW,
                method = "GET",
                path = if (isBff) BFF_FACILITIES_PATH else DETAIL_ENDPOINT,
            )
            val response: Map<String, String> = httpClient.get(requestUrl) {}.body()
            response
        }
    }

    override suspend fun fetchAvailabilityForStops(
        stopIds: List<String>,
    ): ParkingStopBatchResponse? {
        // NSW has no equivalent batch endpoint, so when the resolver routes
        // us to NSW we return null and the caller falls back to the
        // per-facility path. Empty list is a no-op; return an empty payload
        // so the caller can distinguish "BFF on, no stops" from "BFF off".
        val baseUrl = resolver.resolveBaseUrl()
        return when {
            baseUrl.toNetworkUpstream() != NetworkUpstream.BFF -> null
            stopIds.isEmpty() -> ParkingStopBatchResponse()
            else -> fetchBatch(baseUrl = baseUrl, stopIds = capStopIdsForBatch(stopIds))
        }
    }

    /**
     * Hits the BFF parking batch endpoint. When [IS_BFF_PROTO_ENABLED] is on,
     * decodes a `ParkingAvailabilityResponse` proto and maps to the existing
     * `ParkingStopBatchResponse` shape; otherwise hits the JSON batch
     * endpoint at `/v1/parking/availability` (BFF JSON pass-through).
     */
    private suspend fun fetchBatch(
        baseUrl: String,
        stopIds: List<String>,
    ): ParkingStopBatchResponse = networkCaller.call(
        endpoint = BATCH_ENDPOINT,
        // The batch endpoint exists only on the BFF; the caller returns null for NSW.
        upstream = NetworkUpstream.BFF.label,
    ) {
        // getOrThrow at the end: fetchAvailabilityForStops throws rather than returning a
        // Result, because its null already means "BFF off, use the per-facility path" and a
        // Result<T?> would give callers two ways to say nothing. Routing through
        // networkCaller still classifies the throwable, so what escapes is a
        // NetworkException rather than a raw Ktor one.
        val joinedStopIds = stopIds.joinToString(",")
        if (IS_BFF_PROTO_ENABLED) {
            logNetworkCall(
                target = NetworkUpstream.BFF,
                method = "GET",
                path = "/api/v1/parking/availability-proto",
            )
            val bytes: ByteArray = httpClient.get(
                "$baseUrl/api/v1/parking/availability-proto",
            ) {
                url { parameters.append("stopIds", joinedStopIds) }
                accept(ContentType("application", "x-protobuf"))
            }.body()
            return@call ParkingAvailabilityResponse.ADAPTER.decode(bytes)
                .toStopBatchResponse()
        }
        val requestUrl = buildParkRideBatchByStopsUrl(bffBaseUrl = baseUrl)
        logNetworkCall(
            target = NetworkUpstream.BFF,
            method = "GET",
            path = "/v1/parking/availability",
        )
        httpClient.get(requestUrl) {
            url { parameters.append("stopIds", joinedStopIds) }
        }.body()
    }.getOrThrow()
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

private const val DETAIL_ENDPOINT = "/v1/carpark"
private const val BFF_FACILITIES_PATH = "/v1/parking/facilities"
private const val LIST_ENDPOINT = "/v1/carpark/list"
private const val BATCH_ENDPOINT = "/v1/parking/availability"
