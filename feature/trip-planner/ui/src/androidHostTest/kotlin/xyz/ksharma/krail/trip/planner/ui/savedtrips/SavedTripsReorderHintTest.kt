package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

/**
 * The home list has no one time tip about long pressing a trip. The only line about reordering is the hint shown while the rider
 * is reordering, which says how to drag and how to finish.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp", manifest = Config.NONE)
class SavedTripsReorderHintTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val trip = Trip(
        fromStopId = "200060",
        fromStopName = "Central Station",
        toStopId = "200070",
        toStopName = "Town Hall Station",
    )

    private fun showHome() {
        composeRule.setContent {
            PreviewTheme {
                SavedTripsScreen(
                    savedTripsState = SavedTripsState(isSavedTripsLoading = false, savedTrips = persistentListOf(trip)),
                    onEvent = {},
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `the home list never says to long press a trip to reorder or remove it`() {
        showHome()

        composeRule.onNodeWithText(REORDER_TIP, substring = true).assertDoesNotExist()
        composeRule.onNodeWithText(LONG_PRESS_ANY_TRIP, substring = true).assertDoesNotExist()
    }

    @Test
    fun `while the rider reorders, the hint says how to drag and finish, and the tip is still not there`() {
        showHome()

        composeRule.onNodeWithTag(TripPlannerTestTags.savedTripCard(trip.tripId)).performTouchInput { longClick() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(REORDER_HINT).assertExists()
        composeRule.onNodeWithText(REORDER_TIP, substring = true).assertDoesNotExist()
    }

    private companion object {
        const val REORDER_HINT = "💡 Long press and drag cards to reorder. Tap Done when finished."
        const val LONG_PRESS_ANY_TRIP = "Long press any trip"
        const val REORDER_TIP = "reorder or remove it"
    }
}
