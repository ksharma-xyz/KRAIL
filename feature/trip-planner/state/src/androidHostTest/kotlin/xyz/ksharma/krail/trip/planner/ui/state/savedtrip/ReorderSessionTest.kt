package xyz.ksharma.krail.trip.planner.ui.state.savedtrip

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * The reorder reducer of `docs/MAP_HOME_UX.md` rule 31, driven with no ViewModel: enter, move,
 * remove, Done and cancel. The Park & Ride row is one entry among the trips and moves as a block.
 */
class ReorderSessionTest {

    private val row = PARK_RIDE_ROW_KEY

    private fun session(rowIndex: Int? = 2, vararg trips: String = arrayOf("a", "b", "c", "d")) =
        ReorderSession.start(tripIds = trips.toList(), rowIndex = rowIndex)

    // region enter

    @Test
    fun `entering puts the row after the stored number of trips`() {
        assertEquals(listOf("a", "b", row, "c", "d"), session(rowIndex = 2).keys)
        assertEquals(listOf(row, "a", "b", "c", "d"), session(rowIndex = 0).keys)
        assertEquals(listOf("a", "b", "c", "d", row), session(rowIndex = 4).keys)
    }

    @Test
    fun `entering clamps a row index past the trips and has no row for null`() {
        assertEquals(listOf("a", "b", row), session(rowIndex = 9, "a", "b").keys)
        assertEquals(listOf(row, "a"), session(rowIndex = -3, "a").keys)
        assertEquals(listOf("a", "b"), session(rowIndex = null, "a", "b").keys)
        assertEquals(listOf(row), ReorderSession.start(emptyList(), rowIndex = 2).keys)
    }

    @Test
    fun `entering lifts the card that was pressed and ignores a card that is not there`() {
        assertEquals("b", ReorderSession.start(listOf("a", "b"), rowIndex = null, lifted = "b").lifted)
        assertEquals(row, ReorderSession.start(listOf("a", "b"), rowIndex = 1, lifted = row).lifted)
        assertNull(ReorderSession.start(listOf("a", "b"), rowIndex = null, lifted = "zzz").lifted)
    }

    @Test
    fun `a fresh session has no changes`() {
        assertFalse(session().hasChanges)
    }

    // endregion

    // region move

    @Test
    fun `a trip moves among the trips`() {
        val moved = session(rowIndex = null).move(fromKey = "a", toKey = "c")

        assertEquals(listOf("b", "c", "a", "d"), moved.keys)
        assertEquals(listOf("b", "c", "a", "d"), moved.tripOrder)
        assertTrue(moved.hasChanges)
    }

    @Test
    fun `the row moves as one block between trips`() {
        val dragged = session(rowIndex = 2).move(fromKey = row, toKey = "d")

        assertEquals(listOf("a", "b", "c", "d", row), dragged.keys)
        assertEquals(4, dragged.rowIndex)
        assertEquals(listOf("a", "b", "c", "d"), dragged.tripOrder, "Dragging the row never reorders the trips")
    }

    @Test
    fun `the row can be dragged to the top and back`() {
        val top = session(rowIndex = 2).move(fromKey = row, toKey = "a")
        assertEquals(0, top.rowIndex)
        assertEquals(2, top.move(fromKey = row, toKey = "b").rowIndex)
    }

    @Test
    fun `a trip dragged past the row changes the row's index and nothing else`() {
        val moved = session(rowIndex = 2).move(fromKey = "a", toKey = "c")

        assertEquals(listOf("b", row, "c", "a", "d"), moved.keys)
        assertEquals(1, moved.rowIndex)
    }

    @Test
    fun `moving to itself, or from or to a key that is not there, changes nothing`() {
        val start = session()

        assertSame(start, start.move("a", "a"))
        assertSame(start, start.move("zzz", "a"))
        assertSame(start, start.move("a", "zzz"))
    }

    @Test
    fun `moving clears the lift`() {
        val lifted = ReorderSession.start(listOf("a", "b"), rowIndex = null, lifted = "a")

        assertNull(lifted.move("a", "b").lifted)
    }

    @Test
    fun `moving a card back where it was leaves no change`() {
        val start = session()
        val moved = start.move("a", "c")
        assertTrue(moved.hasChanges, "Sanity: the first move is a change")

        val back = moved.move("a", "b")

        assertEquals(start.keys, back.keys)
        assertFalse(back.hasChanges, "Done with the order as it began has nothing to write")
    }

    // endregion

    // region remove

    @Test
    fun `removing a trip stages it and takes it out of the order`() {
        val removed = session().remove("b")

        assertEquals(listOf("a", row, "c", "d"), removed.keys)
        assertEquals(setOf("b"), removed.removed)
        assertEquals(1, removed.rowIndex, "The row now follows one trip")
        assertTrue(removed.hasChanges)
    }

    @Test
    fun `the row can never be removed and an unknown trip changes nothing`() {
        val start = session()

        assertSame(start, start.remove(row))
        assertSame(start, start.remove("zzz"))
    }

    @Test
    fun `removing the same trip twice stages it once`() {
        val twice = session().remove("a").remove("a")

        assertEquals(setOf("a"), twice.removed)
    }

    // endregion

    // region done and cancel

    @Test
    fun `done writes the order, the removed trips and where the row sits`() {
        val result = session(rowIndex = 2)
            .move("a", "d")
            .remove("b")
            .move(row, "c")
            .result()

        // Start [a, b, row, c, d]. Move a to the end, take b off, then move the row to where c is: [c, row, d, a].
        assertEquals(listOf("c", "d", "a"), result.tripOrder)
        assertEquals(setOf("b"), result.removed)
        assertEquals(1, result.rowIndex)
    }

    @Test
    fun `done with nothing moved writes the same order and the same place`() {
        val result = session(rowIndex = 2).result()

        assertEquals(listOf("a", "b", "c", "d"), result.tripOrder)
        assertEquals(2, result.rowIndex)
        assertTrue(result.removed.isEmpty())
    }

    @Test
    fun `no row means no row index to write`() {
        assertNull(session(rowIndex = null).result().rowIndex)
    }

    @Test
    fun `the session is a value, so dropping it is cancel and the saved order is untouched`() {
        val start = session()
        val moved = start.move("a", "d").remove("b")

        assertEquals(listOf("a", "b", row, "c", "d"), start.keys)
        assertEquals(start.startKeys, moved.startKeys, "The order at entry is kept so the screen can animate back")
    }

    // endregion
}
