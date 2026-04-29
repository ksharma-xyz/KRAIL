package xyz.ksharma.krail.trip.planner.ui.components

/**
 * Strips emoji and exotic characters from a user-typed label name and collapses
 * whitespace. We use this both before saving and for case-insensitive dedupe so that
 * `🏠 Home`, `Home`, and `home ` all resolve to the same canonical name.
 */
internal fun normaliseLabelName(input: String): String {
    val cleaned = buildString(input.length) {
        for (c in input) {
            when {
                c.isLetterOrDigit() -> append(c)
                c.isWhitespace() -> append(' ')
                c == '-' || c == '_' || c == '\'' -> append(c)
                // Drop everything else: emoji, surrogate pairs, punctuation, etc.
            }
        }
    }
    return cleaned.trim().replace(Regex("\\s+"), " ")
}

/** Returns true if [a] and [b] resolve to the same canonical label name. */
internal fun labelNamesMatch(a: String, b: String): Boolean =
    normaliseLabelName(a).equals(normaliseLabelName(b), ignoreCase = true)
