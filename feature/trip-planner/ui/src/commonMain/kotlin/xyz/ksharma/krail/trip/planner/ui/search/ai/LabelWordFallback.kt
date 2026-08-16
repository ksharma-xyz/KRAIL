package xyz.ksharma.krail.trip.planner.ui.search.ai

import xyz.ksharma.krail.core.aitext.TripIntentExtraction
import xyz.ksharma.krail.trip.planner.ui.search.ai.resolve.LabelSynonyms

/**
 * Rescues a sentence whose only place is a word the rider named themselves.
 *
 * Reported: "work by 9am tomm" came back with **no place at all** and the rider was told to name
 * a stop. The model has no idea "work" is a place for this rider — it is an ordinary English
 * noun — so it read the sentence as a time and nothing else.
 *
 * This is the deterministic half of the fix. When extraction finds no place, the rider's own
 * text is scanned for a word that is one of their labels (or a synonym of one), and that word
 * becomes the destination. The proper half is teaching the model that these words are places
 * for this rider, which is a prompt change and a separate piece of work.
 *
 * ## Why this does not break the rule about model output
 *
 * `AI_SEARCH_UX.md` requires that nothing the model produces is displayed. Nothing here comes
 * from the model. The word is taken from what the rider typed, matched against labels they
 * created, and resolved through the ordinary label resolver. It is their word and their stop.
 *
 * ## Why whole words only
 *
 * Same reason label matching is exact: "homebush" contains "home" and is a different place
 * entirely. Splitting on non-letters and comparing whole tokens keeps that safe.
 */
internal fun TripIntentExtraction.withLabelWordAsDestination(
    riderText: String,
    labels: List<String>,
): TripIntentExtraction {
    // Only when the model found nothing. A sentence it read correctly is never second-guessed.
    if (originText != null || destinationText != null) return this
    if (labels.isEmpty()) return this

    val labelWord = riderText
        .split(WORD_SEPARATORS)
        .firstOrNull { token ->
            token.isNotBlank() && labels.any { label -> LabelSynonyms.sameMeaning(label, token) }
        }
        ?: return this

    return copy(destinationText = labelWord)
}

// Letters only. Splitting on everything else means "work," and "work." are the same word, and a
// time like "9am" can never be mistaken for one.
private val WORD_SEPARATORS = Regex("[^\\p{L}]+")
