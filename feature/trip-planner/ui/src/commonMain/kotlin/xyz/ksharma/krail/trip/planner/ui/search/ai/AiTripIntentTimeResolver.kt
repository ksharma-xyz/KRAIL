package xyz.ksharma.krail.trip.planner.ui.search.ai

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.krail.core.aitext.TimeIntent
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.JourneyTimeOptions
import kotlin.time.Clock
import kotlin.time.Instant

private val CLOCK_TIME_REGEX = Regex("""\b(\d{1,2})(?::(\d{2}))?\s*(am|pm|AM|PM)?\b""")
private val RELATIVE_MINUTES_REGEX = Regex("""\bin\s+(\d{1,3})\s*(minutes?|mins?)\b""", RegexOption.IGNORE_CASE)
private val RELATIVE_HOURS_REGEX = Regex("""\bin\s+(\d{1,2})\s*(hours?|hrs?)\b""", RegexOption.IGNORE_CASE)
private const val NOON_HOUR = 12
private const val MIDNIGHT_HOUR = 0
private const val HOURS_PER_HALF_DAY = 12
private const val HOURS_PER_DAY = 24
private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = HOURS_PER_DAY * MINUTES_PER_HOUR
private val HOUR_RANGE = 0 until HOURS_PER_DAY
private val MINUTE_RANGE = 0 until MINUTES_PER_HOUR

private const val ON_THE_HOUR_MINUTE = 0
private const val DAYS_PER_WEEK = 7

private val TODAY_WORDS = setOf("today", "tonight", "this evening", "this afternoon", "this morning")
private const val TOMORROW_WORD = "tomorrow"

// Ordered so the index is the offset from Monday, matching kotlinx.datetime's DayOfWeek
// ordinal. "tues"/"thurs"/"weds" are here because riders write them, not because a general
// abbreviation scheme would produce them.
private val WEEKDAY_WORDS: List<List<String>> = listOf(
    listOf("monday", "mon"),
    listOf("tuesday", "tues", "tue"),
    listOf("wednesday", "weds", "wed"),
    listOf("thursday", "thurs", "thu"),
    listOf("friday", "fri"),
    listOf("saturday", "sat"),
    listOf("sunday", "sun"),
)

private data class ResolvedClock(val hour: Int, val minute: Int, val date: LocalDate)

private data class ClockMatch(val hour: Int, val minute: Int, val meridiem: String, val hasExplicitMinutes: Boolean) {
    // No am/pm and no colon-separated minutes at all reads as too ambiguous to trust (e.g.
    // a bare "9" inside unrelated text) — require either a meridiem marker or an explicit
    // ":MM" to treat this as a real clock-time mention, not a coincidental digit.
    fun isPlausible(): Boolean =
        hour in HOUR_RANGE && minute in MINUTE_RANGE && (meridiem.isNotEmpty() || hasExplicitMinutes)

    fun toHour24(): Int = when {
        meridiem == "am" && hour == NOON_HOUR -> MIDNIGHT_HOUR
        meridiem == "pm" && hour != NOON_HOUR -> hour + HOURS_PER_HALF_DAY
        else -> hour
    }
}

private fun parseClockMatch(match: MatchResult): ClockMatch? {
    val hour = match.groupValues[1].toIntOrNull() ?: return null
    val minute = match.groupValues[2].toIntOrNull() ?: ON_THE_HOUR_MINUTE
    return ClockMatch(
        hour = hour,
        minute = minute,
        meridiem = match.groupValues[3].lowercase(),
        hasExplicitMinutes = match.groupValues[2].isNotEmpty(),
    )
}

/**
 * Turns [TimeIntent.timeText] — a verbatim phrase like "6:30pm" or "in twenty minutes" —
 * into an actual [DateTimeSelectionItem]. Deliberately narrow: this is a bounded, hand-built
 * grammar (no KMP library exists for natural-language date/time parsing — checked klibs.io
 * and the wider ecosystem, nothing suitable), not a general parser. Anything it can't
 * confidently resolve returns `null` rather than guessing an hour — matches this feature's
 * "never guess a field the rider didn't actually say" principle already applied to
 * extraction itself; a `null` here means the confirm card's time chip stays empty and the
 * rider fills it in via the existing time sheet, same as any other unresolved guess.
 *
 * Vague day-parts ("morning", "tonight", "this evening") are intentionally NOT resolved to
 * a specific hour here — guessing "morning = 9am" is exactly the kind of invented precision
 * this principle exists to avoid. A future pass could add them behind an explicit, narrower
 * confidence signal if real usage shows it's worth it.
 *
 * @param timeZone Defaults to Sydney, not the device's own timezone — KRAIL is a Sydney-only
 * app (same fixed-zone convention `core/date-time/DateTimeHelper.kt`'s `AEST_TIMEZONE`
 * already uses throughout). A rider whose phone clock is set to a different zone (travelling,
 * a misconfigured device) would otherwise get a wrong "now" and every relative time
 * ("in 20 minutes") would resolve incorrectly.
 */
internal fun resolveTimeIntent(
    timeIntent: TimeIntent?,
    now: Instant = Clock.System.now(),
    timeZone: TimeZone = TimeZone.of("Australia/Sydney"),
): DateTimeSelectionItem? {
    if (timeIntent == null) return null
    val nowLocal = now.toLocalDateTime(timeZone)
    val option = if (timeIntent.isArrival) JourneyTimeOptions.ARRIVE else JourneyTimeOptions.LEAVE
    val text = timeIntent.timeText.trim()

    // A day named in the sentence overrides the "next occurrence" rollover: a rider who said
    // Friday means Friday, even for a time that has already gone today.
    val namedDay = resolveNamedDay(text, nowLocal.date)

    val resolved = resolveClockTime(text, nowLocal.date, nowLocal.hour, nowLocal.minute, namedDay)
        ?: resolveRelativeMinutes(text, nowLocal.hour, nowLocal.minute, nowLocal.date)
        ?: return null

    return DateTimeSelectionItem(option = option, hour = resolved.hour, minute = resolved.minute, date = resolved.date)
}

/**
 * The date a day word in [text] refers to, or null when no day was named.
 *
 * Only days, never times. "tonight" resolves to today's date and nothing else: turning it into
 * an hour would be inventing precision the rider did not give, which is the same line
 * [resolveTimeIntent] draws for every other vague phrase. Without a clock time alongside it, a
 * day on its own still resolves to nothing, because a date with no time is not a departure.
 *
 * A named weekday means its next occurrence, counting today. "Friday" said on a Friday is that
 * day, not the one a week later, which is what a rider means and also what makes the phrase
 * useful at all on the day itself.
 */
private fun resolveNamedDay(text: String, today: LocalDate): LocalDate? {
    val lower = text.lowercase()
    return when {
        lower.contains(TOMORROW_WORD) -> today.plus(1, DateTimeUnit.DAY)
        TODAY_WORDS.any { lower.contains(it) } -> today
        else -> resolveWeekday(lower, today)
    }
}

private fun resolveWeekday(lowercaseText: String, today: LocalDate): LocalDate? {
    val weekdayIndex = WEEKDAY_WORDS.indexOfFirst { names ->
        names.any { name -> Regex("\\b$name\\b").containsMatchIn(lowercaseText) }
    }
    if (weekdayIndex < 0) return null
    val daysAhead = (weekdayIndex - today.dayOfWeek.ordinal + DAYS_PER_WEEK) % DAYS_PER_WEEK
    return today.plus(daysAhead, DateTimeUnit.DAY)
}

/**
 * "Leave now" — the fallback when the rider gave no time at all, or gave one
 * [resolveTimeIntent] couldn't confidently parse (a vague day-part, for instance). Matches
 * this app's own existing convention (`TimeTableEntry`'s `dateTimeSelectionItem == null`
 * already means "now" downstream) — this just makes that explicit in the confirm card
 * instead of showing an unset-looking "Not set" chip for a value that has an obvious,
 * always-safe default.
 */
internal fun leaveNowDateTimeSelectionItem(
    now: Instant = Clock.System.now(),
    timeZone: TimeZone = TimeZone.of("Australia/Sydney"),
): DateTimeSelectionItem {
    val nowLocal = now.toLocalDateTime(timeZone)
    return DateTimeSelectionItem(
        option = JourneyTimeOptions.LEAVE,
        hour = nowLocal.hour,
        minute = nowLocal.minute,
        date = nowLocal.date,
    )
}

/**
 * Tries every regex match in [text], not just the first — a first match that isn't a
 * plausible clock time (e.g. a bare leading number from unrelated context) must not stop a
 * later, genuinely plausible match from being tried.
 *
 * Also rolls the date to tomorrow when the resolved hour:minute is strictly before [nowHour]:
 * [nowMinute] — an extracted clock time is always meant as the *next* occurrence of that
 * time, matching [resolveRelativeMinutes]'s own day-rollover for the relative-minutes path
 * just below. A match for the exact current minute stays today ("leaving at 9am" said at
 * 9:00am means now, not this time tomorrow).
 *
 * [namedDay] switches that off. A rider who said which day meant it, and guessing forward from
 * their own word would be the app overruling them.
 */
private fun resolveClockTime(
    text: String,
    today: LocalDate,
    nowHour: Int,
    nowMinute: Int,
    namedDay: LocalDate? = null,
): ResolvedClock? {
    val clockMatch = CLOCK_TIME_REGEX.findAll(text)
        .mapNotNull(::parseClockMatch)
        .firstOrNull { it.isPlausible() }
        ?: return null

    val hour24 = clockMatch.toHour24()
    if (hour24 !in HOUR_RANGE) return null

    val hasAlreadyPassedToday = hour24 < nowHour || (hour24 == nowHour && clockMatch.minute < nowMinute)
    val date = namedDay
        ?: if (hasAlreadyPassedToday) today.plus(1, DateTimeUnit.DAY) else today
    return ResolvedClock(hour24, clockMatch.minute, date)
}

private fun resolveRelativeMinutes(text: String, nowHour: Int, nowMinute: Int, today: LocalDate): ResolvedClock? {
    val minutesToAdd = RELATIVE_MINUTES_REGEX.find(text)?.groupValues?.get(1)?.toIntOrNull()
        ?: RELATIVE_HOURS_REGEX.find(text)?.groupValues?.get(1)?.toIntOrNull()?.let { it * MINUTES_PER_HOUR }
        ?: return null

    val totalMinutes = nowHour * MINUTES_PER_HOUR + nowMinute + minutesToAdd
    val dayOffset = totalMinutes / MINUTES_PER_DAY
    val wrappedMinutes = totalMinutes % MINUTES_PER_DAY
    val resolvedDate = if (dayOffset > 0) today.plus(dayOffset, DateTimeUnit.DAY) else today
    return ResolvedClock(wrappedMinutes / MINUTES_PER_HOUR, wrappedMinutes % MINUTES_PER_HOUR, resolvedDate)
}
