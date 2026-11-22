package xyz.ksharma.krail.trip.planner.ui.savedtrips

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Where the Park & Ride row sits among the saved trips (`docs/MAP_HOME_UX.md` rule 29): after the
 * second trip by default, after the only trip with one, first with none, and wherever the rider
 * dragged it, clamped to the trips there are.
 */
class HomeStackOrderTest {

    private class Case(val name: String, val trips: Int, val stored: Int?, val expected: Int)

    @Test
    fun `the row is after the second trip until dragged`() {
        val cases = listOf(
            Case("no trips, the row comes first", trips = 0, stored = null, expected = 0),
            Case("one trip, the row follows it", trips = 1, stored = null, expected = 1),
            Case("two trips, the row follows both", trips = 2, stored = null, expected = 2),
            Case("three trips, two then the row", trips = 3, stored = null, expected = 2),
            Case("many trips, still after the second", trips = 12, stored = null, expected = 2),
        )
        cases.forEach { case ->
            assertEquals(case.expected, rowIndexFor(case.trips, case.stored), case.name)
        }
    }

    @Test
    fun `a dragged place is kept and clamped when trips are added or removed`() {
        val cases = listOf(
            Case("dragged to the top", trips = 5, stored = 0, expected = 0),
            Case("dragged to the middle", trips = 5, stored = 3, expected = 3),
            Case("dragged to the very end", trips = 5, stored = 5, expected = 5),
            Case("trips deleted below the stored place", trips = 2, stored = 4, expected = 2),
            Case("all trips deleted", trips = 0, stored = 4, expected = 0),
            Case("trips added keep the stored place", trips = 9, stored = 4, expected = 4),
            Case("a bad negative write counts as first", trips = 5, stored = -3, expected = 0),
            Case("a negative trip count is no trips", trips = -1, stored = 2, expected = 0),
        )
        cases.forEach { case ->
            assertEquals(case.expected, rowIndexFor(case.trips, case.stored), case.name)
        }
    }

    @Test
    fun `trips split around the row in the order the rider keeps them`() {
        val trips = listOf("a", "b", "c", "d")

        assertEquals(emptyList<String>() to trips, trips.splitAroundParkRideRow(0))
        assertEquals(listOf("a") to listOf("b", "c", "d"), trips.splitAroundParkRideRow(1))
        assertEquals(listOf("a", "b") to listOf("c", "d"), trips.splitAroundParkRideRow(2))
        assertEquals(trips to emptyList<String>(), trips.splitAroundParkRideRow(4))
    }

    @Test
    fun `a row index past either end still splits and never throws`() {
        val trips = listOf("a", "b")

        assertEquals(trips to emptyList<String>(), trips.splitAroundParkRideRow(99))
        assertEquals(emptyList<String>() to trips, trips.splitAroundParkRideRow(-1))
        assertEquals(emptyList<String>() to emptyList<String>(), emptyList<String>().splitAroundParkRideRow(2))
    }

    @Test
    fun `the default place is two trips in`() {
        assertEquals(2, DEFAULT_PARK_RIDE_ROW_AFTER_TRIPS)
    }
}
