package xyz.ksharma.krail.trip.planner.ui.searchstop

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.remoteconfig.JsonConfig
import xyz.ksharma.krail.core.remoteconfig.RemoteConfigDefaults
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.core.remoteconfig.flag.asBoolean
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.core.transport.TransportModeSortOrder
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.sandook.NswBusRoutesSandook
import xyz.ksharma.krail.sandook.NswBusTripOptions
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SelectProductClassesForStop
import xyz.ksharma.krail.trip.planner.ui.searchstop.fuzzy.FuzzyStopRanker
import xyz.ksharma.krail.trip.planner.ui.searchstop.fuzzy.normalize
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

class RealStopResultsManager(
    private val sandook: Sandook,
    private val nswBusRoutesSandook: NswBusRoutesSandook,
    private val flag: Flag,
    private val fuzzyStopRanker: FuzzyStopRanker,
) : StopResultsManager {

    // Store selected stops with private setters
    override var selectedFromStop: StopItem? = null
        private set

    override var selectedToStop: StopItem? = null
        private set

    private val highPriorityStopIdList: List<String> by lazy {
        flag.getFlagValue(FlagKeys.HIGH_PRIORITY_STOP_IDS.key).toStopsIdList()
    }

    private val isFuzzyEnabled: Boolean by lazy {
        flag.getFlagValue(FlagKeys.ENABLE_FUZZY_STOP_SEARCH.key).asBoolean(fallback = false)
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
    ): List<SearchStopState.SearchResult> = withContext(Dispatchers.Default) {
        log("fetchStopResults from LOCAL_STOPS")

        // Trim and cap pasted/oversized input at the boundary before it reaches DB LIKE
        // queries, regex normalisation, or Levenshtein matrices. Trim avoids "  central"
        // missing matches because LIKE 'X%' won't tolerate leading whitespace.
        val safeQuery = query.take(MAX_QUERY_LENGTH).trim()

        // Don't search on 1-character queries. The exact path's substring LIKE returns
        // a flood of accidental matches and the fuzzy path is too noisy to be useful.
        if (safeQuery.length < MIN_QUERY_LENGTH) return@withContext emptyList()

        val results = mutableListOf<SearchStopState.SearchResult>()

        // 1. Search for stops by stop name/ID - these go in as individual Stop results
        val exactResults = sandook.selectStops(stopName = safeQuery, excludeProductClassList = listOf())
            .map { it.toStopSearchResult() }
            .let(::prioritiseStops)

        // Fuzzy is best-effort: if it fails (DB hiccup, unexpected scoring input),
        // fall back to exact-only rather than failing the whole search.
        val stopSearchResults = if (isFuzzyEnabled && exactResults.size < MIN_FUZZY_FALLBACK_THRESHOLD) {
            val fuzzyResults = runCatching { fetchFuzzyResults(safeQuery, exactResults) }
                .onFailure { log("Fuzzy fetch failed for query=\"$safeQuery\": ${it.message}") }
                .getOrDefault(emptyList())
            (exactResults + fuzzyResults).distinctBy { it.stopId }.let(::prioritiseStops)
        } else {
            exactResults
        }.take(MAX_STOP_SEARCH_RESULTS)

        results.addAll(
            stopSearchResults.map { SearchStopState.SearchResult.Stop(it.stopName, it.stopId, it.transportModeType) },
        )

        // 2. Search for routes by exact route short name (if enabled)
        // Returns multiple Route results, one per unique headsign (direction)
        if (searchRoutesEnabled) {
            val routeShortName = nswBusRoutesSandook.selectRouteByShortName(safeQuery)
            if (routeShortName != null) {
                val routeResults = buildRouteSearchResults(routeShortName)
                // Add route results at the beginning since they're exact matches
                results.addAll(0, routeResults)
            }
        }

        results
    }

    private fun fetchFuzzyResults(
        query: String,
        exactResults: List<SearchStopState.SearchResult.Stop>,
    ): List<SearchStopState.SearchResult.Stop> {
        val candidates = fetchFuzzyCandidates(query)
        val exactIds = exactResults.map { it.stopId }.toSet()
        return fuzzyStopRanker.rank(query = query, candidates = candidates, topK = MAX_FUZZY_CANDIDATES)
            .filter { it.stopId !in exactIds }
    }

    private fun fetchFuzzyCandidates(query: String): List<SearchStopState.SearchResult.Stop> {
        val normalized = normalize(query)
        val tokens = normalized.split(" ").filter { it.length >= MIN_TOKEN_LENGTH }
        val prefixes = tokens.flatMap { token ->
            buildList {
                add(token.take(NGRAM_LENGTH))
                if (token.length > NGRAM_LENGTH) add(token.substring(NGRAM_OFFSET_1, NGRAM_LENGTH + NGRAM_OFFSET_1))
                if (token.length > NGRAM_LENGTH + NGRAM_OFFSET_1) {
                    add(
                        token.substring(NGRAM_OFFSET_2, NGRAM_LENGTH + NGRAM_OFFSET_2),
                    )
                }
                if (token.length > NGRAM_LENGTH + NGRAM_OFFSET_2) {
                    add(
                        token.substring(NGRAM_OFFSET_3, NGRAM_LENGTH + NGRAM_OFFSET_3),
                    )
                }
            }
        }.distinct().take(MAX_PREFIX_QUERIES)

        val seen = mutableSetOf<String>()
        val candidates = mutableListOf<SearchStopState.SearchResult.Stop>()

        // Seed with high-priority stops so major train stations are always scored,
        // regardless of whether the trigram prefilter finds them.
        for (stopId in highPriorityStopIdList) {
            sandook.selectStops(stopName = stopId, excludeProductClassList = listOf())
                .firstOrNull { it.stopId == stopId }
                ?.let { stop ->
                    seen += stop.stopId
                    candidates += stop.toStopSearchResult()
                }
        }

        // Add trigram-matched candidates. Use a per-prefix cap so a single
        // high-volume trigram (e.g. "hal") cannot consume all 200 slots and
        // prevent the other trigrams from contributing any candidates.
        for (prefix in prefixes) {
            var addedForPrefix = 0
            for (stop in sandook.selectStops(stopName = prefix, excludeProductClassList = listOf())) {
                val canAdd = stop.stopId !in seen &&
                    candidates.size < MAX_FUZZY_CANDIDATES &&
                    addedForPrefix < MAX_CANDIDATES_PER_PREFIX
                if (canAdd) {
                    seen += stop.stopId
                    candidates += stop.toStopSearchResult()
                    addedForPrefix++
                }
                if (addedForPrefix >= MAX_CANDIDATES_PER_PREFIX) break
            }
        }
        return candidates
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
                    ?: TransportMode.Bus,
            )
        }
    }

    // TODO - move to another file and add UT for it. Inject and use.
    override fun prioritiseStops(
        stopResults: List<SearchStopState.SearchResult.Stop>,
    ): List<SearchStopState.SearchResult.Stop> {
        val sortedTransportModes = NswTransportConfig.sortedModes(TransportModeSortOrder.PRIORITY)
        val transportModePriorityMap = sortedTransportModes.mapIndexed { index, transportMode ->
            NswTransportConfig.productClassFor(transportMode) to index
        }.toMap()

        return stopResults.sortedWith(
            compareBy(
                { stopResult ->
                    if (stopResult.stopId in highPriorityStopIdList) 0 else 1
                },
                { stopResult ->
                    stopResult.transportModeType.minOfOrNull {
                        transportModePriorityMap[NswTransportConfig.productClassFor(it)] ?: Int.MAX_VALUE
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
                productClass.toIntOrNull()?.let { NswTransportConfig.modeFromProductClass(it) }
            }
            .toImmutableList()
    }

    // endregion

    companion object {
        private const val MAX_STOP_SEARCH_RESULTS = 50

        // Boundary cap on user-supplied query length. The longest legitimate query
        // observed in 60-day analytics is ~30 chars ("253 cleveland st redfern nsw a");
        // 64 gives ~2x headroom while still rejecting pasted megabyte-sized input
        // before it hits DB LIKE / regex / Levenshtein costs. There's no measurable
        // perf difference between 32 and 64 since all per-candidate work is bounded
        // by candidate-name length, not query length.
        private const val MAX_QUERY_LENGTH = 64

        // Below this length the substring LIKE matches everything and the ranker is noise.
        private const val MIN_QUERY_LENGTH = 2

        private const val MIN_FUZZY_FALLBACK_THRESHOLD = 5
        private const val MAX_FUZZY_CANDIDATES = 200
        private const val MAX_CANDIDATES_PER_PREFIX = 50
        private const val MAX_PREFIX_QUERIES = 4
        private const val MIN_TOKEN_LENGTH = 2
        private const val NGRAM_LENGTH = 3
        private const val NGRAM_OFFSET_1 = 1
        private const val NGRAM_OFFSET_2 = 2
        private const val NGRAM_OFFSET_3 = 3
    }
}
