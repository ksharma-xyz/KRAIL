package xyz.ksharma.krail.trip.planner.ui.intro.v2

import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.FlagValue
import xyz.ksharma.krail.core.testing.fakes.FakeFlag
import xyz.ksharma.krail.trip.planner.ui.intro.DEFAULT_SUGGESTED_STOPS
import xyz.ksharma.krail.trip.planner.ui.intro.SuggestedStop
import xyz.ksharma.krail.trip.planner.ui.intro.SuggestedStops
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.ParkRideCardStation
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntroStateOpsTest {

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

    @Test
    fun `only a location being awaited or a card is in progress or shown`() {
        assertTrue(ParkRideCardPhase.Locating().isInProgressOrShown())
        assertTrue(ParkRideCardPhase.Card(ParkRideCardStation("2155384", "Tallawong")).isInProgressOrShown())
        assertFalse(ParkRideCardPhase.None.isInProgressOrShown())
        assertFalse(ParkRideCardPhase.NoStationNear.isInProgressOrShown())
    }

    // endregion
}
