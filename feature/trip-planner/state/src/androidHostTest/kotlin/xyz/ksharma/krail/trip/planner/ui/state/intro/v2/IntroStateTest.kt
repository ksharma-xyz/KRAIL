package xyz.ksharma.krail.trip.planner.ui.state.intro.v2

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntroStateTest {

    @Test
    fun `the default state is the first screen with nothing answered or picked`() {
        val state = IntroState()

        assertEquals(IntroStep.PARK_RIDE, state.step)
        assertEquals(ParkRideAnswer.ASK, state.parkRideAnswer)
        assertEquals(LocationResult.NOT_ASKED, state.locationResult)
        assertNull(state.nearestStation)
        assertEquals(ParkRideAvailabilityState.Loading, state.availability)
        assertNull(state.startStopId)
        assertNull(state.endStopId)
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
}
