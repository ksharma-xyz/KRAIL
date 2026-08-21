package xyz.ksharma.krail.trip.planner.ui.journeymap.business

import xyz.ksharma.krail.trip.planner.ui.journeymap.business.JourneyStopUiMapper.getTimeInfo
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.StopType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `JourneyStopUiMapper` turns the UTC timestamps on a stop into the rows shown in the journey-map
 * stop sheet. Sydney local time, 12-hour formatting and the "a stop only has the times it has"
 * rule are all decided here.
 */
class JourneyStopUiMapperTest {

    @Test
    fun `GIVEN both times WHEN mapped THEN arrival is listed before departure in Sydney time`() {
        val stop = stop(
            arrivalTime = "2024-09-24T19:00:00Z",
            departureTime = "2024-09-24T19:12:00Z",
        )

        val info = stop.getTimeInfo()

        assertEquals(listOf("Arrival", "Departure"), info.map { it.label })
        // 24 Sep is outside daylight saving, so Sydney is UTC+10 and the times roll to the 25th.
        assertEquals(listOf("5:00 AM", "5:12 AM"), info.map { it.value })
    }

    @Test
    fun `GIVEN a midday UTC time WHEN mapped THEN noon reads 12 PM rather than 0 PM`() {
        val stop = stop(arrivalTime = "2024-09-24T02:30:00Z")

        assertEquals("12:30 PM", stop.getTimeInfo().single().value)
    }

    @Test
    fun `GIVEN only a departure time WHEN mapped THEN no empty arrival row is produced`() {
        val stop = stop(departureTime = "2024-09-24T19:12:00Z")

        val info = stop.getTimeInfo()

        assertEquals(listOf("Departure"), info.map { it.label })
    }

    @Test
    fun `GIVEN no times at all WHEN mapped THEN the sheet gets an empty list`() {
        assertTrue(stop().getTimeInfo().isEmpty())
    }

    @Test
    fun `GIVEN an unparseable timestamp WHEN mapped THEN that row is dropped rather than crashing`() {
        val stop = stop(arrivalTime = "not-a-timestamp", departureTime = "2024-09-24T19:12:00Z")

        val info = stop.getTimeInfo()

        assertEquals(listOf("Departure"), info.map { it.label })
    }

    private fun stop(
        arrivalTime: String? = null,
        departureTime: String? = null,
    ) = JourneyStopFeature(
        stopId = "stop_id",
        stopName = "Stop",
        position = null,
        stopType = StopType.REGULAR,
        platform = null,
        arrivalTime = arrivalTime,
        departureTime = departureTime,
    )
}
