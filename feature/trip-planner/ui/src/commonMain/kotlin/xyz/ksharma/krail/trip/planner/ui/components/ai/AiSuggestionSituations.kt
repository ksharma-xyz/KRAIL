package xyz.ksharma.krail.trip.planner.ui.components.ai

/**
 * When the suggestion applies, what shape it takes, which way it runs, and what time it asks
 * for.
 *
 * This table is the whole time-and-day dimension of the suggestion line, as data rather than as
 * control flow. Adding a situation ("public holiday", "school term morning") is one row, in a
 * list whose order IS the priority: first match wins.
 *
 * The other dimension, WHICH two stops, is deliberately not in here. That is a fallback ladder
 * over the rider's own data and it runs the same way at every hour, so putting it in every row
 * would mean repeating it in every row.
 *
 * ## Configurable later
 *
 * [suggestionSituations] is the only seam that needs to change. A Remote Config version returns
 * the parsed list instead of the constant one and nothing that renders a suggestion has to know.
 */
internal data class SuggestionSituation(
    /** Stable across copy changes, so config and tests can name a row without quoting it. */
    val id: String,
    val dayKind: DayKind,
    /** Inclusive. May be greater than [toHour], which means the band wraps past midnight. */
    val fromHour: Int,
    /** Exclusive. */
    val toHour: Int,
    val shape: SuggestionShape,
    val direction: Direction,
    /**
     * Whether a commute label may be used here.
     *
     * False through the weekend, because suggesting the trip to work on a Saturday is the app
     * saying out loud that it has not been paying attention. True again on a Sunday evening,
     * which is the one weekend moment when the commute is exactly what somebody is thinking
     * about.
     */
    val allowsCommute: Boolean,
    /**
     * Appended verbatim, leading space included. Must still be true at every hour in the band:
     * "by 9pm" at 10pm would be the app suggesting a trip into the past.
     */
    val clause: String,
)

internal enum class DayKind { Weekday, Weekend, Sunday, Any }

/** Out is home to the other place; Back is the reverse. */
internal enum class Direction { Out, Back }

/**
 * Whether the line names both ends or only where the rider is going.
 *
 * [HomeBound] exists because the origin can be left out entirely — the nearby-stop resolver
 * fills it from where they are — and nothing on screen ever showed that. It is also the honest
 * suggestion for a weekend evening: the app has no idea where somebody is on a Saturday night,
 * but it is a fair bet they will want to get home from it.
 */
internal enum class SuggestionShape { Pair, HomeBound }

// Relative, so it is correct at any hour. The absolute clauses each sit in a band that ends
// before the time they name.
internal const val CLAUSE_SOON = " in 20 minutes"

private val DEFAULT_SITUATIONS = listOf(
    // Arrive-by is what a weekday morning is for, and the only place the app demonstrates that
    // this surface understands a deadline rather than just a departure. Ends AT nine: running
    // later meant a rider at 09:30 was told to try arriving by 9am.
    SuggestionSituation(
        id = "weekday-morning",
        dayKind = DayKind.Weekday,
        fromHour = 5,
        toHour = 9,
        shape = SuggestionShape.Pair,
        direction = Direction.Out,
        allowsCommute = true,
        clause = " by 9am",
    ),
    SuggestionSituation(
        id = "weekday-midday",
        dayKind = DayKind.Weekday,
        fromHour = 9,
        toHour = 15,
        shape = SuggestionShape.Pair,
        direction = Direction.Out,
        allowsCommute = true,
        clause = CLAUSE_SOON,
    ),
    // From mid-afternoon the journey people are about to take is the one home.
    SuggestionSituation(
        id = "weekday-evening",
        dayKind = DayKind.Weekday,
        fromHour = 15,
        toHour = 18,
        shape = SuggestionShape.Pair,
        direction = Direction.Back,
        allowsCommute = true,
        clause = " after 6pm",
    ),
    SuggestionSituation(
        id = "weekday-night",
        dayKind = DayKind.Weekday,
        fromHour = 18,
        toHour = 21,
        shape = SuggestionShape.HomeBound,
        direction = Direction.Back,
        allowsCommute = true,
        clause = " by 9pm",
    ),
    // Sunday from mid-afternoon is when people check tomorrow's commute. This is the one place
    // the commute is welcome at a weekend, and it has to say tomorrow or it reads as a trip
    // being suggested for tonight.
    SuggestionSituation(
        id = "sunday-evening",
        dayKind = DayKind.Sunday,
        fromHour = 15,
        toHour = 23,
        shape = SuggestionShape.Pair,
        direction = Direction.Out,
        allowsCommute = true,
        clause = " by 9am tomorrow",
    ),
    // No "by 9am" at a weekend: nobody is being got to work for nine on a Saturday.
    SuggestionSituation(
        id = "weekend-day",
        dayKind = DayKind.Weekend,
        fromHour = 5,
        toHour = 15,
        shape = SuggestionShape.Pair,
        direction = Direction.Out,
        allowsCommute = false,
        clause = CLAUSE_SOON,
    ),
    SuggestionSituation(
        id = "weekend-evening",
        dayKind = DayKind.Weekend,
        fromHour = 15,
        toHour = 21,
        shape = SuggestionShape.HomeBound,
        direction = Direction.Back,
        allowsCommute = false,
        clause = " by 9pm",
    ),
    // Past nine the deadline has gone, so the clause turns relative rather than naming an hour
    // that has already been and gone.
    SuggestionSituation(
        id = "late-night",
        dayKind = DayKind.Any,
        fromHour = 21,
        toHour = 5,
        shape = SuggestionShape.HomeBound,
        direction = Direction.Back,
        allowsCommute = true,
        clause = CLAUSE_SOON,
    ),
)

/** The single seam. See the note on [SuggestionSituation] about making this remote. */
internal fun suggestionSituations(): List<SuggestionSituation> = DEFAULT_SITUATIONS

/**
 * First row whose day and hour both match.
 *
 * Sunday matches [DayKind.Sunday] and [DayKind.Weekend], in that order, so a Sunday-only row
 * placed above the weekend rows shadows them and everything else still falls through to the
 * general weekend behaviour.
 *
 * Falls back to the first row rather than returning null: a rider opening this screen at an
 * hour somebody forgot to cover should see a slightly wrong suggestion, never a blank space
 * where the one explanatory line was meant to be. A test makes that fallback unreachable and
 * exists to keep it unreachable as rows are added.
 */
internal fun situationFor(
    hour: Int,
    isWeekend: Boolean,
    isSunday: Boolean = false,
    situations: List<SuggestionSituation> = suggestionSituations(),
): SuggestionSituation {
    val kinds = when {
        isSunday -> setOf(DayKind.Sunday, DayKind.Weekend, DayKind.Any)
        isWeekend -> setOf(DayKind.Weekend, DayKind.Any)
        else -> setOf(DayKind.Weekday, DayKind.Any)
    }
    return situations.firstOrNull { situation ->
        situation.dayKind in kinds && situation.matchesHour(hour)
    } ?: situations.first()
}

private fun SuggestionSituation.matchesHour(hour: Int): Boolean =
    if (fromHour <= toHour) hour in fromHour until toHour else hour >= fromHour || hour < toHour
