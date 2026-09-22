package xyz.ksharma.krail.trip.planner.ui.search.ai

import xyz.ksharma.krail.core.aitext.TripIntentExtraction
import xyz.ksharma.krail.trip.planner.ui.search.ai.resolve.LabelSynonyms

/**
 * Turns an Ask KRAIL attempt into the handful of enum strings the analytics event carries.
 *
 * A separate file rather than private methods on `AiSearchInputViewModel`, because detekt counts
 * branches in a nested function toward the enclosing one, and `submit` is already the longest
 * path in that class. Each of these is a lookup with no state behind it, which is also what
 * makes them straightforward to test.
 *
 * Everything here is a **fact about what happened**, never a judgement about whether it went
 * well. Whether a match was right, where a threshold sits, and what groups with what all belong
 * to the analytics side, which can revise them across history; the app cannot revise a row it
 * already sent. The one judgement made here is what may leave the device, and that lives in
 * [AiSentenceTemplateRedaction].
 */

/** Which ends found a stop. One enum rather than two booleans: see the event's KDoc. */
internal fun endsResolvedOf(resolved: ResolvedTripIntent?): String = endsOf(
    hasFrom = resolved?.fromStopItem != null,
    hasTo = resolved?.toStopItem != null,
)

/**
 * What the **model** found, before any stop lookup.
 *
 * The gap against [endsResolvedOf] is the diagnostic. `both` extracted with `none` resolved is
 * the stop search failing on a parse that was fine; `none` extracted is the model failing. Today
 * both arrive as the same unresolved attempt with nothing to tell them apart.
 */
internal fun extractedEndsOf(extraction: TripIntentExtraction?): String = endsOf(
    hasFrom = !extraction?.originText.isNullOrBlank(),
    hasTo = !extraction?.destinationText.isNullOrBlank(),
)

private fun endsOf(hasFrom: Boolean, hasTo: Boolean): String = when {
    hasFrom && hasTo -> "both"
    hasFrom -> "from_only"
    hasTo -> "to_only"
    else -> "none"
}

/**
 * Typed, spoken, or both.
 *
 * Crossed with the reason and the attempt index, this is what answers "is speech working"
 * without reading a transcript. A transcript has no ground truth beside it, so it cannot say
 * whether the recogniser or the rider produced an odd sentence; two populations doing the same
 * task can.
 *
 * `mixed` is detected by the field being longer than what was heard, so an edit that **shortens**
 * it is invisible: a rider who speaks and then deletes a word reads as `spoken`. The undercount
 * runs one way only, which is worth knowing before reading a spoken-versus-mixed split as exact.
 * Comparing the two strings properly would mean keeping the transcript around to diff against,
 * and it is not worth holding more of what the rider said to sharpen a bucket.
 */
internal fun inputModeOf(state: AiSearchInputUiState): String {
    val spoke = state.speechTranscript.isNotBlank()
    val typedMoreThanWasHeard = state.typedText.trim().length > state.speechTranscript.trim().length
    return when {
        spoke && typedMoreThanWasHeard -> "mixed"
        spoke -> "spoken"
        else -> "typed"
    }
}

/**
 * Which kind of time the grammar read, if any.
 *
 * More useful than a boolean: a relative phrase is correct at any hour while an absolute one can
 * point into the past, and the two fail differently. The classification happens here rather than
 * downstream precisely because the alternative is sending the rider's time phrase, so this is
 * redaction rather than an interpretation the app has no business making.
 */
internal fun timeShapeOf(extraction: TripIntentExtraction?): String {
    val phrase = extraction?.timeIntent?.timeText?.trim()?.lowercase()
    if (phrase.isNullOrBlank()) return "none"
    return when {
        MARKERS_OF_A_TIME_RELATIVE_TO_NOW.any { phrase.contains(it) } -> "relative"
        phrase.any(Char::isDigit) -> "absolute"
        else -> "day_only"
    }
}

/**
 * Whether the sentence contained a word the rider uses for a place, such as "work" or "office".
 *
 * **The word itself is never sent.** This says only that one was present, which is what
 * separates "the model could not read this" from "the model read it fine and the place was a
 * word only this rider knows" - the failure `withLabelWordAsDestination` exists for.
 *
 * Whole-word, same as every other label comparison, so "homebush" is not "home".
 */
internal fun hadLabelWordIn(riderText: String, labels: List<String>): Boolean {
    if (labels.isEmpty() || riderText.isBlank()) return false
    return riderText.split(LETTERS_ONLY).any { token ->
        token.isNotBlank() && labels.any { label -> LabelSynonyms.sameMeaning(label, token) }
    }
}

/**
 * The shape of a place the rider named that matched no stop.
 *
 * **The word itself is never sent**, which is the whole reason this classification happens on
 * the device rather than downstream: the alternative is shipping the word and deciding later.
 *
 * The distinction earns its place because the fixes differ. A single word that matched nothing
 * is usually a stop-search problem; several words are usually a place we do not carry at all;
 * a word with a digit in it is often a route number typed where a stop was expected.
 *
 * **The values are ordered, not disjoint.** `has_digit` is checked first, so a multi-word place
 * containing a digit reports `has_digit` and never `multi_word`. That is deliberate, since the
 * digit is the more actionable signal, but it means `multi_word` is a partial category: read it
 * as "several words and no digit" rather than as every multi-word failure.
 *
 * Null when there is nothing to classify, which includes the case where the model reworded the
 * rider's place badly enough that it was never quoted back. `spanMatchedVerbatim` already
 * reports that, so it is not silently folded in here.
 */
internal fun unmatchedKindOf(unmatchedPlace: String?): String? {
    val place = unmatchedPlace?.trim()
    if (place.isNullOrBlank()) return null
    return when {
        place.any(Char::isDigit) -> "has_digit"
        place.split(LETTERS_ONLY).count { it.isNotBlank() } > 1 -> "multi_word"
        else -> "single_word"
    }
}

private val MARKERS_OF_A_TIME_RELATIVE_TO_NOW = setOf("in ", "minute", "hour", "soon", "now")

private val LETTERS_ONLY = Regex("[^\\p{L}]+")
