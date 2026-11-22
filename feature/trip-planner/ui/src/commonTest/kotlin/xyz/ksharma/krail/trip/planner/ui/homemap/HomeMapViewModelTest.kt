package xyz.ksharma.krail.trip.planner.ui.homemap

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.connectivity.TransportState
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.core.testing.coroutines.KrailTestScope
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.core.testing.fakes.FakeConnectivityObserver
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.trip.planner.ui.savedtrips.CarParkBlock
import xyz.ksharma.krail.trip.planner.ui.savedtrips.ParkRideTile
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What the home map's ViewModel works out: whether there is a map, whether it is offline, and the
 * Home, Work and `P` pins. Real stops go through the shared fake database, so the position lookup is
 * the real call and not a stub of it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeMapViewModelTest {

    private val sandook = FakeSandook().apply {
        insertNswStop("200070", "Town Hall Station", -33.8732, 151.2070, isParent = null)
        insertNswStop("2155384", "Tallawong Station", -33.7056, 150.9082, isParent = null)
    }
    private val connectivity = FakeConnectivityObserver(initial = TransportState.Up)
    private val flag = FakeFlag()

    private val tallawong = ParkRideTile(
        stationId = "2155384",
        stationName = "Tallawong",
        stopIds = persistentListOf("2155384"),
        totalFreeSpots = 40,
        timeText = "8:05 AM",
        carParks = persistentListOf(
            CarParkBlock("26", "Tallawong P1", 40, 60, "8:05 AM", directionsTo = null),
        ),
    )
    private val home = StopLabel("🏠", "Home", "200070", "Town Hall Station")
    private val homeCleared = StopLabel("🏠", "Home", null, null)

    private fun KrailTestScope.buildViewModel() = HomeMapViewModel(
        sandook = sandook,
        connectivity = connectivity,
        flag = flag,
        ioDispatcher = ioDispatcher,
    )

    /** The screen is in front: something collects the state, as the entry does, until [block] is done. */
    private fun KrailTestScope.whileInFront(viewModel: HomeMapViewModel, block: () -> Unit) {
        val screen = watch(viewModel)
        try {
            block()
        } finally {
            screen.cancel()
        }
    }

    private fun KrailTestScope.watch(viewModel: HomeMapViewModel): Job {
        val job = launch { viewModel.uiState.collect {} }
        runCurrent()
        return job
    }

    private fun mapsOn() = flag.setFlagValue(FlagKeys.SEARCH_STOP_MAPS_AVAILABLE.key, FlagValue.BooleanValue(true))

    private fun HomeMapViewModel.send(vararg labels: StopLabel, tiles: List<ParkRideTile> = emptyList()) =
        onEvent(
            HomeMapEvent.InputsChanged(
                HomeMapInputs(
                    labels = persistentListOf(*labels),
                    tiles = persistentListOf(*tiles.toTypedArray()),
                ),
            ),
        )

    @Test
    fun `with the maps flag off there is no map and no work`() = krailRunTest {
        val viewModel = buildViewModel()

        whileInFront(viewModel) {
            viewModel.send(home, tiles = listOf(tallawong))
            connectivity.goOffline()
            runCurrent()

            val state = viewModel.uiState.value
            assertFalse(state.isMapsAvailable)
            assertFalse(state.mapOffline, "A map that is not there has no offline notice.")
            assertTrue(state.labelPins.isEmpty() && state.parkRidePins.isEmpty())
        }
    }

    @Test
    fun `with the maps flag on the state says so before anything is drawn`() = krailRunTest {
        mapsOn()

        assertTrue(buildViewModel().uiState.value.isMapsAvailable)
    }

    @Test
    fun `a label with a stop becomes a pin at that stop`() = krailRunTest {
        mapsOn()
        val viewModel = buildViewModel()

        whileInFront(viewModel) {
            viewModel.send(home)
            runCurrent()

            val pin = viewModel.uiState.value.labelPins.single()
            assertEquals("Home", pin.labelKey)
            assertEquals(LatLng(-33.8732, 151.2070), pin.position)
        }
    }

    @Test
    fun `a saved station becomes a P pin at its stop`() = krailRunTest {
        mapsOn()
        val viewModel = buildViewModel()

        whileInFront(viewModel) {
            viewModel.send(tiles = listOf(tallawong))
            runCurrent()

            val pin = viewModel.uiState.value.parkRidePins.single()
            assertEquals("2155384", pin.stationId)
            assertEquals(LatLng(-33.7056, 150.9082), pin.position)
        }
    }

    @Test
    fun `the pins follow the labels as they change`() = krailRunTest {
        mapsOn()
        val viewModel = buildViewModel()

        whileInFront(viewModel) {
            viewModel.send(home)
            runCurrent()
            assertEquals(1, viewModel.uiState.value.labelPins.size)

            viewModel.send(homeCleared)
            runCurrent()
            assertTrue(viewModel.uiState.value.labelPins.isEmpty(), "Clearing Home takes its pin off the map.")
        }
    }

    @Test
    fun `labels with no stop ask the database nothing and draw nothing`() = krailRunTest {
        mapsOn()
        val viewModel = buildViewModel()

        whileInFront(viewModel) {
            viewModel.send(*StopLabel.defaults.toTypedArray())
            runCurrent()

            assertTrue(viewModel.uiState.value.labelPins.isEmpty())
        }
    }

    @Test
    fun `the map is offline only while the transport is down`() = krailRunTest {
        mapsOn()
        val viewModel = buildViewModel()

        whileInFront(viewModel) {
            assertFalse(viewModel.uiState.value.mapOffline)

            connectivity.goOffline()
            runCurrent()
            assertTrue(viewModel.uiState.value.mapOffline)

            connectivity.goOnline()
            runCurrent()
            assertFalse(viewModel.uiState.value.mapOffline)
        }
    }

    @Test
    fun `an unknown transport is not called offline`() = krailRunTest {
        mapsOn()
        connectivity.set(TransportState.Unknown)
        val viewModel = buildViewModel()

        whileInFront(viewModel) {
            assertFalse(viewModel.uiState.value.mapOffline)
        }
    }

    @Test
    fun `nothing is worked out for a screen that is not in front`() = krailRunTest {
        mapsOn()
        val viewModel = buildViewModel()
        val screen = watch(viewModel)

        viewModel.send(home)
        runCurrent()
        screen.cancel()
        pumpOnce(PAST_THE_GRACE_MS)
        viewModel.send(homeCleared)
        connectivity.goOffline()
        runCurrent()

        // Nobody is collecting, so the last answer stands and the change waits for the screen to return.
        assertEquals(1, viewModel.uiState.value.labelPins.size)
        assertFalse(viewModel.uiState.value.mapOffline)
    }

    private companion object {
        const val PAST_THE_GRACE_MS = 6_000L
    }
}
