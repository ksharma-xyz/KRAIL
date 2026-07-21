package xyz.ksharma.krail.trip.planner.ui.savedtrips

import xyz.ksharma.krail.sandook.NSWParkRideFacilityDetail
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the home Park & Ride card's display behaviour, which must not change.
 *
 * Keying `SavedParkRide` by source lets one station be held by both a saved trip and an
 * explicit add, so the merge feeding this mapper can now contain the same facility twice.
 * These cases prove that extra row stays invisible: the card list is still one entry per
 * stop, with each facility appearing once.
 */
class ParkRideMapperTest {

    @Test
    fun `a facility held by two sources still produces a single card`() {
        // The same facility arrives twice, as it does when a station is both saved-trip
        // derived and user added.
        val details = listOf(
            facilityDetail(facilityId = "26", stopId = "2155384"),
            facilityDetail(facilityId = "26", stopId = "2155384"),
        )

        val uiState = details.toParkRideUiState()

        assertEquals(1, uiState.size)
        assertEquals(1, uiState.first().facilities.size)
        assertEquals("26", uiState.first().facilities.first().facilityId)
    }

    @Test
    fun `several car parks at one stop stay on one card`() {
        // Tallawong: three car parks behind a single stop.
        val details = listOf(
            facilityDetail(facilityId = "26", stopId = "2155384", facilityName = "Tallawong P1"),
            facilityDetail(facilityId = "27", stopId = "2155384", facilityName = "Tallawong P2"),
            facilityDetail(facilityId = "28", stopId = "2155384", facilityName = "Tallawong P3"),
        )

        val uiState = details.toParkRideUiState()

        assertEquals(1, uiState.size)
        assertEquals(3, uiState.first().facilities.size)
    }

    @Test
    fun `one facility reached from two stops is not shown twice`() {
        // Mona Vale: one car park, two stop IDs. The second stop has nothing left to show
        // once the facility is claimed, so it must not produce an empty card.
        val details = listOf(
            facilityDetail(facilityId = "12", stopId = "210318"),
            facilityDetail(facilityId = "12", stopId = "2103108"),
        )

        val uiState = details.toParkRideUiState()

        assertEquals(1, uiState.size)
        assertEquals(1, uiState.first().facilities.size)
    }

    private fun facilityDetail(
        facilityId: String,
        stopId: String,
        facilityName: String = "Car park $facilityId",
    ) = NSWParkRideFacilityDetail(
        facilityId = facilityId,
        spotsAvailable = 40,
        totalSpots = 100,
        facilityName = facilityName,
        percentageFull = 60,
        stopId = stopId,
        stopName = "Station $stopId",
        timeText = "8:00 AM",
        suburb = "",
        address = "",
        latitude = 0.0,
        longitude = 0.0,
        timestamp = 0,
    )
}
