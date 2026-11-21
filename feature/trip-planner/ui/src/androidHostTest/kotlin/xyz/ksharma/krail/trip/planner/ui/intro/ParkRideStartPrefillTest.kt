package xyz.ksharma.krail.trip.planner.ui.intro

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Which stop, if any, an added Park & Ride station pre-fills as the first trip's start. Run on the
 * real default catalogue against the real stops table, including the rows that do not fit.
 */
class ParkRideStartPrefillTest {

    private val stations = BundledStops.defaultStations()

    private fun prefill(stationName: String) = parkRideStartPrefill(
        station = stations.single { it.stationName == stationName },
        stopNameLookup = BundledStops::name,
    )

    @Test
    fun `a station that resolves pre-fills with its stop id and the stop's own name`() {
        assertEquals(SuggestedStop("275010", "Penrith Station"), prefill("Penrith"))
        assertEquals(SuggestedStop("2155384", "Tallawong Station"), prefill("Tallawong"))
    }

    @Test
    fun `a bus stop keeps its own name and is never called a station`() {
        assertEquals(
            SuggestedStop("210120", "Warriewood B-Line, Pittwater Rd"),
            prefill("Warriewood"),
        )
        assertEquals(
            SuggestedStop("209913", "Dee Why B-Line, Pittwater Rd"),
            prefill("Dee Why"),
        )
    }

    @Test
    fun `a station whose stop is not in the stops table pre-fills nothing`() {
        assertNull(prefill("West Ryde"))
    }

    @Test
    fun `a station with several stops uses one that resolves, not the smallest id`() {
        // Mona Vale's smallest id, 2103108, is not in the table. Its other stop, 210318, is.
        assertEquals(
            SuggestedStop("210318", "Mona Vale B-Line, Barrenjoey Rd"),
            prefill("Mona Vale"),
        )
    }

    @Test
    fun `a lookup that returns a blank name counts as not resolved`() {
        val station = stations.single { it.stationName == "Penrith" }

        assertNull(parkRideStartPrefill(station) { "  " })
        assertNull(parkRideStartPrefill(station) { null })
    }
}
