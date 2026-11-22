package xyz.ksharma.krail.trip.planner.ui.homemap

import xyz.ksharma.krail.trip.planner.ui.search.ai.ResolvedTripIntent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** What an Ask KRAIL answer hands to Search Stop now that home has no To and From block to land it in. */
class AskKrailHandoffRulesTest {

    private val central = StopItem(stopId = "200060", stopName = "Central Station")
    private val townHall = StopItem(stopId = "200070", stopName = "Town Hall Station")

    private fun intent(from: StopItem?, to: StopItem?) = ResolvedTripIntent(
        fromText = from?.stopName,
        fromStopItem = from,
        toText = to?.stopName,
        toStopItem = to,
        dateTimeSelectionItem = null,
        modeHints = emptyList(),
    )

    @Test
    fun `a whole answer hands over both stops`() {
        assertEquals(central to townHall, handoffStopsFor(intent(central, townHall)))
    }

    @Test
    fun `a half answer hands over the stop it found and leaves the other for the rider`() {
        assertEquals(central to null, handoffStopsFor(intent(central, null)))
        assertEquals(null to townHall, handoffStopsFor(intent(null, townHall)))
    }

    @Test
    fun `no answer, or an answer with no stop, opens nothing`() {
        assertNull(handoffStopsFor(null))
        assertNull(handoffStopsFor(intent(null, null)))
    }
}
