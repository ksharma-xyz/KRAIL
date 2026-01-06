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
import xyz.ksharma.krail.sandook.NswBusRoutesSandook
import xyz.ksharma.krail.sandook.NswBusTripOptions
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SelectProductClassesForStop
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeSortOrder
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

class RealStopResultsManager(
    private val sandook: Sandook,
    private val nswBusRoutesSandook: NswBusRoutesSandook,
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

    override suspend fun fetchStopResults(
        query: String,
        searchRoutesEnabled: Boolean, // Default value defined in interface
    ): List<SearchStopState.SearchResult> {
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

        // 2. Search for routes by exact route short name (if enabled)
        // Returns multiple Route results, one per unique headsign (direction)
        if (searchRoutesEnabled) {
            val routeShortName = nswBusRoutesSandook.selectRouteByShortName(query)
            if (routeShortName != null) {
                val routeResults = buildRouteSearchResults(routeShortName)
                // Add route results at the beginning since they're exact matches
                results.addAll(0, routeResults)
            }
        }

        return results
    }

    /**
     * Builds multiple Trip search results from route search.
     * Each trip in the database becomes one Trip result in the UI.
     * For route "702", this might return multiple trips with various headsigns/directions.
     */
    private fun buildRouteSearchResults(routeShortName: String): List<SearchStopState.SearchResult.Trip> {
        // Step 1: Get all route variants for this route (e.g., different networks operating route "702")
        val variants = nswBusRoutesSandook.selectRouteVariantsByShortName(routeShortName)

        if (variants.isEmpty()) return emptyList()

        // Step 2: Batch query - fetch all trips for all variants in ONE query (avoids N+1 problem)
        // If there are 5 variants, this is 1 query instead of 5 separate queries!
        val allRouteIds = variants.map { it.routeId }
        val allTrips: List<NswBusTripOptions> =
            nswBusRoutesSandook.selectTripsByRouteIds(allRouteIds)

        // Step 3: Create a lookup map for fast variant access by routeId
        val variantsByRouteId = variants.associateBy { it.routeId }

        // Step 4: Transform each trip to a Trip UI object
        // Each tripId is unique, so each becomes its own result
        return allTrips.mapNotNull { trip ->
            variantsByRouteId[trip.routeId] ?: return@mapNotNull null

            // Fetch stops for this trip
            val stops = nswBusRoutesSandook.selectStopsByTripId(trip.tripId)

            val tripStops = stops.map { stop ->
                SearchStopState.TripStop(
                    stopId = stop.stopId,
                    stopName = stop.stopName,
                    stopSequence = stop.stopSequence.toInt(),
                    transportModeType = stop.productClasses.toTransportModeList(),
                )
            }.toImmutableList()

            // Return a Trip object for this specific trip
            SearchStopState.SearchResult.Trip(
                tripId = trip.tripId,
                routeShortName = routeShortName,
                headsign = trip.headsign,
                stops = tripStops,
                // default to bus because that's the only option offered in app.
                transportMode = tripStops.firstOrNull()?.transportModeType?.firstOrNull()
                    ?: TransportMode.Bus(),
            )
        }
    }

    // TODO - move to another file and add UT for it. Inject and use.
    override fun prioritiseStops(
        stopResults: List<SearchStopState.SearchResult.Stop>,
    ): List<SearchStopState.SearchResult.Stop> {
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
