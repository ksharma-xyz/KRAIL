package xyz.ksharma.krail.core.speechtotext

/**
 * Decides when a rider has finished talking, from Apple's own voice activity detection.
 *
 * The iOS 26 `SpeechDetector` module answers one question, "is there speech", and stamps every
 * answer with the **audio time** it applies to. This turns that stream of answers into one
 * decision. It lives in `commonMain` rather than beside the bridge that feeds it because Swift
 * source in this project has no test task, and this is the rule the whole surface turns on.
 *
 * ## Why audio time is the point
 *
 * The previous implementation had no voice activity detection available to it, so it watched
 * the transcript instead and called a sentence finished when the words stopped changing. That
 * measured the recogniser, not the rider: recognition lags speech, so every session waited its
 * window **plus** however long the last word took to come back, and revisions arriving after
 * the rider stopped restarted the wait. See
 * `docs/learning/2026-08-23-the-blank-transcript-that-cleared-the-field.md`.
 *
 * Here, [record] is given the time in the audio itself, so the window is the rider's silence
 * and nothing else. It is the same quantity Android's recogniser measures internally, which is
 * why [QUIET_THAT_ENDS_THE_SESSION_SECONDS] can carry the same number Android uses and mean it
 * this time.
 */
internal class SpeechActivityWatch(
    private val quietThatEndsTheSession: Double = QUIET_THAT_ENDS_THE_SESSION_SECONDS,
    private val quietBeforeAnySpeech: Double = QUIET_BEFORE_ANY_SPEECH_SECONDS,
) {

    /** Whether any speech has been detected at all, which changes how long the wait is. */
    var heardSpeech: Boolean = false
        private set

    private var lastSpeechAt: Double = 0.0
    private var latestAudioAt: Double = 0.0

    /**
     * Takes one detector answer. [audioSeconds] is where in the audio it applies, so answers
     * arriving out of order or late cannot move the clock backwards.
     */
    fun record(speechDetected: Boolean, audioSeconds: Double) {
        if (audioSeconds > latestAudioAt) latestAudioAt = audioSeconds
        if (speechDetected) {
            heardSpeech = true
            if (audioSeconds > lastSpeechAt) lastSpeechAt = audioSeconds
        }
    }

    /** Seconds of audio since speech was last heard. Zero until the first answer arrives. */
    fun quietForSeconds(): Double = (latestAudioAt - lastSpeechAt).coerceAtLeast(0.0)

    /**
     * True once the rider has been quiet long enough to be finished.
     *
     * A session that has heard nothing yet gets the longer wait: that window covers someone who
     * tapped the mic and is still deciding what to ask for, not someone who trailed off mid
     * sentence.
     */
    fun riderHasFinished(): Boolean {
        val longEnough = if (heardSpeech) quietThatEndsTheSession else quietBeforeAnySpeech
        return quietForSeconds() >= longEnough
    }
}

// The same number as Android's SILENCE_AFTER_A_COMPLETE_SOUNDING_PHRASE_MILLIS, and for the
// first time the same measurement: seconds of actual silence, from a real voice activity
// detector, rather than seconds without a transcript update. Android picks between two windows
// by how finished the phrase sounds; iOS cannot tell those apart, so it takes the one that fires
// for a trip sentence, which almost always ends on something complete sounding.
//
// A trip said out loud has thinking pauses in it. This is the tolerance for those, and it is why
// the window is seconds rather than the few hundred milliseconds true silence detection would
// otherwise allow.
private const val QUIET_THAT_ENDS_THE_SESSION_SECONDS = 3.0

// Before the first word, matching the legacy path's own grace period.
private const val QUIET_BEFORE_ANY_SPEECH_SECONDS = 6.0
