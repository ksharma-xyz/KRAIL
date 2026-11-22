package xyz.ksharma.krail.trip.planner.ui.intro.v2

import xyz.ksharma.krail.trip.planner.ui.intro.DEFAULT_SUGGESTED_STOPS
import xyz.ksharma.krail.trip.planner.ui.intro.SuggestedStop
import xyz.ksharma.krail.trip.planner.ui.intro.SuggestedStops
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.v2.IntroStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What step 2 shows for each state, as a pure function, so the decision is held here and the screen
 * only draws it.
 */
class IntroFirstTripLayoutTest {

    private val nothing = IntroState(step = IntroStep.FIRST_TRIP)
    private val startOnly = nothing.copy(startStopId = TALLAWONG.stopId, startName = TALLAWONG.name)
    private val both = startOnly.copy(endStopId = CENTRAL.stopId, endName = CENTRAL.name)

    @Test
    fun `nothing chosen offers the start suggestions only`() {
        assertEquals(
            listOf(IntroTripSection.Picking(IntroStopRole.START, selectedStopId = null, otherStopId = null)),
            introTripSections(nothing, changing = false),
        )
    }

    @Test
    fun `a chosen start collapses to one row and the end suggestions follow`() {
        assertEquals(
            listOf(
                IntroTripSection.Chosen(IntroStopRole.START, TALLAWONG.name),
                IntroTripSection.Picking(IntroStopRole.END, selectedStopId = null, otherStopId = TALLAWONG.stopId),
            ),
            introTripSections(startOnly, changing = false),
        )
    }

    @Test
    fun `a start filled in by Add to home is a collapsed row like any other start`() {
        val prefilled = startOnly.copy(startFromParkRide = true, addedParkRide = true)

        assertEquals(introTripSections(startOnly, changing = false), introTripSections(prefilled, changing = false))
    }

    @Test
    fun `both chosen is one tile`() {
        assertEquals(
            listOf(IntroTripSection.Tile(TALLAWONG.name, TALLAWONG.stopId, CENTRAL.name, CENTRAL.stopId)),
            introTripSections(both, changing = false),
        )
    }

    @Test
    fun `changing a trip opens both lists with the current picks marked`() {
        assertEquals(
            listOf(
                IntroTripSection.Picking(IntroStopRole.START, TALLAWONG.stopId, otherStopId = CENTRAL.stopId),
                IntroTripSection.Picking(IntroStopRole.END, CENTRAL.stopId, otherStopId = TALLAWONG.stopId),
            ),
            introTripSections(both, changing = true),
        )
    }

    @Test
    fun `changing a start with no end opens both lists with the start marked`() {
        assertEquals(
            listOf(
                IntroTripSection.Picking(IntroStopRole.START, TALLAWONG.stopId, otherStopId = null),
                IntroTripSection.Picking(IntroStopRole.END, selectedStopId = null, otherStopId = TALLAWONG.stopId),
            ),
            introTripSections(startOnly, changing = true),
        )
    }

    @Test
    fun `changing means nothing until something is chosen`() {
        assertEquals(introTripSections(nothing, changing = false), introTripSections(nothing, changing = true))
    }

    @Test
    fun `an end with no start asks for the start and keeps the end as a collapsed row`() {
        val endOnly = nothing.copy(endStopId = CENTRAL.stopId, endName = CENTRAL.name)

        assertEquals(
            listOf(
                IntroTripSection.Picking(IntroStopRole.START, selectedStopId = null, otherStopId = CENTRAL.stopId),
                IntroTripSection.Chosen(IntroStopRole.END, CENTRAL.name),
            ),
            introTripSections(endOnly, changing = false),
        )
    }

    @Test
    fun `the sections have different keys, so one never takes the place of another`() {
        val keys = introTripSections(both, changing = true).map { it.key } +
            introTripSections(startOnly, changing = false).map { it.key } +
            introTripSections(both, changing = false).map { it.key }

        assertEquals(setOf("picking-START", "picking-END", "chosen-START", "tile"), keys.toSet())
    }

    @Test
    fun `a list does not offer the stop chosen for the other end`() {
        val endNames = DEFAULT_SUGGESTED_STOPS.offeredFor(IntroStopRole.END, otherStopId = CENTRAL.stopId)
        val startNames = DEFAULT_SUGGESTED_STOPS.offeredFor(IntroStopRole.START, otherStopId = CENTRAL.stopId)

        assertFalse(endNames.any { it.stopId == CENTRAL.stopId })
        assertFalse(startNames.any { it.stopId == CENTRAL.stopId })
        assertEquals(DEFAULT_SUGGESTED_STOPS.end.size - 1, endNames.size)
        assertEquals(DEFAULT_SUGGESTED_STOPS.start.size - 1, startNames.size)
    }

    @Test
    fun `a list keeps its order and every stop when the other end is not chosen`() {
        assertEquals(DEFAULT_SUGGESTED_STOPS.start, DEFAULT_SUGGESTED_STOPS.offeredFor(IntroStopRole.START, null))
        assertEquals(DEFAULT_SUGGESTED_STOPS.end, DEFAULT_SUGGESTED_STOPS.offeredFor(IntroStopRole.END, null))
    }

    @Test
    fun `a stop the rider searched for and the lists do not share is offered nothing to hide`() {
        val searched = SuggestedStops(start = listOf(TALLAWONG), end = listOf(CENTRAL))

        assertEquals(listOf(CENTRAL), searched.offeredFor(IntroStopRole.END, otherStopId = "somewhere-else"))
    }

    @Test
    fun `choosing a suggestion is the event for its end of the trip`() {
        val stop = SuggestedStop(stopId = "200060", name = "Central Station")

        assertEquals(IntroUiEvent.PickStart(stop), pickEvent(IntroStopRole.START, stop))
        assertEquals(IntroUiEvent.PickEnd(stop), pickEvent(IntroStopRole.END, stop))
    }

    @Test
    fun `the section names are From and To`() {
        assertEquals("From", IntroStopRole.START.sectionLabel())
        assertEquals("To", IntroStopRole.END.sectionLabel())
    }

    @Test
    fun `each row says its stop and its role to a screen reader`() {
        val tallawong = "Tallawong Station"
        val central = "Central Station"

        assertEquals("Change start stop, now $tallawong", changeStopDescription(IntroStopRole.START, tallawong))
        assertEquals("Change end stop, now $central", changeStopDescription(IntroStopRole.END, central))
        assertEquals("Change stops, now $tallawong to $central", changeTripDescription(tallawong, central))
        assertEquals("Pick Wynyard Station as the end stop", pickStopDescription(IntroStopRole.END, "Wynyard Station"))
        assertEquals("Pick $tallawong as the start stop", pickStopDescription(IntroStopRole.START, tallawong))
        assertEquals("Search another stop for the start", searchStopDescription(IntroStopRole.START))
        assertEquals("Search another stop for the end", searchStopDescription(IntroStopRole.END))
    }

    @Test
    fun `every description contains the words on screen`() {
        assertTrue(searchStopDescription(IntroStopRole.START).contains(SEARCH_ANOTHER_STOP))
        assertTrue(pickStopDescription(IntroStopRole.END, "Wynyard Station").contains("Wynyard Station"))
        assertTrue(changeStopDescription(IntroStopRole.START, "Tallawong Station").contains("Tallawong Station"))
    }

    @Test
    fun `the words the rider reads are the agreed ones`() {
        assertEquals("Save your first trip", FIRST_TRIP_HEADING)
        assertEquals("Search another stop", SEARCH_ANOTHER_STOP)
    }

    @Test
    fun `no rider facing string has a colon, an em dash, an arrow or a middle dot`() {
        val banned = listOf(':', Char(EM_DASH), Char(RIGHT_ARROW), Char(LEFT_ARROW), Char(MIDDLE_DOT))
        val strings = listOf(
            FIRST_TRIP_HEADING,
            FIRST_TRIP_FROM,
            FIRST_TRIP_TO,
            SEARCH_ANOTHER_STOP,
            changeStopDescription(IntroStopRole.START, "Tallawong Station"),
            changeStopDescription(IntroStopRole.END, "Central Station"),
            changeTripDescription("Tallawong Station", "Central Station"),
            pickStopDescription(IntroStopRole.START, "Town Hall Station"),
            pickStopDescription(IntroStopRole.END, "Wynyard Station"),
            searchStopDescription(IntroStopRole.START),
            searchStopDescription(IntroStopRole.END),
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
