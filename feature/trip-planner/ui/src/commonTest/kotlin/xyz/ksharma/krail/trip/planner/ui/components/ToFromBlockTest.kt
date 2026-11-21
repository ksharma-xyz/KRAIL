package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The two rules of the To and From block that are plain functions, `docs/MAP_HOME_UX.md` rules 34
 * and 36. Where the pieces land is read off the real layout in `ToFromBlockLayoutTest`.
 */
class ToFromBlockTest {

    @Test
    fun `the dark fill is on the field being edited`() {
        assertTrue(ToFromField.To.hasDarkFill(editing = ToFromField.To))
        assertTrue(ToFromField.From.hasDarkFill(editing = ToFromField.From))
    }

    @Test
    fun `the dark fill is never on the other field`() {
        assertFalse(ToFromField.From.hasDarkFill(editing = ToFromField.To))
        assertFalse(ToFromField.To.hasDarkFill(editing = ToFromField.From))
    }

    @Test
    fun `neither field has the dark fill while nothing is edited`() {
        assertFalse(ToFromField.To.hasDarkFill(editing = null))
        assertFalse(ToFromField.From.hasDarkFill(editing = null))
    }

    @Test
    fun `the go circle is 40 dp in a 48 dp column with 4 dp around it`() {
        assertEquals(40.dp, goCircleDiameter(column = 48.dp, margin = 4.dp))
    }

    @Test
    fun `the go circle gives up the margin on both sides`() {
        assertEquals(48.dp, goCircleDiameter(column = 48.dp, margin = 0.dp))
        assertEquals(32.dp, goCircleDiameter(column = 48.dp, margin = 8.dp))
    }

    @Test
    fun `a field with a stop says which end it is and the stop in full`() {
        assertEquals(
            "To, Town Hall Station",
            toFromFieldLabel(role = "To", placeholder = "Destination", stopName = "Town Hall Station"),
        )
        assertEquals(
            "From, Central Station",
            toFromFieldLabel(role = "From", placeholder = "Starting from", stopName = "Central Station"),
        )
    }

    @Test
    fun `the label never shortens a long stop name`() {
        val long = "Sydney Olympic Park Station Platform 1 and the long way round"

        assertEquals(
            "To, $long",
            toFromFieldLabel(role = "To", placeholder = "Destination", stopName = long),
        )
    }

    @Test
    fun `an empty field says its own placeholder`() {
        assertEquals("Destination", toFromFieldLabel("To", "Destination", stopName = null))
        assertEquals("Starting from", toFromFieldLabel("From", "Starting from", stopName = ""))
        assertEquals("Starting from", toFromFieldLabel("From", "Starting from", stopName = "   "))
    }
}
