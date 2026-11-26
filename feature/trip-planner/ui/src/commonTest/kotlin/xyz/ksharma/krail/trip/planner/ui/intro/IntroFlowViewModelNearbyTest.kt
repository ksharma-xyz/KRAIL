package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import xyz.ksharma.krail.sandook.db.SelectStopsNearby
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAnswer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The stations near the rider come from the one fix the Park & Ride question already took. They are worked out in
 * the background, kept as ids and names, never saved, and never needed for anything the rider waits on.
 */
class IntroFlowViewModelNearbyTest {

    private val nearCentral = listOf(CENTRAL_STOP, REDFERN_STOP, MUSEUM_STOP)

    private fun row(stop: IntroStop, classes: String = "1") = SelectStopsNearby(
        stopId = stop.stopId,
        stopName = stop.name,
        stopLat = -33.88,
        stopLon = 151.2,
        isParent = null,
        productClasses = classes,
        distanceSquared = 0.0,
    )

    private fun IntroRig.stationsNear(vararg stops: IntroStop) {
        nswStops.nearbyRows = stops.map { row(it) }
    }

    @Test
    fun `a granted fix finds the stations near the rider as ids and names, and asks the stops table once`() =
        krailRunTest {
            val rig = IntroRig(this)
            rig.stationsNear(*nearCentral.toTypedArray())

            rig.showBellaVista()

            assertEquals(nearCentral, rig.state.nearbyStops)
            assertEquals(1, rig.nswStops.nearbyCalls.size)
            assertEquals(LocationResult.GRANTED, rig.state.locationResult)
        }

    @Test
    fun `the stations come from the fix the question took, so no second fix is asked for`() = krailRunTest {
        val rig = IntroRig(this)
        rig.stationsNear(CENTRAL_STOP)

        rig.send(IntroUiEvent.AnswerParkRide(yes = true))
        assertTrue(rig.nswStops.nearbyCalls.isEmpty(), "nothing is asked before the one fix arrives")
        rig.send(IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_CENTRAL))

        val call = rig.nswStops.nearbyCalls.single()
        assertEquals(AT_CENTRAL.latitude, call.centerLat)
        assertEquals(AT_CENTRAL.longitude, call.centerLon)
    }

    @Test
    fun `the stations are found whether or not a Park and Ride station is near`() = krailRunTest {
        val rig = IntroRig(this)
        rig.stationsNear(CENTRAL_STOP, REDFERN_STOP)

        // Central is nowhere near a car park, so no card shows: the rider is told so, and the stations are already found.
        rig.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_CENTRAL),
        )
        assertEquals(IntroStep.PARK_RIDE, rig.state.step, "the message waits for OK")
        assertEquals(listOf(CENTRAL_STOP, REDFERN_STOP), rig.state.nearbyStops)

        rig.send(IntroUiEvent.AcknowledgeNoStation)

        assertEquals(IntroStep.DESTINATION, rig.state.step)
        assertEquals(listOf(CENTRAL_STOP, REDFERN_STOP), rig.state.nearbyStops)
    }

    @Test
    fun `a rider who says no, denies, or has no fix asks the stops table nothing and gets the curated list`() =
        krailRunTest {
            listOf(LocationResult.DENIED, LocationResult.UNAVAILABLE).forEach { result ->
                val rig = IntroRig(this)
                rig.stationsNear(CENTRAL_STOP)

                rig.send(
                    IntroUiEvent.AnswerParkRide(yes = true),
                    IntroUiEvent.LocationResultReceived(result, location = null),
                )

                assertTrue(rig.nswStops.nearbyCalls.isEmpty(), "$result")
                assertTrue(rig.state.nearbyStops.isEmpty(), "$result")
            }

            val no = IntroRig(this)
            no.stationsNear(CENTRAL_STOP)
            no.send(IntroUiEvent.AnswerParkRide(yes = false))
            assertTrue(no.nswStops.nearbyCalls.isEmpty())
            assertTrue(no.state.nearbyStops.isEmpty())

            val noFix = IntroRig(this)
            noFix.stationsNear(CENTRAL_STOP)
            noFix.send(
                IntroUiEvent.AnswerParkRide(yes = true),
                IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, location = null),
            )
            assertTrue(noFix.nswStops.nearbyCalls.isEmpty(), "a granted result with no fix has nothing to search from")
            assertEquals(LocationResult.GRANTED_NO_FIX, noFix.state.locationResult)
        }

    @Test
    fun `a result nobody asked for finds nothing`() = krailRunTest {
        val rig = IntroRig(this)
        rig.stationsNear(CENTRAL_STOP)

        rig.send(IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_CENTRAL))

        assertTrue(rig.nswStops.nearbyCalls.isEmpty())
        assertTrue(rig.state.nearbyStops.isEmpty())
    }

    @Test
    fun `a second result for the same yes asks again nothing`() = krailRunTest {
        val rig = IntroRig(this)
        rig.stationsNear(CENTRAL_STOP)

        rig.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_CENTRAL),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_CENTRAL),
        )

        assertEquals(1, rig.nswStops.nearbyCalls.size)
    }

    @Test
    fun `stops that are not ready inside the wait give the curated list, and the rider is not held`() = krailRunTest {
        val rig = IntroRig(this, initialReadiness = StopsReadiness.Loading)
        rig.stationsNear(CENTRAL_STOP)

        rig.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_CENTRAL),
        )
        pumpOnce(PARK_RIDE_STOPS_WAIT_MS * 2)

        assertTrue(rig.nswStops.nearbyCalls.isEmpty(), "the table is not asked while it is not ready")
        assertTrue(rig.state.nearbyStops.isEmpty())
        assertEquals(IntroStep.DESTINATION, rig.state.step, "the question moved on without them")
    }

    @Test
    fun `stops that become ready inside the wait still give the stations`() = krailRunTest {
        val rig = IntroRig(this, initialReadiness = StopsReadiness.Loading)
        rig.stationsNear(CENTRAL_STOP)
        rig.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_CENTRAL),
        )

        pumpOnce(PARK_RIDE_STOPS_WAIT_MS - 1_000)
        rig.readiness.emit(StopsReadiness.Ready(stopCount = 100))
        runCurrent()

        assertEquals(listOf(CENTRAL_STOP), rig.state.nearbyStops)
    }

    @Test
    fun `stops that failed give the curated list with no error`() = krailRunTest {
        val rig = IntroRig(this, initialReadiness = StopsReadiness.Failed(attempt = 1, reason = "disk full"))
        rig.stationsNear(CENTRAL_STOP)

        rig.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_CENTRAL),
        )

        assertTrue(rig.nswStops.nearbyCalls.isEmpty())
        assertTrue(rig.state.nearbyStops.isEmpty())
    }

    @Test
    fun `a query that fails is the curated list, and the Intro goes on as if the rider had no fix`() = krailRunTest {
        val rig = IntroRig(this)
        rig.nswStops.nearbyFailure = IllegalStateException("database is locked")

        rig.send(
            IntroUiEvent.AnswerParkRide(yes = true),
            IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_CENTRAL),
        )

        assertTrue(rig.state.nearbyStops.isEmpty())
        assertEquals(IntroStep.PARK_RIDE, rig.state.step, "no station near is a message, and it waits for OK")
        rig.send(IntroUiEvent.AcknowledgeNoStation)
        assertEquals(IntroStep.DESTINATION, rig.state.step)
        assertEquals(LocationResult.GRANTED, rig.state.locationResult)
    }

    @Test
    fun `the stations are not saved, so a recreated view model has none and the curated list shows`() = krailRunTest {
        val rig = IntroRig(this)
        rig.stationsNear(CENTRAL_STOP, REDFERN_STOP)
        rig.showBellaVista()
        assertEquals(2, rig.state.nearbyStops.size)

        val restored = IntroRig(this, handle = rig.handle)
        restored.settle()

        assertTrue(restored.state.nearbyStops.isEmpty())
        assertEquals(rig.state.step, restored.state.step)
        assertEquals(rig.state.locationResult, restored.state.locationResult)
    }

    @Test
    fun `a saved state carries no stop that was found near the rider`() = krailRunTest {
        val rig = IntroRig(this)
        rig.stationsNear(CENTRAL_STOP, REDFERN_STOP)
        rig.showBellaVista()

        val saved = rig.handle.get<String>(INTRO_SAVED_STATE_KEY).orEmpty()

        assertTrue("Central" !in saved && "Redfern" !in saved && "200060" !in saved, saved)
    }

    // region the badges of the curated stops

    private fun IntroRig.knowCurated() {
        DEFAULT_SUGGESTED_STOPS.start.forEach { stop ->
            sandook.insertNswStop(stop.stopId, stop.name, -33.0, 151.0, isParent = null)
            sandook.insertNswStopProductClass(stop.stopId, TransportMode.Train.productClass)
        }
    }

    @Test
    fun `the modes of the curated stops are read once the stops are ready`() = krailRunTest {
        val rig = IntroRig(this, initialReadiness = StopsReadiness.Loading)
        rig.knowCurated()
        rig.settle()
        assertTrue(rig.state.stopModes.isEmpty(), "the table is not ready, so no badges yet")

        rig.readiness.emit(StopsReadiness.Ready(stopCount = 100))
        runCurrent()

        assertEquals<List<TransportMode>?>(listOf(TransportMode.Train), rig.state.stopModes["200060"])
        assertEquals<List<TransportMode>?>(listOf(TransportMode.Train), rig.state.stopModes[TALLAWONG.stopId])
    }

    @Test
    fun `the modes are there at once when the stops were ready from the start`() = krailRunTest {
        val rig = IntroRig(this)
        rig.knowCurated()

        rig.settle()

        assertEquals<List<TransportMode>?>(listOf(TransportMode.Train), rig.state.stopModes["200060"])
    }

    @Test
    fun `stops that never become ready leave the rows without badges and change nothing else`() = krailRunTest {
        val rig = IntroRig(this, initialReadiness = StopsReadiness.Failed(attempt = 1, reason = "disk full"))
        rig.knowCurated()

        rig.settle()

        assertTrue(rig.state.stopModes.isEmpty())
        assertEquals(IntroState(step = IntroStep.PARK_RIDE, parkRideAnswer = ParkRideAnswer.ASK), rig.state)
    }

    // endregion
}
