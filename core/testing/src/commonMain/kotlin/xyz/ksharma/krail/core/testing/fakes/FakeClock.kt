package xyz.ksharma.krail.core.testing.fakes

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * A [Clock] the test moves by hand.
 *
 * Use it where the code under test reads the wall clock but does not otherwise
 * depend on coroutine virtual time — cache-expiry checks, "has this departed yet?"
 * comparisons, peak/off-peak windows. Where the code *does* live on a scheduler,
 * prefer `KrailTestScope.clock`, which is already tied to virtual time so
 * `pumpOnce(...)` moves it.
 *
 * [DEFAULT_NOW] is a fixed instant rather than the real clock so a test never
 * depends on the day it runs on.
 */
@OptIn(ExperimentalTime::class)
class FakeClock(private var current: Instant = DEFAULT_NOW) : Clock {

    override fun now(): Instant = current

    /** Moves the clock forward (or back, for a negative [duration]). */
    fun advanceBy(duration: Duration) {
        current += duration
    }

    /** Jumps the clock to an absolute [instant]. */
    fun setTo(instant: Instant) {
        current = instant
    }

    companion object {
        /** A Thursday, midday UTC — no month/DST/weekend edge to trip over. */
        val DEFAULT_NOW: Instant = Instant.parse("2026-04-09T12:00:00Z")
    }
}
