package xyz.ksharma.krail.trip.planner.ui.intro

import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideAvailabilityState
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCarPark
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardPhase
import xyz.ksharma.krail.trip.planner.ui.state.intro.ParkRideCardStation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class IntroParkRideCardTest {

    private val bellaVistaName = ParkRideCarPark(facilityId = "31", name = "Bella Vista")
    private val bellaVistaReading = bellaVistaName.copy(spotsAvailable = 120, percentageFull = 40, timeText = "8:05 AM")

    private fun station(vararg carParks: ParkRideCarPark, availability: ParkRideAvailabilityState) =
        ParkRideCardStation(
            stationId = "2153478",
            stationName = "Bella Vista",
            carParks = persistentListOf(*carParks),
            availability = availability,
        )

    private fun card(vararg carParks: ParkRideCarPark, availability: ParkRideAvailabilityState) =
        ParkRideCardPhase.Card(station(*carParks, availability = availability))

    @Test
    fun `no card phase gives an empty slot`() {
        assertEquals(IntroCardSlot.Empty, introCardSlot(IntroState(parkRideCard = ParkRideCardPhase.None)))
    }

    @Test
    fun `locating gives the skeleton whether or not a saved station is being worked out again`() {
        assertEquals(IntroCardSlot.Skeleton, introCardSlot(IntroState(parkRideCard = ParkRideCardPhase.Locating())))
        assertEquals(
            IntroCardSlot.Skeleton,
            introCardSlot(IntroState(parkRideCard = ParkRideCardPhase.Locating(stationId = "2153478"))),
        )
    }

    @Test
    fun `no station near draws nothing, because that answer leaves nothing to decide and the screen moves on`() {
        assertEquals(
            IntroCardSlot.Empty,
            introCardSlot(IntroState(parkRideCard = ParkRideCardPhase.NoStationNear)),
        )
    }

    @Test
    fun `a card is the same card whether or not it was added, because the bar and not the card carries the action`() {
        val phase = card(bellaVistaReading, availability = ParkRideAvailabilityState.Content)
        val plain = assertIs<IntroCardSlot.Station>(introCardSlot(IntroState(parkRideCard = phase)))
        val added = assertIs<IntroCardSlot.Station>(
            introCardSlot(IntroState(parkRideCard = phase, addedParkRide = true)),
        )

        assertEquals(plain, added)
    }

    @Test
    fun `a reading maps to the numbers the shared card shows`() {
        val facility = station(bellaVistaReading, availability = ParkRideAvailabilityState.Content)
            .toParkRideUiState()
            .facilities
            .single()

        assertEquals(120, facility.spotsAvailable)
        assertEquals(40, facility.percentageFull)
        assertEquals("8:05 AM", facility.timeText)
        assertEquals("Bella Vista", facility.facilityName)
        assertEquals("31", facility.facilityId)
    }

    @Test
    fun `a car park with no reading maps to the cards no reading mark and no time`() {
        val facility = station(bellaVistaName, availability = ParkRideAvailabilityState.Loading)
            .toParkRideUiState()
            .facilities
            .single()

        assertEquals(-1, facility.spotsAvailable)
        assertEquals(-1, facility.percentageFull)
        assertEquals("", facility.timeText)
        assertEquals("Bella Vista", facility.facilityName)
    }

    @Test
    fun `an unavailable station keeps every car park name`() {
        val tallawong = listOf("Tallawong P1", "Tallawong P2", "Tallawong P3").mapIndexed { index, name ->
            ParkRideCarPark(facilityId = "${26 + index}", name = name)
        }.toTypedArray()

        val names = station(*tallawong, availability = ParkRideAvailabilityState.Unavailable)
            .toParkRideUiState()
            .facilities
            .map { it.facilityName }

        assertEquals(listOf("Tallawong P1", "Tallawong P2", "Tallawong P3"), names)
    }

    @Test
    fun `one car park with a reading and one without keep their own numbers`() {
        val facilities = station(
            bellaVistaReading,
            ParkRideCarPark(facilityId = "32", name = "Bella Vista (south)"),
            availability = ParkRideAvailabilityState.Content,
        ).toParkRideUiState().facilities.toList()

        assertEquals(listOf(120, -1), facilities.map { it.spotsAvailable })
    }

    @Test
    fun `a station with no car park names is drawn as one car park named after the station`() {
        val ui = station(availability = ParkRideAvailabilityState.Loading).toParkRideUiState()

        assertEquals(listOf("Bella Vista"), ui.facilities.map { it.facilityName })
        assertEquals("Bella Vista", ui.stopName)
        assertEquals("2153478", ui.stopId)
    }

    @Test
    fun `the words the rider reads are the agreed ones`() {
        assertEquals("Driving to the station", PARK_RIDE_HEADING)
        assertEquals("Yes", PARK_RIDE_YES)
        assertEquals("No", PARK_RIDE_NO)
        assertEquals("Yes, show me free spots", PARK_RIDE_YES_SPOKEN)
        assertEquals("No thanks", PARK_RIDE_NO_SPOKEN)
        assertEquals("Add to home", PARK_RIDE_ADD)
        assertEquals("Not now", PARK_RIDE_NOT_NOW)
    }

    @Test
    fun `no rider facing string has a colon, a question mark, an em dash, an arrow or a middle dot`() {
        val banned = listOf(':', '?', '!', Char(EM_DASH), Char(RIGHT_ARROW), Char(LEFT_ARROW), Char(MIDDLE_DOT))
        val strings = listOf(
            PARK_RIDE_HEADING,
            PARK_RIDE_YES,
            PARK_RIDE_YES_SPOKEN,
            PARK_RIDE_NO,
            PARK_RIDE_NO_SPOKEN,
            PARK_RIDE_ADD,
            PARK_RIDE_NOT_NOW,
            CONFIRM_SAVE,
            CONFIRM_NOT_NOW,
        )

        strings.forEach { text ->
            banned.forEach { glyph -> assertFalse(text.contains(glyph), "\"$text\" contains $glyph") }
        }
    }
}

// Written as code points so no banned glyph is typed into this file.
private const val EM_DASH = 0x2014
private const val RIGHT_ARROW = 0x2192
private const val LEFT_ARROW = 0x2190
private const val MIDDLE_DOT = 0x00B7
