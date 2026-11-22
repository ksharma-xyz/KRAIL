package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.ParkRideUserFacilityEvent
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.core.testing.fakes.FakeNswParkRideSandook
import xyz.ksharma.krail.core.testing.fakes.FakeParkRideService
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource.SavedTrips
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource.UserAdded
import xyz.ksharma.krail.sandook.db.SavedParkRide
import xyz.ksharma.krail.trip.planner.ui.parkride.RealParkRideAvailabilityLoader
import xyz.ksharma.krail.trip.planner.ui.state.intro.LocationResult
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private suspend fun IntroRig.savedRows(source: SavedParkRideSource) =
    parkRideSandook.observeSavedParkRidesBySource(source).first()

private fun IntroRig.parkRideEvents() =
    analytics.getTrackedEvents("park_ride_user_facility").map { assertIs<ParkRideUserFacilityEvent>(it) }

/** Wraps the shared fake and fails the save, the one write Add has to survive. */
private class SaveFailsParkRideSandook(
    private val delegate: NswParkRideSandook = FakeNswParkRideSandook(),
) : NswParkRideSandook by delegate {
    override suspend fun insertOrReplaceSavedParkRides(
        parkRideInfoList: Set<SavedParkRide>,
        source: NswParkRideSandook.Companion.SavedParkRideSource,
    ) {
        error("disk full")
    }
}

class IntroFlowViewModelParkRideAddTest {

    @Test
    fun `add to home saves every car park of the station as a user added row and marks it added`() = krailRunTest {
        val rig = IntroRig(this)
        rig.showTallawong()

        rig.send(IntroUiEvent.ToggleParkRide)

        assertTrue(rig.state.addedParkRide)
        assertEquals(
            listOf("26" to "Tallawong P1", "27" to "Tallawong P2", "28" to "Tallawong P3"),
            rig.savedRows(UserAdded).sortedBy { it.facilityId }.map { it.facilityId to it.facilityName },
            "one row per car park of the station",
        )
        assertEquals(setOf("2155384"), rig.savedRows(UserAdded).map { it.stopId }.toSet())
        assertEquals(setOf("Tallawong Station"), rig.savedRows(UserAdded).map { it.stopName }.toSet())
        assertTrue(rig.savedRows(SavedTrips).isEmpty(), "a saved trip owns none of them")
        assertIs<ParkRideCardPhase.Card>(rig.state.parkRideCard, "the card stays, now Added")
    }

    @Test
    fun `adding tracks one event with source intro and stop ids only`() = krailRunTest {
        val rig = IntroRig(this)
        rig.showTallawong()

        rig.send(IntroUiEvent.ToggleParkRide)

        val event = rig.parkRideEvents().single()
        assertEquals(
            mapOf("facilityId" to "26, 27, 28", "stopId" to "2155384", "action" to "add", "source" to "intro"),
            event.properties,
        )
        val values = event.properties.orEmpty().values.map { it.toString() }
        listOf("Tallawong", "Station", "33.69", "150.90").forEach { forbidden ->
            assertTrue(values.none { it.contains(forbidden) }, "a property carries $forbidden: $values")
        }
    }

    @Test
    fun `a double tap adds once and tracks once`() = krailRunTest {
        val rig = IntroRig(this)
        rig.showTallawong()

        rig.send(IntroUiEvent.ToggleParkRide, IntroUiEvent.ToggleParkRide)

        assertTrue(rig.state.addedParkRide)
        assertEquals(3, rig.savedRows(UserAdded).size)
        assertEquals(1, rig.parkRideEvents().size)
    }

    @Test
    fun `the stop name the loader filed under the stop id is corrected to the station name after add`() =
        krailRunTest {
            val shared = FakeNswParkRideSandook()
            val loader = RealParkRideAvailabilityLoader(shared, FakeParkRideService(), FakeFlag())
            val rig = IntroRig(this, availabilityLoader = loader, parkRideSandook = shared)
            rig.showBellaVista()
            val before = shared.getByStopIds(listOf("2153478")).single()
            assertEquals("2153478", before.stopName, "the loader files a car park that is not saved under its stop id")

            rig.send(IntroUiEvent.ToggleParkRide)

            val after = shared.getByStopIds(listOf("2153478")).single()
            assertEquals("Bella Vista Station", after.stopName)
            assertEquals(before.timestamp, after.timestamp, "the time of the reading, and so the cooldown, is kept")
            assertEquals(before.spotsAvailable, after.spotsAvailable)
        }

    @Test
    fun `adding while the call is still running corrects the name once the reading lands`() = krailRunTest {
        val rig = IntroRig(this)
        val release = CompletableDeferred<Unit>()
        rig.loader.onRefresh = {
            release.await()
            // It read the saved rows before the add, so it files the reading under the stop id.
            rig.parkRideSandook.insertOrReplaceAll(listOf(reading("31", "2153478")))
        }
        rig.showBellaVista()

        rig.send(IntroUiEvent.ToggleParkRide)

        assertTrue(rig.state.addedParkRide, "Add never waits on the network")
        assertEquals(1, rig.savedRows(UserAdded).size)
        assertTrue(rig.parkRideSandook.getByStopIds(listOf("2153478")).isEmpty(), "nothing to correct yet")

        release.complete(Unit)
        runCurrent()

        assertEquals("Bella Vista Station", rig.parkRideSandook.getByStopIds(listOf("2153478")).single().stopName)
    }

    @Test
    fun `add to home after answering no adds nothing`() = krailRunTest {
        val rig = IntroRig(this)
        rig.showBellaVista()

        rig.send(IntroUiEvent.AnswerParkRide(yes = false), IntroUiEvent.ToggleParkRide)

        assertFalse(rig.state.addedParkRide)
        assertTrue(rig.savedRows(UserAdded).isEmpty())
        assertTrue(rig.parkRideEvents().isEmpty())
    }

    @Test
    fun `a save that fails leaves the card not added and tracks nothing`() = krailRunTest {
        val rig = IntroRig(this, parkRideSandook = SaveFailsParkRideSandook())
        rig.showBellaVista()

        rig.send(IntroUiEvent.ToggleParkRide)

        assertFalse(rig.state.addedParkRide, "nothing was saved, so the card is not Added")
        assertTrue(rig.savedRows(UserAdded).isEmpty())
        assertTrue(rig.parkRideEvents().isEmpty())
    }

    @Test
    fun `a station with no stop name is saved under its station name`() = krailRunTest {
        val rig = IntroRig(this, stations = listOf(BELLA_VISTA_STATION.copy(stopName = "")))
        rig.showBellaVista()

        rig.send(IntroUiEvent.ToggleParkRide)

        assertEquals(listOf("Bella Vista"), rig.savedRows(UserAdded).map { it.stopName })
    }

    @Test
    fun `the outcome reports the answer and the location result for every path that adds nothing`() = krailRunTest {
        // Each path ends the Intro with a skip, so only the three Park & Ride params differ.
        val paths = listOf(
            "granted and a station shown, not added" to listOf(
                IntroUiEvent.AnswerParkRide(yes = true),
                IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, FOUR_POINT_SIX_KM_FROM_BELLA_VISTA),
            ),
            "granted and no station near" to listOf(
                IntroUiEvent.AnswerParkRide(yes = true),
                IntroUiEvent.LocationResultReceived(LocationResult.GRANTED, AT_CENTRAL),
            ),
            "denied" to listOf(
                IntroUiEvent.AnswerParkRide(yes = true),
                IntroUiEvent.LocationResultReceived(LocationResult.DENIED, null),
            ),
            "timed out" to listOf(
                IntroUiEvent.AnswerParkRide(yes = true),
                IntroUiEvent.LocationResultReceived(LocationResult.UNAVAILABLE, null),
            ),
            "answered no" to listOf(IntroUiEvent.AnswerParkRide(yes = false)),
            "never answered" to emptyList(),
        )
        val expected = mapOf(
            "granted and a station shown, not added" to Triple("yes", "granted", false),
            "granted and no station near" to Triple("yes", "granted", false),
            "denied" to Triple("yes", "denied", false),
            "timed out" to Triple("yes", "unavailable", false),
            "answered no" to Triple("no", "not_asked", false),
            "never answered" to Triple("skipped", "not_asked", false),
        )

        paths.forEach { (path, events) ->
            val rig = IntroRig(this)

            rig.send(*events.toTypedArray())
            rig.send(IntroUiEvent.Next, IntroUiEvent.FinishPressed)

            val params = rig.outcomeEvent().properties.orEmpty()
            assertEquals(
                expected.getValue(path),
                Triple(params["parkRideAnswer"], params["locationResult"], params["addedParkRide"]),
                path,
            )
        }
    }
}
