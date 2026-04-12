package xyz.ksharma.krail.departures.network.api.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.core.network.NSW_TRANSPORT_BASE_URL
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
