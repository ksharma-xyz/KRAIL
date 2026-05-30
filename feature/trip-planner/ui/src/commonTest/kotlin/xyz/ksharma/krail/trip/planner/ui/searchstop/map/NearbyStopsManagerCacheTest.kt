package xyz.ksharma.krail.trip.planner.ui.searchstop.map

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.krail.core.maps.data.model.NearbyStop
import xyz.ksharma.krail.core.maps.data.repository.NearbyStopsRepository
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapDisplay
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the cache-bypass rule: when the caller's MapUiState has no stops yet,
 * [NearbyStopsManager.loadNearbyStops] must always fetch, even if a prior consumer
 * already populated the cache for the same center.
 *
 * Regression test for the SavedTrips MapStopSelectionPane poisoning the cache shared
 * with SearchStopViewModel, causing SearchStop's initial load to be skipped.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NearbyStopsManagerCacheTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeNearbyStopsRepository
    private lateinit var manager: NearbyStopsManager

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeNearbyStopsRepository()
        manager = createNearbyStopsManager(
            repository = fakeRepository,
            ioDispatcher = testDispatcher,
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fetches when caller has no stops even if cache is warm for same center`() = runTest {
        val center = LatLng(-33.87, 151.21)
        val scope = CoroutineScope(testDispatcher)

        // First consumer loads stops — warms cache for center.
        manager.loadNearbyStops(
            mapState = MapUiState.Ready(mapDisplay = MapDisplay(mapCenter = center)),
            center = center,
            scope = scope,
            onLoadingStateChanged = {},
            onStopsLoaded = {},
            onError = {},
        )
        advanceUntilIdle()
        assertEquals(1, fakeRepository.callCount, "first consumer should fetch")

        // Second consumer at same center but EMPTY stops (fresh ViewModel state) —
        // must NOT use cache; must fetch its own data.
        manager.loadNearbyStops(
            mapState = MapUiState.Ready(mapDisplay = MapDisplay(nearbyStops = persistentListOf(), mapCenter = center)),
            center = center,
            scope = scope,
            onLoadingStateChanged = {},
            onStopsLoaded = {},
            onError = {},
        )
        advanceUntilIdle()
        assertEquals(2, fakeRepository.callCount, "second consumer with empty state should fetch despite warm cache")
    }

    @Test
    fun `skips fetch when caller already has stops and cache is warm`() = runTest {
        val center = LatLng(-33.87, 151.21)
        val scope = CoroutineScope(testDispatcher)
        val existingStop = NearbyStopFeature(
            stopId = "stop-1",
            stopName = "Town Hall",
            position = center,
            transportModes = persistentListOf(),
        )

        // First load warms cache.
        manager.loadNearbyStops(
            mapState = MapUiState.Ready(mapDisplay = MapDisplay(mapCenter = center)),
            center = center,
            scope = scope,
            onLoadingStateChanged = {},
            onStopsLoaded = {},
            onError = {},
        )
        advanceUntilIdle()
        assertEquals(1, fakeRepository.callCount)

        // Same consumer, same center, already has stops in state → cache used, no re-fetch.
        manager.loadNearbyStops(
            mapState = MapUiState.Ready(
                mapDisplay = MapDisplay(
                    nearbyStops = persistentListOf(existingStop),
                    mapCenter = center,
                ),
            ),
            center = center,
            scope = scope,
            onLoadingStateChanged = {},
            onStopsLoaded = {},
            onError = {},
        )
        advanceUntilIdle()
        assertEquals(1, fakeRepository.callCount, "caller with stops should reuse warm cache")
    }
}

private class FakeNearbyStopsRepository : NearbyStopsRepository {
    var callCount = 0
        private set

    override suspend fun getStopsNearby(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double,
        productClasses: Set<Int>,
        maxResults: Int,
    ): List<NearbyStop> {
        callCount++
        return emptyList()
    }
}
