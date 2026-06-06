package xyz.ksharma.krail.trip.planner.network.api.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExclusionParamsTest {

    @Test
    fun `Given empty set Then returns empty map`() {
        val params = buildExclusionParams(emptySet())
        assertTrue(params.isEmpty())
    }

    @Test
    fun `Given empty set Then excludedMeans is not included`() {
        val params = buildExclusionParams(emptySet())
        assertFalse(params.containsKey("excludedMeans"))
    }

    @Test
    fun `Given non-empty set Then includes excludedMeans=checkbox`() {
        val params = buildExclusionParams(setOf(5))
        assertEquals("checkbox", params["excludedMeans"])
    }

    @Test
    fun `Given bus excluded Then only exclMOT_5 set`() {
        val params = buildExclusionParams(setOf(5))
        assertEquals("1", params["exclMOT_5"])
        assertFalse(params.containsKey("exclMOT_11"), "Excluding bus must not auto-exclude school bus")
    }

    @Test
    fun `Given school bus excluded Then only exclMOT_11 set`() {
        val params = buildExclusionParams(setOf(11))
        assertEquals("1", params["exclMOT_11"])
        assertFalse(params.containsKey("exclMOT_5"), "Excluding school bus must not auto-exclude regular bus")
    }

    @Test
    fun `Given both bus and school bus excluded Then both exclMOT_5 and exclMOT_11 set`() {
        val params = buildExclusionParams(setOf(5, 11))
        assertEquals("1", params["exclMOT_5"])
        assertEquals("1", params["exclMOT_11"])
    }

    @Test
    fun `Given train excluded Then exclMOT_1 set to 1`() {
        val params = buildExclusionParams(setOf(1))
        assertEquals("1", params["exclMOT_1"])
    }

    @Test
    fun `Given metro excluded Then exclMOT_2 set to 1`() {
        val params = buildExclusionParams(setOf(2))
        assertEquals("1", params["exclMOT_2"])
    }

    @Test
    fun `Given light rail excluded Then exclMOT_4 set to 1`() {
        val params = buildExclusionParams(setOf(4))
        assertEquals("1", params["exclMOT_4"])
    }

    @Test
    fun `Given coach excluded Then exclMOT_7 set to 1`() {
        val params = buildExclusionParams(setOf(7))
        assertEquals("1", params["exclMOT_7"])
    }

    @Test
    fun `Given ferry excluded Then exclMOT_9 set to 1`() {
        val params = buildExclusionParams(setOf(9))
        assertEquals("1", params["exclMOT_9"])
    }

    @Test
    fun `Given multiple modes excluded Then all corresponding params present`() {
        val params = buildExclusionParams(setOf(1, 5, 9))
        assertEquals("checkbox", params["excludedMeans"])
        assertEquals("1", params["exclMOT_1"])
        assertEquals("1", params["exclMOT_5"])
        assertFalse(params.containsKey("exclMOT_11"), "School bus not in exclusion set — must not be excluded")
        assertEquals("1", params["exclMOT_9"])
        assertFalse(params.containsKey("exclMOT_2"))
        assertFalse(params.containsKey("exclMOT_4"))
        assertFalse(params.containsKey("exclMOT_7"))
    }

    @Test
    fun `Given bus excluded Then param value is 1 not mode number`() {
        val params = buildExclusionParams(setOf(5))
        assertEquals("1", params["exclMOT_5"], "Value must be '1' not '5' — NSW API treats it as a boolean flag")
    }

    @Test
    fun `Given train excluded Then param name uses underscore`() {
        val params = buildExclusionParams(setOf(1))
        assertTrue(params.containsKey("exclMOT_1"), "Param must be 'exclMOT_1' with underscore, not 'exclMOT1'")
        assertFalse(params.containsKey("exclMOT1"), "Param 'exclMOT1' without underscore must not be present")
    }
}
