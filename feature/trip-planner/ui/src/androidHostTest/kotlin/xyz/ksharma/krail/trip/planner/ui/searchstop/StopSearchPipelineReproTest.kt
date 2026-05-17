package xyz.ksharma.krail.trip.planner.ui.searchstop

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SavedTrip
import xyz.ksharma.krail.sandook.SelectProductClassesForStop
import xyz.ksharma.krail.sandook.SelectRecentSearchStops
import xyz.ksharma.krail.sandook.SelectServiceAlertsByJourneyId
import xyz.ksharma.krail.sandook.StopLabels
import xyz.ksharma.krail.trip.planner.ui.searchstop.fuzzy.DefaultFuzzyStopRanker
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * End-to-end reproduction of what the user actually sees on their phone.
 *
 * Unlike FuzzyStopSearchEvalTest (which tests only the ranker in isolation), this drives
 * the REAL [RealStopResultsManager] pipeline — exact DB search, trigram prefilter,
 * fuzzy rank, then prioritiseStops — against the same 37 208 NSW stops the app ships
 * (decoded from io/gtfs/.../NSW_STOPS.pb). The fake DB replicates the real SQL exactly:
 * `stopId = q OR stopName LIKE '%q%' COLLATE NOCASE`, results in file/insertion order.
 */
// Exact prod value of high_priority_stop_ids (mirror of
// RemoteConfigDefaults.HIGH_PRIORITY_STOP_IDS). Keep in sync.
private const val PROD_HIGH_PRIORITY_STOP_IDS =
    """{"stop_ids":["200060","200070","200080","206010","2150106","200017","200039",""" +
        """"201016","201039","201080","200066","200030","200046","200050","2155384",""" +
        """"276220","214710","215020","214110","201510","220010","214810","213510",""" +
        """"200020","2154391","2155383","2155382","2153478","2154392","2153477",""" +
        """"2126158","211310"]}"""

@OptIn(ExperimentalCoroutinesApi::class)
class StopSearchPipelineReproTest {

    private fun csvStops(): List<SelectProductClassesForStop>? {
        val lines = this::class.java.classLoader
            ?.getResourceAsStream("nsw_stops_eval.csv")
            ?.bufferedReader()
            ?.readLines() ?: return null
        return lines.drop(1).filter { it.isNotBlank() }.mapNotNull { line ->
            val c = line.split("|")
            if (c.size < 3) return@mapNotNull null
            SelectProductClassesForStop(
                stopId = c[0], stopName = c[1], stopLat = 0.0, stopLon = 0.0,
                isParent = null, productClasses = c[2],
            )
        }
    }

    private fun manager(
        stops: List<SelectProductClassesForStop>,
        highPriorityJson: String = PROD_HIGH_PRIORITY_STOP_IDS,
    ): RealStopResultsManager {
        val sandook = object : Sandook {
            // Real SQL: exact id OR case-insensitive substring, file order, distinct by id.
            override fun selectStops(stopName: String, excludeProductClassList: List<Int>) =
                stops.filter { it.stopId == stopName || it.stopName.contains(stopName, ignoreCase = true) }
            override fun selectStopsByIds(stopIds: List<String>) =
                stops.filter { it.stopId in stopIds.toSet() }
            override fun selectStopCoordinatesBatch(stopIds: List<String>) = emptyMap<String, Pair<Double, Double>>()
            override fun observeStopLabels(): Flow<List<StopLabels>> = emptyFlow()
            override fun upsertStopLabel(label: String, emoji: String, stopId: String?, stopName: String?, sortOrder: Long) = error("x")
            override fun updateStopLabelStop(label: String, stopId: String?, stopName: String?) = error("x")
            override fun deleteStopLabel(label: String) = error("x")
            override fun clearStopLabels() = error("x")
            override fun insertOrReplaceTheme(productClass: Long) = error("x")
            override fun getProductClass(): Long? = error("x")
            override fun clearTheme() = error("x")
            override fun insertOrReplaceTrip(tripId: String, fromStopId: String, fromStopName: String, toStopId: String, toStopName: String) = error("x")
            override fun deleteTrip(tripId: String) = error("x")
            override fun selectAllTrips(): List<SavedTrip> = error("x")
            override fun observeAllTrips(): Flow<List<SavedTrip>> = emptyFlow()
            override fun selectTripById(tripId: String): SavedTrip? = error("x")
            override fun updateSavedTripSortOrder(tripId: String, sortOrder: Long) = error("x")
            override fun clearSavedTrips() = error("x")
            override fun getAlerts(journeyId: String): List<SelectServiceAlertsByJourneyId> = error("x")
            override fun clearAlerts() = error("x")
            override fun insertAlerts(journeyId: String, alerts: List<SelectServiceAlertsByJourneyId>) = error("x")
            override fun insertNswStop(stopId: String, stopName: String, stopLat: Double, stopLon: Double, isParent: Boolean?) = error("x")
            override fun stopsCount(): Int = error("x")
            override fun productClassCount(): Int = error("x")
            override fun insertNswStopProductClass(stopId: String, productClass: Int) = error("x")
            override fun <R> insertTransaction(block: () -> R): R = error("x")
            override fun clearNswStopsTable() = error("x")
            override fun clearNswProductClassTable() = error("x")
            override fun insertOrReplaceRecentSearchStop(stopId: String) = error("x")
            override fun selectRecentSearchStops(): List<SelectRecentSearchStops> = error("x")
            override fun clearRecentSearchStops() = error("x")
            override fun cleanupOrphanedRecentSearchStops() = error("x")
            override fun cleanupOldRecentSearchStops() = error("x")
        }
        // Mirror the real prod high_priority_stop_ids so the pipeline behaves exactly
        // like the phone (these 15 major stops are seeded into the candidate pool
        // regardless of the trigram prefilter). Must stay in sync with
        // RemoteConfigDefaults.HIGH_PRIORITY_STOP_IDS.
        val flag = FakeFlag(
            mapOf(
                FlagKeys.HIGH_PRIORITY_STOP_IDS.key to FlagValue.JsonValue(highPriorityJson),
                FlagKeys.ENABLE_FUZZY_STOP_SEARCH.key to FlagValue.BooleanValue(true),
            ),
        )
        return RealStopResultsManager(sandook, NoOpBusRoutes, flag, DefaultFuzzyStopRanker(), UnconfinedTestDispatcher())
    }

    /**
     * Representative typo / partial / concatenation scenarios distilled from prod
     * zero-result analytics (PII-free: no street addresses or personal data). Each must
     * surface its intended station/place in the top 5 through the real pipeline. Prints
     * a summary so regressions are easy to read.
     */
    @Test
    fun `typo and partial scenarios resolve to intended stop`() = runTest {
        val stops = csvStops() ?: return@runTest
        val mgr = manager(stops)
        val cases = listOf(
            "cenntral" to "Central Station",
            "central s ta" to "Central Station",
            "toqn hall" to "Town Hall Station",
            "ten hall" to "Town Hall Station",
            "wnyard" to "Wynyard Station",
            "wynard" to "Wynyard Station",
            "resfern" to "Redfern Station",
            "redferb" to "Redfern Station",
            "bakstown" to "Bankstown Station",
            "nlacktown" to "Blacktown Station",
            "blacktpw" to "Blacktown Station",
            "stratjfield" to "Strathfield Station",
            "atrathfield" to "Strathfield Station",
            "achofields" to "Schofields Station",
            "circle quay" to "Circular Quay",
            "parramatta train" to "Parramatta Station",
            "parramatta sta" to "Parramatta Station",
            "concord wet" to "Concord West Station",
            "chatdwood" to "Chatswood Station",
            "castle hill metro" to "Castle Hill Station",
            "kellyville metro" to "Kellyville Station",
            "rouse hill metro" to "Rouse Hill Station",
            "victoria cross station" to "Victoria Cross Station",
            "macquary park" to "Macquarie Park Station",
            "hutstville" to "Hurstville Station",
        )
        val misses = mutableListOf<String>()
        cases.forEach { (q, expect) ->
            val top = mgr.fetchStopResults(q, searchRoutesEnabled = false)
                .filterIsInstance<SearchStopState.SearchResult.Stop>().take(5).map { it.stopName }
            val hit = top.any { it.contains(expect, ignoreCase = true) }
            println("${if (hit) "OK " else "MISS"} \"$q\" -> want \"$expect\" :: $top")
            if (!hit) misses += "\"$q\"→\"$expect\""
        }
        assertTrue(misses.isEmpty(), "Scenarios not resolving in top 5: $misses")
    }

    /**
     * Major stations were being starved out of the fuzzy candidate pool by the trigram
     * prefilter (high-volume trigrams like "par" capped at 50 before reaching the station
     * in DB file order). Adding their primary IDs to high_priority_stop_ids seeds them
     * unconditionally, so typo/partial queries resolve to the station. Guards the prod
     * default in RemoteConfigDefaults (kept in sync with [PROD_HIGH_PRIORITY_STOP_IDS]).
     */
    @Test
    fun `seeded major stations resolve from typo and partial queries`() = runTest {
        val stops = csvStops() ?: return@runTest
        val mgr = manager(stops) // uses PROD_HIGH_PRIORITY_STOP_IDS (incl. the 9 added)
        val cases = listOf(
            "paramatta" to "Parramatta Station",
            "parramatta" to "Parramatta Station",
            "blackyown" to "Blacktown Station",
            "blacktown" to "Blacktown Station",
            "schofiled" to "Schofields Station",
            "sevenhills" to "Seven Hills Station",
            "lidcome" to "Lidcombe Station",
            "redfern" to "Redfern Station",
            "bankstown" to "Bankstown Station",
            "strathfield" to "Strathfield Station",
            "circular quay" to "Circular Quay Station",
        )
        cases.forEach { (q, station) ->
            val top = mgr.fetchStopResults(q, searchRoutesEnabled = false)
                .filterIsInstance<SearchStopState.SearchResult.Stop>().take(5).map { it.stopName }
            assertTrue(
                top.any { it.equals(station, ignoreCase = true) },
                "\"$q\" expected \"$station\" in top 5 but got: $top",
            )
        }
    }

    /**
     * Regression guard for the original "fuzzy search returns nothing" bug, expressed as
     * a small **representative** set — one synthetic-style query per behaviour class — so
     * we are not committing an export of real user analytics to a public repo. Each query
     * must surface its intended stop in the top 5 through the real pipeline.
     */
    @Test
    fun `each fuzzy input class resolves to its intended stop`() = runTest {
        val stops = csvStops() ?: return@runTest
        val mgr = manager(stops)
        // (query, expected substring, behaviour class)
        val cases = listOf(
            Triple("unsw high", "UNSW High", "partial place name"),
            Triple("oxford street", "Oxford St", "street type spelled out"),
            Triple("port macquarie", "Macquarie", "two-word place"),
            Triple("haymarket", "Haymarket", "single token"),
            Triple("chinatown", "Chinatown", "concatenated landmark"),
            Triple("concord west", "Concord West", "suburb + direction"),
            Triple("parramatta wharf", "Parramatta Wharf", "place + facility"),
            Triple("picton", "Picton", "outer station"),
            Triple("kemps creek", "Kemps Creek", "two-word locality"),
            Triple("surry hills", "Surry Hills", "inner suburb"),
            Triple("liverpool hos", "Liverpool Hospital", "abbreviated facility"),
            Triple("sydney airp", "Sydney Airport", "abbreviated landmark"),
            Triple("stockland she", "Stockland Shellharbour", "shopping centre partial"),
        )
        cases.forEach { (q, expect, klass) ->
            val top = mgr.fetchStopResults(q, searchRoutesEnabled = false)
                .filterIsInstance<SearchStopState.SearchResult.Stop>().take(5).map { it.stopName }
            assertTrue(
                top.any { it.contains(expect, ignoreCase = true) },
                "[$klass] \"$q\" expected \"$expect\" in top 5 but got: $top",
            )
        }
    }

    @Test
    fun `cowper street ranks real Cowper St stops first and excludes near-homophones`() = runTest {
        val stops = csvStops()
        if (stops == null) {
            println("SKIP: nsw_stops_eval.csv not found. Run: python3 scripts/extract_nsw_stops.py")
            return@runTest
        }
        val names = manager(stops)
            .fetchStopResults("cowper street", searchRoutesEnabled = false)
            .filterIsInstance<SearchStopState.SearchResult.Stop>()
            .map { it.stopName }
        val top15 = names.take(15)

        // The word-order/leading bonus must put stops whose name *is* Cowper St
        // ("Cowper St at Prince St") at the very top — not landmark-qualifier stops
        // ("Connolly Park, Cowper St"), wrong-road stops ("Page St at Cowper Ave"),
        // or near-homophones ("Cooper St …"). Position 1 is the single Coach-mode
        // "Cowper Coach Stop" (transport-mode grouping is intentional), so we assert
        // on the genuine Cowper-St road stops filling the rest of the top.
        val cowperStLeading = top15.count { it.startsWith("Cowper St", ignoreCase = true) }
        assertTrue(
            cowperStLeading >= 12,
            "Expected ≥12 of top 15 to be 'Cowper St …' stops but got $cowperStLeading. Top 15: $top15",
        )

        // None of these weaker matches may appear in the top 15: near-homophones
        // (Cooper / Cowpasture), wrong Cowper road type (Ave/Rd/Dr/Cir), or stops
        // where Cowper St is only a trailing cross-street qualifier.
        listOf(
            Regex("(?i)cowpasture"),
            Regex("(?i)\\bcooper\\b"),
            Regex("(?i)cowper (ave|rd|dr|cir)\\b"),
            Regex("(?i), cowper st"),
            Regex("(?i)(before|after|opp|at) cowper st"),
        ).forEach { weak ->
            val leaked = top15.filter { weak.containsMatchIn(it) }
            assertTrue(
                leaked.isEmpty(),
                "Weak match /$weak/ must not be in top 15 for 'cowper street' but was: $leaked",
            )
        }
    }
}
