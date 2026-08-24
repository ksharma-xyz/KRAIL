package xyz.ksharma.krail.trip.planner.ui.search.ai

/**
 * Joins spoken words onto whatever the rider had already written in the field.
 *
 * Speaking adds to the field rather than replacing it, because the field is the only copy of
 * what they typed and a mic that lives inside a text field is another way to type. See
 * `AiSearchInputViewModel.startListening`.
 *
 * A single space separates the two, unless one side already carries whitespace at the join, so
 * "meet me at" spoken into gives "meet me at central station" rather than running the words
 * together or doubling the gap. An empty field returns the transcript untouched: that is the
 * ordinary case and it must not gain a leading space.
 *
 * A top-level function in its own file rather than a private method, because
 * `AiSearchInputViewModel` is at its `TooManyFunctions` limit and a local function would not
 * have helped. It is a pure string join with no ViewModel state in it, so it tests directly.
 */
internal fun joinSpokenText(existing: String, spoken: String): String = when {
    existing.isBlank() -> spoken
    spoken.isEmpty() -> existing
    existing.last().isWhitespace() || spoken.first().isWhitespace() -> existing + spoken
    else -> "$existing $spoken"
}
