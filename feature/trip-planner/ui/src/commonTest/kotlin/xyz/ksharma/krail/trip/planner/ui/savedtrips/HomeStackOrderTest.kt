package xyz.ksharma.krail.trip.planner.ui.savedtrips

import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.PARK_RIDE_ROW_KEY
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ReorderSession
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Where the Park & Ride row sits among the saved trips (`docs/MAP_HOME_UX.md` rule 29): after the
 * second trip by default, after the only trip with one, first with none, and wherever the rider
 * dragged it, clamped to the trips there are. During a reorder the stack is the session's order.
 */
class HomeStackOrderTest {

    private class Case(val name: String, val trips: Int, val stored: Int?, val expected: Int)

    @Test
    fun `the row is after the second trip until dragged`() {
        val cases = listOf(
            Case("no trips, the row comes first", trips = 0, stored = null, expected = 0),
            Case("one trip, the row follows it", trips = 1, stored = null, expected = 1),
            Case("two trips, the row follows both", trips = 2, stored = null, expected = 2),
            Case("three trips, two then the row", trips = 3, stored = null, expected = 2),
            Case("many trips, still after the second", trips = 12, stored = null, expected = 2),
        )
        cases.forEach { case ->
            assertEquals(case.expected, rowIndexFor(case.trips, case.stored), case.name)
        }
    }

    @Test
    fun `a dragged place is kept and clamped when trips are added or removed`() {
        val cases = listOf(
            Case("dragged to the top", trips = 5, stored = 0, expected = 0),
            Case("dragged to the middle", trips = 5, stored = 3, expected = 3),
            Case("dragged to the very end", trips = 5, stored = 5, expected = 5),
            Case("trips deleted below the stored place", trips = 2, stored = 4, expected = 2),
            Case("all trips deleted", trips = 0, stored = 4, expected = 0),
            Case("trips added keep the stored place", trips = 9, stored = 4, expected = 4),
            Case("a bad negative write counts as first", trips = 5, stored = -3, expected = 0),
            Case("a negative trip count is no trips", trips = -1, stored = 2, expected = 0),
        )
        cases.forEach { case ->
            assertEquals(case.expected, rowIndexFor(case.trips, case.stored), case.name)
        }
    }

    @Test
    fun `the stack keys put the row after the stored number of trips`() {
        val trips = listOf("a", "b", "c", "d")
        val row = PARK_RIDE_ROW_KEY

        assertEquals(
            listOf("a", "b", row, "c", "d"),
            homeStackKeys(trips, hasRow = true, storedIndex = null, session = null),
        )
        assertEquals(
            listOf(row, "a", "b", "c", "d"),
            homeStackKeys(trips, hasRow = true, storedIndex = 0, session = null),
        )
        assertEquals(
            listOf("a", "b", "c", "d", row),
            homeStackKeys(trips, hasRow = true, storedIndex = 9, session = null),
        )
        assertEquals(listOf(row), homeStackKeys(emptyList(), hasRow = true, storedIndex = null, session = null))
        assertEquals(listOf("a"), homeStackKeys(listOf("a"), hasRow = false, storedIndex = 0, session = null))
        assertEquals(
            emptyList<String>(),
            homeStackKeys(emptyList(), hasRow = false, storedIndex = null, session = null),
        )
    }

    @Test
    fun `during a reorder the stack is the session's order, with a removed trip gone`() {
        val trips = listOf("a", "b", "c")
        val session = ReorderSession.start(trips, rowIndex = 1).move("a", "c").remove("b")

        assertEquals(
            session.keys.toList(),
            homeStackKeys(trips, hasRow = true, storedIndex = 1, session = session),
        )
        // Start [a, row, b, c]. Moving a to where c is gives [row, b, c, a], and taking b off gives [row, c, a].
        assertEquals(
            listOf(PARK_RIDE_ROW_KEY, "c", "a"),
            homeStackKeys(trips, hasRow = true, storedIndex = 1, session = session),
        )
    }

    @Test
    fun `a session key that is no longer on home is dropped and never throws`() {
        val session = ReorderSession.start(listOf("a", "b"), rowIndex = 1)

        assertEquals(
            listOf("a", "b"),
            homeStackKeys(listOf("a", "b"), hasRow = false, storedIndex = null, session = session),
        )
        assertEquals(
            listOf("a", PARK_RIDE_ROW_KEY),
            homeStackKeys(listOf("a"), hasRow = true, storedIndex = null, session = session),
        )
    }

    @Test
    fun `cancel is the saved order again`() {
        val trips = listOf("a", "b", "c")
        val saved = homeStackKeys(trips, hasRow = true, storedIndex = null, session = null)
        val dragging = ReorderSession.start(trips, rowIndex = 2).move("a", "c")

        // Start [a, b, row, c]. Moving a to where c is gives [b, row, c, a].
        assertEquals(listOf("b", PARK_RIDE_ROW_KEY, "c", "a"), dragging.keys.toList())
        assertEquals(listOf("a", "b", PARK_RIDE_ROW_KEY, "c"), saved, "Dropping the session shows the saved order")
    }

    @Test
    fun `the default place is two trips in`() {
        assertEquals(2, DEFAULT_PARK_RIDE_ROW_AFTER_TRIPS)
    }

    // region the one-time suggestion takes the row's place

    private class SuggestionCase(
        val name: String,
        val trips: List<String>,
        val show: Boolean,
        val stored: Int?,
        val expected: List<String>,
    )

    @Test
    fun `the suggestion card sits where the row would, after the second trip until the row was dragged`() {
        val key = PARK_RIDE_SUGGESTION_KEY
        val four = listOf("a", "b", "c", "d")
        listOf(
            SuggestionCase("no trips, the card comes first", emptyList(), true, null, listOf(key)),
            SuggestionCase("one trip, the card follows it", listOf("a"), true, null, listOf("a", key)),
            SuggestionCase("two trips, the card follows both", listOf("a", "b"), true, null, listOf("a", "b", key)),
            SuggestionCase("four trips, after the second", four, true, null, listOf("a", "b", key, "c", "d")),
            SuggestionCase("a dragged place is kept", four, true, 3, listOf("a", "b", "c", key, "d")),
            SuggestionCase(
                "a dragged place past the trips is clamped",
                listOf("a", "b"),
                true,
                9,
                listOf("a", "b", key),
            ),
            SuggestionCase("a dragged place at the top", listOf("a", "b"), true, 0, listOf(key, "a", "b")),
            SuggestionCase("not shown, the trips alone", listOf("a", "b", "c"), false, null, listOf("a", "b", "c")),
            SuggestionCase("not shown with no trips, an empty stack", emptyList(), false, null, emptyList()),
        ).forEach { case ->
            assertEquals(case.expected, withSuggestionKey(case.trips, case.show, case.stored), case.name)
        }
    }

    @Test
    fun `the suggestion key cannot be mistaken for the row`() {
        assertEquals(false, PARK_RIDE_SUGGESTION_KEY == PARK_RIDE_ROW_KEY)
    }

    // endregion
}
