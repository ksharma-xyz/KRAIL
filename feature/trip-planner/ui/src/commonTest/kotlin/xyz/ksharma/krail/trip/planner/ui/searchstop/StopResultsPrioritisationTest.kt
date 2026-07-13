package xyz.ksharma.krail.trip.planner.ui.searchstop

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.sandook.RecentSearchLocation
import xyz.ksharma.krail.sandook.RecentSearchLocations
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SavedTrip
import xyz.ksharma.krail.sandook.SelectProductClassesForStop
import xyz.ksharma.krail.sandook.SelectServiceAlertsByJourneyId
import xyz.ksharma.krail.sandook.StopLabels
import xyz.ksharma.krail.trip.planner.ui.searchstop.fuzzy.DefaultFuzzyStopRanker
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StopResultsPrioritisationTest {

    private fun managerWithHighPriorityIds(vararg ids: String): RealStopResultsManager {
        val json = """{"stop_ids": [${ids.joinToString(",") { "\"$it\"" }}]}"""
        val fakeFlag = MapBackedFakeFlag(
            mapOf(
                FlagKeys.HIGH_PRIORITY_STOP_IDS.key to FlagValue.JsonValue(json),
                FlagKeys.ENABLE_FUZZY_STOP_SEARCH.key to FlagValue.BooleanValue(false),
            ),
        )
        return RealStopResultsManager(
            sandook = NoOpSandook,
            nswBusRoutesSandook = NoOpBusRoutes,
            flag = fakeFlag,
            fuzzyStopRanker = DefaultFuzzyStopRanker(),
            defaultDispatcher = Dispatchers.Unconfined,
        )
    }

    private fun stop(id: String, name: String, vararg modes: TransportMode) =
        SearchStopState.SearchResult.Stop(
            stopId = id,
            stopName = name,
            transportModeType = persistentListOf(*modes),
        )

    // region high-priority ordering

    @Test
    fun `prioritiseStops places high-priority stop first over higher-ranked transport mode`() {
        val manager = managerWithHighPriorityIds("HP001")

        // Bus is high-priority; Train normally ranks above Bus in NSW priority ordering
        val busHighPriority = stop("HP001", "Bus Terminal", TransportMode.Bus)
        val trainNormal = stop("OTHER", "Central Station", TransportMode.Train)

        val result = manager.prioritiseStops(listOf(trainNormal, busHighPriority))

        assertEquals("HP001", result.first().stopId)
    }

    @Test
    fun `prioritiseStops places all high-priority stops before non-priority stops`() {
        val manager = managerWithHighPriorityIds("HP001", "HP002")

        val hp1 = stop("HP001", "Alpha Bus Stop", TransportMode.Bus)
        val hp2 = stop("HP002", "Beta Bus Stop", TransportMode.Bus)
        val trainNormal = stop("OTHER", "Gamma Train Station", TransportMode.Train)

        val result = manager.prioritiseStops(listOf(trainNormal, hp2, hp1))

        val topTwoIds = result.take(2).map { it.stopId }.toSet()
        assertTrue("HP001" in topTwoIds, "Expected HP001 in top 2 but got $topTwoIds")
        assertTrue("HP002" in topTwoIds, "Expected HP002 in top 2 but got $topTwoIds")
        assertEquals("OTHER", result.last().stopId)
    }

    @Test
    fun `prioritiseStops sorts non-priority stops by transport mode when no high-priority match`() {
        val manager = managerWithHighPriorityIds("HP001")

        val busStop = stop("BUS", "Somewhere Bus Depot", TransportMode.Bus)
        val trainStop = stop("TRAIN", "Central Station", TransportMode.Train)

        val result = manager.prioritiseStops(listOf(busStop, trainStop))

        // Train ranks above Bus in NSW priority — HP001 is absent so transport order applies
        assertEquals("TRAIN", result.first().stopId)
    }

    // region bus is always last among transport modes

    @Test
    fun `prioritiseStops places metro before bus`() {
        val manager = managerWithHighPriorityIds()

        val busStop = stop("BUS", "Random Bus Stop", TransportMode.Bus)
        val metroStop = stop("METRO", "Metro Station", TransportMode.Metro)

        val result = manager.prioritiseStops(listOf(busStop, metroStop))

        assertEquals("METRO", result.first().stopId)
    }

    @Test
    fun `prioritiseStops places light rail before bus`() {
        val manager = managerWithHighPriorityIds()

        val busStop = stop("BUS", "Random Bus Stop", TransportMode.Bus)
        val lightRailStop = stop("LR", "Light Rail Stop", TransportMode.LightRail)

        val result = manager.prioritiseStops(listOf(busStop, lightRailStop))

        assertEquals("LR", result.first().stopId)
    }

    @Test
    fun `prioritiseStops places ferry before bus`() {
        val manager = managerWithHighPriorityIds()

        val busStop = stop("BUS", "Random Bus Stop", TransportMode.Bus)
        val ferryStop = stop("FERRY", "Ferry Wharf", TransportMode.Ferry)

        val result = manager.prioritiseStops(listOf(busStop, ferryStop))

        assertEquals("FERRY", result.first().stopId)
    }

    @Test
    fun `prioritiseStops places coach before bus`() {
        val manager = managerWithHighPriorityIds()

        val busStop = stop("BUS", "Random Bus Stop", TransportMode.Bus)
        val coachStop = stop("COACH", "Coach Terminal", TransportMode.Coach)

        val result = manager.prioritiseStops(listOf(busStop, coachStop))

        assertEquals("COACH", result.first().stopId)
    }

    @Test
    fun `prioritiseStops places all non-bus modes before bus regardless of input order`() {
        val manager = managerWithHighPriorityIds()

        val busStop = stop("BUS", "Bus Interchange", TransportMode.Bus)
        val trainStop = stop("TRAIN", "Central Station", TransportMode.Train)
        val metroStop = stop("METRO", "Metro Station", TransportMode.Metro)
        val lightRailStop = stop("LR", "Light Rail Stop", TransportMode.LightRail)
        val ferryStop = stop("FERRY", "Circular Quay Wharf", TransportMode.Ferry)
        val coachStop = stop("COACH", "Coach Terminal", TransportMode.Coach)

        val result = manager.prioritiseStops(
            listOf(busStop, metroStop, lightRailStop, trainStop, coachStop, ferryStop),
        )

        val busIndex = result.indexOfFirst { it.stopId == "BUS" }
        val nonBusIds = listOf("TRAIN", "METRO", "LR", "FERRY", "COACH")
        nonBusIds.forEach { id ->
            val idx = result.indexOfFirst { it.stopId == id }
            assertTrue(idx < busIndex, "Expected $id (index $idx) before BUS (index $busIndex)")
        }
    }

    @Test
    fun `prioritiseStops sorts non-bus modes among themselves by priority`() {
        val manager = managerWithHighPriorityIds()

        val trainStop = stop("TRAIN", "Central Station", TransportMode.Train)
        val metroStop = stop("METRO", "Metro Station", TransportMode.Metro)
        val lightRailStop = stop("LR", "Light Rail Stop", TransportMode.LightRail)
        val ferryStop = stop("FERRY", "Circular Quay Wharf", TransportMode.Ferry)
        val coachStop = stop("COACH", "Coach Terminal", TransportMode.Coach)

        val result = manager.prioritiseStops(
            listOf(coachStop, ferryStop, lightRailStop, metroStop, trainStop),
        )

        assertEquals(listOf("TRAIN", "METRO", "LR", "FERRY", "COACH"), result.map { it.stopId })
    }

    // endregion

    @Test
    fun `prioritiseStops is idempotent`() {
        val manager = managerWithHighPriorityIds("HP001")

        val highPriority = stop("HP001", "Metro Station", TransportMode.Metro)
        val normal = stop("OTHER", "Bus Stop", TransportMode.Bus)

        val pass1 = manager.prioritiseStops(listOf(normal, highPriority))
        val pass2 = manager.prioritiseStops(pass1)

        assertEquals(pass1.map { it.stopId }, pass2.map { it.stopId })
    }

    @Test
    fun `prioritiseStops returns empty list for empty input`() {
        val manager = managerWithHighPriorityIds("HP001")
        val result = manager.prioritiseStops(emptyList())
        assertTrue(result.isEmpty())
    }

    // endregion
}

// Sandook fake local to this file — prioritiseStops is sync and never touches the DB.
private object NoOpSandook : Sandook {
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
    override fun selectStops(stopName: String, excludeProductClassList: List<Int>): List<SelectProductClassesForStop> = error("not used")
    override fun selectStopsByIds(stopIds: List<String>): List<SelectProductClassesForStop> = error("not used")
    override fun selectStopCoordinatesBatch(stopIds: List<String>): Map<String, Pair<Double, Double>> = error("not used")
    override fun upsertRecentSearchLocation(location: RecentSearchLocation) = error("not used")
    override fun selectRecentSearchLocations(): List<RecentSearchLocations> = error("not used")
    override fun clearRecentSearchLocations() = error("not used")
    override fun cleanupOldRecentSearchLocations() = error("not used")
}
