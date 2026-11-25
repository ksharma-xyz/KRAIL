package xyz.ksharma.krail.trip.planner.ui.searchstop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeNearbyStopsManagerForMap
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeParkRideMapSource
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeRemoteAddressResultsManager
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeStopResultsManager
import xyz.ksharma.krail.trip.planner.ui.testfakes.parkRideCardOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The stop search map marks the Park & Ride stations, and nothing is read for them when there is no map. */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchStopViewModelParkRideTest {

    private val testDispatcher = StandardTestDispatcher()
    private val analytics = FakeAnalytics()
    private val parkRide = FakeParkRideMapSource()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(mapsAvailable: Boolean) = SearchStopViewModel(
        analytics = analytics,
        stopResultsManager = FakeStopResultsManager(),
        remoteAddressResultsManager = FakeRemoteAddressResultsManager(),
        flag = FakeFlag().apply {
            setFlagValue(FlagKeys.SEARCH_STOP_MAPS_AVAILABLE.key, FlagValue.BooleanValue(mapsAvailable))
        },
        nearbyStopsManager = FakeNearbyStopsManagerForMap(),
        ioDispatcher = testDispatcher,
        sandook = FakeSandook(),
        searchSessionStore = RealSearchSessionStore(),
        parkRideMapSource = parkRide,
    )

    @Test
    fun `starting the map reads the stations into it, once`() = runTest {
        val viewModel = viewModel(mapsAvailable = true)
        val job = backgroundScope.launch { viewModel.uiState.collect { } }

        viewModel.onEvent(SearchStopUiEvent.InitializeMap)
        viewModel.onEvent(SearchStopUiEvent.InitializeMap)
        advanceUntilIdle()

        val map = assertIs<MapUiState.Ready>(viewModel.uiState.value.mapUiState)
        assertEquals(listOf("2155384"), map.mapDisplay.parkRideStops.map { it.stopId })
        assertEquals(1, parkRide.stationReads)
        job.cancel()
    }

    @Test
    fun `with no map nothing is read`() = runTest {
        val viewModel = viewModel(mapsAvailable = false)

        viewModel.onEvent(SearchStopUiEvent.InitializeMap)
        advanceUntilIdle()

        assertEquals(0, parkRide.stationReads)
    }

    @Test
    fun `an opened parking card shows on the map and goes when closed, and no stop tap is counted`() = runTest {
        val viewModel = viewModel(mapsAvailable = true)
        val job = backgroundScope.launch { viewModel.uiState.collect { } }
        viewModel.onEvent(SearchStopUiEvent.InitializeMap)
        advanceUntilIdle()

        viewModel.onEvent(SearchStopUiEvent.ParkRideCardOpened("2155384"))
        parkRide.card.value = parkRideCardOf("2155384")
        advanceUntilIdle()
        assertEquals(
            parkRideCardOf("2155384"),
            assertIs<MapUiState.Ready>(viewModel.uiState.value.mapUiState).mapDisplay.parkRideCard,
        )

        viewModel.onEvent(SearchStopUiEvent.ParkRideCardClosed)
        advanceUntilIdle()
        assertNull(assertIs<MapUiState.Ready>(viewModel.uiState.value.mapUiState).mapDisplay.parkRideCard)
        assertTrue(analytics.getTrackedEvents("nearby_stop_click").isEmpty())
        job.cancel()
    }

    @Test
    fun `directions go to the maps app`() = runTest {
        viewModel(mapsAvailable = true)
            .onEvent(SearchStopUiEvent.ParkRideDirectionsClicked(-33.69, 150.9, "Tallawong P1"))

        assertEquals(Triple(-33.69, 150.9, "Tallawong P1"), parkRide.lastDirections)
    }
}
