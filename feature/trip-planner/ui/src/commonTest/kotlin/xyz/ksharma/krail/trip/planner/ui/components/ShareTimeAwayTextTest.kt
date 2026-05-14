package xyz.ksharma.krail.trip.planner.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class ShareTimeAwayTextTest {

    private fun fixedClock(utc: String) = object : Clock {
        override fun now(): Instant = Instant.parse(utc)
    }

    @Test
    fun `returns time from now to arrival in minutes`() {
        val clock = fixedClock("2026-05-14T08:00:00Z")
        val arrival = "2026-05-14T08:45:00Z" // 45 mins away

        val result = shareTimeAwayText(
            destinationUtcDateTime = arrival,
            totalTravelTime = "30 mins",
            clock = clock,
        )

        assertEquals("45 mins", result)
    }

    @Test
    fun `returns time from now to arrival in hours and minutes`() {
        val clock = fixedClock("2026-05-14T08:00:00Z")
        val arrival = "2026-05-14T09:35:00Z" // 1h 35m away

        val result = shareTimeAwayText(
            destinationUtcDateTime = arrival,
            totalTravelTime = "30 mins",
            clock = clock,
        )

        assertEquals("1h 35m", result)
    }

    @Test
    fun `returns journey duration when destinationUtcDateTime is empty`() {
        val result = shareTimeAwayText(
            destinationUtcDateTime = "",
            totalTravelTime = "30 mins",
        )

        assertEquals("30 mins", result)
    }

    @Test
    fun `uses absolute value so past arrivals still produce positive duration`() {
        // Journey has already arrived — clock is after the destination time.
        val clock = fixedClock("2026-05-14T09:10:00Z")
        val arrival = "2026-05-14T09:00:00Z" // 10 mins in the past

        val result = shareTimeAwayText(
            destinationUtcDateTime = arrival,
            totalTravelTime = "30 mins",
            clock = clock,
        )

        assertEquals("10 mins", result)
    }

    @Test
    fun `formats exact hours without a minutes suffix`() {
        val clock = fixedClock("2026-05-14T08:00:00Z")
        val arrival = "2026-05-14T10:00:00Z" // exactly 2h

        val result = shareTimeAwayText(
            destinationUtcDateTime = arrival,
            totalTravelTime = "30 mins",
            clock = clock,
        )

        assertEquals("2h", result)
    }

    @Test
    fun `formats single minute correctly`() {
        val clock = fixedClock("2026-05-14T08:00:00Z")
        val arrival = "2026-05-14T08:01:00Z" // 1 min away

        val result = shareTimeAwayText(
            destinationUtcDateTime = arrival,
            totalTravelTime = "30 mins",
            clock = clock,
        )

        assertEquals("1 min", result)
    }
}
