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

/**
 * The reasons both platform implementations report, and the only ones a caller may branch on.
 *
 * They were string literals typed out separately in the Android service, the iOS service and
 * the UI that checks them. Nothing kept the three in sync, so renaming one of them silently
 * removed a message rather than breaking a build. Anything a platform reports that is not one
 * of these is still passed through as its own string for logs, and callers treat it as an
 * unknown failure rather than guessing.
 */
object SpeechUnavailableReasons {
    const val NOT_AVAILABLE = "not_available"
    const val PERMISSION_REQUIRED = "permission_required"
    const val NO_RESULT = "no_result"
}

sealed interface SpeechToTextAvailability {
    data object Available : SpeechToTextAvailability
    data class Unavailable(val reason: String) : SpeechToTextAvailability
}

/**
 * [Partial] and [Final] always carry words. A recogniser reporting a blank transcription is
 * saying it has nothing to add, never that the rider unsaid what they said, so an
 * implementation drops it rather than emitting it: the caller writes each transcript straight
 * into the field the rider is looking at, and a blank one erases the sentence they just watched
 * arrive. A session that heard nothing at all ends as [Error] with
 * [SpeechUnavailableReasons.NO_RESULT], which is a session with no result rather than a session
 * whose result is nothing.
 */
sealed interface SpeechToTextResult {
    data class Partial(val text: String) : SpeechToTextResult
    data class Final(val text: String) : SpeechToTextResult
    data class Error(val reason: String) : SpeechToTextResult
}
