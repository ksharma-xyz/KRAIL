package xyz.ksharma.krail.core.datetime

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.krail.core.datetime.DateTimeHelper.formatTo12HourTime
import xyz.ksharma.krail.core.datetime.DateTimeHelper.isDateInFuture
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toFormattedDurationTimeString
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toGenericFormattedTimeString
import xyz.ksharma.krail.core.datetime.DateTimeHelper.utcToAEST
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
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

    @Test
    fun testToGenericFormattedTimeString() {
        assertEquals("40 mins ago", (-40).minutes.toGenericFormattedTimeString())
        assertEquals("Now", 0.minutes.toGenericFormattedTimeString())
        assertEquals("in 1h 20m", 80.minutes.toGenericFormattedTimeString())
        assertEquals("in 2h", 120.minutes.toGenericFormattedTimeString())
    }

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
}
