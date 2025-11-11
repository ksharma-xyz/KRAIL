package xyz.ksharma.krail.trip.planner.network.api.service

import app.krail.bff.proto.JourneyList
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError

/**
 * Trip planning service that uses Protocol Buffers for communication.
 * This provides 83% smaller response sizes compared to JSON.
 */
internal class ProtobufTripPlanningService(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher,
    private val baseUrl: String,
) {

    /**
     * Get trip plan using protobuf endpoint.
     *
     * @param originStopId Origin stop ID (e.g., "10101100")
     * @param destinationStopId Destination stop ID (e.g., "10101120")
     * @param depArr "dep" for departure, "arr" for arrival
     * @param date Date in YYYYMMDD format
     * @param time Time in HHmm format
     * @param excludedModes Comma-separated transport mode IDs (e.g., "1,5")
     * @return Result containing JourneyList or error
     */
    suspend fun getTripPlan(
        originStopId: String,
        destinationStopId: String,
        depArr: String = "dep",
        date: String? = null,
        time: String? = null,
        excludedModes: String? = null,
    ): Result<JourneyList> = withContext(ioDispatcher) {
        try {
            log("ProtobufAPI: Calling $baseUrl/api/v1/trip/plan-proto")
            log("ProtobufAPI: origin=$originStopId, destination=$destinationStopId")

            val response = httpClient.get("$baseUrl/api/v1/trip/plan-proto") {
                parameter("origin", originStopId)
                parameter("destination", destinationStopId)
                parameter("depArr", depArr)
                date?.let { parameter("date", it) }
                time?.let { parameter("time", it) }
                excludedModes?.let { parameter("excludedModes", it) }

                // Request protobuf response
                headers.append("Accept", "application/protobuf")
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    // Decode protobuf binary
                    val bytes = response.body<ByteArray>()
                    log("ProtobufAPI: Received ${bytes.size} bytes")

                    val journeyList = JourneyList.ADAPTER.decode(bytes)
                    log("ProtobufAPI: Decoded ${journeyList.journeys.size} journeys")

                    Result.success(journeyList)
                }
                else -> {
                    // Errors are returned as JSON
                    val errorBody = response.body<String>()
                    logError("ProtobufAPI Error: ${response.status} - $errorBody")
                    Result.failure(Exception("API Error: ${response.status} - $errorBody"))
                }
            }
        } catch (e: Exception) {
            logError("ProtobufAPI Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}

