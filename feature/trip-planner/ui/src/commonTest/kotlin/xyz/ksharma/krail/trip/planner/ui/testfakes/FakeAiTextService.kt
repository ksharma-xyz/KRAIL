package xyz.ksharma.krail.trip.planner.ui.testfakes

import xyz.ksharma.krail.core.aitext.AiAvailability
import xyz.ksharma.krail.core.aitext.AiTextService
import xyz.ksharma.krail.core.aitext.TripIntentExtraction

internal class FakeAiTextService : AiTextService {
    var availability: AiAvailability = AiAvailability.Available
    var extractionAvailability: AiAvailability = AiAvailability.Available
    var summarizeResult: String? = null
    var extractionResult: TripIntentExtraction? = null
    var lastExtractedText: String? = null

    /** How many times [summarize] ran, so a caller's dedupe guard can be asserted on. */
    var summarizeCallCount: Int = 0
        private set

    override suspend fun checkAvailability(): AiAvailability = availability

    override suspend fun checkExtractionAvailability(): AiAvailability = extractionAvailability

    override suspend fun summarize(text: String): String? {
        summarizeCallCount++
        return summarizeResult
    }

    override suspend fun extractTripIntent(text: String): TripIntentExtraction? {
        lastExtractedText = text
        return extractionResult
    }
}
