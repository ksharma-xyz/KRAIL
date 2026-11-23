package xyz.ksharma.krail.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import xyz.ksharma.krail.core.navigation.NavigationState
import xyz.ksharma.krail.trip.planner.ui.navigation.SavedTripsRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.TimeTableRoute
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The back stack the go circle on Search Stop leaves behind, on the real [Navigator].
 *
 * The go circle pushes the timetable from Search Stop (`navigateToTimeTable` is `pushSingleInstance`), so the
 * stack is Saved Trips, Search Stop, Timetable, and back from the results lands on Search Stop with its
 * From and To still set. Back from Search Stop leaves for Saved Trips as before.
 */
class TripResultsBackStackTest {

    private val home = SavedTripsRoute
    private val searchStop = SearchStopRoute(fieldType = SearchStopFieldType.TO)
    private val timetable = timetable(from = "200060", to = "200070")
    private val stack = NavBackStack<NavKey>(home)
    private val navigator = Navigator(
        NavigationState(
            startRoute = home,
            topLevelRoute = mutableStateOf(home),
            backStacks = mapOf(home to stack),
        ),
    )

    private fun timetable(from: String, to: String) = TimeTableRoute(
        fromStopId = from,
        fromStopName = "From $from",
        toStopId = to,
        toStopName = "To $to",
    )

    private fun stackNow(): List<NavKey> = stack.toList()

    @Test
    fun `after the go circle the stack is Saved Trips, Search Stop, Timetable`() {
        navigator.goTo(searchStop)
        navigator.pushSingleInstance(timetable)

        assertEquals(listOf<NavKey>(home, searchStop, timetable), stackNow())
    }

    @Test
    fun `back from the results lands on Search Stop`() {
        navigator.goTo(searchStop)
        navigator.pushSingleInstance(timetable)

        navigator.pop()

        assertEquals(listOf<NavKey>(home, searchStop), stackNow())
    }

    @Test
    fun `back again from Search Stop leaves for Saved Trips`() {
        navigator.goTo(searchStop)
        navigator.pushSingleInstance(timetable)
        navigator.pop()

        navigator.pop()

        assertEquals(listOf<NavKey>(home), stackNow())
    }

    @Test
    fun `back from Search Stop before any go leaves for Saved Trips with nothing on the stack above`() {
        navigator.goTo(searchStop)

        navigator.pop()

        assertEquals(listOf<NavKey>(home), stackNow())
    }

    @Test
    fun `a second go from the same Search Stop replaces nothing and stacks one timetable`() {
        navigator.goTo(searchStop)
        navigator.pushSingleInstance(timetable)
        navigator.pop()
        val other = timetable(from = "200060", to = "200080")

        navigator.pushSingleInstance(other)

        assertEquals(listOf<NavKey>(home, searchStop, other), stackNow())
    }

    @Test
    fun `a leg edit from the timetable still returns to the timetable`() {
        navigator.goTo(searchStop)
        navigator.pushSingleInstance(timetable)
        val legEdit = SearchStopRoute(fieldType = SearchStopFieldType.FROM, editTripLeg = true)
        navigator.goTo(legEdit)

        navigator.pop()

        assertEquals(listOf<NavKey>(home, searchStop, timetable), stackNow())
    }
}
