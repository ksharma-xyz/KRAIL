package xyz.ksharma.krail.trip.planner.network.api.service

import app.krail.bff.proto.JourneyList
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.ParametersBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.network.IS_BFF_LOCAL_OVERRIDE_SET
import xyz.ksharma.krail.core.network.IS_BFF_PROTO_ENABLED
import xyz.ksharma.krail.core.network.KRAIL_BFF_BASE_URL
import xyz.ksharma.krail.core.network.NSW_TRANSPORT_BASE_URL
import xyz.ksharma.krail.core.network.NetworkTarget
import xyz.ksharma.krail.core.network.logNetworkCall
import xyz.ksharma.krail.trip.planner.network.api.mapper.journeyListToTripResponse
import xyz.ksharma.krail.trip.planner.network.api.model.StopFinderResponse
import xyz.ksharma.krail.trip.planner.network.api.model.StopType
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.network.api.service.stop_finder.StopFinderRequestParams
import xyz.ksharma.krail.trip.planner.network.api.service.trip.TripRequestParams

internal class RealTripPlanningService(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher,
) : TripPlanningService {

    private val tripBaseUrl: String =
        if (IS_BFF_LOCAL_OVERRIDE_SET) KRAIL_BFF_BASE_URL else NSW_TRANSPORT_BASE_URL

    override suspend fun trip(
        originStopId: String,
        destinationStopId: String,
        depArr: DepArr,
        date: String?,
        time: String?,
        excludeProductClassSet: Set<Int>,
    ): TripResponse = withContext(ioDispatcher) {
        // Phase C: when both the BFF local-override and the proto flag are
        // on, hit /api/v1/trip/plan-proto and decode a JourneyList via Wire,
        // then map to TripResponse so the existing UI mappers (notably
        // JourneyMapMapper, which reads coords for polylines) work unchanged.
        // Otherwise fall back to the NSW-shaped JSON path on either NSW
        // direct or BFF JSON pass-through, depending on IS_BFF_LOCAL_OVERRIDE_SET.
        val spec = TripRequestSpec(
            originStopId = originStopId,
            destinationStopId = destinationStopId,
            depArr = depArr,
            date = date,
            time = time,
            excludeProductClassSet = excludeProductClassSet,
        )
        if (IS_BFF_LOCAL_OVERRIDE_SET && IS_BFF_PROTO_ENABLED) {
            logNetworkCall(
                target = NetworkTarget.BFF,
                method = "GET",
                path = "/api/v1/trip/plan-proto",
            )
            val bytes: ByteArray = httpClient.get("$KRAIL_BFF_BASE_URL/api/v1/trip/plan-proto") {
                url { appendTripQueryParams(spec) }
                accept(ContentType("application", "x-protobuf"))
            }.body()
            return@withContext journeyListToTripResponse(JourneyList.ADAPTER.decode(bytes))
        }
        logNetworkCall(
            target = if (IS_BFF_LOCAL_OVERRIDE_SET) NetworkTarget.BFF else NetworkTarget.NSW,
            method = "GET",
            path = "/v1/tp/trip",
        )
        httpClient.get("$tripBaseUrl/v1/tp/trip") {
            url { appendTripQueryParams(spec) }
        }.body()
    }

    private fun io.ktor.http.URLBuilder.appendTripQueryParams(spec: TripRequestSpec) {
        parameters.append(TripRequestParams.nameOrigin, spec.originStopId)
        parameters.append(TripRequestParams.nameDestination, spec.destinationStopId)

        parameters.append(TripRequestParams.depArrMacro, spec.depArr.macro)
        spec.date?.let { parameters.append(TripRequestParams.itdDate, it) }
        spec.time?.let { parameters.append(TripRequestParams.itdTime, it) }

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
            excludeProductClassSet = spec.excludeProductClassSet,
            parameters = parameters,
        )
    }

    private data class TripRequestSpec(
        val originStopId: String,
        val destinationStopId: String,
        val depArr: DepArr,
        val date: String?,
        val time: String?,
        val excludeProductClassSet: Set<Int>,
    )

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

    override suspend fun stopFinder(
        stopSearchQuery: String,
        stopType: StopType,
    ): StopFinderResponse = withContext(ioDispatcher) {
        // stop_finder always goes to NSW direct — BFF has no equivalent endpoint.
        // Phase D will replace this with local search against a stops dataset.
        logNetworkCall(
            target = NetworkTarget.NSW,
            method = "GET",
            path = "/v1/tp/stop_finder",
        )
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
}

enum class DepArr(val macro: String) {
    DEP("dep"),
    ARR("arr"),
}
