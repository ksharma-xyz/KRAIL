package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.coroutines.flow.first
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.ParkRideUserFacilityEvent
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource.SavedTrips
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource.UserAdded
import xyz.ksharma.krail.sandook.db.SavedParkRide
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideMapping
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.StationPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val BELLA_VISTA_STOP = SuggestedStop(stopId = "2153478", name = "Bella Vista Station")

private suspend fun IntroRig.savedRows(source: SavedParkRideSource) =
    parkRideSandook.observeSavedParkRidesBySource(source).first()

private fun IntroRig.parkRideEvents() =
    analytics.getTrackedEvents("park_ride_user_facility").map { assertIs<ParkRideUserFacilityEvent>(it) }

class IntroFlowViewModelParkRideStartTest {

    @Test
    fun `add fills in the start with the station stop and its own name`() = krailRunTest {
        val rig = IntroRig(this)
        rig.showBellaVista()

        rig.send(IntroUiEvent.AddParkRideAndContinue)

        assertEquals(BELLA_VISTA_STOP.stopId, rig.state.startStopId)
        assertEquals(BELLA_VISTA_STOP.name, rig.state.startName, "the stop's own name")
        assertTrue(rig.state.startFromParkRide)
        assertNull(rig.state.endStopId)
    }

    @Test
    fun `the smallest stop of the station that resolves is the start`() = krailRunTest {
        val hornsby = ParkRideStationPickerItem(
            stationId = "207720",
            stationName = "Hornsby",
            stopName = "",
            carParkNames = listOf("Hornsby"),
            mappings = listOf(
                ParkRideMapping(stopId = "207720", facilityId = "25", facilityName = "Hornsby"),
                ParkRideMapping(stopId = "207763", facilityId = "25", facilityName = "Hornsby"),
            ),
            position = StationPosition(latitude = -33.7032, longitude = 151.0993),
        )
        val rig = IntroRig(this, stations = listOf(hornsby), stopNames = mapOf("207763" to "Hornsby Station"))
        rig.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, LatLng(-33.7032, 151.0993)),
        )

        rig.send(IntroUiEvent.AddParkRideAndContinue)

        assertEquals("207763", rig.state.startStopId, "207720 is not in the stops table")
        assertEquals("Hornsby Station", rig.state.startName)
    }

    @Test
    fun `a station none of whose stops resolve is added with no start`() = krailRunTest {
        val rig = IntroRig(this, stopNames = emptyMap())
        rig.showBellaVista()

        rig.send(IntroUiEvent.AddParkRideAndContinue)

        assertTrue(rig.state.addedParkRide)
        assertEquals(1, rig.savedRows(UserAdded).size)
        assertNull(rig.state.startStopId)
        assertFalse(rig.state.startFromParkRide)
    }

    @Test
    fun `a start the rider already picked is never overridden`() = krailRunTest {
        // The rider went back to this screen after choosing their own start.
        val rig = IntroRig(
            this,
            handle = savedAt(ON_PARK_RIDE.copy(startStopId = CENTRAL.stopId, startName = CENTRAL.name)),
        )
        rig.showBellaVista()

        rig.send(IntroUiEvent.AddParkRideAndContinue)

        assertTrue(rig.state.addedParkRide)
        assertEquals(CENTRAL.stopId, rig.state.startStopId)
        assertEquals(CENTRAL.name, rig.state.startName)
        assertFalse(rig.state.startFromParkRide)
    }

    @Test
    fun `a stop already chosen as the end is not filled in as the start`() = krailRunTest {
        val rig = IntroRig(
            this,
            handle = savedAt(ON_PARK_RIDE.copy(endStopId = BELLA_VISTA_STOP.stopId, endName = BELLA_VISTA_STOP.name)),
        )
        rig.showBellaVista()

        rig.send(IntroUiEvent.AddParkRideAndContinue)

        assertTrue(rig.state.addedParkRide)
        assertNull(rig.state.startStopId, "a trip from a stop to itself is never made for the rider")
        assertEquals(BELLA_VISTA_STOP.stopId, rig.state.endStopId)
    }

    @Test
    fun `reaching the trip step with a station found but not added leaves the start empty`() = krailRunTest {
        val rig = IntroRig(this)
        rig.showBellaVista()

        rig.send(IntroUiEvent.DeclineParkRide)

        assertEquals(IntroStep.DESTINATION, rig.state.step)
        assertNull(rig.state.startStopId)
        assertFalse(rig.state.addedParkRide)
        assertTrue(rig.savedRows(UserAdded).isEmpty())
    }

    @Test
    fun `the added station, its start and the answer survive a new view model`() =
        krailRunTest {
            val before = IntroRig(this)
            before.showBellaVista()
            before.send(IntroUiEvent.AddParkRideAndContinue)

            val after = IntroRig(this, handle = before.handle, parkRideSandook = before.parkRideSandook)
            after.settle()

            assertEquals(before.state, after.state, "the same Added card, the same start")
            assertTrue(after.state.addedParkRide)
            assertTrue(after.state.startFromParkRide)
            assertEquals(1, after.savedRows(UserAdded).size)
        }

    @Test
    fun `granted and added reports added, and the trip saved from the filled in start comes from the intro`() =
        krailRunTest {
            val rig = IntroRig(this)
            rig.showBellaVista()

            rig.send(IntroUiEvent.AddParkRideAndContinue)
            // The origin screen is passed over, because the car park is the start, so one pick reaches the confirm screen.
            rig.send(IntroUiEvent.ChooseStop(CENTRAL.toIntroStop()))
            assertEquals(IntroStep.CONFIRM, rig.state.step)
            rig.send(IntroUiEvent.FinishPressed)

            val outcome = rig.outcomeEvent().properties.orEmpty()
            assertEquals("yes", outcome["parkRideAnswer"])
            assertEquals("granted", outcome["locationResult"])
            assertEquals(true, outcome["addedParkRide"])
            assertEquals(true, outcome["savedTrip"])
            assertEquals(false, outcome["skipped"])
            val trip = rig.sandook.selectAllTrips().single()
            assertEquals(listOf("2153478", "200060"), listOf(trip.fromStopId, trip.toStopId))
            assertEquals(1, rig.parkRideEvents().size, "the add is tracked once, and the finish adds nothing")
        }

    @Test
    fun `a Park and Ride start is the origin, so the origin screen is passed over`() =
        krailRunTest {
            val rig = IntroRig(this)
            rig.showBellaVista()

            rig.send(IntroUiEvent.AddParkRideAndContinue)

            assertEquals(
                listOf(IntroStep.THEME, IntroStep.PARK_RIDE, IntroStep.DESTINATION, IntroStep.CONFIRM),
                rig.state.stepsInFlow(),
            )
        }
}
