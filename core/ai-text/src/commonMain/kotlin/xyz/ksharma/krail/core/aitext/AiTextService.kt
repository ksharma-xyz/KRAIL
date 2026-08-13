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
     * [AiAvailability.Unavailable.reason] — `"downloadable"`/`"downloading"` should render a
     * distinct "setting up on-device AI" state rather than the generic "couldn't understand
     * that" a genuine parse failure gets. Calling this also kicks off the on-demand model
     * download as a side effect when the reason is `"downloadable"`, same fire-and-forget
     * shape as [checkAvailability]'s own summarizer download trigger.
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
 * Whether [AiTextService] can be used right now. [Unavailable.reason] is for logging only —
 * never shown to the user, since both features this backs are additive and must degrade
 * silently to today's UI.
 */
sealed interface AiAvailability {
    data object Available : AiAvailability
    data class Unavailable(val reason: String) : AiAvailability
}
