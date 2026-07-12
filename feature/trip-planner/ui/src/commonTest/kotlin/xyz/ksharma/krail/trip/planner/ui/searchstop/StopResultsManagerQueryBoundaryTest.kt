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
import xyz.ksharma.krail.trip.planner.ui.searchstop.fuzzy.FuzzyStopRanker
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Boundary-condition tests for [RealStopResultsManager.fetchStopResults]:
 * input trimming, length capping, short-circuit on too-short queries, and
 * partial-success when the fuzzy ranker throws.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StopResultsManagerQueryBoundaryTest {

    @Test
    fun `single character query never triggers substring LIKE search`() = runTest {
        // Whether the curated short-query path returns matches or not, single-letter
        // queries must skip selectStops (the '%a%' path) to avoid the result-set flood.
        val sandook = RecordingSandook()
        val manager = manager(sandook)

        manager.fetchStopResults("a", searchRoutesEnabled = false)

        assertTrue(
            sandook.selectStopsCalls.isEmpty(),
            "Expected no substring LIKE call for 1-char query, got: ${sandook.selectStopsCalls}",
        )
    }

    @Test
    fun `single character query returns empty when no high-priority stops are configured`() = runTest {
        val sandook = RecordingSandook()
        val manager = manager(sandook, highPriorityIds = emptyList())

        val result = manager.fetchStopResults("a", searchRoutesEnabled = false)

        assertTrue(result.isEmpty())
        assertTrue(
            sandook.selectStopsByIdsCalls.isEmpty(),
            "Expected no DB lookup when high-priority list is empty",
        )
    }

    @Test
    fun `single character query surfaces high-priority stops whose words start with the letter`() = runTest {
        val stops = listOf(
            stopRow("HP1", "Central Station"),
            stopRow("HP2", "Town Hall Station"),
            stopRow("HP3", "Circular Quay"),
            stopRow("HP4", "Macquarie University"),
        )
        val sandook = RecordingSandook(returns = stops)
        val manager = manager(
            sandook,
            highPriorityIds = listOf("HP1", "HP2", "HP3", "HP4"),
        )

        val result = manager.fetchStopResults("c", searchRoutesEnabled = false)

        val resultIds = result.filterIsInstance<SearchStopState.SearchResult.Stop>().map { it.stopId }
        // Central + Circular Quay start with 'c'. Macquarie has no word starting with 'c'
        // (substring LIKE would have wrongly surfaced it). Town Hall has none either.
        assertEquals(setOf("HP1", "HP3"), resultIds.toSet())
        assertEquals(
            listOf(listOf("HP1", "HP2", "HP3", "HP4")),
            sandook.selectStopsByIdsCalls,
            "Expected one batch lookup of the configured high-priority list",
        )
    }

    @Test
    fun `single character query matches mid-name word prefix as well as leading prefix`() = runTest {
        val stops = listOf(
            stopRow("HP1", "Town Hall Station"),
            stopRow("HP2", "Mascot"),
        )
        val sandook = RecordingSandook(returns = stops)
        val manager = manager(sandook, highPriorityIds = listOf("HP1", "HP2"))

        val result = manager.fetchStopResults("h", searchRoutesEnabled = false)

        val resultIds = result.filterIsInstance<SearchStopState.SearchResult.Stop>().map { it.stopId }
        // 'h' matches the second word "Hall" in Town Hall Station, not Mascot.
        assertEquals(listOf("HP1"), resultIds)
    }

    @Test
    fun `whitespace-only query returns empty without DB call`() = runTest {
        val sandook = RecordingSandook()
        val manager = manager(sandook)

        val result = manager.fetchStopResults("   ", searchRoutesEnabled = false)

        assertTrue(result.isEmpty())
        assertTrue(sandook.selectStopsCalls.isEmpty())
    }

    @Test
    fun `empty query returns empty without DB call`() = runTest {
        val sandook = RecordingSandook()
        val manager = manager(sandook)

        val result = manager.fetchStopResults("", searchRoutesEnabled = false)

        assertTrue(result.isEmpty())
        assertTrue(sandook.selectStopsCalls.isEmpty())
    }

    @Test
    fun `query is trimmed before DB lookup`() = runTest {
        val sandook = RecordingSandook()
        val manager = manager(sandook)

        manager.fetchStopResults("  central  ", searchRoutesEnabled = false)

        assertEquals(listOf("central"), sandook.selectStopsCalls)
    }

    @Test
    fun `query longer than max length is truncated to 64 chars before DB`() = runTest {
        val sandook = RecordingSandook()
        val manager = manager(sandook)

        manager.fetchStopResults("a".repeat(200), searchRoutesEnabled = false)

        assertEquals(1, sandook.selectStopsCalls.size)
        assertEquals(MAX_QUERY_LENGTH_EXPECTED, sandook.selectStopsCalls.first().length)
    }

    @Test
    fun `query at max length passes through unmodified`() = runTest {
        val sandook = RecordingSandook()
        val manager = manager(sandook)
        val maxLengthQuery = "a".repeat(MAX_QUERY_LENGTH_EXPECTED)

        manager.fetchStopResults(maxLengthQuery, searchRoutesEnabled = false)

        assertEquals(listOf(maxLengthQuery), sandook.selectStopsCalls)
    }

    @Test
    fun `two-character query is allowed and reaches DB`() = runTest {
        val sandook = RecordingSandook()
        val manager = manager(sandook)

        manager.fetchStopResults("ce", searchRoutesEnabled = false)

        assertEquals(listOf("ce"), sandook.selectStopsCalls)
    }

    @Test
    fun `fuzzy ranker throwing does not break exact results`() = runTest {
        val exactRow = stopRow("EX1", "Central Station")
        val sandook = RecordingSandook(returns = listOf(exactRow))
        val throwingRanker = object : FuzzyStopRanker {
            override fun rank(
                query: String,
                candidates: List<SearchStopState.SearchResult.Stop>,
                topK: Int,
            ): List<SearchStopState.SearchResult.Stop> = error("simulated ranker failure")
        }
        val manager = manager(sandook, ranker = throwingRanker, fuzzyEnabled = true)

        val result = manager.fetchStopResults("ce", searchRoutesEnabled = false)

        // Exact path still surfaces "Central Station" even though fuzzy threw.
        val resultIds = result.filterIsInstance<SearchStopState.SearchResult.Stop>().map { it.stopId }
        assertTrue(
            "EX1" in resultIds,
            "Expected exact result EX1 to survive ranker failure, got: $resultIds",
        )
    }

    @Test
    fun `high-priority stops use single batch DB call instead of N round-trips`() = runTest {
        // Three high-priority IDs; with the batch fix this is one selectStopsByIds call,
        // not three selectStops calls.
        val sandook = RecordingSandook()
        val manager = manager(sandook, fuzzyEnabled = true, highPriorityIds = listOf("HP1", "HP2", "HP3"))

        manager.fetchStopResults("ce", searchRoutesEnabled = false)

        assertEquals(
            listOf(listOf("HP1", "HP2", "HP3")),
            sandook.selectStopsByIdsCalls,
            "Expected exactly one batch lookup for all 3 high-priority IDs",
        )
    }

    // -- helpers --

    private fun manager(
        sandook: Sandook,
        ranker: FuzzyStopRanker = DefaultFuzzyStopRanker(),
        fuzzyEnabled: Boolean = false,
        highPriorityIds: List<String> = emptyList(),
    ): RealStopResultsManager {
        val idsJson = highPriorityIds.joinToString(",") { "\"$it\"" }
        val fakeFlag = MapBackedFakeFlag(
            mapOf(
                FlagKeys.HIGH_PRIORITY_STOP_IDS.key to FlagValue.JsonValue("""{"stop_ids":[$idsJson]}"""),
                FlagKeys.ENABLE_FUZZY_STOP_SEARCH.key to FlagValue.BooleanValue(fuzzyEnabled),
            ),
        )
        return RealStopResultsManager(
            sandook = sandook,
            nswBusRoutesSandook = NoOpBusRoutes,
            flag = fakeFlag,
            fuzzyStopRanker = ranker,
            defaultDispatcher = UnconfinedTestDispatcher(),
        )
    }

    private fun stopRow(id: String, name: String) = SelectProductClassesForStop(
        stopId = id,
        stopName = name,
        stopLat = 0.0,
        stopLon = 0.0,
        isParent = null,
        productClasses = "",
    )

    private companion object {
        // Mirrors RealStopResultsManager.MAX_QUERY_LENGTH; kept local so test breaks
        // loudly if the production cap changes without an intentional test update.
        const val MAX_QUERY_LENGTH_EXPECTED = 64
    }
}

private class RecordingSandook(
    private val returns: List<SelectProductClassesForStop> = emptyList(),
) : Sandook {
    val selectStopsCalls = mutableListOf<String>()
    val selectStopsByIdsCalls = mutableListOf<List<String>>()

    override fun selectStops(
        stopName: String,
        excludeProductClassList: List<Int>,
    ): List<SelectProductClassesForStop> {
        selectStopsCalls += stopName
        return returns
    }

    override fun selectStopsByIds(stopIds: List<String>): List<SelectProductClassesForStop> {
        selectStopsByIdsCalls += stopIds
        val wanted = stopIds.toSet()
        return returns.filter { it.stopId in wanted }
    }

    override fun observeStopLabels(): Flow<List<StopLabels>> = emptyFlow()
    override fun upsertStopLabel(label: String, emoji: String, stopId: String?, stopName: String?, sortOrder: Long) = error("not used")
    override fun updateStopLabelStop(label: String, stopId: String?, stopName: String?) = error("not used")
    override fun renameStopLabel(label: String, newLabel: String) = error("not used")
    override fun deleteStopLabel(label: String) = error("not used")
    override fun clearStopLabels() = error("not used")
    override fun insertOrReplaceTheme(productClass: Long) = error("not used")
    override fun getProductClass(): Long? = error("not used")
    override fun clearTheme() = error("not used")
    override fun insertOrReplaceTrip(tripId: String, fromStopId: String, fromStopName: String, toStopId: String, toStopName: String) = error("not used")
    override fun deleteTrip(tripId: String) = error("not used")
    override fun selectAllTrips(): List<SavedTrip> = error("not used")
    override fun observeAllTrips(): Flow<List<SavedTrip>> = emptyFlow()
    override fun selectTripById(tripId: String): SavedTrip? = error("not used")
    override fun updateSavedTripSortOrder(tripId: String, sortOrder: Long) = error("not used")
    override fun clearSavedTrips() = error("not used")
    override fun getAlerts(journeyId: String): List<SelectServiceAlertsByJourneyId> = error("not used")
    override fun clearAlerts() = error("not used")
    override fun insertAlerts(journeyId: String, alerts: List<SelectServiceAlertsByJourneyId>) = error("not used")
    override fun insertNswStop(stopId: String, stopName: String, stopLat: Double, stopLon: Double, isParent: Boolean?) = error("not used")
    override fun stopsCount(): Int = error("not used")
    override fun productClassCount(): Int = error("not used")
    override fun insertNswStopProductClass(stopId: String, productClass: Int) = error("not used")
    override fun <R> insertTransaction(block: () -> R): R = error("not used")
    override fun clearNswStopsTable() = error("not used")
    override fun clearNswProductClassTable() = error("not used")
    override fun selectStopCoordinatesBatch(stopIds: List<String>): Map<String, Pair<Double, Double>> = error("not used")
    override fun insertOrReplaceRecentSearchStop(stopId: String) = error("not used")
    override fun selectRecentSearchStops(): List<SelectRecentSearchStops> = error("not used")
    override fun clearRecentSearchStops() = error("not used")
    override fun cleanupOrphanedRecentSearchStops() = error("not used")
    override fun cleanupOldRecentSearchStops() = error("not used")
}
