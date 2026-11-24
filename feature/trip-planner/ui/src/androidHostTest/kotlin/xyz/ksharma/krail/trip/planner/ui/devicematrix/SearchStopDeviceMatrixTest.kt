package xyz.ksharma.krail.trip.planner.ui.devicematrix

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.testing.device.DeviceMatrixRule
import xyz.ksharma.krail.core.testing.device.DeviceMatrixScope
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.searchstop.SearchStopFixtures
import xyz.ksharma.krail.trip.planner.ui.searchstop.SearchStopScreen
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapDisplay
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * Search Stop on every device profile (`docs/testing/DEVICE_PROFILES.md`), in the map state, with its Options card open, and in
 * the list state. The iPhone Duo outer screen and the landscape phones deliver a side inset that a screen padding by the display
 * cutout would miss, which hid the swap and the go circle under the status pill (`SearchStopSideInsetLayoutTest`). Here the whole
 * screen is held to it on every device, in the single pane and, on the wide windows, in the dual pane.
 *
 * The map is replaced by a placeholder in a host test, so its own controls (the location control and the info icon) are not
 * measured here; they sit on the map's own insets and are covered on a device.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class SearchStopDeviceMatrixTest {

    @get:Rule
    val matrix = DeviceMatrixRule()

    private val central = StopItem(stopName = "Central Station", stopId = "stop_central")
    private val townHall = StopItem(stopName = "Town Hall", stopId = "stop_town_hall")

    private fun DeviceMatrixScope.showScreen(locationAllowed: Boolean) {
        val display = if (locationAllowed) MapDisplay(userLocation = LatLng(-33.8688, 151.2093)) else MapDisplay()
        val state: SearchStopState = SearchStopFixtures.recentWithHomeSet()
            .copy(isMapsAvailable = true, mapUiState = MapUiState.Ready(mapDisplay = display))
        setContent(theme = { QuietBareTheme(it) }) {
            SearchStopScreen(
                searchStopState = state,
                presetFrom = central,
                presetTo = townHall,
                dualPaneMapUiState = MapUiState.Ready(mapDisplay = display),
            )
        }
    }

    @Test
    fun `the map state keeps the box, the swap, the go circle and the location notice inside the safe area`() =
        matrix.forEachDeviceProfile(screen = "SearchStopScreen") {
            showScreen(locationAllowed = false)

            assertNothingInteractiveUnderSystemBars()
            assertTouchReaches(onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_REVERSE))
            assertTouchReaches(onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_SEARCH))
        }

    @Test
    fun `the Options button and its open card stay inside the safe area`() =
        matrix.forEachDeviceProfile(screen = "SearchStopScreen") {
            showScreen(locationAllowed = true)

            assertNothingInteractiveUnderSystemBars()
            // The Options pill is a map state control. The dual pane has the list in its place, and no pill.
            if (onAllNodesWithTagCount(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON) > 0) {
                assertTouchReaches(onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_MODE_BUTTON))
                rule.waitForIdle()
                assertNothingInteractiveUnderSystemBars()
            }
        }

    @Test
    fun `the list state keeps the box, the recents and Select on map inside the safe area and scrolls clear of the bottom bar`() =
        matrix.forEachDeviceProfile(screen = "SearchStopScreen") {
            showScreen(locationAllowed = false)
            onNodeWithTag(TripPlannerTestTags.SEARCH_ROW_FROM).performClick()
            rule.waitForIdle()

            assertNothingInteractiveUnderSystemBars()
            assertScrollEndClearsBottomBar(onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_RECENTS_LIST))
        }

    private fun DeviceMatrixScope.onAllNodesWithTagCount(tag: String): Int =
        onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().size
}

private val QuietKeyboard = object : SoftwareKeyboardController {
    override fun show() = Unit
    override fun hide() = Unit
}

/** The bare theme with the keyboard held down, as the screen's own tests hold it: a host test has no keyboard to raise. */
@Composable
private fun QuietBareTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSoftwareKeyboardController provides QuietKeyboard) {
        BareKrailTheme(content = content)
    }
}
