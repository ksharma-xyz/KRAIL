package xyz.ksharma.krail.trip.planner.ui.searchstop

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.remoteconfig.JsonConfig
import xyz.ksharma.krail.core.remoteconfig.RemoteConfigDefaults
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SelectProductClassesForStop
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeSortOrder
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

class RealStopResultsManager(
    private val sandook: Sandook,
    private val flag: Flag,
) : StopResultsManager {

    // Store selected stops with private setters
    override var selectedFromStop: StopItem? = null
        private set

    override var selectedToStop: StopItem? = null
        private set

    private val highPriorityStopIdList: List<String> by lazy {
        flag.getFlagValue(FlagKeys.HIGH_PRIORITY_STOP_IDS.key).toStopsIdList()
    }

    // Methods to update selected stops
    override fun setSelectedFromStop(stopItem: StopItem?) {
        if (stopItem != null) {
            selectedFromStop = stopItem
            saveRecentSearchStop(stopItem)
        }
        log("StopResultsManager - setSelectedFromStop: $stopItem")
    }

    override fun setSelectedToStop(stopItem: StopItem?) {
        if (stopItem != null) {
            selectedToStop = stopItem
            saveRecentSearchStop(stopItem)
        }
        log("StopResultsManager - setSelectedToStop: $stopItem")
    }

    override fun reverseSelectedStops() {
        val temp = selectedFromStop
        selectedFromStop = selectedToStop
        selectedToStop = temp
        log("StopResultsManager - reverseSelectedStops: from=$selectedFromStop, to=$selectedToStop")
    }

    override fun clearSelectedStops() {
        selectedFromStop = null
        selectedToStop = null
        log("StopResultsManager - clearSelectedStops")
    }

    override suspend fun fetchStopResults(query: String): List<SearchStopState.SearchResult> {
        log("fetchStopResults from LOCAL_STOPS")

        val results = mutableListOf<SearchStopState.SearchResult>()

        // 1. Search for stops by stop name/ID - these go in as individual Stop results
        val stopResults: List<SelectProductClassesForStop> =
            sandook.selectStops(stopName = query, excludeProductClassList = listOf())

        val stopSearchResults = stopResults
            .map { it.toStopSearchResult() }
            .let(::prioritiseStops)
            .take(50)
            .map { SearchStopState.SearchResult.Stop(it.stopName, it.stopId, it.transportModeType) }

        results.addAll(stopSearchResults)

        // 2. Search for routes by exact route short name - this goes in as a Route result
        val routeShortName = sandook.selectRouteByShortName(query)
        if (routeShortName != null) {
            val routeResult = buildRouteSearchResult(routeShortName)
            if (routeResult != null) {
                // Add route result at the beginning since it's an exact match
                results.add(0, routeResult)
            }
        }

        return results
    }

    /**
     * Builds a complete Route search result with all variants, trips, and stops
     */
    private fun buildRouteSearchResult(routeShortName: String): SearchStopState.SearchResult.Route? {
        val variants = sandook.selectRouteVariantsByShortName(routeShortName)

        if (variants.isEmpty()) return null

        val routeVariants = variants.map { variant ->
            val trips = sandook.selectTripsByRouteId(variant.routeId)

            val tripOptions = trips.map { trip ->
                val stops = sandook.selectStopsByTripId(trip.tripId)

                val tripStops = stops.map { stop ->
                    SearchStopState.TripStop(
                        stopId = stop.stopId,
                        stopName = stop.stopName,
                        stopSequence = stop.stopSequence.toInt(),
                        transportModeType = stop.productClasses.toTransportModeList(),
                    )
                }.toImmutableList()

                SearchStopState.TripOption(
                    tripId = trip.tripId,
                    headsign = trip.headsign,
                    stops = tripStops,
                )
            }.toImmutableList()

            SearchStopState.RouteVariant(
                routeId = variant.routeId,
                routeName = variant.routeName,
                trips = tripOptions,
            )
        }.toImmutableList()

        return SearchStopState.SearchResult.Route(
            routeShortName = routeShortName,
            variants = routeVariants,
        )
    }

    // TODO - move to another file and add UT for it. Inject and use.
    override fun prioritiseStops(stopResults: List<SearchStopState.SearchResult.Stop>): List<SearchStopState.SearchResult.Stop> {
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
                { it.stopName },
            ),
        )
    }

    override fun fetchLocalStopName(stopId: String): String? {
        val resultsDb = sandook.selectStops(stopName = stopId, excludeProductClassList = listOf())
        return resultsDb
            .firstOrNull { it.stopId == stopId }
            ?.toStopSearchResult()
            ?.stopName
    }

    private fun SelectProductClassesForStop.toStopSearchResult() = SearchStopState.SearchResult.Stop(
        stopId = stopId,
        stopName = stopName,
        transportModeType = this.productClasses.toTransportModeList(),
    )

    private fun FlagValue.toStopsIdList(): List<String> {
        return when (this) {
            is FlagValue.JsonValue -> {
                log("flagValue: ${this.value}")
                val jsonObject = JsonConfig.lenient.parseToJsonElement(value).jsonObject
                jsonObject["stop_ids"]?.jsonArray?.map {
                    it.toString().trim('"')
                } ?: emptyList()
            }

            else -> {
                val defaultJson: String = RemoteConfigDefaults.getDefaults()
                    .firstOrNull { it.first == FlagKeys.HIGH_PRIORITY_STOP_IDS.key }?.second as String
                JsonConfig.lenient.parseToJsonElement(defaultJson).jsonArray.map {
                    it.toString().trim('"')
                }
            }
        }
    }

    // region Recent Search Stop

    override suspend fun recentSearchStops(): List<SearchStopState.StopResult> {
        return sandook.selectRecentSearchStops().map { recentStop ->
            SearchStopState.StopResult(
                stopId = recentStop.stopId,
                stopName = recentStop.stopName,
                transportModeType = recentStop.productClasses.toTransportModeList(),
            )
        }
    }

    private fun saveRecentSearchStop(stopItem: StopItem) {
        sandook.insertOrReplaceRecentSearchStop(stopId = stopItem.stopId)
    }

    override fun clearRecentSearchStops() {
        sandook.clearRecentSearchStops()
        log("StopResultsManager - clearRecentSearchStops")
    }

    /**
     * Extension function to parse comma-separated product classes string into TransportMode list
     */
    private fun String.toTransportModeList(): ImmutableList<TransportMode> {
        return this.split(",")
            .mapNotNull { productClass ->
                productClass.toIntOrNull()?.let { TransportMode.toTransportModeType(it) }
            }
            .toImmutableList()
    }

    // endregion
}
