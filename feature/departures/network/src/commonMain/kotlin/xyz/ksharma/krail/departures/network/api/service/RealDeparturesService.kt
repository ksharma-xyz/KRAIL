package xyz.ksharma.krail.departures.network.api.service

import app.krail.bff.proto.DepartureBoardResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.core.network.BffEndpointResolver
import xyz.ksharma.krail.core.network.IS_BFF_PROTO_ENABLED
import xyz.ksharma.krail.core.network.NSW_TRANSPORT_BASE_URL
import xyz.ksharma.krail.core.network.NetworkUpstream
import xyz.ksharma.krail.core.network.logNetworkCall
import xyz.ksharma.krail.core.network.toNetworkUpstream
import xyz.ksharma.krail.departures.network.api.mapper.toDepartureMonitorResponse
import xyz.ksharma.krail.departures.network.api.model.DepartureMonitorResponse

internal class RealDeparturesService(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher,
    private val resolver: BffEndpointResolver,
) : DeparturesService {

    override suspend fun departures(
        stopId: String,
        date: String?,
        time: String?,
    ): DepartureMonitorResponse = withContext(ioDispatcher) {
        // Resolver picks NSW vs BFF (debug-store in debug builds, Firebase RC
        // in release). When BFF is chosen AND the proto flag is on, hit the
        // proto endpoint and decode a DepartureBoardResponse, then map to the
        // existing DepartureMonitorResponse so downstream UI is unchanged.
        // Otherwise hit the JSON endpoint on whichever base URL the resolver
        // chose (NSW direct or BFF JSON pass-through).
        val baseUrl = resolver.resolveBaseUrl()
        val upstream = baseUrl.toNetworkUpstream()

        if (upstream == NetworkUpstream.BFF && IS_BFF_PROTO_ENABLED) {
            logNetworkCall(
                target = NetworkUpstream.BFF,
                method = "GET",
                path = "/api/v1/stops/$stopId/departures-proto",
            )
            val bytes: ByteArray = httpClient.get(
                "$baseUrl/api/v1/stops/$stopId/departures-proto",
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

        if (upstream == NetworkUpstream.BFF) {
            logNetworkCall(
                target = NetworkUpstream.BFF,
                method = "GET",
                path = "/v1/stops/$stopId/departures",
            )
            httpClient.get("$baseUrl/v1/stops/$stopId/departures") {
                url {
                    date?.let { parameters.append("date", it) }
                    time?.let { parameters.append("time", it) }
                }
            }.body()
        } else {
            logNetworkCall(
                target = NetworkUpstream.NSW,
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
