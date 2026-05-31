package xyz.ksharma.krail.trip.planner.ui.mapstopselection

import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.state.mapstopselection.MapStopSelectionEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeNearbyStopsManagerForMap
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class MapStopSelectionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeNearbyStopsManager: FakeNearbyStopsManagerForMap
    private lateinit var viewModel: MapStopSelectionViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeNearbyStopsManager = FakeNearbyStopsManagerForMap()
        viewModel = MapStopSelectionViewModel(
            nearbyStopsManager = fakeNearbyStopsManager,
            scope = CoroutineScope(SupervisorJob() + testDispatcher),
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        fakeNearbyStopsManager.reset()
    }

    @Test
    fun `initial state is Ready`() = runTest {
        viewModel.mapUiState.test {
            assertIs<MapUiState.Ready>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `UserLocationUpdated updates userLocation in state`() = runTest {
        val location = LatLng(latitude = -33.87, longitude = 151.21)

        viewModel.mapUiState.test {
            awaitItem() // initial Ready

            viewModel.onEvent(MapStopSelectionEvent.UserLocationUpdated(location))
            advanceUntilIdle()

            val state = awaitItem() as MapUiState.Ready
            assertEquals(location, state.mapDisplay.userLocation)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `UserLocationUpdated triggers nearby stops load`() = runTest {
        val location = LatLng(latitude = -33.87, longitude = 151.21)

        viewModel.mapUiState.test {
            awaitItem() // activate WhileSubscribed

            viewModel.onEvent(MapStopSelectionEvent.UserLocationUpdated(location))
            advanceUntilIdle()

            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, fakeNearbyStopsManager.loadNearbyStopsCallCount)
    }

    @Test
    fun `MapCenterChanged updates mapCenter in state`() = runTest {
        val center = LatLng(latitude = -33.90, longitude = 151.18)

        viewModel.mapUiState.test {
            awaitItem() // initial Ready

            viewModel.onEvent(MapStopSelectionEvent.MapCenterChanged(center))
            advanceUntilIdle()

            val state = awaitItem() as MapUiState.Ready
            assertEquals(center, state.mapDisplay.mapCenter)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `MapCenterChanged triggers nearby stops load`() = runTest {
        val center = LatLng(latitude = -33.90, longitude = 151.18)

        viewModel.mapUiState.test {
            awaitItem() // activate WhileSubscribed

            viewModel.onEvent(MapStopSelectionEvent.MapCenterChanged(center))
            advanceUntilIdle()

            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, fakeNearbyStopsManager.loadNearbyStopsCallCount)
        assertEquals(center, fakeNearbyStopsManager.lastCenter)
    }

    @Test
    fun `MapCenterChanged uses updated center for load, not stale default`() = runTest {
        val center = LatLng(latitude = -33.90, longitude = 151.18)

        viewModel.mapUiState.test {
            awaitItem()

            viewModel.onEvent(MapStopSelectionEvent.MapCenterChanged(center))
            advanceUntilIdle()

            cancelAndIgnoreRemainingEvents()
        }

        // The center passed to NearbyStopsManager must match the updated mapCenter,
        // not the Sydney default from initial state.
        assertEquals(center, fakeNearbyStopsManager.lastCenter)
    }

    @Test
    fun `UserLocationUpdated with null does not persist non-null location`() = runTest {
        viewModel.mapUiState.test {
            awaitItem() // initial Ready — userLocation is null by default

            viewModel.onEvent(MapStopSelectionEvent.UserLocationUpdated(null))
            advanceUntilIdle()

            cancelAndIgnoreRemainingEvents()
        }

        // Verify null was passed through to the NearbyStopsManager's mapState
        val passedState = fakeNearbyStopsManager.lastMapState
        assertEquals(null, passedState?.mapDisplay?.userLocation)
    }
}
