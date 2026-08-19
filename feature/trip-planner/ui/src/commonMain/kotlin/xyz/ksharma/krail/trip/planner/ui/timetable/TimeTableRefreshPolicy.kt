package xyz.ksharma.krail.trip.planner.ui.timetable

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Whether the timetable on screen is a live board that should keep re-fetching.
 *
 * Lives in its own file rather than on [TimeTableViewModel] so the decision can be table-tested
 * without standing up a ViewModel — the rule is about two timestamps, nothing more.
 *
 * The rule: a board pinned to a moment that has not arrived yet is a **plan**, and re-fetching
 * it changes nothing a rider can see. A board with no pin, or pinned to a moment that has
 * passed, is a **live board** and stays fresh.
 *
 * This used to compare calendar dates (`LocalDate.isFuture()`), which answers a coarser
 * question: it treats "6pm this evening" as not-future because the date is today, and refreshes
 * a plan every 30 seconds for the rest of the day. Comparing instants is the same rule at the
 * granularity the rider actually chose.
 */
@OptIn(ExperimentalTime::class)
internal fun shouldAutoRefresh(
    selection: DateTimeSelectionItem?,
    now: Instant,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): Boolean {
    val pinnedAt = selection?.toInstant(zone) ?: return true
    return pinnedAt <= now
}

/**
 * The wall-clock instant a [DateTimeSelectionItem] names, in [zone].
 *
 * Both `LEAVE` and `ARRIVE` resolve the same way: the selection names one moment, and whether
 * the rider meant to depart or arrive then does not change whether that moment has passed.
 */
@OptIn(ExperimentalTime::class)
internal fun DateTimeSelectionItem.toInstant(zone: TimeZone = TimeZone.currentSystemDefault()): Instant =
    LocalDateTime(date, LocalTime(hour, minute)).toInstant(zone)
