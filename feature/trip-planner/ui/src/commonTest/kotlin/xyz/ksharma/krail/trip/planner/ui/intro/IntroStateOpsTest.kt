package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAvailabilityState
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCarPark
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardStation
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntroStateOpsTest {

    private val named = ParkRideCarPark(facilityId = "26", name = "Tallawong P1")
    private val read = ParkRideCarPark(
        facilityId = "27",
        name = "Tallawong P2",
        spotsAvailable = 0,
        percentageFull = 100,
    )
    private val loading = IntroState(
        parkRideCard = ParkRideCardPhase.Card(
            ParkRideCardStation(stationId = "2155384", stationName = "Tallawong", carParks = persistentListOf(named)),
        ),
    )

    private val bothPicked = IntroState(
        startStopId = "1",
        startName = "One",
        endStopId = "2",
        endName = "Two",
    )

    @Test
    fun `a start pick sets the start and keeps a different end`() {
        val picked = bothPicked.withPick(isStart = true, stopId = "3", stopName = "Three")

        assertEquals("3", picked.startStopId)
        assertEquals("Three", picked.startName)
        assertEquals("2", picked.endStopId)
        assertEquals("Two", picked.endName)
    }

    @Test
    fun `an end pick sets the end and keeps a different start`() {
        val picked = bothPicked.withPick(isStart = false, stopId = "3", stopName = "Three")

        assertEquals("3", picked.endStopId)
        assertEquals("Three", picked.endName)
        assertEquals("1", picked.startStopId)
        assertEquals("One", picked.startName)
    }

    @Test
    fun `a start pick equal to the end keeps the pick and clears the end`() {
        val picked = bothPicked.withPick(isStart = true, stopId = "2", stopName = "Two")

        assertEquals("2", picked.startStopId)
        assertNull(picked.endStopId)
        assertNull(picked.endName)
        assertFalse(picked.hasTrip)
    }

    @Test
    fun `an end pick equal to the start keeps the pick and clears the start`() {
        val picked = bothPicked.withPick(isStart = false, stopId = "1", stopName = "One")

        assertEquals("1", picked.endStopId)
        assertNull(picked.startStopId)
        assertNull(picked.startName)
        assertFalse(picked.hasTrip)
    }

    @Test
    fun `a pair is a trip with the same id the other save paths use`() {
        val trip = bothPicked.toTripOrNull()

        assertEquals(Trip(fromStopId = "1", fromStopName = "One", toStopId = "2", toStopName = "Two"), trip)
        assertEquals("1->2", trip?.tripId)
    }

    @Test
    fun `a pair missing any of its four fields is no trip`() {
        listOf(
            bothPicked.copy(startStopId = null),
            bothPicked.copy(startName = null),
            bothPicked.copy(endStopId = null),
            bothPicked.copy(endName = null),
        ).forEach { assertNull(it.toTripOrNull(), "state $it") }
    }

    @Test
    fun `the flag lists are used when they are valid, as json or as a string`() {
        val json = """{"start":[{"stopId":"1","name":"One"}],"end":[{"stopId":"2","name":"Two"}]}"""
        val expected = SuggestedStops(
            start = listOf(SuggestedStop("1", "One")),
            end = listOf(SuggestedStop("2", "Two")),
        )

        listOf(FlagValue.JsonValue(json), FlagValue.StringValue(json)).forEach { value ->
            val flag = FakeFlag().apply { setFlagValue(FlagKeys.INTRO_SUGGESTED_STOPS.key, value) }

            assertEquals(expected, flag.introSuggestedStops(), "flag value $value")
        }
    }

    @Test
    fun `the bundled lists are used when the flag is unset, empty, unusable or not text`() {
        val unusable = listOf<FlagValue?>(
            null,
            FlagValue.StringValue(""),
            FlagValue.JsonValue("{not json"),
            FlagValue.JsonValue("""{"start":[],"end":[]}"""),
            FlagValue.BooleanValue(true),
        )

        unusable.forEach { value ->
            val flag = FakeFlag()
            value?.let { flag.setFlagValue(FlagKeys.INTRO_SUGGESTED_STOPS.key, it) }

            assertEquals(DEFAULT_SUGGESTED_STOPS, flag.introSuggestedStops(), "flag value $value")
        }
    }

    // region Park & Ride card

    private fun IntroState.station() = (parkRideCard as ParkRideCardPhase.Card).station

    @Test
    fun `a reading on any car park settles the card as content with every car park`() {
        val settled = loading.withReadings("2155384", listOf(named, read))

        assertEquals(ParkRideAvailabilityState.Content, settled.station().availability)
        assertEquals(listOf(named, read), settled.station().carParks)
    }

    @Test
    fun `no reading on any car park settles the card as unavailable and keeps the names`() {
        listOf(listOf(named), emptyList(), null).forEach { carParks ->
            val settled = loading.withReadings("2155384", carParks)

            assertEquals(ParkRideAvailabilityState.Unavailable, settled.station().availability, "$carParks")
            assertEquals(listOf(named), settled.station().carParks, "$carParks")
        }
    }

    @Test
    fun `readings for another station, or when there is no card, change nothing`() {
        assertEquals(loading, loading.withReadings("2153478", listOf(read)))
        assertEquals(IntroState(), IntroState().withReadings("2155384", listOf(read)))
    }

    @Test
    fun `only a location being awaited, a card or the no station message is in progress or shown`() {
        assertTrue(ParkRideCardPhase.Locating().isInProgressOrShown())
        assertTrue(loading.parkRideCard.isInProgressOrShown())
        assertTrue(ParkRideCardPhase.NoStationNear.isInProgressOrShown(), "a second yes must not start another look")
        assertFalse(ParkRideCardPhase.None.isInProgressOrShown())
    }

    // endregion

    // region Add to home and the start it fills in

    @Test
    fun `adding fills in the start only when no start is picked and the stop is not the end`() {
        val filled = IntroState().withAddedParkRide("1", "One")
        assertEquals("1", filled.startStopId)
        assertEquals("One", filled.startName)
        assertTrue(filled.startFromParkRide)
        assertTrue(filled.addedParkRide)

        val picked = IntroState(startStopId = "9", startName = "Nine").withAddedParkRide("1", "One")
        assertEquals("9", picked.startStopId, "a start the rider picked stays")
        assertFalse(picked.startFromParkRide)
        assertTrue(picked.addedParkRide)

        val sameAsEnd = IntroState(endStopId = "1", endName = "One").withAddedParkRide("1", "One")
        assertNull(sameAsEnd.startStopId, "never a trip from a stop to itself")
        assertEquals("1", sameAsEnd.endStopId)
        assertTrue(sameAsEnd.addedParkRide)
    }

    @Test
    fun `adding with no stop that resolves is added with no start`() {
        val added = IntroState().withAddedParkRide(null, null)

        assertTrue(added.addedParkRide)
        assertNull(added.startStopId)
        assertFalse(added.startFromParkRide)
    }

    @Test
    fun `removing clears the start only while it is the one add filled in`() {
        val filled = IntroState().withAddedParkRide("1", "One")
        val removed = filled.withoutParkRide()
        assertFalse(removed.addedParkRide)
        assertNull(removed.startStopId)
        assertNull(removed.startName)
        assertFalse(removed.startFromParkRide)

        val picked = IntroState(startStopId = "9", startName = "Nine", addedParkRide = true).withoutParkRide()
        assertFalse(picked.addedParkRide)
        assertEquals("9", picked.startStopId, "a start the rider picked is not the Intro's to clear")
    }

    @Test
    fun `any pick of the start, even the stop add chose, makes it the rider's`() {
        val filled = IntroState().withAddedParkRide("1", "One")

        assertFalse(filled.withPick(isStart = true, stopId = "1", stopName = "One").startFromParkRide)
        assertFalse(filled.withPick(isStart = true, stopId = "2", stopName = "Two").startFromParkRide)
        // An end pick that equals the start clears the start, and with it the claim that add chose it.
        val cleared = filled.withPick(isStart = false, stopId = "1", stopName = "One")
        assertNull(cleared.startStopId)
        assertFalse(cleared.startFromParkRide)
        // An end pick that leaves the start alone leaves the claim alone.
        assertTrue(filled.withPick(isStart = false, stopId = "2", stopName = "Two").startFromParkRide)
    }

    // endregion

    @Test
    fun `a finish writes the pair only for a yes, only when it is complete and only once`() {
        val trip = bothPicked.toTripOrNull()
        val startOnly = IntroState(startStopId = "1", startName = "One")
        val endOnly = IntroState(endStopId = "2", endName = "Two")
        val rows = listOf(
            // state, save, trip written
            Triple(bothPicked, true, trip),
            Triple(bothPicked, false, null),
            Triple(bothPicked.copy(saved = true), true, null),
            Triple(bothPicked.copy(saved = true), false, null),
            Triple(startOnly, true, null),
            Triple(endOnly, true, null),
            Triple(IntroState(), true, null),
            Triple(IntroState(), false, null),
        )

        rows.forEach { (state, save, written) ->
            assertEquals(written, state.tripToWrite(save), "state=$state save=$save")
        }
    }
}
