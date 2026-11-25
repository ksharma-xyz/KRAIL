package xyz.ksharma.krail.trip.planner.ui.mapstopselection

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import xyz.ksharma.krail.core.maps.data.model.NearbyStop
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.trip.planner.ui.state.mapstopselection.MapStopSelectionEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeNearbyStopsManagerForMap
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeParkRideMapSource
import xyz.ksharma.krail.trip.planner.ui.testfakes.parkRideCardOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** The wide screen map marks the Park & Ride stations, and only a map on screen makes it read them. */
@OptIn(ExperimentalCoroutinesApi::class)
class MapStopSelectionParkRideTest {

    private val testDispatcher = StandardTestDispatcher()
    private val nearbyStops = FakeNearbyStopsManagerForMap()
    private val parkRide = FakeParkRideMapSource()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = MapStopSelectionViewModel(
        nearbyStopsManager = nearbyStops,
        parkRideMapSource = parkRide,
        ioDispatcher = testDispatcher,
    )

    private fun MapStopSelectionViewModel.display() = assertIs<MapUiState.Ready>(mapUiState.value).mapDisplay

    private fun pan(latitude: Double) = MapStopSelectionEvent.MapCenterChanged(LatLng(latitude, 151.2))

    @Test
    fun `nothing is read until a map sends an event`() = runTest {
        viewModel()
        advanceUntilIdle()

        assertEquals(0, parkRide.stationReads)
    }

    @Test
    fun `the stations are read once, however often the map moves`() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(pan(-33.8))
        viewModel.onEvent(pan(-33.9))
        advanceUntilIdle()
        viewModel.onEvent(pan(-34.0))
        advanceUntilIdle()

        assertEquals(1, parkRide.stationReads)
    }

    @Test
    fun `nearby stops loading afterwards keeps the stations`() = runTest {
        nearbyStops.stopsToReturn = listOf(
            NearbyStop(
                stopId = "200060",
                stopName = "Central",
                latitude = -33.88,
                longitude = 151.2,
                transportModes = emptyList(),
            ),
        )
        val viewModel = viewModel()
        val job = backgroundScope.launch { viewModel.mapUiState.collect { } }

        viewModel.onEvent(pan(-33.8))
        advanceUntilIdle()
        viewModel.onEvent(pan(-33.9))
        advanceUntilIdle()

        assertEquals(listOf("2155384"), viewModel.display().parkRideStops.map { it.stopId })
        assertEquals(listOf("200060"), viewModel.display().nearbyStops.map { it.stopId })
        job.cancel()
    }

    @Test
    fun `an opened parking card shows, follows its numbers and goes when closed`() = runTest {
        val viewModel = viewModel()
        val job = backgroundScope.launch { viewModel.mapUiState.collect { } }

        viewModel.onEvent(MapStopSelectionEvent.ParkRideCardOpened("2155384"))
        parkRide.card.value = parkRideCardOf("2155384", isLoading = true)
        advanceUntilIdle()
        assertEquals(parkRideCardOf("2155384", isLoading = true), viewModel.display().parkRideCard)

        parkRide.card.value = parkRideCardOf("2155384")
        advanceUntilIdle()
        assertEquals(parkRideCardOf("2155384"), viewModel.display().parkRideCard)

        viewModel.onEvent(MapStopSelectionEvent.ParkRideCardClosed)
        parkRide.card.value = parkRideCardOf("2155384", isLoading = true)
        advanceUntilIdle()
        assertNull(viewModel.display().parkRideCard)
        assertEquals(listOf("2155384"), parkRide.cardsAskedFor)
        job.cancel()
    }

    @Test
    fun `directions go to the maps app`() = runTest {
        viewModel().onEvent(MapStopSelectionEvent.ParkRideDirectionsClicked(-33.69, 150.9, "Tallawong P1"))

        assertEquals(Triple(-33.69, 150.9, "Tallawong P1"), parkRide.lastDirections)
    }
}
