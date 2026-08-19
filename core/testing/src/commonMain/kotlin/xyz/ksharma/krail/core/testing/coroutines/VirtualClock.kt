package xyz.ksharma.krail.core.testing.coroutines

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * A [Clock] that reads [scheduler]'s virtual time.
 *
 * Inject it wherever production code compares a `delay`-driven poll tick against a wall-clock
 * timestamp — a refresh window, a cooldown, a cache expiry. Without it the two disagree: the
 * test jumps virtual time by 30 seconds while `Clock.System.now()` moves a fraction of a
 * millisecond, so the window under test never opens and the assertion passes or fails for a
 * reason that has nothing to do with the code.
 *
 * [KrailTestScope.clock] is this, bound to the scope's own scheduler. Use this factory
 * directly in tests that still own a bare `StandardTestDispatcher`.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
fun virtualClock(scheduler: TestCoroutineScheduler): Clock = object : Clock {
    override fun now(): Instant = VIRTUAL_EPOCH + scheduler.currentTime.milliseconds
}

/** Virtual time zero, as a wall-clock instant. A Thursday, midday UTC. */
@OptIn(ExperimentalTime::class)
val VIRTUAL_EPOCH: Instant = Instant.parse("2026-04-09T12:00:00Z")
