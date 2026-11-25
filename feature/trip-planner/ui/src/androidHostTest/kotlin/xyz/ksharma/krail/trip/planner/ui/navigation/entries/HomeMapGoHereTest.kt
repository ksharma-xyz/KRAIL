package xyz.ksharma.krail.trip.planner.ui.navigation.entries

import xyz.ksharma.krail.trip.planner.ui.components.ToFromField
import xyz.ksharma.krail.trip.planner.ui.flow.RecordingTripPlannerNavigator
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.searchstop.SearchStopTrip
import xyz.ksharma.krail.trip.planner.ui.searchstop.initialSearchStopFlow
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.test.Test
import kotlin.test.assertEquals

/** Go here on the home side map opens journey search with the stop as its destination. */
class HomeMapGoHereTest {

    private val navigator = RecordingTripPlannerNavigator()
    private val central = StopItem(stopId = "200060", stopName = "Central Station")

    @Test
    fun `go here opens journey search holding the stop as the destination`() {
        navigator.goHereFromHomeMap(central)

        assertEquals(listOf(SearchStopFieldType.FROM), navigator.searchStopRequests)
        assertEquals(listOf<Pair<StopItem?, StopItem?>>(null to central), navigator.searchStopPresets)
    }

    @Test
    fun `the search it opens is editing where from`() {
        navigator.goHereFromHomeMap(central)

        val (from, to) = navigator.searchStopPresets.single()
        val flow = initialSearchStopFlow(
            fieldType = navigator.searchStopRequests.single(),
            editTripLeg = false,
            preset = SearchStopTrip(from = from, to = to),
        )

        assertEquals(ToFromField.From, flow.editing)
    }
}
