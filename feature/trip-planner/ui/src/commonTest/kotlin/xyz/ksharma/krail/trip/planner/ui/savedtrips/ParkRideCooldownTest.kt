package xyz.ksharma.krail.trip.planner.ui.savedtrips

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * The one cooldown decision every Park & Ride fetch goes through (opening home, opening a tile,
 * a tick of the poll), driven at its boundary: never fetched, one second inside, exactly the
 * cooldown, and the reset a failed call leaves so a retry does not wait.
 */
@OptIn(ExperimentalTime::class)
class ParkRideCooldownTest {

    private val now = Instant.fromEpochSeconds(10_000L)

    private class Case(val name: String, val lastCall: Long?, val cooldown: Duration, val offCooldown: Boolean, val left: Duration)

    private val cases = listOf(
        Case("never fetched", lastCall = null, cooldown = 120.seconds, offCooldown = true, left = Duration.ZERO),
        Case(
            "fetched just now",
            lastCall = 10_000L,
            cooldown = 120.seconds,
            offCooldown = false,
            left = 120.seconds,
        ),
        Case(
            "one second inside the peak cooldown",
            lastCall = 10_000L - 119,
            cooldown = 120.seconds,
            offCooldown = false,
            left = 1.seconds,
        ),
        Case(
            "exactly the peak cooldown",
            lastCall = 10_000L - 120,
            cooldown = 120.seconds,
            offCooldown = true,
            left = Duration.ZERO,
        ),
        Case(
            "one second past the peak cooldown",
            lastCall = 10_000L - 121,
            cooldown = 120.seconds,
            offCooldown = true,
            left = Duration.ZERO,
        ),
        Case(
            "inside the off peak cooldown",
            lastCall = 10_000L - 300,
            cooldown = 600.seconds,
            offCooldown = false,
            left = 300.seconds,
        ),
        Case(
            "exactly the off peak cooldown",
            lastCall = 10_000L - 600,
            cooldown = 600.seconds,
            offCooldown = true,
            left = Duration.ZERO,
        ),
        Case(
            "reset after a failed call",
            lastCall = Instant.DISTANT_PAST.epochSeconds,
            cooldown = 600.seconds,
            offCooldown = true,
            left = Duration.ZERO,
        ),
        Case(
            "a zero cooldown never waits",
            lastCall = 10_000L,
            cooldown = Duration.ZERO,
            offCooldown = true,
            left = Duration.ZERO,
        ),
    )

    @Test
    fun `a facility is off cooldown once a full cooldown has passed`() {
        cases.forEach { case ->
            assertEquals(
                case.offCooldown,
                isOffCooldown(case.lastCall, case.cooldown, now),
                "Off cooldown for ${case.name}",
            )
            assertEquals(case.left, cooldownLeft(case.lastCall, case.cooldown, now), "Time left for ${case.name}")
        }
    }

    @Test
    fun `a stamp in the future is on cooldown and never off it`() {
        assertFalse(isOffCooldown(lastCallEpochSeconds = 10_060L, cooldown = 120.seconds, now = now))
        assertEquals(180.seconds, cooldownLeft(lastCallEpochSeconds = 10_060L, cooldown = 120.seconds, now = now))
    }

    @Test
    fun `every saved station is asked once on open, with open tiles first`() {
        assertEquals(listOf("A", "B", "C"), stopsToRefreshOnOpen(listOf("A", "B", "C"), emptySet()))
        assertEquals(listOf("C", "A", "B"), stopsToRefreshOnOpen(listOf("A", "B", "C"), setOf("C")))
        assertEquals(listOf("A", "B"), stopsToRefreshOnOpen(listOf("A", "B", "A"), emptySet()))
        assertEquals(listOf("A"), stopsToRefreshOnOpen(listOf("A"), setOf("Z")))
        assertEquals(emptyList<String>(), stopsToRefreshOnOpen(emptyList(), emptySet()))
    }
}
