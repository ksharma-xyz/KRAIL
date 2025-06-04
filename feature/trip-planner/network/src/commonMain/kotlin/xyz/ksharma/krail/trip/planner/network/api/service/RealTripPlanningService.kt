package xyz.ksharma.krail.trip.planner.network.api.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.ParametersBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.network.NSW_TRANSPORT_BASE_URL
import xyz.ksharma.krail.trip.planner.network.api.model.StopFinderResponse
import xyz.ksharma.krail.trip.planner.network.api.model.StopType
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.network.api.service.stop_finder.StopFinderRequestParams
import xyz.ksharma.krail.trip.planner.network.api.service.trip.TripRequestParams

class RealTripPlanningService(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher,
) : TripPlanningService {

    override suspend fun trip(
        originStopId: String,
        destinationStopId: String,
        depArr: DepArr,
        date: String?,
        time: String?,
        excludeProductClassSet: Set<Int>,
    ): TripResponse = withContext(ioDispatcher) {

        httpClient.get("$NSW_TRANSPORT_BASE_URL/v1/tp/trip") {
            url {
                parameters.append(TripRequestParams.nameOrigin, originStopId)
                parameters.append(TripRequestParams.nameDestination, destinationStopId)

                parameters.append(TripRequestParams.depArrMacro, depArr.macro)
                date?.let { parameters.append(TripRequestParams.itdDate, date) }
                time?.let { parameters.append(TripRequestParams.itdTime, time) }

                parameters.append(TripRequestParams.typeDestination, "any")
                parameters.append(TripRequestParams.calcNumberOfTrips, "6")
                parameters.append(TripRequestParams.typeOrigin, "any")
                parameters.append(TripRequestParams.tfNSWTR, "true")
                parameters.append(TripRequestParams.version, "10.2.1.42")
                parameters.append(TripRequestParams.coordOutputFormat, "EPSG:4326")
                parameters.append(TripRequestParams.itOptionsActive, "1")
                parameters.append(TripRequestParams.computeMonomodalTripBicycle, "false")
                parameters.append(TripRequestParams.cycleSpeed, "16")
                parameters.append(TripRequestParams.useElevationData, "1")
                parameters.append(TripRequestParams.outputFormat, "rapidJSON")

                addExcludedTransportModes(
                    excludeProductClassSet = excludeProductClassSet,
                    parameters = parameters,
                )
            }
        }.body()
    }

    override suspend fun stopFinder(
        stopSearchQuery: String,
        stopType: StopType,
    ): StopFinderResponse = withContext(ioDispatcher) {
        httpClient.get("${NSW_TRANSPORT_BASE_URL}/v1/tp/stop_finder") {
            url {
                parameters.append(StopFinderRequestParams.nameSf, stopSearchQuery)

                parameters.append(StopFinderRequestParams.typeSf, stopType.type)
                parameters.append(StopFinderRequestParams.coordOutputFormat, "EPSG:4326")
                parameters.append(StopFinderRequestParams.outputFormat, "rapidJSON")
//                parameters.append(StopFinderRequestParams.version, "10.2.1.42")
                parameters.append(StopFinderRequestParams.tfNSWSF, "true")
            }
        }.body()
    }

    override fun closeHttpConnection() {
        httpClient.close()
    }

    private fun addExcludedTransportModes(
        excludeProductClassSet: Set<Int>,
        parameters: ParametersBuilder,
    ) {
        log("Exclude transport mode - $excludeProductClassSet")
        parameters.append(TripRequestParams.excludedMeans, "checkbox")

        if (excludeProductClassSet.contains(1)) {
            parameters.append(TripRequestParams.exclMOT1, "1")
        }
        if (excludeProductClassSet.contains(2)) {
            parameters.append(TripRequestParams.exclMOT2, "2")
        }
        if (excludeProductClassSet.contains(4)) {
            parameters.append(TripRequestParams.exclMOT4, "4")
        }
        if (excludeProductClassSet.contains(5) || excludeProductClassSet.contains(11)) {
            parameters.append(TripRequestParams.exclMOT5, "5")
            parameters.append(TripRequestParams.exclMOT11, "11")
        }
        if (excludeProductClassSet.contains(7)) {
            parameters.append(TripRequestParams.exclMOT7, "7")
        }
        if (excludeProductClassSet.contains(9)) {
            parameters.append(TripRequestParams.exclMOT9, "9")
        }
    }
}

enum class DepArr(val macro: String) {
    DEP("dep"),
    ARR("arr")
}
