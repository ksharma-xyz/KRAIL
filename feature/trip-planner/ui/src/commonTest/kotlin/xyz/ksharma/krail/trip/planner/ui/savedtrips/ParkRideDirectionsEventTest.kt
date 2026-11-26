package xyz.ksharma.krail.trip.planner.ui.savedtrips

import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ParkRideDirectionsEventTest {

    private val facility = ParkRideUiState.ParkRideFacilityDetail(
        facilityName = "Tallawong P1",
        spotsAvailable = 120,
        percentageFull = 40,
        totalSpots = 200,
        stopId = "2155384",
        timeText = "8:00 AM",
        facilityId = "1",
        latitude = -33.69,
        longitude = 150.9,
    )

    @Test
    fun `directions go to the car park's own position under its own name`() {
        assertEquals(
            SavedTripUiEvent.ParkRideDirectionsClick(
                latitude = -33.69,
                longitude = 150.9,
                label = "Tallawong P1",
                stopId = "2155384",
                facilityId = "1",
            ),
            parkRideDirectionsEvent(facility),
        )
    }

    @Test
    fun `half a position is no position`() {
        assertNull(parkRideDirectionsEvent(facility.copy(latitude = null)))
        assertNull(parkRideDirectionsEvent(facility.copy(longitude = null)))
    }
}
