package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntroStopNamesTest {

    @Test
    fun `the station of a stop is its name up to the first comma`() {
        listOf(
            "Central Station" to "Central Station",
            "Chatswood Station, Stand A" to "Chatswood Station",
            "Central Station, Railway Square, Stand J" to "Central Station",
            "  Juniors Kingsford Light Rail , Stand B" to "Juniors Kingsford Light Rail",
            "Macquarie Rd after Tallawong Rd" to "Macquarie Rd after Tallawong Rd",
            "" to "",
        ).forEach { (name, station) -> assertEquals(station, stationNameOf(name), "\"$name\"") }
    }

    @Test
    fun `two stops are the same place when the id matches or the station name matches`() {
        val rows = listOf(
            Triple(CENTRAL_STOP, CENTRAL_STOP, true),
            Triple(CENTRAL_STOP, IntroStop("200060", "Central"), true), // the same id under another name
            Triple(CHATSWOOD_STOP, CHATSWOOD_STAND_A, true), // the same station, another stop
            Triple(CHATSWOOD_STAND_A, CHATSWOOD_STAND_B, true),
            Triple(
                CENTRAL_STOP,
                IntroStop("999", "  central   station "),
                true,
            ), // case and spaces are not a difference
            Triple(CENTRAL_STOP, TOWN_HALL_STOP, false),
            Triple(CHATSWOOD_STOP, IntroStop("206999", "Chatswood Oval, Fullers Rd"), false),
            Triple(SCHOFIELDS_STOP, TALLAWONG_STOP, false),
        )

        rows.forEach { (a, b, same) ->
            assertEquals(same, a.isSameStopAs(b), "$a and $b")
            assertEquals(same, b.isSameStopAs(a), "$b and $a, the other way round")
        }
    }

    @Test
    fun `a station with stands collapses to the stop named as the station, wherever it arrives`() {
        assertEquals(
            listOf(CHATSWOOD_STOP),
            listOf(CHATSWOOD_STAND_A, CHATSWOOD_STOP, CHATSWOOD_STAND_B).collapsedByStationName(),
        )
        assertEquals(
            listOf(JUNIORS_KINGSFORD),
            listOf(JUNIORS_KINGSFORD_STAND_B, JUNIORS_KINGSFORD_STAND_A, JUNIORS_KINGSFORD).collapsedByStationName(),
        )
    }

    @Test
    fun `a station with no stop named as the station keeps its nearest stop, with its own id and name`() {
        assertEquals(
            listOf(CHATSWOOD_STAND_B),
            listOf(CHATSWOOD_STAND_B, CHATSWOOD_STAND_A).collapsedByStationName(),
        )
    }

    @Test
    fun `collapsing keeps the order the stations arrive in, which is nearest first`() {
        val arrived = listOf(
            CENTRAL_STOP,
            CHATSWOOD_STAND_A,
            REDFERN_STOP,
            CHATSWOOD_STOP,
            JUNIORS_KINGSFORD_STAND_A,
            JUNIORS_KINGSFORD,
        )

        assertEquals(
            listOf(CENTRAL_STOP, CHATSWOOD_STOP, REDFERN_STOP, JUNIORS_KINGSFORD),
            arrived.collapsedByStationName(),
        )
    }

    @Test
    fun `different stations are not collapsed and an empty list stays empty`() {
        val stations = listOf(CENTRAL_STOP, REDFERN_STOP, MUSEUM_STOP)

        assertEquals(stations, stations.collapsedByStationName())
        assertEquals(emptyList(), emptyList<IntroStop>().collapsedByStationName())
    }

    @Test
    fun `leaving out a stop removes the same place and nothing when there is none to leave out`() {
        val rows = listOf(CENTRAL_STOP, CHATSWOOD_STOP, REDFERN_STOP)

        assertEquals(listOf(CENTRAL_STOP, REDFERN_STOP), rows.without(CHATSWOOD_STAND_A))
        assertEquals(listOf(CHATSWOOD_STOP, REDFERN_STOP), rows.without(CENTRAL_STOP))
        assertEquals(rows, rows.without(null))
        assertEquals(rows, rows.without(TOWN_HALL_STOP))
    }

    @Test
    fun `a suggested stop is left out by the same rule, and its id and name carry over`() {
        val suggested = listOf(CENTRAL_STOP.asSuggested(), WYNYARD_STOP.asSuggested())

        assertEquals(listOf(WYNYARD_STOP.asSuggested()), suggested.withoutStop(CENTRAL_STOP))
        assertEquals(suggested, suggested.withoutStop(null))
        assertEquals(CENTRAL_STOP, CENTRAL_STOP.asSuggested().toIntroStop())
        assertFalse(suggested.withoutStop(WYNYARD_STOP).any { it.stopId == WYNYARD_STOP.stopId })
        assertTrue(suggested.withoutStop(WYNYARD_STOP).isNotEmpty())
    }
}
