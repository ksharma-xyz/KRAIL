package xyz.ksharma.krail.core.aitext

import kotlinx.serialization.Serializable

/**
 * On-device text AI, scoped to slot-filling and summarization — not open chat. Backed by
 * Apple's Foundation Models framework on iOS and ML Kit GenAI (Gemini Nano) on Android.
 *
 * Every method is `suspend` and returns a nullable/sealed result rather than throwing. A
 * caller never needs to catch anything from this interface: model unavailability, a
 * mid-call SDK failure, or a device that simply doesn't support on-device AI all collapse
 * into the same "nothing to show" outcome, which callers treat identically to the feature
 * not existing. See `docs/proposals/on-device-ai-foray.md`.
 */
interface AiTextService {

    /**
     * Cheap enough to call before every use — implementations cache the underlying
     * platform check rather than re-querying the SDK each time. Never throws. Governs
     * [summarize] only — [extractTripIntent] is backed by a separate on-device model with
     * its own independent availability/download state, see [checkExtractionAvailability].
     */
    suspend fun checkAvailability(): AiAvailability

    /**
     * Same shape as [checkAvailability] but for the model backing [extractTripIntent]
     * specifically. Unlike [summarize] (which degrades silently — see [AiAvailability]'s
     * doc), a rider who typed something into the AI search-input flow and hit submit needs
     * to see *something* happen, so callers here are expected to actually branch on
     * [AiAvailability.Unavailable.reason], which is one of [AiUnavailableReasons]:
     * [AiUnavailableReasons.MODEL_DOWNLOADING] should render a distinct "setting up on-device
     * AI" state rather than the generic "couldn't understand that" a genuine parse failure
     * gets, and the two permanent reasons should say which of them it was. Calling this also
     * kicks off the on-demand model download as a side effect when one is available, same
     * fire-and-forget shape as [checkAvailability]'s own summarizer download trigger.
     */
    suspend fun checkExtractionAvailability(): AiAvailability

    /**
     * Produces a short summary of [text], or `null` if the model is unavailable or the
     * call fails for any reason (guardrail rejection, timeout, OOM, SDK error). Callers
     * must treat `null` as "render nothing" — never surface an error to the user for this.
     */
    suspend fun summarize(text: String): String?

    /**
     * Pulls trip-planning fields out of free text (typed, spoken-then-transcribed, or
     * OCR'd from a screenshot) — e.g. "leaving home around 9, need to be at central by
     * 6:30pm" → origin "home", destination "central", an arrival-by time of "6:30pm".
     *
     * Returns `null` on unavailability or any failure, same contract as [summarize]. Text
     * fields (not resolved dates/times) on purpose: this only labels *what role* a phrase
     * plays in the sentence, not what date/time it actually resolves to — the model
     * shouldn't be trusted with date arithmetic (today's date, timezone, "tomorrow" vs
     * "next Tuesday"), and turning `timeText` into an actual clock time is deterministic
     * app-code the caller already owns (`DateTimeSelectionItem` et al.). A field the rider
     * didn't mention comes back `null` (or an empty list for `modeHints`) — never guessed.
     */
    suspend fun extractTripIntent(text: String): TripIntentExtraction?
}

/**
 * @param originText Free-text place name as the rider said it (e.g. "home", "Central
 * Station"), not resolved against any stop database — the caller runs this through the
 * same stop/address search a manual pick would use.
 * @param destinationText Same shape as [originText].
 * @param timeIntent The rider's time constraint, unparsed — see [TimeIntent].
 * @param modeHints Free-text transport-mode words the rider mentioned (e.g. "train",
 * "avoid the bus"), verbatim — not resolved against [xyz.ksharma.krail.core.transport
 * .TransportMode]. Empty if the rider didn't mention a mode preference.
 */
@Serializable
data class TripIntentExtraction(
    val originText: String? = null,
    val destinationText: String? = null,
    val timeIntent: TimeIntent? = null,
    val modeHints: List<String> = emptyList(),
)

/**
 * @param isArrival `true` for "arrive by"/"need to be there by", `false` for "leave
 * at"/"leaving around". Mirrors [xyz.ksharma.krail.core.aitext]'s deliberate avoidance of a
 * feature-module dependency — the caller maps this to its own `JourneyTimeOptions`.
 * @param timeText The time phrase verbatim (e.g. "9am", "6:30pm", "in twenty minutes") —
 * unparsed, see [AiTextService.extractTripIntent]'s doc for why.
 */
@Serializable
data class TimeIntent(
    val isArrival: Boolean,
    val timeText: String,
)

/**
 * The reasons both platform implementations report, and the only ones a caller may branch on.
 *
 * Modelled on [xyz.ksharma.krail.core.speechtotext.SpeechUnavailableReasons], and added for
 * the same failure it was added for. The Android service reported ML Kit's own words
 * ("downloadable", "downloading", "unsupported_device") and the iOS service reported Swift's
 * string interpolation of `SystemLanguageModel.Availability.UnavailableReason`
 * ("deviceNotEligible", "appleIntelligenceNotEnabled", "modelNotReady"). The one caller that
 * branches on these tested for the Android spellings only, so on iOS *every* unavailability,
 * including the temporary "still downloading" one, fell through to the generic could-not-read
 * message and told riders to reword a sentence that was never the problem.
 *
 * Anything a platform reports that is not one of these is passed through as its own string
 * for logs, and callers treat it as an unknown failure rather than guessing.
 */
object AiUnavailableReasons {

    /** Temporary and retriable: the on-device model is downloading, or is about to. */
    const val MODEL_DOWNLOADING = "model_downloading"

    /** Permanent for this device: no hardware support, or an OS too old to have the API. */
    const val DEVICE_UNSUPPORTED = "device_unsupported"

    /**
     * The device could do it and the rider has it switched off. The only reason with a fix the
     * rider can actually carry out, which is why it is not folded into [DEVICE_UNSUPPORTED].
     */
    const val NEEDS_DEVICE_SETTING = "needs_device_setting"

    /** The availability check itself failed. Says nothing about the device either way. */
    const val CHECK_FAILED = "check_failed"
}

/**
 * Whether [AiTextService] can be used right now.
 *
 * [Unavailable.reason] is one of [AiUnavailableReasons] wherever a platform can map onto one.
 * For [AiTextService.summarize] it stays log-only — that feature is additive and degrades
 * silently. For [AiTextService.checkExtractionAvailability] the caller is expected to branch
 * on it, because a rider who typed a sentence and pressed send is owed a reason.
 */
sealed interface AiAvailability {
    data object Available : AiAvailability
    data class Unavailable(val reason: String) : AiAvailability
}
