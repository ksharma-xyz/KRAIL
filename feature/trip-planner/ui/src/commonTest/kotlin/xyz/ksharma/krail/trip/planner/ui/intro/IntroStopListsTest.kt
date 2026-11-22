package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IntroStopListsTest {

    // The curated lists the app ships with, so these are the lists a rider gets.
    private val curatedStarts = DEFAULT_SUGGESTED_STOPS.start
    private val curatedEnds = DEFAULT_SUGGESTED_STOPS.end

    private fun ids(rows: List<IntroStop>) = rows.map { it.stopId }

    // region fromSuggestions

    @Test
    fun `with no fix the origin list is the curated starts alone, in their order`() {
        assertEquals(
            listOf(TALLAWONG_STOP, CENTRAL_STOP, SCHOFIELDS_STOP, PARRAMATTA_STOP, TOWN_HALL_STOP),
            fromSuggestions(nearby = emptyList(), curatedStarts = curatedStarts),
        )
    }

    @Test
    fun `three nearby stations come first, nearest first, and the curated starts fill the five`() {
        val nearby = listOf(REDFERN_STOP, MUSEUM_STOP, ST_JAMES_STOP)

        assertEquals(
            listOf(REDFERN_STOP, MUSEUM_STOP, ST_JAMES_STOP, TALLAWONG_STOP, CENTRAL_STOP),
            fromSuggestions(nearby, curatedStarts),
        )
    }

    @Test
    fun `fewer than three nearby stations are made up from the curated starts`() {
        assertEquals(
            listOf(REDFERN_STOP, TALLAWONG_STOP, CENTRAL_STOP, SCHOFIELDS_STOP, PARRAMATTA_STOP),
            fromSuggestions(listOf(REDFERN_STOP), curatedStarts),
        )
        assertEquals(
            listOf(REDFERN_STOP, MUSEUM_STOP, TALLAWONG_STOP, CENTRAL_STOP, SCHOFIELDS_STOP),
            fromSuggestions(listOf(REDFERN_STOP, MUSEUM_STOP), curatedStarts),
        )
    }

    @Test
    fun `only the nearest three stations lead, however many are nearby`() {
        val nearby = listOf(REDFERN_STOP, MUSEUM_STOP, ST_JAMES_STOP, SYDENHAM_STOP, WYNYARD_STOP)

        val rows = fromSuggestions(nearby, curatedStarts)

        assertEquals(listOf(REDFERN_STOP, MUSEUM_STOP, ST_JAMES_STOP), rows.take(3))
        assertEquals(false, SYDENHAM_STOP in rows, "the fourth nearest does not lead")
    }

    @Test
    fun `a station that is nearby and curated shows once, at its nearby place`() {
        val rows = fromSuggestions(listOf(CENTRAL_STOP, REDFERN_STOP), curatedStarts)

        assertEquals(
            listOf(CENTRAL_STOP, REDFERN_STOP, TALLAWONG_STOP, SCHOFIELDS_STOP, PARRAMATTA_STOP),
            rows,
        )
        assertEquals(rows.size, ids(rows).distinct().size)
    }

    @Test
    fun `a station that is the same by name but not by id shows once`() {
        val sameNameOtherId = IntroStop("999999", "Central Station")

        val rows = fromSuggestions(listOf(sameNameOtherId), curatedStarts)

        assertEquals(1, rows.count { it.name == "Central Station" })
        assertEquals("999999", rows.first().stopId, "the nearby one is kept, the curated one is the duplicate")
    }

    @Test
    fun `stands of one nearby station collapse to one row before the curated starts fill in`() {
        val nearby = listOf(CHATSWOOD_STAND_A, CHATSWOOD_STOP, CHATSWOOD_STAND_B, REDFERN_STOP)

        assertEquals(
            listOf(CHATSWOOD_STOP, REDFERN_STOP, TALLAWONG_STOP, CENTRAL_STOP, SCHOFIELDS_STOP),
            fromSuggestions(nearby, curatedStarts),
        )
    }

    @Test
    fun `the size caps the list, and a list shorter than the size is not padded`() {
        assertEquals(2, fromSuggestions(listOf(REDFERN_STOP, MUSEUM_STOP, ST_JAMES_STOP), curatedStarts, size = 2).size)
        assertEquals(
            listOf(CENTRAL_STOP, WYNYARD_STOP),
            fromSuggestions(emptyList(), listOf(CENTRAL_STOP.asSuggested(), WYNYARD_STOP.asSuggested())),
        )
        assertEquals(emptyList(), fromSuggestions(emptyList(), emptyList()))
    }

    // endregion

    // region the destination list

    @Test
    fun `the destination list is the curated ends alone and never moves with a nearby station`() {
        assertEquals(
            listOf(CENTRAL_STOP, WYNYARD_STOP, PARRAMATTA_STOP, TOWN_HALL_STOP, MARTIN_PLACE_STOP),
            destinationSuggestions(curatedEnds),
        )
    }

    @Test
    fun `the destination list shows a place once and is capped at its size`() {
        val ends = listOf(CENTRAL_STOP, CHATSWOOD_STOP, CHATSWOOD_STAND_A, WYNYARD_STOP).map { it.asSuggested() }

        assertEquals(listOf(CENTRAL_STOP, CHATSWOOD_STOP, WYNYARD_STOP), destinationSuggestions(ends))
        assertEquals(listOf(CENTRAL_STOP), destinationSuggestions(ends, size = 1))
        assertEquals(emptyList(), destinationSuggestions(emptyList()))
    }

    // endregion
}
