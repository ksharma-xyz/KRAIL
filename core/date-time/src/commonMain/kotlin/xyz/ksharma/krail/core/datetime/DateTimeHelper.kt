package xyz.ksharma.krail.core.datetime

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.krail.core.log.logError
import kotlin.math.absoluteValue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

object DateTimeHelper {

    private const val ERROR_MESSAGE = "Error parsing date string:"

    /**
     * Converts a UTC ISO 8601 date-time string (e.g., "2025-06-13T06:48:13Z")
     * to a 12-hour time format string (e.g., "6:48 am").
     *
     * @receiver String The UTC date-time string in ISO 8601 format.
     * @return String The formatted 12-hour time string.
     */
    @OptIn(ExperimentalTime::class)
    fun String.formatTo12HourTime(): String {
        val localDateTime = Instant.parse(this).toLocalDateTime(TimeZone.UTC)
        val hour =
            if (localDateTime.hour % 12 == 0) 12 else localDateTime.hour % 12 // Ensure 12-hour format
        val minute = localDateTime.minute.toString().padStart(2, '0')
        val amPm = if (localDateTime.hour < 12) "am" else "pm"
        return "$hour:$minute $amPm"
    }

    @OptIn(ExperimentalTime::class)
    fun String.utcToAEST(): String {
        val instant = Instant.parse(this)
        val aestZone = TimeZone.of("Australia/Sydney")
        val localDateTime = instant.toLocalDateTime(aestZone)
        return localDateTime.toString()
    }

    @OptIn(ExperimentalTime::class)
    fun String.utcToLocalDateTimeAEST(): LocalDateTime {
        val instant = Instant.parse(this)
        val aestZone = TimeZone.of("Australia/Sydney")
        val localDateTime = instant.toLocalDateTime(aestZone)
        return localDateTime
    }

    fun LocalDateTime.toHHMM(): String {
        val hour = if (this.hour % 12 == 0) 12 else this.hour % 12 // Ensure 12-hour format
        val minute = this.minute.toString().padStart(2, '0')
        val amPm = if (this.hour < 12) "AM" else "PM"
        return "$hour:$minute $amPm"
    }

    /* fun String.aestToHHMM(): String {
         val dateTimeString = if (this.length == 16) "$this:00" else this
         val localDateTime = Instant.parse(dateTimeString).toLocalDateTime(TimeZone.of("Australia/Sydney"))
         val hour = if (localDateTime.hour % 12 == 0) 12 else localDateTime.hour % 12 // Ensure 12-hour format
         val minute = localDateTime.minute.toString().padStart(2, '0')
         val amPm = if (localDateTime.hour < 12) "AM" else "PM"
         return "$hour:$minute $amPm"
     }*/

    @OptIn(ExperimentalTime::class)
    fun calculateTimeDifferenceFromNow(
        utcDateString: String,
        now: Instant = Clock.System.now(),
    ): Duration {
        val instant = Instant.parse(utcDateString)
        return instant - now
    }

    fun Duration.toGenericFormattedTimeString(): String {
        val totalMinutes = this.toLong(DurationUnit.MINUTES)
        val hours = this.toLong(DurationUnit.HOURS)
        val partialMinutes = totalMinutes - (hours * 60.minutes.inWholeMinutes)

        return when {
            totalMinutes < 0 -> "${totalMinutes.absoluteValue} ${if (totalMinutes.absoluteValue == 1L) "min" else "mins"} ago"
            totalMinutes == 0L -> "Now"
            hours == 1L -> "in ${hours.absoluteValue}h ${partialMinutes.absoluteValue}m"
            hours >= 2 -> "in ${hours.absoluteValue}h"
            else -> "in ${totalMinutes.absoluteValue} ${if (totalMinutes.absoluteValue == 1L) "min" else "mins"}"
        }
    }

    fun Duration.toFormattedDurationTimeString(): String {
        val totalMinutes = this.toLong(DurationUnit.MINUTES)
        val hours = this.toLong(DurationUnit.HOURS)
        val partialMinutes = totalMinutes - (hours * 60.minutes.inWholeMinutes)

        return when {
            hours >= 1 && partialMinutes == 0L -> "${hours.absoluteValue}h"
            hours >= 1 -> "${hours.absoluteValue}h ${partialMinutes.absoluteValue}m"
            else -> "${totalMinutes.absoluteValue} ${if (totalMinutes.absoluteValue == 1L) "min" else "mins"}"
        }
    }

    @OptIn(ExperimentalTime::class)
    fun calculateTimeDifference(
        utcDateString1: String,
        utcDateString2: String,
    ): Duration {
        val instant1 = Instant.parse(utcDateString1)
        val instant2 = Instant.parse(utcDateString2)
        return (instant1 - instant2).absoluteValue
    }

    /**
     * Returns true if the given date is in the future, false otherwise.
     */
    @OptIn(ExperimentalTime::class)
    fun LocalDate?.isFuture(): Boolean {
        return this?.let {
            it > Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        } ?: false
    }

    @OptIn(ExperimentalTime::class)
    fun Instant.isBefore(other: Instant): Boolean = this < other

    @OptIn(ExperimentalTime::class)
    fun Instant.isAfter(other: Instant): Boolean = this > other

    /**
     * Parses a date-time string in the format "yyyy-MM-dd'T'HH:mm:ss"
     * and returns the time in "h:mm AM/PM" (12-hour) format.
     *
     * Example: "2025-06-13T07:05:55" -> "7:05 AM"
     */
    fun String.toSimple12HourTime(): String {
        val localDateTime = LocalDateTime.parse(this)
        val hour = if (localDateTime.hour % 12 == 0) 12 else localDateTime.hour % 12
        val minute = localDateTime.minute.toString().padStart(2, '0')
        val amPm = if (localDateTime.hour < 12) "AM" else "PM"
        return "$hour:$minute $amPm"
    }

    /**
     * Checks if the string represents a date in the future.
     * The string should be in ISO-8601 format (e.g., "2023-12-31").
     *
     * @receiver String The date string to check.
     * @return Boolean true if the date is in the future, false otherwise.
     */
    @OptIn(ExperimentalTime::class)
    fun String.isDateInFuture(): Boolean {
        return runCatching {
            val localDate = LocalDate.parse(this)
            val today = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            localDate > today
        }.onFailure { e ->
            logError("$ERROR_MESSAGE: $this", e)
        }.getOrDefault(false)
    }

    /**
     * Checks if the string represents today's date or a future date.
     * The string should be in ISO-8601 format (e.g., "2023-12-31").
     * Returns true if the date is today or in the future, false if it's in the past.
     *
     * @receiver String The date string to check.
     * @return Boolean true if the date is today or in the future, false otherwise.
     */
    @OptIn(ExperimentalTime::class)
    fun String.isDateTodayOrInFuture(): Boolean {
        return runCatching {
            val localDate = LocalDate.parse(this)
            val today = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            localDate >= today
        }.onFailure { e ->
            logError("$ERROR_MESSAGE: $this", e)
        }.getOrDefault(false)
    }

    /**
     * Checks if the string represents today's date or a past date.
     * The string should be in ISO-8601 format (e.g., "2023-12-31").
     * Returns true if the date is today or in the past, false if it's in the future.
     *
     * @receiver String The date string to check.
     * @return Boolean true if the date is today or in the past, false otherwise.
     */
    @OptIn(ExperimentalTime::class)
    fun String.isDateTodayOrInPast(): Boolean {
        return runCatching {
            val localDate = LocalDate.parse(this)
            val today = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            localDate <= today
        }.onFailure { e ->
            logError("$ERROR_MESSAGE $this", e)
        }.getOrDefault(false)
    }
}
