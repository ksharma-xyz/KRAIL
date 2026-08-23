package xyz.ksharma.krail.core.speechtotext.testfakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import xyz.ksharma.krail.core.speechtotext.SpeechToTextAvailability
import xyz.ksharma.krail.core.speechtotext.SpeechToTextResult
import xyz.ksharma.krail.core.speechtotext.SpeechToTextService

/**
 * A configurable stand-in for one of the two implementations
 * `PreferredSpeechToTextService` chooses between.
 *
 * Named and shared rather than declared inside the test, which is the shape
 * `verifyNoAdHocBoundaryFakes` is asking for and the same one
 * `:feature:trip-planner:ui` uses for this interface. [transcript] is how a test tells which
 * of the two answered, since that is the only thing the caller can observe about the choice.
 */
internal class FakeSpeechToTextService(
    private val availability: SpeechToTextAvailability,
    private val transcript: String,
) : SpeechToTextService {

    var stopCount: Int = 0
        private set

    override suspend fun checkAvailability(): SpeechToTextAvailability = availability

    override fun startListening(): Flow<SpeechToTextResult> =
        flowOf(SpeechToTextResult.Final(transcript))

    override fun stopListening() {
        stopCount++
    }
}
