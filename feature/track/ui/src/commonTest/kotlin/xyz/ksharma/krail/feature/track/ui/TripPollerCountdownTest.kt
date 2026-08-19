package xyz.ksharma.krail.feature.track.ui

import xyz.ksharma.krail.feature.track.TrackTripState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Locks the exact strings the countdown card shows, and the exact set of states the poll loop
 * stops on.
 *
 * These were inline in `computeCountdown` and `startPolling` and only reachable through a
 * running poller, which is why both functions grew past detekt's complexity threshold and were
 * suppressed rather than tested. Every row below is the output the inline version produced.
 */
class TripPollerCountdownTest {

    @Test
    fun `Given a duration Then the short countdown value reads as the card shows it`() {
        val cases = listOf(
            Duration.ZERO to "0s",
            45.seconds to "45s",
            59.seconds to "59s",
            60.seconds to "1m",
            (3.minutes + 20.seconds) to "3m 20s",
            12.minutes to "12m",
            59.minutes to "59m",
            // An hour or more hands over to the app's generic relative string.
            1.hours to "in 1h 0m",
            (2.hours + 30.minutes) to "in 2h",
        )

        cases.forEach { (duration, expected) ->
            assertEquals(expected, shortCountdownValue(duration), "for $duration")
        }
    }

    @Test
    fun `Given time still to run Then the label is arriving in`() {
        assertEquals(
            "Arriving in" to "3m 20s",
            arrivedOrArrivingLabel(3.minutes + 20.seconds, hasPassedArrivalMoment = false),
        )
        assertEquals(
            "Arriving in" to "0s",
            arrivedOrArrivingLabel(Duration.ZERO, hasPassedArrivalMoment = false),
            "the arrival second itself still counts down, matching the pre-refactor behaviour",
        )
    }

    @Test
    fun `Given arrival has passed Then the label is arrived`() {
        assertEquals(
            "Arrived" to "just now",
            arrivedOrArrivingLabel((-30).seconds, hasPassedArrivalMoment = false),
            "under a minute past arrival reads as just now",
        )
        assertEquals(
            "Arrived" to "1 min ago",
            arrivedOrArrivingLabel((-60).seconds, hasPassedArrivalMoment = false),
            "a minute or more past arrival gets the relative string",
        )
    }

    @Test
    fun `Given the arrival latch is set Then a later estimate cannot reopen the countdown`() {
        // A refreshed estimate pushing arrival back into the future must not undo "Arrived".
        assertEquals(
            "Arrived" to "just now",
            arrivedOrArrivingLabel(5.minutes, hasPassedArrivalMoment = true),
        )
    }

    @Test
    fun `Given each poll state Then only Tracking keeps the loop running`() {
        assertNull(
            pollLoopExitReason(TrackTripState.Tracking(journey = trackedJourney())),
            "Tracking is the one state worth polling again",
        )
        assertEquals("Arrived", pollLoopExitReason(TrackTripState.Arrived(journey = trackedJourney())))
        assertEquals("ArrivedAndFinished", pollLoopExitReason(TrackTripState.ArrivedAndFinished))
        assertEquals("NotFound", pollLoopExitReason(TrackTripState.NotFound))
        assertEquals("Error", pollLoopExitReason(TrackTripState.Error))
    }

    private fun trackedJourney() = xyz.ksharma.krail.feature.track.TrackedJourneyDisplay(
        fromStopId = "from",
        toStopId = "to",
        fromStopName = "Origin",
        toStopName = "Destination",
        originTime = "08:00",
        scheduledOriginTime = null,
        destinationTime = "08:30",
        originUtcDateTime = "2026-04-09T08:00:00Z",
        destinationUtcDateTime = "2026-04-09T08:30:00Z",
        travelTime = "30 mins",
        legs = kotlinx.collections.immutable.persistentListOf(),
    )
}
