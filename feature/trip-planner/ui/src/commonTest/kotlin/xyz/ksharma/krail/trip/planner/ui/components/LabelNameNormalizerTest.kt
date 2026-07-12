package xyz.ksharma.krail.trip.planner.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LabelNameNormalizerTest {

    @Test
    fun `normaliseLabelName trims whitespace`() {
        assertEquals("Home", normaliseLabelName("  Home  "))
    }

    @Test
    fun `normaliseLabelName collapses internal whitespace`() {
        assertEquals("My Place", normaliseLabelName("My   Place"))
    }

    @Test
    fun `normaliseLabelName strips leading and trailing emoji`() {
        assertEquals("Home", normaliseLabelName("🏠 Home"))
        assertEquals("Home", normaliseLabelName("Home 🏠"))
        assertEquals("Home", normaliseLabelName("🏠 Home 🏠"))
    }

    @Test
    fun `normaliseLabelName drops emoji embedded in the middle of a name`() {
        assertEquals("My Home", normaliseLabelName("My 🏠 Home"))
    }

    @Test
    fun `normaliseLabelName preserves apostrophes hyphens and underscores`() {
        assertEquals("Mum's place", normaliseLabelName("Mum's place"))
        assertEquals("Co-Working", normaliseLabelName("Co-Working"))
        assertEquals("My_Spot", normaliseLabelName("My_Spot"))
    }

    @Test
    fun `normaliseLabelName drops other punctuation`() {
        assertEquals("Home", normaliseLabelName("Home!!!"))
        // Multi-token keeps the words, just drops the commas in between.
        assertEquals("Home Sweet Home", normaliseLabelName("Home, Sweet, Home"))
    }

    @Test
    fun `normaliseLabelName returns blank for input that's only emoji or punctuation`() {
        assertEquals("", normaliseLabelName("🏠"))
        assertEquals("", normaliseLabelName("!!!"))
        assertEquals("", normaliseLabelName("   "))
    }

    @Test
    fun `normaliseLabelName caps length at LABEL_NAME_MAX_LENGTH`() {
        val tooLong = "A".repeat(LABEL_NAME_MAX_LENGTH + 10)
        val result = normaliseLabelName(tooLong)
        assertEquals(LABEL_NAME_MAX_LENGTH, result.length)
        assertEquals("A".repeat(LABEL_NAME_MAX_LENGTH), result)
    }

    @Test
    fun `normaliseLabelName leaves no trailing space when truncation lands on a word boundary`() {
        // 21 'A's + space + 'B' — truncating at 20 chars alone would land exactly on
        // the space, leaving a trailing space; the result must be re-trimmed.
        val input = "A".repeat(LABEL_NAME_MAX_LENGTH - 1) + " B"
        val result = normaliseLabelName(input)
        assertEquals("A".repeat(LABEL_NAME_MAX_LENGTH - 1), result)
        assertFalse(result.endsWith(" "))
    }

    @Test
    fun `normaliseLabelName enforces length cap even without going through the TextField`() {
        // This is the actual defense-in-depth case: a direct call (e.g. from a
        // ViewModel handler) with no TextField involved must still be capped.
        val direct = normaliseLabelName("This name is way way way too long for a pill")
        assertTrue(direct.length <= LABEL_NAME_MAX_LENGTH)
    }

    @Test
    fun `labelNamesMatch is case-insensitive`() {
        assertTrue(labelNamesMatch("Home", "home"))
        assertTrue(labelNamesMatch("HOME", "home"))
    }

    @Test
    fun `labelNamesMatch ignores leading-trailing whitespace and emoji`() {
        assertTrue(labelNamesMatch("🏠 Home", "Home"))
        assertTrue(labelNamesMatch("Home", "  Home  "))
        assertTrue(labelNamesMatch("Home", "🏠 home 🏠"))
    }

    @Test
    fun `labelNamesMatch returns false for genuinely different names`() {
        assertFalse(labelNamesMatch("Home", "Work"))
        assertFalse(labelNamesMatch("Home", "Homely"))
    }

    // region filterLabelNameInput

    @Test
    fun `filterLabelNameInput keeps letters and digits`() {
        assertEquals("Gym2", filterLabelNameInput("Gym2").toString())
    }

    @Test
    fun `filterLabelNameInput keeps apostrophes hyphens and underscores`() {
        assertEquals("Mum's place", filterLabelNameInput("Mum's place").toString())
        assertEquals("Co-Working", filterLabelNameInput("Co-Working").toString())
        assertEquals("My_Spot", filterLabelNameInput("My_Spot").toString())
    }

    @Test
    fun `filterLabelNameInput drops emoji and other punctuation`() {
        assertEquals("Home", filterLabelNameInput("🏠Home!!!").toString())
        assertEquals("Home Sweet Home", filterLabelNameInput("Home, Sweet, Home").toString())
    }

    @Test
    fun `filterLabelNameInput does not trim or collapse whitespace, unlike normaliseLabelName`() {
        // This runs per-keystroke on the raw TextField value — collapsing whitespace
        // here would fight the user's cursor mid-type (e.g. deleting a just-typed
        // trailing space before they've finished the next word). Whitespace collapse
        // only happens once, at save time, via normaliseLabelName.
        assertEquals("  Home  ", filterLabelNameInput("  Home  ").toString())
        assertEquals("My   Place", filterLabelNameInput("My   Place").toString())
    }

    @Test
    fun `filterLabelNameInput returns empty for input that's only emoji or punctuation`() {
        assertEquals("", filterLabelNameInput("🏠").toString())
        assertEquals("", filterLabelNameInput("!!!").toString())
    }

    // endregion
}
