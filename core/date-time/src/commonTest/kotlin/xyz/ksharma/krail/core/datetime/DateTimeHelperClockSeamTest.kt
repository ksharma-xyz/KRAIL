package xyz.ksharma.krail.core.datetime

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.krail.core.datetime.DateTimeHelper.isDateInFuture
import xyz.ksharma.krail.core.datetime.DateTimeHelper.isDateTodayOrInFuture
import xyz.ksharma.krail.core.datetime.DateTimeHelper.isDateTodayOrInPast
import xyz.ksharma.krail.core.datetime.DateTimeHelper.isFuture
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Demonstrates the clock seam on [DateTimeHelper]: the same date answers differently
 * purely because the caller moved `now`. Before the seam these four functions read
 * `Clock.System.now()` directly and could only be tested against the day the suite
 * happened to run on.
 *
 * "Today" is derived from the injected instant in the system default zone rather
 * than hard-coded, so the assertions hold in every timezone CI might run in.
 */
@OptIn(ExperimentalTime::class)
class DateTimeHelperClockSeamTest {

    private val t0 = Instant.parse("2026-04-09T12:00:00Z")

    private fun todayAt(now: Instant): LocalDate =
        now.toLocalDateTime(TimeZone.currentSystemDefault()).date

    @Test
    fun `Given a fixed date When now moves past it Then isFuture flips to false`() {
        val date = todayAt(t0).plus(1, DateTimeUnit.DAY)

        assertTrue(date.isFuture(t0), "one day ahead of t0 is future")
        assertFalse(date.isFuture(t0 + 2.days), "the same date is not future once now passes it")
    }

    @Test
    fun `Given a fixed date string When now moves past it Then isDateInFuture flips to false`() {
        val date = todayAt(t0).plus(3, DateTimeUnit.DAY).toString()

        assertTrue(date.isDateInFuture(t0))
        assertFalse(date.isDateInFuture(t0 + 5.days))
    }

    @Test
    fun `Given a fixed date string When now crosses it Then today-or-future and today-or-past swap`() {
        val date = todayAt(t0).plus(2, DateTimeUnit.DAY).toString()

        // Two days ahead of t0.
        assertTrue(date.isDateTodayOrInFuture(t0))
        assertFalse(date.isDateTodayOrInPast(t0))

        // Now is that day: both are true.
        val onTheDay = t0 + 2.days
        assertTrue(date.isDateTodayOrInFuture(onTheDay))
        assertTrue(date.isDateTodayOrInPast(onTheDay))

        // Now is past it.
        val after = t0 + 4.days
        assertFalse(date.isDateTodayOrInFuture(after))
        assertTrue(date.isDateTodayOrInPast(after))
    }
}
