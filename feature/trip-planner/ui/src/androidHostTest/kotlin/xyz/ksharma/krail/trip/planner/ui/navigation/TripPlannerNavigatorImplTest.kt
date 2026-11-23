package xyz.ksharma.krail.trip.planner.ui.navigation

import androidx.navigation3.runtime.NavKey
import xyz.ksharma.krail.core.navigation.NavigatorBase
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * What the trip planner navigator asks the app navigator for. The go circle on Search Stop opens the trip
 * results from Search Stop, so the one call it makes must push the timetable and must not pop anything: a
 * pop would remove Search Stop before the results open, and back from the results would land on Saved Trips.
 */
class TripPlannerNavigatorImplTest {

    private val base = RecordingNavigatorBase()
    private val navigator = TripPlannerNavigatorImpl(base)

    @Test
    fun `opening the timetable pushes it as the single instance and pops nothing`() {
        navigator.navigateToTimeTable(
            fromStopId = "200060",
            fromStopName = "Central Station",
            toStopId = "200070",
            toStopName = "Town Hall Station",
            dateTimeSelectionJson = "{}",
        )

        assertEquals(
            listOf(
                "pushSingleInstance:" + TimeTableRoute(
                    fromStopId = "200060",
                    fromStopName = "Central Station",
                    toStopId = "200070",
                    toStopName = "Town Hall Station",
                    dateTimeSelectionJson = "{}",
                ),
            ),
            base.calls,
        )
    }

    @Test
    fun `the To and From form opens as a plain push so the caller stays under it`() {
        navigator.navigateToSearchStop(SearchStopFieldType.TO)

        assertEquals(listOf("goTo:" + SearchStopRoute(fieldType = SearchStopFieldType.TO)), base.calls)
    }

    @Test
    fun `going back pops one route`() {
        navigator.goBack()

        assertEquals(listOf("pop"), base.calls)
    }
}

private class RecordingNavigatorBase : NavigatorBase {
    val calls = mutableListOf<String>()

    override fun goTo(route: NavKey) {
        calls += "goTo:$route"
    }

    override fun pop() {
        calls += "pop"
    }

    override fun resetRoot(route: NavKey) {
        calls += "resetRoot:$route"
    }

    override fun replaceCurrent(route: NavKey) {
        calls += "replaceCurrent:$route"
    }

    override fun updateTheme(hexColorCode: String) {
        calls += "updateTheme:$hexColorCode"
    }

    override fun pushSingleInstance(route: NavKey) {
        calls += "pushSingleInstance:$route"
    }
}
