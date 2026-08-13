package xyz.ksharma.krail.trip.planner.ui.search.ai

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import xyz.ksharma.krail.core.aitext.TimeIntent
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.JourneyTimeOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class AiTripIntentTimeResolverTest {

    // Monday 2026-08-10, 09:00 UTC — fixed so date-rollover assertions are deterministic.
    private val fixedNow = Instant.parse("2026-08-10T09:00:00Z")
    private val utc = TimeZone.UTC

    @Test
    fun `null timeIntent resolves to null`() {
        assertNull(resolveTimeIntent(null, fixedNow, utc))
    }

    @Test
    fun `resolves a 12-hour clock time with am`() {
        val result = resolveTimeIntent(TimeIntent(isArrival = false, timeText = "9am"), fixedNow, utc)

        assertEquals(JourneyTimeOptions.LEAVE, result?.option)
        assertEquals(9, result?.hour)
        assertEquals(0, result?.minute)
        assertEquals(LocalDate(2026, 8, 10), result?.date)
    }

    @Test
    fun `resolves a 12-hour clock time with pm and minutes`() {
        val result = resolveTimeIntent(TimeIntent(isArrival = true, timeText = "6:30pm"), fixedNow, utc)

        assertEquals(JourneyTimeOptions.ARRIVE, result?.option)
        assertEquals(18, result?.hour)
        assertEquals(30, result?.minute)
    }

    @Test
    fun `noon via 12pm resolves to hour 12, not 0`() {
        val result = resolveTimeIntent(TimeIntent(isArrival = true, timeText = "12pm"), fixedNow, utc)
        assertEquals(12, result?.hour)
    }

    @Test
    fun `12am resolves to midnight hour 0`() {
        val result = resolveTimeIntent(TimeIntent(isArrival = false, timeText = "12am"), fixedNow, utc)
        assertEquals(0, result?.hour)
    }

    @Test
    fun `explicit 24-hour-style HH-colon-MM without meridiem resolves`() {
        val result = resolveTimeIntent(TimeIntent(isArrival = false, timeText = "18:30"), fixedNow, utc)
        assertEquals(18, result?.hour)
        assertEquals(30, result?.minute)
    }

    @Test
    fun `bare ambiguous number without meridiem or minutes does not resolve`() {
        // "9" alone in free text is too ambiguous to trust as a clock time (could be part
        // of anything) — must come with either am/pm or explicit minutes.
        assertNull(resolveTimeIntent(TimeIntent(isArrival = false, timeText = "leaving in 9"), fixedNow, utc))
    }

    @Test
    fun `relative minutes resolves against the fixed now`() {
        val result = resolveTimeIntent(TimeIntent(isArrival = false, timeText = "in 20 minutes"), fixedNow, utc)
        assertEquals(9, result?.hour)
        assertEquals(20, result?.minute)
        assertEquals(LocalDate(2026, 8, 10), result?.date)
    }

    @Test
    fun `relative hours resolves against the fixed now`() {
        val result = resolveTimeIntent(TimeIntent(isArrival = false, timeText = "in 2 hours"), fixedNow, utc)
        assertEquals(11, result?.hour)
        assertEquals(0, result?.minute)
    }

    @Test
    fun `relative minutes crossing midnight rolls the date forward`() {
        // now is 09:00 — "in 900 minutes" is +15h, landing at 00:00 the next day.
        val result = resolveTimeIntent(TimeIntent(isArrival = false, timeText = "in 900 minutes"), fixedNow, utc)
        assertEquals(0, result?.hour)
        assertEquals(0, result?.minute)
        assertEquals(LocalDate(2026, 8, 11), result?.date)
    }

    @Test
    fun `vague day-part phrase does not resolve, never guessed`() {
        assertNull(resolveTimeIntent(TimeIntent(isArrival = false, timeText = "this evening"), fixedNow, utc))
        assertNull(resolveTimeIntent(TimeIntent(isArrival = true, timeText = "tomorrow morning"), fixedNow, utc))
    }
}
