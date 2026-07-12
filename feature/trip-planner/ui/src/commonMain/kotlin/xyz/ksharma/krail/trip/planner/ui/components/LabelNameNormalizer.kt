package xyz.ksharma.krail.trip.planner.ui.components

/**
 * Label names are short pill/chip text (e.g. "Home", "Gym") rendered inline next to
 * transport-mode icons and in a horizontally-scrolling row — there's no wrap/ellipsis
 * handling anywhere a label renders, so an unbounded name would break that layout.
 * Enforced at the TextField (`maxLength`) in both AssignNewLabelSheet and
 * ManageStopLabelRow's rename field. Matches the cap SearchTopBar already uses for
 * its (longer-lived) search query field, halved since labels are meant to be short.
 */
internal const val LABEL_NAME_MAX_LENGTH = 20

/**
 * Strips emoji and exotic characters from a user-typed label name, collapses
 * whitespace, and caps the result at [LABEL_NAME_MAX_LENGTH]. We use this both before
 * saving and for case-insensitive dedupe so that `🏠 Home`, `Home`, and `home ` all
 * resolve to the same canonical name.
 *
 * This is the actual DB-write path (`SearchStopViewModel`'s `CreateLabel`/
 * `RenameLabel` handlers call it before persisting), so it's the real enforcement
 * point for both the character set and the length cap — not just a UI nicety. The
 * TextField-level `filter`/`maxLength` in AssignNewLabelSheet and
 * ManageStopLabelRow's rename field exist so the user sees the constraint as they
 * type, but this function is what a caller that skipped the UI (a test, a future
 * entry point) still can't bypass.
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
    return cleaned.trim().replace(Regex("\\s+"), " ").take(LABEL_NAME_MAX_LENGTH).trim()
}

/** Returns true if [a] and [b] resolve to the same canonical label name. */
internal fun labelNamesMatch(a: String, b: String): Boolean =
    normaliseLabelName(a).equals(normaliseLabelName(b), ignoreCase = true)

/**
 * Keystroke-level allowlist mirroring [normaliseLabelName]'s character rules. Passed
 * to the TextField `filter` param in both AssignNewLabelSheet and ManageStopLabelRow's
 * rename field so disallowed characters (emoji, punctuation) never appear even
 * transiently, instead of being silently stripped later.
 */
internal fun filterLabelNameInput(input: CharSequence): CharSequence =
    input.filter { c -> c.isLetterOrDigit() || c.isWhitespace() || c == '-' || c == '_' || c == '\'' }
