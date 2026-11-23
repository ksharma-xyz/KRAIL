package xyz.ksharma.krail.trip.planner.ui.parkride.suggestion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeNswParkRideSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.core.testing.helpers.CallLog
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource.UserAdded
import xyz.ksharma.krail.sandook.SandookPreferences.Companion.KEY_PARK_RIDE_SUGGESTION_SHOWN
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.parkride.ParkRideStationAdder
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideMapping
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.StationPosition
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeAppReviewManager
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeParkRideCatalogue
import kotlin.test.assertEquals

/**
 * A rotation destroys the composition and keeps the ViewModel. The card, drawn through the slot the home entry
 * builds, must still be there after a turn if the rider has not answered it, must not be drawn again once they
 * have, and must never write the shown flag a second time. The save and restore is the same one the home flow
 * tests use, and the ViewModel is the real one on the shared fakes, kept across it as a real one is.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [ROTATION_TEST_SDK], qualifiers = "w411dp-h891dp", manifest = Config.NONE)
class ParkRideSuggestionRotationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val callLog = CallLog()
    private val parkRideSandook = FakeNswParkRideSandook()
    private val flag = FakeFlag().apply {
        setFlagValue(FlagKeys.SEARCH_STOP_MAPS_AVAILABLE.key, FlagValue.BooleanValue(true))
    }
    private val viewModel = ParkRideSuggestionViewModel(
        preferences = FakeSandookPreferences(callLog),
        sources = ParkRideSuggestionSources(
            catalogue = FakeParkRideCatalogue(stations = listOf(TALLAWONG)),
            sandook = FakeSandook(),
            parkRideSandook = parkRideSandook,
        ),
        adder = ParkRideStationAdder(parkRideSandook, FakeAnalytics(), FakeAppReviewManager()),
        flag = flag,
        // Unconfined, as the home flow tests have it: there is no TestScope here to advance.
        ioDispatcher = Dispatchers.Unconfined,
    )
    private val restoration by lazy { StateRestorationTester(composeRule) }

    @Test
    fun `an unanswered card is still there after a turn and is not marked shown twice`() {
        showHomeAt(TALLAWONG_AT)
        card().assertExists()
        assertEquals(1, shownWrites())

        repeat(TURNS) { turn() }

        card().assertExists()
        assertEquals(1, shownWrites(), "A turn draws the card again but never writes the flag again")
    }

    @Test
    fun `after Add to home a turn keeps the stations and does not bring the card back`() {
        showHomeAt(TALLAWONG_AT)

        composeRule.onNodeWithTag(TripPlannerTestTags.ParkRideSuggestion.ADD_TO_HOME).performClick()
        composeRule.waitForIdle()
        card().assertDoesNotExist()
        repeat(TURNS) { turn() }

        card().assertDoesNotExist()
        val rows = runBlocking { parkRideSandook.observeSavedParkRidesBySource(UserAdded).first() }
        assertEquals(setOf("26", "27", "28"), rows.map { it.facilityId }.toSet())
        assertEquals(1, shownWrites())
    }

    @Test
    fun `after Not now a turn does not bring the card back and adds nothing`() {
        showHomeAt(TALLAWONG_AT)

        composeRule.onNodeWithTag(TripPlannerTestTags.ParkRideSuggestion.NOT_NOW).performClick()
        composeRule.waitForIdle()
        repeat(TURNS) { turn() }

        card().assertDoesNotExist()
        val rows = runBlocking { parkRideSandook.observeSavedParkRidesBySource(UserAdded).first() }
        assertEquals(emptyList(), rows)
        assertEquals(1, shownWrites())
    }

    @Test
    fun `with no position the card is never drawn, through any number of turns`() {
        viewModel.onEvent(ParkRideSuggestionEvent.LocationChanged(null))
        restoration.setContent { PreviewTheme { Suggestion() } }
        composeRule.waitForIdle()

        repeat(TURNS) { turn() }

        card().assertDoesNotExist()
        assertEquals(0, shownWrites())
    }

    private fun showHomeAt(position: LatLng) {
        viewModel.onEvent(ParkRideSuggestionEvent.LocationChanged(position))
        restoration.setContent { PreviewTheme { Suggestion() } }
        composeRule.waitForIdle()
    }

    private fun turn() {
        restoration.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()
    }

    private fun card() = composeRule.onNodeWithTag(TripPlannerTestTags.ParkRideSuggestion.CARD)

    private fun shownWrites() = callLog.count("pref:$KEY_PARK_RIDE_SUGGESTION_SHOWN=true")

    @Composable
    private fun Suggestion() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        rememberParkRideSuggestionSlot(viewModel, state)?.let { ParkRideSuggestionItem(it) }
    }

    private companion object {
        const val TURNS = 3
        val TALLAWONG_AT = LatLng(-33.7056, 150.9082)

        val TALLAWONG = ParkRideStationPickerItem(
            stationId = "2155384",
            stationName = "Tallawong",
            stopName = "Tallawong Station",
            carParkNames = listOf("Tallawong P1", "Tallawong P2", "Tallawong P3"),
            mappings = listOf(
                ParkRideMapping("2155384", "26", "Tallawong P1"),
                ParkRideMapping("2155384", "27", "Tallawong P2"),
                ParkRideMapping("2155384", "28", "Tallawong P3"),
            ),
            position = StationPosition(TALLAWONG_AT.latitude, TALLAWONG_AT.longitude),
        )
    }
}

private const val ROTATION_TEST_SDK = 34
