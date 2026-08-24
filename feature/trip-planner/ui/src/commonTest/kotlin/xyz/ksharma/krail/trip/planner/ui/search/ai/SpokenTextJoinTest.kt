package xyz.ksharma.krail.trip.planner.ui.search.ai

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The join between what the rider typed and what they then said. Every case here is something
 * a rider can see in the field, so the edges matter more than they look: a stray leading space
 * or two words run together is the whole visible result of speaking.
 */
class SpokenTextJoinTest {

    @Test
    fun `an empty field gives the transcript untouched`() {
        // The ordinary case, and the one a naive join gets wrong with a leading space.
        assertEquals("central to town hall", joinSpokenText("", "central to town hall"))
    }

    @Test
    fun `a whitespace-only field is treated as empty`() {
        assertEquals("central station", joinSpokenText("   ", "central station"))
    }

    @Test
    fun `typed words keep a single space before the spoken ones`() {
        assertEquals("meet me at central", joinSpokenText("meet me at", "central"))
    }

    @Test
    fun `a field already ending in a space does not gain a second one`() {
        assertEquals("meet me at central", joinSpokenText("meet me at ", "central"))
    }

    @Test
    fun `a transcript that starts with a space does not gain one either`() {
        assertEquals("meet me at central", joinSpokenText("meet me at", " central"))
    }

    @Test
    fun `an empty transcript leaves the field exactly as it was`() {
        // Nothing heard must not append a trailing space to what the rider typed.
        assertEquals("meet me at", joinSpokenText("meet me at", ""))
    }
}
