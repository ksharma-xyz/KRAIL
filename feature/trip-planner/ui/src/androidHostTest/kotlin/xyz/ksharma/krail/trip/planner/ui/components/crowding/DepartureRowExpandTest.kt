package xyz.ksharma.krail.trip.planner.ui.components.crowding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.departures.ui.state.model.StopDeparture
import xyz.ksharma.krail.feature.crowding.CarriageLevels
import xyz.ksharma.krail.feature.crowding.SeatLevel
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.components.DepartureBoardBody

/**
 * An opened departure row stays open through a configuration change and through the poll that
 * replaces the whole list every refresh. The expanded set lives above the board's branches,
 * so this goes through DepartureBoardBody, not the row list alone.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel6, manifest = Config.NONE)
class DepartureRowExpandTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun departure(relativeTime: String) = StopDeparture(
        lineNumber = "T2",
        lineColorCode = "#0098CD",
        transportModeName = "Train",
        destinationName = DESTINATION,
        departureTimeText = "8:59 am",
        departureUtcDateTime = "2026-10-26T21:59:00Z",
        relativeTimeText = relativeTime,
        realtimeTripId = "204R.1324.165.12.H.8.92002332",
        carriages = CarriageLevels(List(8) { SeatLevel.SEATS_AVAILABLE }),
        carriageSentence = "Seats expected all along the train.",
    )

    @Test
    fun openedRowSurvivesRecreationAndRefresh() {
        val restoration = StateRestorationTester(composeRule)
        var state by mutableStateOf(
            DeparturesState(isLoading = false, departures = persistentListOf(departure("in 5 mins"))),
        )
        restoration.setContent {
            PreviewTheme {
                DepartureBoardBody(state = state, onRetry = {}, onLoadPreviousDepartures = {})
            }
        }
        rows().assertCountEquals(0)

        composeRule.onNodeWithText(DESTINATION).performClick()
        rows().assertCountEquals(1)

        restoration.emulateSavedInstanceStateRestore()
        rows().assertCountEquals(1)

        // The next poll hands over a brand new list for the same run.
        state = DeparturesState(isLoading = false, departures = persistentListOf(departure("in 4 mins")))
        composeRule.waitForIdle()
        rows().assertCountEquals(1)

        composeRule.onNodeWithText(DESTINATION).performClick()
        composeRule.mainClock.advanceTimeBy(FOLD_AWAY_MS)
        rows().assertCountEquals(0)
    }

    private fun rows() = composeRule.onAllNodesWithTag(CARRIAGE_ROW_TAG, useUnmergedTree = true)

    private companion object {
        const val DESTINATION = "towards Leppington"
        const val FOLD_AWAY_MS = 1_000L
    }
}
