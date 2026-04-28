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
}
