package xyz.ksharma.krail.feature.crowding

import xyz.ksharma.krail.feature.crowding.SeatLevel.FEW_SEATS
import xyz.ksharma.krail.feature.crowding.SeatLevel.SEATS_AVAILABLE
import xyz.ksharma.krail.feature.crowding.SeatLevel.STANDING_ONLY
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** One test per eligibility condition in the design review. */
class NextServiceRuleTest {

    private val current =
        ServiceOption(id = "now", routeKey = "T1 Penrith", departureEpochSeconds = 0, level = STANDING_ONLY)
    private val quieterSoon =
        ServiceOption(id = "next", routeKey = "T1 Penrith", departureEpochSeconds = 300, level = SEATS_AVAILABLE)

    private fun pick(
        later: List<ServiceOption> = listOf(quieterSoon),
        from: ServiceOption = current,
        connection: Boolean = false,
        night: Boolean = false,
    ) = NextServiceRule.pick(from, later, hasLaterConnection = connection, isNight = night)

    @Test
    fun `a quieter service on the same route soon is named`() {
        assertEquals(quieterSoon, pick())
    }

    @Test
    fun `one level quieter is enough`() {
        assertEquals("next", pick(later = listOf(quieterSoon.copy(level = FEW_SEATS)))?.id)
    }

    @Test
    fun `the same level is not named`() {
        assertNull(pick(later = listOf(quieterSoon.copy(level = STANDING_ONLY))))
    }

    @Test
    fun `a fuller service is not named`() {
        assertNull(
            pick(from = current.copy(level = SEATS_AVAILABLE), later = listOf(quieterSoon.copy(level = FEW_SEATS))),
        )
    }

    @Test
    fun `more than ten minutes later is not named`() {
        assertNull(pick(later = listOf(quieterSoon.copy(departureEpochSeconds = 601))))
        assertEquals("next", pick(later = listOf(quieterSoon.copy(departureEpochSeconds = 600)))?.id)
    }

    @Test
    fun `a later connection blocks it`() {
        assertNull(pick(connection = true))
    }

    @Test
    fun `night blocks it`() {
        assertNull(pick(night = true))
    }

    @Test
    fun `another route or direction is not named`() {
        assertNull(pick(later = listOf(quieterSoon.copy(routeKey = "T1 Hornsby"))))
    }

    @Test
    fun `an earlier service or one with no level is not named`() {
        assertNull(pick(later = listOf(quieterSoon.copy(departureEpochSeconds = -60))))
        assertNull(pick(later = listOf(quieterSoon.copy(level = null))))
        assertNull(pick(from = current.copy(level = null)))
    }

    @Test
    fun `the soonest qualifying service wins`() {
        val sooner = quieterSoon.copy(id = "sooner", departureEpochSeconds = 120, level = FEW_SEATS)
        assertEquals("sooner", pick(later = listOf(quieterSoon, sooner))?.id)
    }
}
