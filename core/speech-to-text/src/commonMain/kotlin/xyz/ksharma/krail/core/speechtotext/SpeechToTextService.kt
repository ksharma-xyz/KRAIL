package xyz.ksharma.krail.core.speechtotext

import kotlinx.coroutines.flow.Flow

/**
 * On-device speech-to-text for the "Speak" tab of the AI search-input flow
 * (`docs/investigations/ai_search_input_mockup.html` stage 03). Backed by Android's
 * `SpeechRecognizer` and iOS's `SFSpeechRecognizer` + `AVAudioEngine` — see this module's
 * README for why those platform APIs are wrapped directly rather than a third-party
 * library.
 *
 * Same never-throws contract as [xyz.ksharma.krail.core.aitext.AiTextService]:
 * unavailability and mid-call failures both collapse to an outcome the caller renders
 * identically to the feature not existing, never a surfaced error.
 */
interface SpeechToTextService {

    /**
     * Cheap enough to call before every use — implementations cache the underlying
     * platform check rather than re-querying the OS each time. Never throws.
     */
    suspend fun checkAvailability(): SpeechToTextAvailability

    /**
     * Starts listening and emits partial transcripts as the rider speaks, then a single
     * [SpeechToTextResult.Final] once they stop, or [SpeechToTextResult.Error] on failure.
     * The flow completes after the final/error result — call this again to listen again.
     */
    fun startListening(): Flow<SpeechToTextResult>

    /** Stops listening early (e.g. the rider tapped away) without waiting for a final result. */
    fun stopListening()
}

sealed interface SpeechToTextAvailability {
    data object Available : SpeechToTextAvailability
    data class Unavailable(val reason: String) : SpeechToTextAvailability
}

sealed interface SpeechToTextResult {
    data class Partial(val text: String) : SpeechToTextResult
    data class Final(val text: String) : SpeechToTextResult
    data class Error(val reason: String) : SpeechToTextResult
}
