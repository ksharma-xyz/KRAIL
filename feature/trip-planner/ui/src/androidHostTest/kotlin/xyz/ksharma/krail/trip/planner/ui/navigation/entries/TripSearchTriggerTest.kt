package xyz.ksharma.krail.trip.planner.ui.navigation.entries

import kotlinx.datetime.LocalDate
import xyz.ksharma.krail.trip.planner.ui.flow.RecordingTripPlannerNavigator
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.JourneyTimeOptions
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The go circle on Search Stop opens the trip results from Search Stop itself, so Search Stop stays on the
 * back stack and back from the results lands on it with From and To still filled. These pin the entry's
 * half of that: it navigates forward to the timetable and never back, it writes both stops where the classic
 * home reads them, the time a rider set travels, and the analytics call keeps its place in the order.
 */
class TripSearchTriggerTest {

    private val central = StopItem(stopId = "200060", stopName = "Central Station")
    private val townHall = StopItem(stopId = "200070", stopName = "Town Hall Station")
    private val navigator = RecordingTripPlannerNavigator()
    private val events = mutableListOf<SavedTripUiEvent>()

    private fun go(dateTimeSelectionJson: String? = null, from: StopItem = central, to: StopItem = townHall) =
        startTripFromSearchStop(
            from = from,
            to = to,
            dateTimeSelectionJson = dateTimeSelectionJson,
            onEvent = { events += it },
            tripPlannerNavigator = navigator,
        )

    @Test
    fun `the go circle opens the timetable and never goes back, so Search Stop stays on the stack`() {
        go()

        assertEquals(listOf(central.stopId to townHall.stopId), navigator.timeTableRequests)
        assertEquals(0, navigator.goBackRequests, "Going back would pop Search Stop before the results open.")
    }

    @Test
    fun `both stops are written for the classic home bars before the load is counted and the timetable opens`() {
        go()

        assertEquals(
            listOf(
                SavedTripUiEvent.FromStopChanged(central.toJsonString()),
                SavedTripUiEvent.ToStopChanged(townHall.toJsonString()),
                SavedTripUiEvent.AnalyticsLoadTimeTableClick(
                    fromStopId = central.stopId,
                    toStopId = townHall.stopId,
                ),
            ),
            events,
        )
    }

    @Test
    fun `a time the rider set travels with the trip`() {
        val sixPm = DateTimeSelectionItem(
            option = JourneyTimeOptions.ARRIVE,
            hour = 18,
            minute = 0,
            date = LocalDate(2026, 11, 23),
        )

        go(dateTimeSelectionJson = sixPm.toJsonString())

        val carried = navigator.timeTableDateTimes.single()
        assertEquals(sixPm, carried?.let(DateTimeSelectionItem::fromJsonString))
    }

    @Test
    fun `no held time means the timetable opens for now`() {
        go(dateTimeSelectionJson = null)

        assertNull(navigator.timeTableDateTimes.single())
    }

    @Test
    fun `a trip from a stop to itself opens nothing and counts nothing`() {
        triggerTripSearch(
            fromStop = central,
            toStop = central,
            onEvent = { events += it },
            tripPlannerNavigator = navigator,
        )

        assertTrue(navigator.timeTableRequests.isEmpty())
        assertTrue(events.isEmpty())
    }

    @Test
    fun `a missing stop opens nothing`() {
        triggerTripSearch(
            fromStop = null,
            toStop = townHall,
            onEvent = { events += it },
            tripPlannerNavigator = navigator,
        )

        assertTrue(navigator.timeTableRequests.isEmpty())
        assertTrue(events.isEmpty())
    }
}
