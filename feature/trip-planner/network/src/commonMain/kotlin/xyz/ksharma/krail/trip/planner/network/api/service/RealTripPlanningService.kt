package xyz.ksharma.krail.trip.planner.network.api.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.ParametersBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.network.IS_BFF_LOCAL_OVERRIDE_SET
import xyz.ksharma.krail.core.network.IS_BFF_PROTO_FOR_TRIP_RESULTS_ENABLED
import xyz.ksharma.krail.core.network.KRAIL_BFF_BASE_URL
import xyz.ksharma.krail.core.network.NSW_TRANSPORT_BASE_URL
import xyz.ksharma.krail.core.network.NetworkTarget
import xyz.ksharma.krail.core.network.logNetworkCall
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
        // Phase C scaffold — when the proto flag flips on (and the BFF override
        // is set), this method should hit /api/v1/trip/plan-proto and decode
        // a JourneyList protobuf via :io:bff-api's Wire-generated classes.
        //
        // Wiring steps for the morning session (see docs/BFF_PHASE_A_MORNING.md §5):
        //   1. Add `implementation(projects.io.bffApi)` to this module's
        //      build.gradle.kts (commonMain dependencies).
        //   2. Implement JourneyListMapper (currently throws NotImplementedError).
        //   3. Replace this scaffold with the real proto fetch:
        //      ```
        //      val bytes: ByteArray = httpClient.get(
        //          "$KRAIL_BFF_BASE_URL/api/v1/trip/plan-proto"
        //      ) { /* same query params as the JSON path */ }.body()
        //      JourneyList.ADAPTER.decode(bytes).toTripResponse()
        //      ```
        //   4. Keep the JSON paths below as the kill-switch fallback.
        //
        // Today this branch is unreachable (IS_BFF_PROTO_FOR_TRIP_RESULTS_ENABLED
        // is const false) so the compiler dead-code-eliminates it. We keep the
        // throw to make the unwired contract loud if someone flips the flag.
        @Suppress("KotlinConstantConditions", "UnreachableCode")
        if (IS_BFF_LOCAL_OVERRIDE_SET && IS_BFF_PROTO_FOR_TRIP_RESULTS_ENABLED) {
            error(
                "BFF proto path enabled but the JourneyList mapper is not yet wired up. " +
                    "Add :io:bff-api dependency, implement JourneyListMapper, " +
                    "then replace this scaffold (see docs/BFF_PHASE_A_MORNING.md §5).",
            )
        }
        logNetworkCall(
            target = if (IS_BFF_LOCAL_OVERRIDE_SET) NetworkTarget.BFF else NetworkTarget.NSW,
            method = "GET",
            path = "/v1/tp/trip",
        )
        httpClient.get("$tripBaseUrl/v1/tp/trip") {
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
