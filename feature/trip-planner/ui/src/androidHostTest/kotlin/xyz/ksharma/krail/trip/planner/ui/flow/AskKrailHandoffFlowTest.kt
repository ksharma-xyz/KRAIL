package xyz.ksharma.krail.trip.planner.ui.flow

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import org.junit.Test
import xyz.ksharma.krail.core.aitext.TripIntentExtraction
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.navigation.StopSelectedResult

private const val SETTLE_BEAT_MILLIS = 2_000L

private const val CENTRAL = "Central Station"
private const val TOWN_HALL = "Town Hall"
private const val PARRAMATTA = "Parramatta Station"
private const val PARRAMATTA_ID = "10103"

/**
 * Pins the shipped defect fixed by `closeAfterHandoff`'s phase step-down: the Ask KRAIL dialog
 * handed its resolved stops to the home row through a `LaunchedEffect` keyed on the RESOLVED
 * phase, kept the phase after the handoff, and so replayed the AI's stops over stops the rider
 * had since picked by hand the next time the entry recomposed.
 *
 * No layer below this one could see it. The ViewModel tests never re-run the collector; the
 * screen tests never navigate. It needs the entry, the effect and a leave-and-return, which is
 * what [FlowTest] is for.
 */
class AskKrailHandoffFlowTest : FlowTest() {

    @Test
    fun aiStopsDoNotReplayOverAManualPickAfterLeavingAndReturning() {
        fakes.aiTextService.extractionResult = TripIntentExtraction(
            originText = CENTRAL,
            destinationText = TOWN_HALL,
            timeIntent = null,
        )
        launchHome()

        askKrail("central to town hall")
        // The settle beat: the dialog closes itself onto the row it just filled, and the phase
        // steps down in the same emission.
        letTimePass(SETTLE_BEAT_MILLIS)

        // The handoff itself. Both fields are the AI's at this point.
        composeRule.onNodeWithText(CENTRAL).assertIsDisplayed()
        composeRule.onNodeWithText(TOWN_HALL).assertIsDisplayed()

        // The rider disagrees about where they are starting and picks their own From, the way
        // the stop-search screen reports one back.
        pickStop(
            StopSelectedResult(
                fieldType = SearchStopFieldType.FROM,
                stopId = PARRAMATTA_ID,
                stopName = PARRAMATTA,
            ),
        )
        composeRule.onNodeWithText(PARRAMATTA).assertIsDisplayed()

        // Coming back from the stop-search screen re-launches the writing effect. With the
        // gate still open it wrote the AI's Central Station back over Parramatta here.
        leaveAndReturn()

        // The defect, stated directly: the AI's origin is gone for good and must not come back.
        composeRule.onNodeWithText(CENTRAL).assertDoesNotExist()
        composeRule.onNodeWithText(PARRAMATTA).assertIsDisplayed()
        // The field the rider did not touch is still the AI's, which is the point of writing
        // per field rather than per resolve.
        composeRule.onNodeWithText(TOWN_HALL).assertIsDisplayed()
    }
}
