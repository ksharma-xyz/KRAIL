package xyz.ksharma.krail.trip.planner.ui.timetable.business

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private val SYDNEY = TimeZone.of("Australia/Sydney")

// Joins the parts of a time or a date so a narrow screen or a large font wraps between them, never inside one.
private const val NBSP = '\u00A0'

private const val NOON = 12
private const val TEN = 10
private const val SHORT_NAME_LENGTH = 3

/**
 * When the alert is in force, from the alert's own start and end fields and nothing else:
 * "Mon 23 Nov, 11:20 PM to Tue 24 Nov, 2:00 AM", "Mon 23 Nov, 11:20 PM to 2:00 AM" when both fall on one
 * day, or "From Mon 23 Nov, 11:20 PM" when NSW gives no end. Spaces inside a date or a time are
 * non-breaking, so the text only wraps between them.
 *
 * The times come from the feed's `active_period`, a data field. The free text NSW writes beside it is
 * not used because the plain-text and HTML versions of the same alert can disagree.
 */
@OptIn(ExperimentalTime::class)
internal fun formatDisruptionWindow(
    startEpochSec: Long,
    endEpochSec: Long?,
    zone: TimeZone = SYDNEY,
): String {
    val start = Instant.fromEpochSeconds(startEpochSec).toLocalDateTime(zone)
    val end = endEpochSec?.let { Instant.fromEpochSeconds(it).toLocalDateTime(zone) }
    return when {
        end == null -> "From ${start.dayAndDate()}, ${start.clock()}"
        end.date == start.date -> "${start.dayAndDate()}, ${start.clock()} to ${end.clock()}"
        else -> "${start.dayAndDate()}, ${start.clock()} to ${end.dayAndDate()}, ${end.clock()}"
    }
}

/** "Mon 23 Nov". */
private fun LocalDateTime.dayAndDate(): String =
    "${dayOfWeek.shortName()}$NBSP$day$NBSP${month.shortName()}"

/** "11:20 PM", the way the rest of the app writes a clock time. */
private fun LocalDateTime.clock(): String {
    val displayHour = if (hour == 0 || hour == NOON) NOON else hour % NOON
    val minutes = if (minute < TEN) "0$minute" else minute.toString()
    return "$displayHour:$minutes$NBSP${if (hour < NOON) "AM" else "PM"}"
}

private fun DayOfWeek.shortName(): String = name.take(SHORT_NAME_LENGTH).lowercase().replaceFirstChar { it.uppercase() }

private fun Month.shortName(): String = name.take(SHORT_NAME_LENGTH).lowercase().replaceFirstChar { it.uppercase() }
