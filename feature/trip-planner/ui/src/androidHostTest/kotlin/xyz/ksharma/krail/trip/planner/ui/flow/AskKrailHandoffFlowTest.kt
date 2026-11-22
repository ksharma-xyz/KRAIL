package xyz.ksharma.krail.trip.planner.ui.flow

import org.junit.Test
import xyz.ksharma.krail.core.aitext.TripIntentExtraction
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.navigation.StopSelectedResult
import kotlin.test.assertEquals

private const val SETTLE_BEAT_MILLIS = 2_000L

private const val CENTRAL = "Central Station"
private const val TOWN_HALL = "Town Hall"
private const val PARRAMATTA = "Parramatta Station"
private const val PARRAMATTA_ID = "10103"

/**
 * Pins the shipped defect fixed by `closeAfterHandoff`'s phase step-down: the Ask KRAIL dialog
 * handed its resolved stops on through a `LaunchedEffect` keyed on the RESOLVED phase, kept the
 * phase after the handoff, and so replayed the AI's stops over stops the rider had since picked
 * by hand the next time the entry recomposed.
 *
 * The home has one search bar and no From and To rows, so the answer opens Search Stop holding the
 * stops it found, once, and the stops are also written to the Saved Trips ViewModel that Search Stop
 * reads. Both are checked here, and neither may come back after the rider picks their own.
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
        // The settle beat: the dialog closes itself onto the home it just answered, and the phase
        // steps down in the same emission.
        letTimePass(SETTLE_BEAT_MILLIS)

        // The handoff itself: Search Stop opens once, holding both of the AI's stops.
        assertEquals(1, navigator.searchStopRequests.size, "The answer opens Search Stop once.")
        assertEquals(
            listOf<Pair<String?, String?>>(CENTRAL to TOWN_HALL),
            navigator.searchStopPresets.map { (from, to) -> from?.stopName to to?.stopName },
        )
        assertEquals(CENTRAL, fakes.savedTripsViewModel.uiState.value.fromStop?.stopName)

        // The rider disagrees about where they are starting and picks their own From, the way
        // the stop-search screen reports one back.
        pickStop(
            StopSelectedResult(
                fieldType = SearchStopFieldType.FROM,
                stopId = PARRAMATTA_ID,
                stopName = PARRAMATTA,
            ),
        )
        assertEquals(PARRAMATTA, fakes.savedTripsViewModel.uiState.value.fromStop?.stopName)

        // Coming back from the stop-search screen re-launches the writing effect. With the
        // gate still open it wrote the AI's Central Station back over Parramatta here, and it
        // opened Search Stop a second time.
        leaveAndReturn()

        // The defect, stated directly: the AI's origin is gone for good and must not come back.
        assertEquals(PARRAMATTA, fakes.savedTripsViewModel.uiState.value.fromStop?.stopName)
        assertEquals(1, navigator.searchStopRequests.size, "Coming back never opens Search Stop again.")
        // The field the rider did not touch is still the AI's, which is the point of writing
        // per field rather than per resolve.
        assertEquals(TOWN_HALL, fakes.savedTripsViewModel.uiState.value.toStop?.stopName)
    }
}
