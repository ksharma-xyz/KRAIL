package xyz.ksharma.krail.trip.planner.ui.search.ai.resolve

/**
 * The rider's word for a place, and the other words that mean the same place.
 *
 * Labels are matched exactly, which is what stops "home" capturing "Homebush". The cost is that
 * a rider has to use the app's word rather than their own: someone whose label is `Work` says
 * "office by Monday morning" and gets nothing, because "office" is not "work".
 *
 * Every entry here is still a whole word matched exactly, so the Homebush protection is
 * untouched. Only the vocabulary widens.
 *
 * Kept deliberately small. A synonym that is nearly right is worse than none: it sends a rider
 * to a stop they did not name, and they have no way to see why. Words go in only when they are
 * the same place to anybody, in Australian English, without context. "Place", "mine" and "the
 * city" are all excluded for that reason.
 */
internal object LabelSynonyms {

    private val GROUPS: List<Set<String>> = listOf(
        setOf("home", "house"),
        setOf("work", "office", "job", "workplace"),
        setOf("uni", "university", "campus", "college"),
        setOf("school"),
        setOf("gym"),
    )

    /**
     * Labels for a place the rider goes on a schedule, in every accepted wording.
     *
     * Used to keep a commute off a weekend suggestion. Derived from the groups rather than
     * listed again, so adding "office" to the work group covers both matching and the weekend
     * rule at once.
     */
    val commuteLabels: Set<String> =
        GROUPS.filter { group -> group.first() in setOf("work", "uni", "school") }.flatten().toSet()

    /**
     * The group a word belongs to, or the word itself when it belongs to none.
     *
     * Returning the word unchanged means a rider's own invented label ("Nan's", "Bouldering")
     * still compares equal to itself and nothing else, so adding groups here can never break
     * a label that is not in one.
     */
    fun canonical(label: String): String {
        val normalised = label.trim().lowercase()
        return GROUPS.firstOrNull { normalised in it }?.first() ?: normalised
    }

    /**
     * True when this word is one the app treats as a rider's name for a place.
     *
     * These must never reach a stop-name search. "work" with no Work label set matched
     * **Blacktown Workers Club**, and a rider who says "work" has never in the history of
     * anything meant a club whose name contains those letters. A label word either resolves to
     * a label or to nothing at all.
     */
    fun isLabelWord(word: String): Boolean {
        val normalised = word.trim().lowercase()
        return GROUPS.any { normalised in it }
    }

    /** True when two labels name the same kind of place. */
    fun sameMeaning(first: String, second: String): Boolean = canonical(first) == canonical(second)
}
