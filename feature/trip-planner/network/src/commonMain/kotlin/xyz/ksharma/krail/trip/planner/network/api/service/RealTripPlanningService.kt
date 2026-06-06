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
import xyz.ksharma.krail.core.network.BffEndpointResolver
import xyz.ksharma.krail.core.network.IS_BFF_PROTO_ENABLED
import xyz.ksharma.krail.core.network.NSW_TRANSPORT_BASE_URL
import xyz.ksharma.krail.core.network.NetworkUpstream
import xyz.ksharma.krail.core.network.logNetworkCall
import xyz.ksharma.krail.core.network.toNetworkUpstream
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.trip.planner.network.api.mapper.journeyListToTripResponse
import xyz.ksharma.krail.trip.planner.network.api.model.StopFinderResponse
import xyz.ksharma.krail.trip.planner.network.api.model.StopType
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.network.api.service.stop_finder.StopFinderRequestParams
import xyz.ksharma.krail.trip.planner.network.api.service.trip.TripRequestParams

internal class RealTripPlanningService(
    private val httpClient: HttpClient,
    private val ioDispatcher: CoroutineDispatcher,
    private val resolver: BffEndpointResolver,
) : TripPlanningService {

    override suspend fun trip(
        originStopId: String,
        destinationStopId: String,
        depArr: DepArr,
        date: String?,
        time: String?,
        excludeProductClassSet: Set<Int>,
    ): TripResponse = withContext(ioDispatcher) {
        // Phase C integrated with the BffEndpointResolver pattern. Resolver
        // decides NSW vs BFF (debug builds via DebugNetworkConfigStore,
        // release via Firebase RC `enable_proto_bff`). If the resolver picks
        // BFF AND the proto flag is on, hit /api/v1/trip/plan-proto and decode
        // a JourneyList via Wire — that path carries the polyline data the
        // journey-map needs. Otherwise hit the NSW-shaped JSON endpoint on
        // whichever base URL the resolver chose (NSW direct, or BFF JSON
        // pass-through when the proto flag is off).
        val spec = TripRequestSpec(
            originStopId = originStopId,
            destinationStopId = destinationStopId,
            depArr = depArr,
            date = date,
            time = time,
            excludeProductClassSet = excludeProductClassSet,
        )
        val baseUrl = resolver.resolveBaseUrl()
        val upstream = baseUrl.toNetworkUpstream()

        if (upstream == NetworkUpstream.BFF && IS_BFF_PROTO_ENABLED) {
            logNetworkCall(
                target = NetworkUpstream.BFF,
                method = "GET",
                path = "/api/v1/trip/plan-proto",
            )
            val bytes: ByteArray = httpClient.get("$baseUrl/api/v1/trip/plan-proto") {
                url { appendTripQueryParams(spec) }
                accept(ContentType("application", "x-protobuf"))
            }.body()
            return@withContext journeyListToTripResponse(JourneyList.ADAPTER.decode(bytes))
        }

        logNetworkCall(
            target = upstream,
            method = "GET",
            path = "/v1/tp/trip",
        )
        httpClient.get("$baseUrl/v1/tp/trip") {
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
        buildExclusionParams(excludeProductClassSet).forEach { (key, value) ->
            parameters.append(key, value)
        }
    }

    override suspend fun stopFinder(
        stopSearchQuery: String,
        stopType: StopType,
    ): StopFinderResponse = withContext(ioDispatcher) {
        // stop_finder always goes to NSW direct. BFF has no equivalent endpoint.
        // Phase D will replace this with local search against a stops dataset.
        logNetworkCall(
            target = NetworkUpstream.NSW,
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

/**
 * Builds the NSW API query params required to exclude specific transport modes.
 *
 * NSW API rules (verified against official docs):
 * - `excludedMeans=checkbox` MUST NOT be sent when the set is empty. Sending it with no
 *   accompanying `exclMOT_*` params causes the API to return only default/fallback modes
 *   (typically bus), silently dropping trains and other modes.
 * - Each `exclMOT_<N>` param name uses an underscore before the number (`exclMOT_5`, not
 *   `exclMOT5`). The API silently ignores params with the wrong name — no error returned.
 * - Each `exclMOT_<N>` value must be `"1"` (boolean flag). The mode number itself (e.g. `"5"`)
 *   is not a valid value and is silently ignored by the API.
 * - Bus and school bus share a single exclusion toggle — excluding either excludes both.
 */
internal fun buildExclusionParams(excludeProductClassSet: Set<Int>): Map<String, String> {
    if (excludeProductClassSet.isEmpty()) return emptyMap()
    val excludeBus = excludeProductClassSet.contains(TransportMode.Bus.productClass) ||
        excludeProductClassSet.contains(TransportMode.SCHOOL_BUS_PRODUCT_CLASS)
    return buildMap {
        put(TripRequestParams.excludedMeans, "checkbox")
        if (excludeProductClassSet.contains(TransportMode.Train.productClass)) put(TripRequestParams.exclMOT1, "1")
        if (excludeProductClassSet.contains(TransportMode.Metro.productClass)) put(TripRequestParams.exclMOT2, "1")
        if (excludeProductClassSet.contains(TransportMode.LightRail.productClass)) put(TripRequestParams.exclMOT4, "1")
        if (excludeBus) {
            put(TripRequestParams.exclMOT5, "1")
            put(TripRequestParams.exclMOT11, "1")
        }
        if (excludeProductClassSet.contains(TransportMode.Coach.productClass)) put(TripRequestParams.exclMOT7, "1")
        if (excludeProductClassSet.contains(TransportMode.Ferry.productClass)) put(TripRequestParams.exclMOT9, "1")
    }
}

enum class DepArr(val macro: String) {
    DEP("dep"),
    ARR("arr"),
}
