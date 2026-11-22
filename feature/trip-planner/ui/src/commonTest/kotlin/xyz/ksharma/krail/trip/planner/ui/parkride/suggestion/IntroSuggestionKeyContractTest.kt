package xyz.ksharma.krail.trip.planner.ui.parkride.suggestion

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.core.testing.coroutines.KrailTestScope
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeNswParkRideSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.trip.planner.ui.intro.IntroCompletion
import xyz.ksharma.krail.trip.planner.ui.parkride.ParkRideStationAdder
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideMapping
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.StationPosition
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeAppReviewManager
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeParkRideCatalogue
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The one first-run key the Intro and the map home share. The Intro writes `KEY_PARK_RIDE_SUGGESTION_SHOWN`
 * when it has asked the Park & Ride question (`IntroCompletion.writeSuggestionShown`), and the map home's
 * `ParkRideSuggestionViewModel` reads the same preference before it offers its card, and writes it when the
 * card is first drawn. Each side has its own test against its own half. This one runs the real Intro write
 * and the real suggestion ViewModel over one preferences store, so renaming or splitting the key on either
 * side fails here, and a rider is never asked about Park & Ride twice.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IntroSuggestionKeyContractTest {

    private val preferences = FakeSandookPreferences()
    private val sandook = FakeSandook()
    private val parkRideSandook = FakeNswParkRideSandook()
    private val analytics = FakeAnalytics()
    private val flag = FakeFlag().apply {
        setFlagValue(FlagKeys.SEARCH_STOP_MAPS_AVAILABLE.key, FlagValue.BooleanValue(true))
    }

    // Every screen the test puts in front is taken away again when the test ends, as the screen leaving would.
    private val screens = mutableListOf<Job>()

    private fun KrailTestScope.suggestionViewModel() = ParkRideSuggestionViewModel(
        preferences = preferences,
        sources = ParkRideSuggestionSources(
            catalogue = FakeParkRideCatalogue(stations = listOf(TALLAWONG)),
            sandook = sandook,
            parkRideSandook = parkRideSandook,
        ),
        adder = ParkRideStationAdder(parkRideSandook, analytics, FakeAppReviewManager()),
        flag = flag,
        ioDispatcher = ioDispatcher,
    )

    private fun KrailTestScope.introCompletion() = IntroCompletion(
        sandook = sandook,
        preferences = preferences,
        analytics = analytics,
        ioDispatcher = ioDispatcher,
    )

    /** A home screen that opens now with the rider standing at the station: the view model it gets. */
    private fun KrailTestScope.homeWithRiderAtStation(): ParkRideSuggestionViewModel {
        val viewModel = suggestionViewModel()
        screens += launch { viewModel.uiState.collect {} }
        runCurrent()
        viewModel.onEvent(ParkRideSuggestionEvent.LocationChanged(TALLAWONG_AT))
        runCurrent()
        return viewModel
    }

    private fun contractTest(body: suspend KrailTestScope.() -> Unit) = krailRunTest {
        try {
            body()
        } finally {
            screens.forEach { it.cancel() }
        }
    }

    @Test
    fun `a rider the Intro has not asked is offered the home card`() = contractTest {
        val home = homeWithRiderAtStation()

        assertNotNull(home.uiState.value.tile, "The control: with the key unset a station at the rider is offered")
    }

    @Test
    fun `a rider the Intro asked is never offered the home card`() = contractTest {
        introCompletion().writeSuggestionShown()
        runCurrent()

        val home = homeWithRiderAtStation()

        assertNull(home.uiState.value.tile, "The Intro's write is the key the suggestion reads")
    }

    @Test
    fun `a card drawn once marks the key, so the next home is not offered it`() = contractTest {
        val first = homeWithRiderAtStation()
        val tile = assertNotNull(first.uiState.value.tile)
        first.onEvent(ParkRideSuggestionEvent.Shown(tile.stationId))
        runCurrent()

        val next = homeWithRiderAtStation()

        assertNull(next.uiState.value.tile, "A second home after the card was drawn is not offered it")
    }

    private companion object {
        val TALLAWONG_AT = LatLng(-33.7056, 150.9082)

        val TALLAWONG = ParkRideStationPickerItem(
            stationId = "2155384",
            stationName = "Tallawong",
            stopName = "Tallawong Station",
            carParkNames = listOf("Tallawong P1"),
            mappings = listOf(ParkRideMapping("2155384", "26", "Tallawong P1")),
            position = StationPosition(TALLAWONG_AT.latitude, TALLAWONG_AT.longitude),
        )
    }
}
