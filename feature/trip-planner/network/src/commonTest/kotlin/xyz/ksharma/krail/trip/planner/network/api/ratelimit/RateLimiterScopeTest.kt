package xyz.ksharma.krail.trip.planner.network.api.ratelimit

import kotlinx.coroutines.launch
import org.koin.dsl.koinApplication
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.trip.planner.network.api.di.tripPlannerNetworkModule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.time.Duration.Companion.seconds

/**
 * Holds the two rules [NetworkRateLimiter]'s own KDoc states but the graph did not enforce.
 *
 * 1. **One limiter per consumer.** The trigger channel is a broadcast: every flow built from
 *    the same limiter runs its block when *anyone* calls `triggerEvent()`. Bound as a Koin
 *    `single`, two timetable screens shared one channel, so one screen's refresh fetched
 *    for both.
 * 2. **A trigger is consumed once.** A trigger fired while nothing is collecting must reach
 *    the next collector — that is how the screen fetches on entry — but must not be replayed
 *    to every collector after that.
 */
class RateLimiterScopeTest {

    @Test
    fun `Given the DI graph When RateLimiter is resolved twice Then each consumer gets its own`() {
        val koin = koinApplication { modules(tripPlannerNetworkModule) }.koin
        try {
            assertNotSame(
                koin.get<RateLimiter>(),
                koin.get<RateLimiter>(),
                "each consumer must own its rate limiter — a shared one broadcasts triggers",
            )
        } finally {
            koin.close()
        }
    }

    @Test
    fun `Given two DI-resolved limiters When one is triggered Then only its own block runs`() = krailRunTest {
        val koin = koinApplication { modules(tripPlannerNetworkModule) }.koin
        val limiterA = koin.get<RateLimiter>()
        val limiterB = koin.get<RateLimiter>()
        var aCalls = 0
        var bCalls = 0

        val collectA = launch { limiterA.rateLimitFlow { aCalls++ }.collect {} }
        val collectB = launch { limiterB.rateLimitFlow { bCalls++ }.collect {} }
        runCurrent()

        limiterA.triggerEvent()
        pumpOnce(SETTLE)

        assertEquals(1, aCalls, "the limiter that was triggered must run its block")
        assertEquals(0, bCalls, "a second consumer must not fetch off another consumer's trigger")

        collectA.cancel()
        collectB.cancel()
        koin.close()
    }

    @Test
    fun `Given a trigger fired with no collector When collected later Then it runs once, not on every collection`() =
        krailRunTest {
            val limiter = NetworkRateLimiter()
            var calls = 0

            // The screen triggers before its collector attaches — that trigger must not be lost.
            limiter.triggerEvent()

            val first = launch { limiter.rateLimitFlow { calls++ }.collect {} }
            pumpOnce(SETTLE)
            assertEquals(1, calls, "a pending trigger must reach the first collector")
            first.cancel()

            // A later collection (screen re-entry, a new fetchTrip job) must not re-run the
            // trigger that has already been served.
            val second = launch { limiter.rateLimitFlow { calls++ }.collect {} }
            pumpOnce(SETTLE)
            assertEquals(1, calls, "an already-consumed trigger must not be replayed to a new collector")
            second.cancel()
        }

    private companion object {
        /** Longer than the limiter's own rate-limit interval, so nothing is left mid-debounce. */
        val SETTLE = 2.seconds
    }
}
