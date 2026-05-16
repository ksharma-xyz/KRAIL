package xyz.ksharma.krail.core.datetime

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.krail.core.datetime.DateTimeHelper.formatTo12HourTime
import xyz.ksharma.krail.core.datetime.DateTimeHelper.isDateInFuture
import xyz.ksharma.krail.core.datetime.DateTimeHelper.isDateTodayOrInFuture
import xyz.ksharma.krail.core.datetime.DateTimeHelper.isDateTodayOrInPast
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toDepartureRelativeString
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toFormattedDurationTimeString
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toGenericFormattedTimeString
import xyz.ksharma.krail.core.datetime.DateTimeHelper.utcToAEST
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class DateTimeHelperTest {

    @OptIn(ExperimentalTime::class)
    @Test
    fun testCalculateTimeDifferenceFromNow() {
        val difference = DateTimeHelper.calculateTimeDifferenceFromNow(
            utcDateString = "2024-10-07T09:00:00Z",
            now = Instant.parse("2024-10-07T08:20:00Z"),
        )
        assertEquals(40L, difference.inWholeMinutes)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testCalculateTimeDifferenceNextDay() {
        val difference = DateTimeHelper.calculateTimeDifferenceFromNow(
            utcDateString = "2024-10-08T09:00:00Z",
            now = Instant.parse("2024-10-07T08:20:00Z"),
        )
        assertEquals(1480L, difference.inWholeMinutes)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testCalculateTimeDifferencePreviousDay() {
        val difference = DateTimeHelper.calculateTimeDifferenceFromNow(
            utcDateString = "2024-10-06T09:00:00Z",
            now = Instant.parse("2024-10-07T08:20:00Z"),
        )
        assertEquals(-1400L, difference.inWholeMinutes)
    }

    @Test
    fun testFormatTo12HourTime() {
        assertEquals("12:00 am", "2024-10-07T00:00:00Z".formatTo12HourTime())
        assertEquals("1:00 am", "2024-10-07T01:00:00Z".formatTo12HourTime())
        assertEquals("12:00 pm", "2024-10-07T12:00:00Z".formatTo12HourTime())
        assertEquals("1:00 pm", "2024-10-07T13:00:00Z".formatTo12HourTime())
    }

    @Test
    fun testUtcToAEST() {
        assertEquals("2024-10-07T11:00:10", "2024-10-07T00:00:10Z".utcToAEST())
        assertEquals("2024-10-07T12:00", "2024-10-07T01:00:00Z".utcToAEST())
        assertEquals("2024-10-07T12:00:23", "2024-10-07T01:00:23Z".utcToAEST())
    }

    // region toGenericFormattedTimeString

    @Test
    fun `toGenericFormattedTimeString — past departures under 1 h`() {
        assertEquals("1 min ago", (-1).minutes.toGenericFormattedTimeString())
        assertEquals("2 mins ago", (-2).minutes.toGenericFormattedTimeString())
        assertEquals("40 mins ago", (-40).minutes.toGenericFormattedTimeString())
        assertEquals("59 mins ago", (-59).minutes.toGenericFormattedTimeString())
    }

    @Test
    fun `toGenericFormattedTimeString — past departures 1h to 2h show Xh Ym ago`() {
        assertEquals("1h ago", (-60).minutes.toGenericFormattedTimeString())
        assertEquals("1h 1m ago", (-61).minutes.toGenericFormattedTimeString())
        assertEquals("1h 20m ago", (-80).minutes.toGenericFormattedTimeString())
        assertEquals("1h 30m ago", (-90).minutes.toGenericFormattedTimeString())
        assertEquals("1h 59m ago", (-119).minutes.toGenericFormattedTimeString())
    }

    @Test
    fun `toGenericFormattedTimeString — past departures 2h or more drop minutes`() {
        assertEquals("2 hours ago", (-120).minutes.toGenericFormattedTimeString())
        assertEquals("3 hours ago", (-180).minutes.toGenericFormattedTimeString())
    }

    @Test
    fun `toGenericFormattedTimeString — Now zone within 15 s each side`() {
        // Exactly zero
        assertEquals("Now", 0.minutes.toGenericFormattedTimeString())
        // Positive — right at and just inside the threshold
        assertEquals("Now", 15.seconds.toGenericFormattedTimeString())
        // Negative — up to 59 s in the past falls in "Now" (totalMinutes truncates to 0)
        assertEquals("Now", (-1).seconds.toGenericFormattedTimeString())
        assertEquals("Now", (-15).seconds.toGenericFormattedTimeString())
        assertEquals("Now", (-59).seconds.toGenericFormattedTimeString())
    }

    @Test
    fun `toGenericFormattedTimeString — 16 s to 59 s floors to in 1 min`() {
        assertEquals("in 1 min", 16.seconds.toGenericFormattedTimeString())
        assertEquals("in 1 min", 30.seconds.toGenericFormattedTimeString())
        assertEquals("in 1 min", 59.seconds.toGenericFormattedTimeString())
    }

    @Test
    fun `toGenericFormattedTimeString — minutes range`() {
        assertEquals("in 1 min", 1.minutes.toGenericFormattedTimeString())
        assertEquals("in 2 mins", 2.minutes.toGenericFormattedTimeString())
        assertEquals("in 30 mins", 30.minutes.toGenericFormattedTimeString())
        assertEquals("in 59 mins", 59.minutes.toGenericFormattedTimeString())
    }

    @Test
    fun `toGenericFormattedTimeString — 1h range shows partial minutes`() {
        assertEquals("in 1h 0m", 60.minutes.toGenericFormattedTimeString())
        assertEquals("in 1h 1m", 61.minutes.toGenericFormattedTimeString())
        assertEquals("in 1h 20m", 80.minutes.toGenericFormattedTimeString())
        assertEquals("in 1h 59m", 119.minutes.toGenericFormattedTimeString())
    }

    @Test
    fun `toGenericFormattedTimeString — 2 h or more drops minutes`() {
        assertEquals("in 2h", 120.minutes.toGenericFormattedTimeString())
        assertEquals("in 3h", 180.minutes.toGenericFormattedTimeString())
        assertEquals("in 5h", 300.minutes.toGenericFormattedTimeString())
    }

    // endregion

    // region toDepartureRelativeString
    //
    // Fixed `now` = 2024-06-15T01:00:00Z → AEST (UTC+10 in June) = 2024-06-15 11:00 AM
    // June 15, 2024 = Saturday.

    @OptIn(ExperimentalTime::class)
    @Test
    fun `toDepartureRelativeString — past departures delegate to relative label`() {
        val now = Instant.parse("2024-06-15T01:00:00Z")
        // 5 s ago → within Now zone
        assertEquals("Now", "2024-06-15T00:59:55Z".toDepartureRelativeString(now))
        // 59 s ago → still Now (totalMinutes truncates to 0)
        assertEquals("Now", "2024-06-15T00:59:01Z".toDepartureRelativeString(now))
        // Exactly 1 min ago → "1 min ago"
        assertEquals("1 min ago", "2024-06-15T00:59:00Z".toDepartureRelativeString(now))
        // 40 mins ago
        assertEquals("40 mins ago", "2024-06-15T00:20:00Z".toDepartureRelativeString(now))
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `toDepartureRelativeString — Now and imminent future`() {
        val now = Instant.parse("2024-06-15T01:00:00Z")
        assertEquals("Now", "2024-06-15T01:00:00Z".toDepartureRelativeString(now))
        assertEquals("Now", "2024-06-15T01:00:15Z".toDepartureRelativeString(now))
        assertEquals("in 1 min", "2024-06-15T01:00:16Z".toDepartureRelativeString(now))
        assertEquals("in 1 min", "2024-06-15T01:00:59Z".toDepartureRelativeString(now))
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `toDepartureRelativeString — minutes and hours under threshold`() {
        val now = Instant.parse("2024-06-15T01:00:00Z")
        assertEquals("in 1 min", "2024-06-15T01:01:00Z".toDepartureRelativeString(now))
        assertEquals("in 40 mins", "2024-06-15T01:40:00Z".toDepartureRelativeString(now))
        assertEquals("in 59 mins", "2024-06-15T01:59:00Z".toDepartureRelativeString(now))
        assertEquals("in 1h 0m", "2024-06-15T02:00:00Z".toDepartureRelativeString(now))
        assertEquals("in 1h 20m", "2024-06-15T02:20:00Z".toDepartureRelativeString(now))
        // 2h+ drops partial minutes (by design — hours are enough at that distance)
        assertEquals("in 2h", "2024-06-15T03:00:00Z".toDepartureRelativeString(now))
        // 3h 59m is just under the 4h contextual threshold; hours=3 → "in 3h"
        assertEquals("in 3h", "2024-06-15T04:59:00Z".toDepartureRelativeString(now))
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `toDepartureRelativeString — at and beyond 4h switches to absolute label`() {
        // now = 2024-06-15T01:00:00Z (11:00 AM AEST, Saturday Jun 15)
        val now = Instant.parse("2024-06-15T01:00:00Z")

        // Exactly 4h later → 15:00 AEST on Jun 15 = "Today at 3:00 PM"
        assertEquals("Today at 3:00 PM", "2024-06-15T05:00:00Z".toDepartureRelativeString(now))

        // Same day, evening → 20:00 AEST = "Today at 8:00 PM"
        assertEquals("Today at 8:00 PM", "2024-06-15T10:00:00Z".toDepartureRelativeString(now))

        // Next day (AEST date crosses midnight) → Jun 16 = "Tomorrow at 12:00 AM"
        assertEquals("Tomorrow at 12:00 AM", "2024-06-15T14:00:00Z".toDepartureRelativeString(now))

        // Tomorrow afternoon → "Tomorrow at 3:00 PM"
        assertEquals("Tomorrow at 3:00 PM", "2024-06-16T05:00:00Z".toDepartureRelativeString(now))

        // Jun 17 (Monday) → "Mon 17 Jun at 11:00 AM"
        assertEquals("Mon 17 Jun at 11:00 AM", "2024-06-17T01:00:00Z".toDepartureRelativeString(now))
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `toDepartureRelativeString — midnight boundary stays relative when under threshold`() {
        // 23:55 AEST (13:55 UTC) → departure 10 min later crosses into the next calendar day
        // but is still only 10 minutes away — must stay relative
        val now = Instant.parse("2024-06-15T13:55:00Z") // 23:55 AEST
        assertEquals("in 10 mins", "2024-06-15T14:05:00Z".toDepartureRelativeString(now))
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `toDepartureRelativeString — midnight boundary switches to absolute when over threshold`() {
        // 23:00 AEST (13:00 UTC) → departure 5 h later crosses midnight and is over threshold
        val now = Instant.parse("2024-06-15T13:00:00Z") // 23:00 AEST
        val result = "2024-06-15T18:00:00Z".toDepartureRelativeString(now) // 04:00 AEST next day
        assertEquals("Tomorrow at 4:00 AM", result)
    }

    // endregion

    @Test
    fun testToFormattedDurationTimeString() {
        assertEquals("1h 20m", 80.minutes.toFormattedDurationTimeString())
        assertEquals("2h", 120.minutes.toFormattedDurationTimeString())
        assertEquals("40 mins", 40.minutes.toFormattedDurationTimeString())
    }

    @Test
    fun testCalculateTimeDifference() {
        val difference1 = DateTimeHelper.calculateTimeDifference(
            utcDateString1 = "2024-10-07T09:00:00Z",
            utcDateString2 = "2024-10-07T08:20:00Z",
        )
        assertEquals(40L, difference1.inWholeMinutes)

        val difference2 = DateTimeHelper.calculateTimeDifference(
            utcDateString1 = "2024-10-07T09:00:00Z",
            utcDateString2 = "2024-10-06T09:00:00Z",
        )
        assertEquals(1440L, difference2.inWholeMinutes)

        val difference3 = DateTimeHelper.calculateTimeDifference(
            utcDateString1 = "2024-10-07T09:00:00Z",
            utcDateString2 = "2024-10-07T09:00:00Z",
        )
        assertEquals(0L, difference3.inWholeMinutes)
    }


    // region isDateInFuture tests
    @OptIn(ExperimentalTime::class)
    @Test
    fun testIsDateInFuture_validFutureDate() {
        val futureDate = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
            .plus(1, DateTimeUnit.DAY)
            .toString()
        assertEquals(true, futureDate.isDateInFuture())
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testIsDateInFuture_todayDate() {
        val todayDate = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
            .toString()
        assertEquals(false, todayDate.isDateInFuture())
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testIsDateInFuture_pastDate() {
        val pastDate = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
            .minus(1, DateTimeUnit.DAY)
            .toString()
        assertEquals(false, pastDate.isDateInFuture())
    }

    @Test
    fun testIsDateInFuture_invalidFormat() {
        assertEquals(false, "not-a-date".isDateInFuture())
    }

    // endregion

    // region isDateTodayOrInFuture tests
    @OptIn(ExperimentalTime::class)
    @Test
    fun testIsDateTodayOrInFuture() {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date

        // Test data: (dayOffset, expectedResult, testCase)
        val testCases = listOf(
            Triple(1, true, "future date (tomorrow)"),
            Triple(30, true, "far future date (30 days)"),
            Triple(0, true, "today (endDate is inclusive)"),
            Triple(-1, false, "past date (yesterday)"),
        )

        testCases.forEach { (dayOffset, expected, testCase) ->
            val dateStr = today.plus(dayOffset, DateTimeUnit.DAY).toString()
            assertEquals(expected, dateStr.isDateTodayOrInFuture(), "Failed for: $testCase")
        }

        // Invalid format test
        assertEquals(false, "not-a-date".isDateTodayOrInFuture(), "Failed for: invalid format")
    }

    // endregion

    // region isDateTodayOrInPast tests
    @OptIn(ExperimentalTime::class)
    @Test
    fun testIsDateTodayOrInPast() {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date

        // Test data: (dayOffset, expectedResult, testCase)
        val testCases = listOf(
            Triple(-1, true, "past date (yesterday)"),
            Triple(-30, true, "far past date (30 days ago)"),
            Triple(0, true, "today (startDate is inclusive)"),
            Triple(1, false, "future date (tomorrow)"),
        )

        testCases.forEach { (dayOffset, expected, testCase) ->
            val dateStr = today.plus(dayOffset, DateTimeUnit.DAY).toString()
            assertEquals(expected, dateStr.isDateTodayOrInPast(), "Failed for: $testCase")
        }

        // Invalid format test
        assertEquals(false, "not-a-date".isDateTodayOrInPast(), "Failed for: invalid format")
    }

    // endregion
}
