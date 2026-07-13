package xyz.ksharma.krail.trip.planner.ui.searchstop

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.trip.planner.ui.searchstop.fuzzy.DefaultFuzzyStopRanker
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.LocationKind
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class RealStopResultsManagerRecentLocationsTest {

    @Test
    fun `address selection persists its display metadata for recents`() = runTest {
        val manager = manager(FakeSandook())

        manager.setSelectedFromStop(
            StopItem(
                stopId = "streetID:123",
                stopName = "123 Example St, Sydney",
                locationKind = LocationKind.ADDRESS,
                addressType = "singlehouse",
            ),
        )

        val recent = manager.recentSearchStops().single()

        assertEquals("streetID:123", recent.stopId)
        assertEquals("123 Example St, Sydney", recent.stopName)
        assertEquals(LocationKind.ADDRESS, recent.locationKind)
        assertEquals("singlehouse", recent.addressType)
    }

    @Test
    fun `selecting the same location moves it to the top without duplication`() = runTest {
        val manager = manager(FakeSandook())
        val address = StopItem(
            stopId = "poiID:museum",
            stopName = "Museum of Sydney",
            locationKind = LocationKind.ADDRESS,
            addressType = "poi",
        )
        val secondAddress = StopItem(
            stopId = "streetID:456",
            stopName = "456 Example St, Sydney",
            locationKind = LocationKind.ADDRESS,
            addressType = "street",
        )

        manager.setSelectedFromStop(address)
        manager.setSelectedToStop(secondAddress)
        manager.setSelectedFromStop(address)

        assertEquals(
            listOf("poiID:museum", "streetID:456"),
            manager.recentSearchStops().map { it.stopId },
        )
    }

    @Test
    fun `mixed locations share a five item recency cap`() = runTest {
        val sandook = FakeSandook().apply {
            insertNswStop(
                stopId = "200060",
                stopName = "Central Station",
                stopLat = 0.0,
                stopLon = 0.0,
                isParent = null,
            )
            insertNswStopProductClass("200060", 1)
        }
        val manager = manager(sandook)

        manager.setSelectedFromStop(StopItem("Central Station", "200060"))
        (1..5).forEach { index ->
            manager.setSelectedToStop(
                StopItem(
                    stopId = "streetID:$index",
                    stopName = "$index Example St, Sydney",
                    locationKind = LocationKind.ADDRESS,
                    addressType = "street",
                ),
            )
        }

        val recents = manager.recentSearchStops()

        assertEquals(5, recents.size)
        assertEquals((5 downTo 1).map { "streetID:$it" }, recents.map { it.stopId })
    }

    private fun manager(sandook: FakeSandook): RealStopResultsManager = RealStopResultsManager(
        sandook = sandook,
        nswBusRoutesSandook = NoOpBusRoutes,
        flag = MapBackedFakeFlag(
            mapOf(
                FlagKeys.HIGH_PRIORITY_STOP_IDS.key to FlagValue.JsonValue("{\"stop_ids\":[]}"),
                FlagKeys.ENABLE_FUZZY_STOP_SEARCH.key to FlagValue.BooleanValue(false),
            ),
        ),
        fuzzyStopRanker = DefaultFuzzyStopRanker(),
        defaultDispatcher = UnconfinedTestDispatcher(),
    )
}
