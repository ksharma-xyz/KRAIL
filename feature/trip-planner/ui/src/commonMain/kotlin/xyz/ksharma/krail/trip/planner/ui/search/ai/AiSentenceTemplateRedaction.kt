package xyz.ksharma.krail.trip.planner.ui.search.ai

import xyz.ksharma.krail.core.aitext.TripIntentExtraction

/**
 * Turns a rider's sentence into the only form of it allowed to leave the device: a template
 * with every place and time replaced by a placeholder.
 *
 * ```
 * "get me home from 12 smith st by 6"  ->  "get me <PLACE> from <PLACE> by <TIME>"
 * ```
 *
 * The template answers the question the raw sentence was wanted for. "What do riders most
 * commonly say" is a question about phrasing, and the phrasing is what survives substitution;
 * the street name was never the part carrying the answer.
 *
 * Analytics goes straight to Firebase from [xyz.ksharma.krail.core.analytics.RealAnalytics], so
 * whatever this returns is stored as-is by a third party and no later step can take it back.
 * That is why the decision is made here rather than downstream, the same argument as
 * [xyz.ksharma.krail.trip.planner.ui.searchstop.SearchQueryAnalyticsRedaction] one surface
 * along.
 *
 * ## The model is not trusted
 *
 * Substitution is driven by [TripIntentExtraction], which the on-device model produced, and the
 * model is wrong often enough that masking built on its output alone would leak whenever it
 * erred. It also errs *silently*: a place it fails to report stays in the sentence with nothing
 * to indicate anything went wrong.
 *
 * So there are two gates, and the second asks the model nothing:
 *
 * 1. **Every span the model claims must appear verbatim in what the rider typed.** Catches the
 *    model rewording or inventing: "bondi junctn" typed, "Bondi Junction" returned.
 * 2. **After substitution, every remaining word must be in [ALLOWED_WORDS].** Catches the model
 *    missing a place entirely, which gate 1 cannot see because nothing failed.
 *
 * Gate 2 provides the guarantee. It inverts the usual shape: rather than detecting the sensitive
 * parts and removing them, it permits only a sentence whose every non-placeholder word is
 * already known to be safe. Gate 1 is kept for the signal it produces rather than for protection
 * (see [Result.spanMatchedVerbatim]); an unmatched span leaves raw text behind, which gate 2
 * rejects anyway.
 *
 * **Failing a gate omits the template, never the event.** Everything else the attempt records
 * still describes what happened; only the phrasing is missing.
 *
 * ## Most sentences are dropped, and that is the design working
 *
 * Common phrasings are built from common words by definition and pass. An unusual sentence is
 * unusual precisely because it names a person, a clinic or a street, and those fail. The data
 * the gates permit is the data the question wanted.
 */
object AiSentenceTemplateRedaction {

    const val PLACE_TOKEN = "<PLACE>"
    const val TIME_TOKEN = "<TIME>"

    /**
     * Longer than a sentence riders actually speak. Not a privacy control, gate 2 is, but an
     * unbounded field eventually receives something pasted and a truncated value would arrive
     * looking like a real one. Over this the template is dropped rather than shortened.
     */
    const val MAX_TEMPLATE_LENGTH = 120

    /**
     * The security boundary. Every word a template may contain outside a placeholder.
     *
     * Deliberately small and boring: function words, movement verbs, and the connective
     * vocabulary riders actually use. Transport modes are here rather than substituted, because
     * a mode names a kind of vehicle and not a place, and "avoid the bus" is a phrasing worth
     * keeping whole.
     *
     * Rules for changing it:
     * - **No proper nouns, ever.** Not suburb names, not line names, not "central". A word does
     *   not become safe by being a common place.
     * - **No digits.** A digit outside [TIME_TOKEN] fails the gate, which is what keeps a street
     *   number out.
     * - Whole-word and case-insensitive, no stemming. Same discipline as `LabelSynonyms`, for
     *   the same reason: a nearly-right match is worse than none.
     * - Each addition widens what can be sent, so it is reviewed as a permission change is, and
     *   `AiSentenceTemplateRedactionTest` gains a case for it.
     * - **Keep opposites together.** `before` shipped without `after`, so "get me there before
     *   9" survived and "after 9" did not: a one-sided bias in exactly the dimension the data
     *   exists to measure, and invisible once collected. Held by
     *   `the allowlist carries both halves of every pair`.
     *
     * Deliberately absent, and this is a judgement rather than a safety rule: `station`, `stop`,
     * `platform`, `wharf`. None of them leaks, since a bare "station" names no place, but they
     * sit closest to place vocabulary and the rule reads better as a bright line than as a line
     * with four exceptions. The phrasings they would recover are worth less than the boundary
     * staying simple to review.
     */
    val ALLOWED_WORDS: Set<String> = setOf(
        // articles, prepositions, conjunctions
        "a", "about", "after", "an", "and", "around", "as", "at", "before", "by", "for",
        "from", "in", "into", "no", "not", "of", "on", "or", "out", "past", "the", "then", "to",
        "towards", "until", "via", "with", "without",
        // pronouns and determiners
        "any", "best", "here", "i", "it", "let", "lets", "me", "my", "our", "that", "there",
        "this", "us", "we",
        // movement and intent verbs
        "arrive", "back", "catch", "find", "get", "getting", "go", "going", "head", "heading",
        "leave", "leaving", "reach", "ride", "show", "take", "taking", "travel", "travelling",
        "trip", "way",
        // auxiliaries and modals
        "am", "are", "be", "been", "can", "could", "do", "does", "dont", "is", "need", "needs",
        "should", "want", "wanna", "was", "will", "would",
        // question and framing words that are not themselves times
        "closest", "fastest", "how", "last", "long", "near", "nearest", "next", "now",
        "quickest", "soon", "time", "what", "whats", "when", "where", "which",
        // transport modes: a kind of vehicle, never a place. Plurals included because riders
        // write "show me trains to X", and a phrasing lost to a missing "s" is a phrasing
        // missing from the distribution for a reason that has nothing to do with riders.
        "bus", "buses", "coach", "coaches", "ferries", "ferry", "light", "metro", "rail",
        "train", "trains", "tram", "trams",
        // politeness and filler riders actually type
        "please", "pls", "thanks",
    )

    private val CHARS_THAT_END_A_WORD: Set<Char> = (" \t\n\r,.?!;:'\"()/-").toSet()

    /**
     * @param template the substituted sentence, or `null` when a gate failed or there was
     * nothing to send. Null is the common case and is not an error.
     * @param spanMatchedVerbatim whether every span the model reported was found verbatim in the
     * rider's text. **A measurement, not a safety property.** Crossed with what resolved, it
     * separates three outcomes that otherwise look identical: the model correcting a typo that
     * then resolved, the model rewriting into something matching nothing, and our own stop
     * search failing on the rider's actual words. The last is our bug and the first two are not,
     * and today all three arrive as the same unresolved attempt.
     */
    data class Result(
        val template: String?,
        val spanMatchedVerbatim: Boolean,
    )

    fun redact(riderText: String, extraction: TripIntentExtraction?): Result {
        val trimmed = riderText.trim()
        if (trimmed.isEmpty()) return Result(template = null, spanMatchedVerbatim = true)

        val spans = spansOf(extraction)
        val matchedAll = spans.all { (text, _) -> trimmed.contains(text, ignoreCase = true) }
        val substituted = spans.fold(trimmed) { acc, (text, token) ->
            acc.replace(text, token, ignoreCase = true)
        }

        return Result(
            template = substituted.takeIf { it.passesGateTwo() },
            spanMatchedVerbatim = matchedAll,
        )
    }

    /**
     * Longest first, so a span containing another substitutes whole rather than leaving a
     * fragment behind for the shorter one to cut in half.
     *
     * Mode hints are deliberately absent: they are allowlisted words, not places.
     */
    private fun spansOf(extraction: TripIntentExtraction?): List<Pair<String, String>> =
        buildList {
            extraction?.originText?.let { add(it to PLACE_TOKEN) }
            extraction?.destinationText?.let { add(it to PLACE_TOKEN) }
            extraction?.timeIntent?.timeText?.let { add(it to TIME_TOKEN) }
        }
            .filter { (text, _) -> text.isNotBlank() }
            .sortedByDescending { (text, _) -> text.length }

    /**
     * Gate 2, plus the length cap.
     *
     * Splitting on punctuation as well as whitespace matters: without it "sarah," is one token,
     * never matches the allowlist entry it would have matched, and the gate's verdict would
     * depend on where a rider put their commas.
     *
     * A sentence with no placeholder in it at all still passes when every word is allowlisted.
     * That is deliberate: "when is the next train" named no place, which is the
     * `NO_PLACE_MENTIONED` failure, and its phrasing is exactly what would explain that failure.
     */
    private fun String.passesGateTwo(): Boolean {
        if (length > MAX_TEMPLATE_LENGTH) return false
        return replace(PLACE_TOKEN, " ")
            .replace(TIME_TOKEN, " ")
            .split { it in CHARS_THAT_END_A_WORD }
            .all { it.lowercase() in ALLOWED_WORDS }
    }

    /** Splits on [CHARS_THAT_END_A_WORD], dropping empties, without a spread operator. */
    private inline fun String.split(isSeparator: (Char) -> Boolean): List<String> {
        val words = mutableListOf<String>()
        val word = StringBuilder()
        forEach { char ->
            if (isSeparator(char)) {
                if (word.isNotEmpty()) words.add(word.toString())
                word.clear()
            } else {
                word.append(char)
            }
        }
        if (word.isNotEmpty()) words.add(word.toString())
        return words
    }
}
