package xyz.ksharma.krail.trip.planner.ui.searchstop

import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.remote_config.RemoteConfigDefaults
import xyz.ksharma.krail.core.remote_config.flag.Flag
import xyz.ksharma.krail.core.remote_config.flag.FlagKeys
import xyz.ksharma.krail.core.remote_config.flag.FlagValue
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SelectProductClassesForStop
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeSortOrder
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState

class RealStopResultsManager(
    private val sandook: Sandook,
    private val flag: Flag,
) : StopResultsManager {

    private val highPriorityStopIdList: List<String> by lazy {
        flag.getFlagValue(FlagKeys.HIGH_PRIORITY_STOP_IDS.key).toStopsIdList()
    }

    override suspend fun fetchStopResults(query: String): List<SearchStopState.StopResult> {
        log("fetchStopResults from LOCAL_STOPS")
        val resultsDb: List<SelectProductClassesForStop> =
            sandook.selectStops(stopName = query, excludeProductClassList = listOf())

        val results = resultsDb
            .map { it.toStopResult() }
            .let(::prioritiseStops)
            .take(50)

        return results
    }

    // TODO - move to another file and add UT for it. Inject and use.
    override fun prioritiseStops(stopResults: List<SearchStopState.StopResult>): List<SearchStopState.StopResult> {
        val sortedTransportModes = TransportMode.sortedValues(TransportModeSortOrder.PRIORITY)
        val transportModePriorityMap = sortedTransportModes.mapIndexed { index, transportMode ->
            transportMode.productClass to index
        }.toMap()

        return stopResults.sortedWith(
            compareBy(
                { stopResult ->
                    if (stopResult.stopId in highPriorityStopIdList) 0 else 1
                },
                { stopResult ->
                    stopResult.transportModeType.minOfOrNull {
                        transportModePriorityMap[it.productClass] ?: Int.MAX_VALUE
                    } ?: Int.MAX_VALUE
                },
                { it.stopName }
            )
        )
    }

    override fun fetchLocalStopName(stopId: String): String? {
        val resultsDb = sandook.selectStops(stopName = stopId, excludeProductClassList = listOf())
        return resultsDb
            .firstOrNull { it.stopId == stopId }
            ?.toStopResult()
            ?.stopName
    }


    private fun filterProductClasses(
        stopResults: List<SearchStopState.StopResult>,
        excludedProductClasses: List<Int> = emptyList(),
    ): List<SearchStopState.StopResult> {
        return stopResults.filter { stopResult ->
            val productClasses = stopResult.transportModeType.map { it.productClass }
            productClasses.any { it !in excludedProductClasses }
        }
    }

    private fun SelectProductClassesForStop.toStopResult() = SearchStopState.StopResult(
        stopId = stopId,
        stopName = stopName,
        transportModeType = this.productClasses.split(",").mapNotNull {
            TransportMode.toTransportModeType(it.toInt())
        }.toImmutableList(),
    )

    private fun FlagValue.toStopsIdList(): List<String> {
        return when (this) {
            is FlagValue.JsonValue -> {
                log("flagValue: ${this.value}")
                val jsonObject = Json.parseToJsonElement(value).jsonObject
                jsonObject["stop_ids"]?.jsonArray?.map {
                    it.toString().trim('"')
                } ?: emptyList()
            }

            else -> {
                val defaultJson: String = RemoteConfigDefaults.getDefaults()
                    .firstOrNull { it.first == FlagKeys.HIGH_PRIORITY_STOP_IDS.key }?.second as String
                Json.parseToJsonElement(defaultJson).jsonArray.map {
                    it.toString().trim('"')
                }
            }
        }
    }
}
