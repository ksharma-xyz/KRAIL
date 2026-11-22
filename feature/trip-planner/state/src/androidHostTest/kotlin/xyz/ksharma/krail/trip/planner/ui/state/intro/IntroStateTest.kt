package xyz.ksharma.krail.trip.planner.ui.state.intro

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

        assertEquals(IntroFirstStep, state.step)
        assertNull(state.selectedThemeId)
        assertEquals(ParkRideAnswer.ASK, state.parkRideAnswer)
        assertEquals(LocationResult.NOT_ASKED, state.locationResult)
        assertEquals(ParkRideCardPhase.None, state.parkRideCard)
        assertNull(state.startStopId)
        assertNull(state.endStopId)
        assertFalse(state.startFromParkRide)
        assertFalse(state.addedParkRide)
        assertFalse(state.usedSearch)
        assertFalse(state.saved)
        assertTrue(state.nearbyStops.isEmpty(), "no stations until a fix has found some")
        assertTrue(state.stopModes.isEmpty())
        assertEquals("", state.query)
        assertEquals(IntroSearchPhase.Idle, state.search)
    }

    @Test
    fun `a stop is an id and a name, and nothing that could be a position`() {
        val fields = IntroStop::class.java.declaredFields.filterNot { it.isSynthetic || it.name.startsWith("$") }

        assertEquals(setOf("stopId", "name"), fields.map { it.name }.toSet())
        assertTrue(fields.all { it.type == String::class.java }, "fields: ${fields.map { it.type.simpleName }}")
    }

    @Test
    fun `a search phase is idle, found or found nothing, and a found phase keeps its stops in order`() {
        val central = IntroStop("200060", "Central Station")
        val redfern = IntroStop("201510", "Redfern Station")
        val found = IntroSearchPhase.Results(persistentListOf(central, redfern))

        assertEquals(listOf(central, redfern), found.stops)
        assertNotEquals<IntroSearchPhase>(IntroSearchPhase.Idle, IntroSearchPhase.NoMatch)
        assertIs<IntroSearchPhase.Results>(found)
    }

    @Test
    fun `the screens are in the order a rider meets them`() {
        assertEquals(
            listOf(
                IntroStep.THEME,
                IntroStep.PARK_RIDE,
                IntroStep.DESTINATION,
                IntroStep.ORIGIN,
                IntroStep.CONFIRM,
            ),
            IntroStep.entries,
        )
    }

    @Test
    fun `the flow opens on the theme screen, the first of the five`() {
        assertEquals(IntroStep.THEME, IntroFirstStep)
        assertEquals(IntroStep.entries.first(), IntroFirstStep)
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
