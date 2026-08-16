package xyz.ksharma.krail.trip.planner.ui.components.ai

/**
 * When the suggestion applies, which way the journey runs, and what time it asks for.
 *
 * This table is the whole time-and-day dimension of the suggestion line, as data rather than as
 * control flow. Adding a situation ("Friday night", "public holiday", "school term morning") is
 * one row here, in a list whose order IS the priority: first match wins.
 *
 * It was an `if` for the weekend and another `if` for the clause, which was shorter and not
 * extendible. A seventh case meant editing the middle of a function and hoping the ordering
 * still held; here a case is a line, and the ordering is visible.
 *
 * The other dimension, WHICH two stops, is deliberately not in here. That is a fallback ladder
 * over the rider's own data (labels, then a saved trip, then two known stations) and it runs the
 * same way regardless of the hour. Putting it in every row would mean repeating it in every row.
 *
 * ## Configurable later
 *
 * [suggestionSituations] is the only seam that needs to change. A Remote Config version returns
 * the parsed list instead of the constant one and nothing that renders a suggestion has to know.
 * That is deliberately NOT wired yet: copy that changes a few times a year does not need a
 * remote pipe, and a remote-driven string that renders to every rider is its own validation
 * problem. Build the seam, wire the pipe when there is something that cannot wait for a release.
 */
internal data class SuggestionSituation(
    /** Stable across copy changes, so config and tests can name a row without quoting it. */
    val id: String,
    val dayKind: DayKind,
    /** Inclusive. May be greater than [toHour], which means the band wraps past midnight. */
    val fromHour: Int,
    /** Exclusive. */
    val toHour: Int,
    val direction: Direction,
    /**
     * Appended verbatim, leading space included. Must still be true at every hour in the band:
     * "by 9am" at 11am would be the app suggesting a trip into the past.
     */
    val clause: String,
)

internal enum class DayKind { Weekday, Weekend, Any }

/** Out is home to the other place; Back is the reverse. */
internal enum class Direction { Out, Back }

// Relative, so it is correct at any hour. The absolute clauses each sit in a band that ends
// before the time they name.
internal const val CLAUSE_SOON = " in 20 minutes"

private val DEFAULT_SITUATIONS = listOf(
    // Arrive-by is what a weekday morning is for, and the only place the app demonstrates that
    // this surface understands a deadline rather than just a departure.
    // Ends AT nine, not after it. Running to ten meant a rider opening this at 09:30 was told
    // to try arriving by 9am, which is the app suggesting a trip into the past.
    SuggestionSituation("weekday-morning", DayKind.Weekday, 5, 9, Direction.Out, " by 9am"),
    SuggestionSituation("weekday-midday", DayKind.Weekday, 9, 15, Direction.Out, CLAUSE_SOON),
    // From mid-afternoon the journey people are about to take is the one home.
    SuggestionSituation("weekday-evening", DayKind.Weekday, 15, 18, Direction.Back, " after 6pm"),
    SuggestionSituation("weekday-night", DayKind.Weekday, 18, 23, Direction.Back, CLAUSE_SOON),
    // No "by 9am" at a weekend: nobody is being got to work for nine on a Saturday, and the
    // suggestion is the one line on the screen that is supposed to show the app paying attention.
    SuggestionSituation("weekend-day", DayKind.Weekend, 5, 15, Direction.Out, CLAUSE_SOON),
    SuggestionSituation("weekend-evening", DayKind.Weekend, 15, 23, Direction.Back, CLAUSE_SOON),
    // Last, and Any, so it catches whatever the bands above did not on either kind of day.
    SuggestionSituation("late-night", DayKind.Any, 23, 5, Direction.Out, CLAUSE_SOON),
)

/** The single seam. See the note on [SuggestionSituation] about making this remote. */
internal fun suggestionSituations(): List<SuggestionSituation> = DEFAULT_SITUATIONS

/**
 * First row whose day and hour both match.
 *
 * Falls back to the first row rather than returning null: a rider opening this screen at an hour
 * somebody forgot to cover should see a slightly wrong suggestion, never a blank space where the
 * one explanatory line was meant to be. `everyHourOfEveryDayHasASituation` in the test makes
 * that fallback unreachable, and is there to keep it unreachable as rows are added.
 */
internal fun situationFor(
    hour: Int,
    isWeekend: Boolean,
    situations: List<SuggestionSituation> = suggestionSituations(),
): SuggestionSituation {
    val dayKind = if (isWeekend) DayKind.Weekend else DayKind.Weekday
    return situations.firstOrNull { situation ->
        situation.matchesDay(dayKind) && situation.matchesHour(hour)
    } ?: situations.first()
}

private fun SuggestionSituation.matchesDay(dayKind: DayKind): Boolean =
    this.dayKind == DayKind.Any || this.dayKind == dayKind

private fun SuggestionSituation.matchesHour(hour: Int): Boolean =
    if (fromHour <= toHour) hour in fromHour until toHour else hour >= fromHour || hour < toHour
