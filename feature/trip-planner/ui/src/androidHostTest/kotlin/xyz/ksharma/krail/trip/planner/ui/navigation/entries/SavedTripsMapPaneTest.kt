package xyz.ksharma.krail.trip.planner.ui.navigation.entries

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.aagya.permission.PermissionStatus
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.testfakes.FakeUserLocationManager
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The location permission notice on the wide window's map pane: the Search Stop chip at the map's top left
 * corner while the permission is not granted, and nothing once it is. The map itself is held in its loading
 * state, which draws no native map.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE, qualifiers = "w600dp-h900dp")
class SavedTripsMapPaneTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `while the permission is not granted the chip sits at the map's top left corner`() {
        setPane(PermissionStatus.NotDetermined)

        composeRule.onNodeWithText("Location permission required", useUnmergedTree = true).assertExists()
        val chip = composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        assertEquals(16.dp, chip.left, "The chip is on the page inset from the pane's left edge.")
        assertTrue(chip.top >= 8.dp, "The chip is below the pane's top safe inset: $chip")
        assertTrue(chip.top < 200.dp, "The chip is at the top of the pane: $chip")
    }

    @Test
    fun `once the permission is granted there is no chip`() {
        setPane(PermissionStatus.Granted)

        composeRule.onNodeWithTag(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL).assertDoesNotExist()
    }

    private fun setPane(status: PermissionStatus) {
        composeRule.setContent {
            PreviewTheme {
                SavedTripsMapPane(
                    mapUiState = MapUiState.Loading,
                    userLocationManager = FakeUserLocationManager(permissionStatus = status),
                    onMapEvent = {},
                    onStopSelected = {},
                    labels = persistentListOf(),
                    onEvent = {},
                )
            }
        }
        composeRule.waitForIdle()
    }
}
