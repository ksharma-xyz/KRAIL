package xyz.ksharma.krail.trip.planner.ui.flow

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import org.junit.Test
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.navigation.StopSelectedResult

private const val CENTRAL = "Central Station"
private const val CENTRAL_ID = "10101"
private const val TOWN_HALL = "Town Hall"
private const val TOWN_HALL_ID = "10102"

/**
 * The ordinary two-tap trip: pick a From, pick a To, with the device rotated in between.
 *
 * Pins the state-survives-recreation item in `docs/FEATURE_QUALITY_CHECKLIST.md`. Filling the
 * row takes two separate trips to another screen, so anything that lived only in the entry's
 * composition would lose the first stop on the way to the second — and rotation is exactly
 * when a rider changes their mind about a stop.
 */
class SearchStopRoundTripFlowTest : FlowTest() {

    @Test
    fun bothStopsLandOnTheRowWhenTheScreenIsRecreatedBetweenThePicks() {
        launchHome()

        pickStop(
            StopSelectedResult(
                fieldType = SearchStopFieldType.FROM,
                stopId = CENTRAL_ID,
                stopName = CENTRAL,
            ),
        )
        composeRule.onNodeWithText(CENTRAL).assertIsDisplayed()

        recreate()

        // The From stop was picked before the recreation and has to still be there after it.
        composeRule.onNodeWithText(CENTRAL).assertIsDisplayed()

        pickStop(
            StopSelectedResult(
                fieldType = SearchStopFieldType.TO,
                stopId = TOWN_HALL_ID,
                stopName = TOWN_HALL,
            ),
        )

        composeRule.onNodeWithText(CENTRAL).assertIsDisplayed()
        composeRule.onNodeWithText(TOWN_HALL).assertIsDisplayed()
    }
}
