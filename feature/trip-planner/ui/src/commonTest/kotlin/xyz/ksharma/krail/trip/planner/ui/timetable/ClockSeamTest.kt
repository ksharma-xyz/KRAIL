package xyz.ksharma.krail.trip.planner.ui.timetable

import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.core.transport.nsw.NswTransportMode
import xyz.ksharma.krail.trip.planner.ui.savedtrips.parkRideApiCooldown
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

/**
 * Demonstrates the clock seam on the two trip-planner pieces that used to read
 * `Clock.System.now()` inline: [TimeTableState.JourneyCardInfo]'s departure checks and
 * `parkRideApiCooldown`'s peak window.
 *
 * The journey test drives the seam from `KrailTestScope.clock`, which is wired to the
 * coroutine scheduler — so `pumpOnce` moves virtual time AND the wall clock together,
 * and a journey crosses its departure because the test advanced time, not because the
 * suite ran at the right moment.
 */
@OptIn(ExperimentalTime::class)
class ClockSeamTest {

    @Test
    fun `Given an upcoming journey When virtual time crosses departure Then it has started`() = krailRunTest {
        val departure = clock.now() + 5.minutes
        val journey = journeyDepartingAt(departure)

        assertFalse(journey.hasJourneyStarted(clock.now()), "5 minutes out, the journey has not started")
        assertFalse(journey.hasJourneyEnded(clock.now()))

        pumpOnce(6.minutes)

        assertTrue(journey.hasJourneyStarted(clock.now()), "virtual time crossed departure — journey has started")
        assertFalse(journey.hasJourneyEnded(clock.now()), "arrival is still 20 minutes out")

        pumpOnce(25.minutes)

        assertTrue(journey.hasJourneyEnded(clock.now()), "virtual time crossed arrival — journey has ended")
    }

    @Test
    fun `Given a peak-hour instant Then the park-ride cooldown is the peak one`() {
        val zone = TimeZone.currentSystemDefault()
        val inPeak = LocalDateTime(2026, 4, 9, PEAK_HOUR, 0).toInstant(zone)
        val outsidePeak = LocalDateTime(2026, 4, 9, OFF_PEAK_HOUR, 0).toInstant(zone)

        assertEquals(
            PEAK_COOLDOWN_SECONDS.seconds,
            parkRideApiCooldown(PEAK_COOLDOWN_SECONDS, OFF_PEAK_COOLDOWN_SECONDS, inPeak),
        )
        assertEquals(
            OFF_PEAK_COOLDOWN_SECONDS.seconds,
            parkRideApiCooldown(PEAK_COOLDOWN_SECONDS, OFF_PEAK_COOLDOWN_SECONDS, outsidePeak),
        )
    }

    private fun journeyDepartingAt(departure: kotlin.time.Instant) = TimeTableState.JourneyCardInfo(
        timeText = "in 5 mins",
        originTime = "8:10 AM",
        originUtcDateTime = departure.toString(),
        destinationTime = "8:30 AM",
        destinationUtcDateTime = (departure + JOURNEY_LENGTH).toString(),
        travelTime = "20 mins",
        transportModeLines = persistentListOf(
            TransportModeLine(transportMode = NswTransportMode.Train, lineName = "T1"),
        ),
        legs = persistentListOf(),
        totalUniqueServiceAlerts = 0,
    )

    private companion object {
        val JOURNEY_LENGTH = 20.minutes
        const val PEAK_HOUR = 7
        const val OFF_PEAK_HOUR = 14
        const val PEAK_COOLDOWN_SECONDS = 60L
        const val OFF_PEAK_COOLDOWN_SECONDS = 600L
    }
}
