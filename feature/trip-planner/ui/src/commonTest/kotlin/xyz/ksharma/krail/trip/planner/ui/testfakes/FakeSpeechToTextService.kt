package xyz.ksharma.krail.trip.planner.ui.testfakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import xyz.ksharma.krail.core.speechtotext.SpeechToTextAvailability
import xyz.ksharma.krail.core.speechtotext.SpeechToTextResult
import xyz.ksharma.krail.core.speechtotext.SpeechToTextService

internal class FakeSpeechToTextService : SpeechToTextService {
    val level = MutableStateFlow(0f)
    override val voiceLevel: StateFlow<Float> = level
    var availability: SpeechToTextAvailability = SpeechToTextAvailability.Available
    val results = MutableSharedFlow<SpeechToTextResult>(extraBufferCapacity = 8)
    var stopListeningCallCount = 0
        private set

    override suspend fun checkAvailability(): SpeechToTextAvailability = availability

    override fun startListening(): Flow<SpeechToTextResult> = results

    override fun stopListening() {
        stopListeningCallCount++
    }
}
