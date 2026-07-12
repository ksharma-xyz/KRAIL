package xyz.ksharma.krail.trip.planner.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class StopLabelIconsTest {

    @Test
    fun `stopLabelIcon matches known suggestion names case-insensitively`() {
        assertNotNull(stopLabelIcon("Home"))
        assertNotNull(stopLabelIcon("HOME"))
        assertNotNull(stopLabelIcon("home"))
    }

    @Test
    fun `stopLabelIcon trims whitespace before matching`() {
        assertEquals(stopLabelIcon("Home"), stopLabelIcon("  Home  "))
    }

    @Test
    fun `stopLabelIcon returns null for unknown names`() {
        assertNull(stopLabelIcon("Grandma's House"))
    }

    @Test
    fun `stopLabelIcon resolves Uni and University to the same icon`() {
        assertEquals(stopLabelIcon("Uni"), stopLabelIcon("University"))
        assertEquals(stopLabelIcon("uni"), stopLabelIcon("UNIVERSITY"))
    }

    @Test
    fun `stopLabelIcon resolves School to an icon`() {
        assertNotNull(stopLabelIcon("School"))
    }

    @Test
    fun `stopLabelColor returns the same preset colour regardless of case`() {
        assertEquals(stopLabelColor("Home"), stopLabelColor("HOME"))
        assertEquals(stopLabelColor("Home"), stopLabelColor("home"))
    }

    @Test
    fun `stopLabelColor gives distinct presets to Home Work and Beach`() {
        val home = stopLabelColor("Home")
        val work = stopLabelColor("Work")
        val beach = stopLabelColor("Beach")

        assertEquals(setOf(home, work, beach).size, 3)
    }

    @Test
    fun `stopLabelColor is stable for unknown names across calls`() {
        assertEquals(stopLabelColor("Grandma's House"), stopLabelColor("Grandma's House"))
    }

    @Test
    fun `stopLabelColor treats Uni and University the same as the School preset family`() {
        assertEquals(stopLabelColor("Uni"), stopLabelColor("University"))
    }
}
