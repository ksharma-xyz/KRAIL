package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * A Park & Ride card that is open has to still be open after the device is rotated.
 *
 * The rider's half of that state lives in the screen; the ViewModel's half — the set of stops
 * it keeps refreshing — lives in a ViewModel that survives recreation either way. So losing
 * the screen's half does not merely redraw the cards closed: it puts the two halves out of
 * step, and because the next tap is read against a map that has forgotten everything, it sends
 * "expand" for a stop that is already expanding. There is then no gesture left that removes
 * that stop from the refresh set.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [RESTORE_TEST_SDK],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class SavedTripsParkRideRestoreTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val restorationTester by lazy { StateRestorationTester(composeRule) }

    private val events = mutableListOf<SavedTripUiEvent>()

    @Test
    fun `an open park and ride card is still open after the screen is recreated`() {
        showSavedTrips()

        composeRule.onNodeWithText(STOP_NAME).performClick()
        composeRule.waitForIdle()

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(FACILITY_NAME).assertIsDisplayed()
    }

    @Test
    fun `tapping the card after recreation closes it rather than opening it again`() {
        showSavedTrips()

        composeRule.onNodeWithText(STOP_NAME).performClick()
        composeRule.waitForIdle()
        assertEquals(true, lastParkRideClick().isExpanded, "Sanity: the first tap opens it")

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(FACILITY_NAME).performClick()
        composeRule.waitForIdle()

        assertEquals(
            false,
            lastParkRideClick().isExpanded,
            "The screen forgot the card was open, so the tap read as opening it again — the " +
                "stop can never be taken back out of the ViewModel's refresh set",
        )
    }

    // Not covered here: rotating far enough to cross into the dual-pane layout, which moves
    // the list to DualPaneScaffold's slot and so gives anything remembered inside it a new
    // home. Robolectric can be told to change qualifiers, but doing so recreates the Activity
    // out from under the compose rule ("setContent should be called first"), and the state is
    // hoisted above that branch precisely so no test can see the difference. Verified by hand
    // on a device instead; see the comment on `expandedMap`.

    private fun showSavedTrips() {
        restorationTester.setContent {
            PreviewTheme {
                SavedTripsScreen(
                    savedTripsState = STATE_WITH_ONE_PARK_RIDE,
                    onEvent = { event -> events += event },
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun lastParkRideClick() =
        assertIs<SavedTripUiEvent.ParkRideCardClick>(events.last())

    private companion object {
        const val STOP_NAME = "Bella Vista"
        const val FACILITY_NAME = "Bella Vista Car Park"

        val STATE_WITH_ONE_PARK_RIDE = SavedTripsState(
            isSavedTripsLoading = false,
            parkRideUiState = persistentListOf(
                ParkRideUiState(
                    stopId = "2153478",
                    stopName = STOP_NAME,
                    isLoading = false,
                    facilities = persistentSetOf(
                        ParkRideUiState.ParkRideFacilityDetail(
                            spotsAvailable = 120,
                            totalSpots = 774,
                            facilityName = FACILITY_NAME,
                            percentageFull = 84,
                            stopId = "2153478",
                            timeText = "10:00 AM",
                            facilityId = "31",
                        ),
                    ),
                ),
            ),
        )
    }
}

internal const val RESTORE_TEST_SDK = 34
