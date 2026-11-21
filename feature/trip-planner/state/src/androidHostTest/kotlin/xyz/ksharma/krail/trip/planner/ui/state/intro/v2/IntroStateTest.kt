package xyz.ksharma.krail.trip.planner.ui.state.intro.v2

import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntroStateTest {

    @Test
    fun `the default state is the first screen with nothing answered or picked`() {
        val state = IntroState()

        assertEquals(IntroStep.PARK_RIDE, state.step)
        assertEquals(ParkRideAnswer.ASK, state.parkRideAnswer)
        assertEquals(LocationResult.NOT_ASKED, state.locationResult)
        assertEquals(ParkRideCardPhase.None, state.parkRideCard)
        assertNull(state.startStopId)
        assertNull(state.endStopId)
        assertFalse(state.startFromParkRide)
        assertFalse(state.addedParkRide)
        assertFalse(state.usedSearch)
        assertFalse(state.saved)
    }

    @Test
    fun `a trip needs both ends`() {
        assertFalse(IntroState().hasTrip, "nothing picked")
        assertFalse(IntroState(startStopId = "A").hasTrip, "a start with no end")
        assertFalse(IntroState(endStopId = "B").hasTrip, "an end with no start")
        assertTrue(IntroState(startStopId = "A", endStopId = "B").hasTrip, "both ends picked")
    }

    @Test
    fun `a car park has a reading when its spots are known, even when none are left`() {
        assertFalse(ParkRideCarPark(facilityId = "26", name = "Tallawong P1").hasReading, "name only")
        assertTrue(
            ParkRideCarPark(
                facilityId = "26",
                name = "Tallawong P1",
                spotsAvailable = 0,
                percentageFull = 100,
                timeText = "8:05 AM",
            ).hasReading,
            "a full car park is still a reading",
        )
    }

    @Test
    fun `a station on the card starts loading with no car parks, and a phase keeps only what it was given`() {
        val bare = ParkRideCardStation(stationId = "2155384", stationName = "Tallawong")
        val read = ParkRideCarPark(facilityId = "26", name = "Tallawong P1", spotsAvailable = 5)
        val settled = ParkRideCardStation(
            stationId = "2155384",
            stationName = "Tallawong",
            carParks = persistentListOf(read),
            availability = ParkRideAvailabilityState.Content,
        )

        assertEquals(ParkRideAvailabilityState.Loading, bare.availability)
        assertTrue(bare.carParks.isEmpty())
        assertEquals(listOf(read), settled.carParks)
        assertEquals(ParkRideAvailabilityState.Content, settled.availability)
        assertNull(ParkRideCardPhase.Locating().stationId, "a first look has no station yet")
        assertEquals("2155384", ParkRideCardPhase.Locating("2155384").stationId, "a saved card carries its station")
        assertEquals(settled, assertIs<ParkRideCardPhase.Card>(ParkRideCardPhase.Card(settled)).station)
        assertNotEquals<ParkRideCardPhase>(ParkRideCardPhase.NoStationNear, ParkRideCardPhase.None)
    }
}
