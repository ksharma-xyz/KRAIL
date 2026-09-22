package xyz.ksharma.krail.trip.planner.ui.search.ai

import xyz.ksharma.krail.core.aitext.TimeIntent
import xyz.ksharma.krail.core.aitext.TripIntentExtraction
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The enum strings the attempt event carries.
 *
 * Each is a fact about what happened rather than a judgement about whether it went well, so
 * these tests assert the mapping and nothing about quality.
 */
class AiAttemptTelemetryTest {

    @Test
    fun `ends resolved reports which fields found a stop`() {
        assertEquals("both", endsResolvedOf(intent(from = stop("A"), to = stop("B"))))
        assertEquals("from_only", endsResolvedOf(intent(from = stop("A"))))
        assertEquals("to_only", endsResolvedOf(intent(to = stop("B"))))
        assertEquals("none", endsResolvedOf(intent()))
        assertEquals("none", endsResolvedOf(null))
    }

    @Test
    fun `extracted ends reports what the model found, before any lookup`() {
        // The gap against endsResolved is the diagnostic: the model found both ends here, so a
        // `none` on the other side is the stop search failing rather than the model.
        assertEquals(
            "both",
            extractedEndsOf(extraction(origin = "home", destination = "work")),
        )
        assertEquals("to_only", extractedEndsOf(extraction(destination = "work")))
        assertEquals("none", extractedEndsOf(extraction()))
        assertEquals("none", extractedEndsOf(null))
    }

    @Test
    fun `a blank extracted field counts as absent, not present`() {
        assertEquals("none", extractedEndsOf(extraction(origin = "   ")))
    }

    @Test
    fun `time shape separates a relative phrase from an absolute one`() {
        assertEquals("relative", timeShapeOf(extraction(time = "in 20 minutes")))
        assertEquals("relative", timeShapeOf(extraction(time = "in 2 hours")))
        assertEquals("absolute", timeShapeOf(extraction(time = "9am")))
        assertEquals("day_only", timeShapeOf(extraction(time = "tomorrow")))
        assertEquals("none", timeShapeOf(extraction()))
    }

    @Test
    fun `input mode distinguishes speaking from typing from both`() {
        assertEquals("typed", inputModeOf(state(typed = "home to work")))
        assertEquals("spoken", inputModeOf(state(typed = "home to work", heard = "home to work")))
        assertEquals(
            "mixed",
            inputModeOf(state(typed = "home to work by 9", heard = "home to work")),
        )
    }

    @Test
    fun `unmatched kind describes the shape of a place, never the place`() {
        assertEquals("single_word", unmatchedKindOf("rozelle"))
        assertEquals("multi_word", unmatchedKindOf("the clinic"))
        assertEquals("has_digit", unmatchedKindOf("12 smith st"))
    }

    @Test
    fun `unmatched kind is absent when there is nothing to classify`() {
        // Includes the case where the model reworded the rider's place badly enough that it was
        // never quoted back. spanMatchedVerbatim reports that; this does not fold it in.
        assertNull(unmatchedKindOf(null))
        assertNull(unmatchedKindOf("  "))
    }

    @Test
    fun `a label word is reported as present without reporting which one`() {
        assertEquals(true, hadLabelWordIn("get me to work by 9", labels = listOf("Work")))
        assertEquals(true, hadLabelWordIn("office by monday", labels = listOf("Work")))
        assertEquals(false, hadLabelWordIn("get me to homebush", labels = listOf("Home")))
        assertEquals(false, hadLabelWordIn("get me to work", labels = emptyList()))
    }

    private fun stop(id: String) = StopItem(stopName = id, stopId = id)

    private fun intent(from: StopItem? = null, to: StopItem? = null) = ResolvedTripIntent(
        fromText = null,
        fromStopItem = from,
        toText = null,
        toStopItem = to,
        dateTimeSelectionItem = null,
        modeHints = emptyList(),
    )

    private fun extraction(
        origin: String? = null,
        destination: String? = null,
        time: String? = null,
    ) = TripIntentExtraction(
        originText = origin,
        destinationText = destination,
        timeIntent = time?.let { TimeIntent(isArrival = true, timeText = it) },
        modeHints = emptyList(),
    )

    private fun state(typed: String, heard: String = "") =
        AiSearchInputUiState(typedText = typed, speechTranscript = heard)
}
