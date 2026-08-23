package xyz.ksharma.krail.core.speechtotext

import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * One listening session's running state: what has been heard, when the last word arrived, and
 * what to send on for each transcription the recogniser produces.
 *
 * Used by `IosSpeechToTextService`, which is the platform that has to work this out for itself —
 * Android's recogniser ends a session on its own and reports its own results. It lives in
 * `commonMain` anyway, because iOS source has no test task in this project and these are the two
 * rules the surface actually turns on:
 *
 * **A blank transcription is never sent on as one.** `endAudio` asks the recogniser to finish,
 * and the result that comes back can carry an empty transcription for the segment that was open
 * at the time rather than a repeat of the sentence — the ordinary ending of a session here, not
 * a rare one, since the caller waits seconds of quiet before finishing. Sent on, it reached the
 * rider's field as their sentence being cleared for them a beat after they watched it arrive.
 * The last words actually heard are what a session is worth, so a blank final reports those, and
 * a session that heard nothing at all reports [SpeechUnavailableReasons.NO_RESULT] — which is
 * what `AndroidSpeechToTextService` does with a missing recognition candidate.
 *
 * **Only new words mean the rider is still talking.** The recogniser goes on revising what it
 * already heard for seconds after they stop, and every revision arrives as a changed transcript.
 *
 * [timeSource] is a parameter so the quiet window can be tested without waiting through it.
 */
internal class TranscriptWatch(private val timeSource: TimeSource = TimeSource.Monotonic) {

    var heardAnything: Boolean = false
        private set

    private var lastWordsHeard: String = ""
    private var lastTranscript: String = ""
    private var wordsSoFar: Int = 0
    private var lastNewWordAt: TimeMark = timeSource.markNow()

    /** Null means there is nothing worth sending on for this transcription. */
    fun record(text: String, isFinal: Boolean): SpeechToTextResult? {
        if (text != lastTranscript) {
            lastTranscript = text
            if (text.isNotBlank()) {
                heardAnything = true
                lastWordsHeard = text
            }
            // Words, not changes. Capitalising a station name, swapping a homophone or
            // re-punctuating all arrive as a changed transcript seconds after the rider stopped
            // talking. Resetting the quiet window on any change let a finished sentence keep the
            // microphone open on the strength of the recogniser talking to itself, and the
            // session ran to the ViewModel's ceiling instead of ending on its own.
            val words = text.split(' ', '\n', '\t').count { it.isNotBlank() }
            if (words > wordsSoFar) {
                wordsSoFar = words
                lastNewWordAt = timeSource.markNow()
            }
        }
        return when {
            isFinal && lastWordsHeard.isBlank() ->
                SpeechToTextResult.Error(reason = SpeechUnavailableReasons.NO_RESULT)

            isFinal -> SpeechToTextResult.Final(text = lastWordsHeard)
            text.isNotBlank() -> SpeechToTextResult.Partial(text = text)
            else -> null
        }
    }

    /** How long since a new word arrived, which is what "the rider stopped" means here. */
    fun quietForMillis(): Long = lastNewWordAt.elapsedNow().inWholeMilliseconds
}
