package xyz.ksharma.krail.trip.planner.ui.searchstop

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.sandook.NswBusRouteVariants
import xyz.ksharma.krail.sandook.NswBusRoutesSandook
import xyz.ksharma.krail.sandook.NswBusTripOptions
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SavedTrip
import xyz.ksharma.krail.sandook.SelectProductClassesForStop
import xyz.ksharma.krail.sandook.SelectRecentSearchStops
import xyz.ksharma.krail.sandook.SelectServiceAlertsByJourneyId
import xyz.ksharma.krail.sandook.SelectStopsByTripId
import xyz.ksharma.krail.sandook.StopLabels
import xyz.ksharma.krail.trip.planner.ui.searchstop.fuzzy.DefaultFuzzyStopRanker
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StopResultsPrioritisationTest {

    private fun managerWithHighPriorityIds(vararg ids: String): RealStopResultsManager {
        val json = """{"stop_ids": [${ids.joinToString(",") { "\"$it\"" }}]}"""
        val fakeFlag = FakeFlag(
            mapOf(
                FlagKeys.HIGH_PRIORITY_STOP_IDS.key to FlagValue.JsonValue(json),
                FlagKeys.ENABLE_FUZZY_STOP_SEARCH.key to FlagValue.BooleanValue(false),
            ),
        )
        return RealStopResultsManager(
            sandook = NoOpSandook,
            nswBusRoutesSandook = NoOpNswBusRoutesSandook,
            flag = fakeFlag,
            fuzzyStopRanker = DefaultFuzzyStopRanker(),
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

// region fakes

private class FakeFlag(private val values: Map<String, FlagValue>) : Flag {
    override fun getFlagValue(key: String): FlagValue =
        values[key] ?: error("FakeFlag: no value registered for key '$key'")
}

private object NoOpSandook : Sandook {
    override fun observeStopLabels(): Flow<List<StopLabels>> = emptyFlow()
    override fun upsertStopLabel(label: String, emoji: String, stopId: String?, stopName: String?, sortOrder: Long) = error("not used")
    override fun updateStopLabelStop(label: String, stopId: String?, stopName: String?) = error("not used")
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
    override fun selectStopCoordinatesBatch(stopIds: List<String>): Map<String, Pair<Double, Double>> = error("not used")
    override fun insertOrReplaceRecentSearchStop(stopId: String) = error("not used")
    override fun selectRecentSearchStops(): List<SelectRecentSearchStops> = error("not used")
    override fun clearRecentSearchStops() = error("not used")
    override fun cleanupOrphanedRecentSearchStops() = error("not used")
    override fun cleanupOldRecentSearchStops() = error("not used")
}

private object NoOpNswBusRoutesSandook : NswBusRoutesSandook {
    override fun selectRouteByShortName(routeShortName: String): String? = error("not used")
    override fun selectRouteVariantsByShortName(routeShortName: String): List<NswBusRouteVariants> = error("not used")
    override fun selectTripsByRouteId(routeId: String): List<NswBusTripOptions> = error("not used")
    override fun selectTripsByRouteIds(routeIds: List<String>): List<NswBusTripOptions> = error("not used")
    override fun selectStopsByTripId(tripId: String): List<SelectStopsByTripId> = error("not used")
    override fun busRouteGroupsCount(): Int = error("not used")
    override fun insertBusRouteGroup(routeShortName: String) = error("not used")
    override fun insertBusRouteVariant(routeId: String, routeShortName: String, routeName: String) = error("not used")
    override fun insertBusTripOption(tripId: String, routeId: String, headsign: String) = error("not used")
    override fun insertBusTripStop(tripId: String, stopId: String, stopSequence: Int) = error("not used")
    override fun clearNswBusRoutesData() = error("not used")
    override fun insertTransaction(body: () -> Unit) = error("not used")
}

// endregion
