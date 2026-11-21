package xyz.ksharma.krail.trip.planner.ui.intro.v2

import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideAnswer
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideCardPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IntroFlowViewModelParkRideRestoreTest {

    @Test
    fun `the answer and the chosen station survive a new view model, and the card is worked out again`() =
        krailRunTest {
            val before = IntroRig(this)
            before.refreshWritesBellaVista()
            before.send(
                IntroUiEvent.AnswerParkRide(yes = true),
                IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, FOUR_POINT_SIX_KM_FROM_BELLA_VISTA),
            )

            val after = IntroRig(this, handle = before.handle, parkRideSandook = before.parkRideSandook)
            assertEquals(
                ParkRideCardPhase.Locating(stationId = "2153478"),
                after.state.parkRideCard,
                "a skeleton until the station is worked out again",
            )
            after.refreshWritesBellaVista()
            runCurrent()

            assertEquals(ParkRideAnswer.YES, after.state.parkRideAnswer)
            assertEquals(LocationResult.GRANTED, after.state.locationResult)
            assertEquals(before.state, after.state, "the same card, with the same readings")
            // The loader and its cooldown decide whether this costs a network call.
            assertEquals(1, after.loader.refreshedMappings.size)
        }

    @Test
    fun `no coordinate is ever saved, shown in state or kept after the fix is used`() = krailRunTest {
        val rig = IntroRig(this)
        val fix = LatLng(latitude = -33.771961, longitude = 150.944024)
        rig.refreshWritesBellaVista()
        rig.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, fix),
        )

        val saved = assertNotNull(rig.handle.get<String>(INTRO_SAVED_STATE_KEY))
        listOf("33.771961", "150.944024", "latitude", "longitude", "-33.", "150.9").forEach { forbidden ->
            assertFalse(forbidden in saved, "the saved state holds $forbidden: $saved")
            assertFalse(forbidden in rig.state.toString(), "the state holds $forbidden: ${rig.state}")
        }
        assertTrue("2153478" in saved, "the station is kept, the rider's position is not: $saved")
    }

    @Test
    fun `no station near and the answer survive a new view model`() = krailRunTest {
        val before = IntroRig(this)
        before.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_CENTRAL),
        )

        val after = IntroRig(this, handle = before.handle)
        after.settle()

        assertEquals(before.state, after.state)
        assertEquals(ParkRideCardPhase.NoStationNear, after.state.parkRideCard)
        assertTrue(after.loader.refreshedMappings.isEmpty())
    }

    @Test
    fun `a process killed while locating asks the question again`() = krailRunTest {
        val before = IntroRig(this)
        before.send(IntroUiEvent.AnswerParkRide(yes = true))

        val after = IntroRig(this, handle = before.handle)
        after.settle()
        assertEquals(ParkRideCardPhase.None, after.state.parkRideCard, "no permission result is coming")

        after.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_TALLAWONG),
        )
        assertEquals("Tallawong", after.card().stationName)
    }

    @Test
    fun `a saved station that is no longer in the catalogue becomes no station near`() = krailRunTest {
        val before = IntroRig(this)
        before.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, FOUR_POINT_SIX_KM_FROM_BELLA_VISTA),
        )

        val after = IntroRig(this, handle = before.handle, stations = listOf(KELLYVILLE_STATION))
        after.settle()

        assertEquals(ParkRideCardPhase.NoStationNear, after.state.parkRideCard)
        assertTrue(after.loader.refreshedMappings.isEmpty())
    }
}
