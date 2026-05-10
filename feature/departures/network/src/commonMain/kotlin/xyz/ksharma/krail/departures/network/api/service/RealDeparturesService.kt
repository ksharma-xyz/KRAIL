package xyz.ksharma.krail.departures.network.api.service

import app.krail.bff.proto.DepartureBoardResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.core.network.IS_BFF_LOCAL_OVERRIDE_SET
import xyz.ksharma.krail.core.network.IS_BFF_PROTO_ENABLED
import xyz.ksharma.krail.core.network.KRAIL_BFF_BASE_URL
import xyz.ksharma.krail.core.network.NSW_TRANSPORT_BASE_URL
import xyz.ksharma.krail.core.network.NetworkTarget
import xyz.ksharma.krail.core.network.logNetworkCall
import xyz.ksharma.krail.departures.network.api.mapper.toDepartureMonitorResponse
import xyz.ksharma.krail.departures.network.api.model.DepartureMonitorResponse

internal class RealDeparturesService(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher,
) : DeparturesService {

    override suspend fun departures(
        stopId: String,
        date: String?,
        time: String?,
    ): DepartureMonitorResponse = withContext(ioDispatcher) {
        // Phase C: when both the BFF local-override and the proto flag are
        // on, hit /api/v1/stops/{stopId}/departures-proto and decode a
        // DepartureBoardResponse via Wire, then map to the existing
        // DepartureMonitorResponse so the existing UI layer works unchanged.
        // Otherwise fall back to the JSON paths (BFF JSON pass-through or
        // NSW direct).
        if (IS_BFF_LOCAL_OVERRIDE_SET && IS_BFF_PROTO_ENABLED) {
            logNetworkCall(
                target = NetworkTarget.BFF,
                method = "GET",
                path = "/api/v1/stops/$stopId/departures-proto",
            )
            val bytes: ByteArray = httpClient.get(
                "$KRAIL_BFF_BASE_URL/api/v1/stops/$stopId/departures-proto",
            ) {
                url {
                    date?.let { parameters.append("date", it) }
                    time?.let { parameters.append("time", it) }
                }
                accept(ContentType("application", "x-protobuf"))
            }.body()
            return@withContext DepartureBoardResponse.ADAPTER.decode(bytes)
                .toDepartureMonitorResponse()
        }
        if (IS_BFF_LOCAL_OVERRIDE_SET) {
            logNetworkCall(
                target = NetworkTarget.BFF,
                method = "GET",
                path = "/v1/stops/$stopId/departures",
            )
            httpClient.get("$KRAIL_BFF_BASE_URL/v1/stops/$stopId/departures") {
                url {
                    date?.let { parameters.append("date", it) }
                    time?.let { parameters.append("time", it) }
                }
            }.body()
        } else {
            logNetworkCall(
                target = NetworkTarget.NSW,
                method = "GET",
                path = "/v1/tp/departure_mon",
            )
            httpClient.get("$NSW_TRANSPORT_BASE_URL/v1/tp/departure_mon") {
                url {
                    parameters.append(DepartureRequestParams.OUTPUT_FORMAT, "rapidJSON")
                    parameters.append(DepartureRequestParams.COORD_OUTPUT_FORMAT, "EPSG:4326")
                    parameters.append(DepartureRequestParams.MODE, "direct")
                    parameters.append(DepartureRequestParams.TYPE_DM, "stop")
                    parameters.append(DepartureRequestParams.NAME_DM, stopId)
                    parameters.append(DepartureRequestParams.DEPARTURE_MONITOR_MACRO, "true")
                    parameters.append(DepartureRequestParams.TF_NSW_DM, "true")

                    date?.let { parameters.append(DepartureRequestParams.ITD_DATE, it) }
                    time?.let { parameters.append(DepartureRequestParams.ITD_TIME, it) }
                }
            }.body()
        }
    }
}
