package xyz.ksharma.krail.trip.planner.ui.search.ai.resolve

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LabelSynonymsTest {

    @Test
    fun `office means work`() {
        // The reported case: a rider whose label is Work said "office by Monday morning" and
        // the resolver returned nothing, because "office" is not the string "work".
        assertTrue(LabelSynonyms.sameMeaning("Work", "office"))
        assertTrue(LabelSynonyms.sameMeaning("office", "Work"))
    }

    @Test
    fun `the whole work group is interchangeable`() {
        listOf("work", "office", "job", "workplace").forEach { first ->
            listOf("work", "office", "job", "workplace").forEach { second ->
                assertTrue(
                    LabelSynonyms.sameMeaning(first, second),
                    "\"$first\" and \"$second\" should mean the same place",
                )
            }
        }
    }

    @Test
    fun `groups do not leak into each other`() {
        assertFalse(LabelSynonyms.sameMeaning("home", "work"))
        assertFalse(LabelSynonyms.sameMeaning("uni", "school"))
        assertFalse(LabelSynonyms.sameMeaning("gym", "office"))
    }

    @Test
    fun `matching stays whole-word, so Homebush is still safe`() {
        // The reason label matching is exact in the first place. Widening the vocabulary must
        // not widen it into substrings.
        assertFalse(LabelSynonyms.sameMeaning("home", "homebush"))
        assertFalse(LabelSynonyms.sameMeaning("work", "workshop"))
        assertFalse(LabelSynonyms.sameMeaning("uni", "union square"))
    }

    @Test
    fun `a label the rider invented compares equal only to itself`() {
        // Nothing here should touch labels outside the groups, however the groups grow.
        assertTrue(LabelSynonyms.sameMeaning("Bouldering", "bouldering"))
        assertFalse(LabelSynonyms.sameMeaning("Bouldering", "gym"))
        assertFalse(LabelSynonyms.sameMeaning("Nan's", "home"))
    }

    @Test
    fun `case and surrounding space do not matter`() {
        assertTrue(LabelSynonyms.sameMeaning("  OFFICE ", "work"))
        assertEquals("work", LabelSynonyms.canonical(" Office "))
    }

    @Test
    fun `commute labels cover every accepted wording of the scheduled places`() {
        // Used by the weekend rule. An Office label has to count as the commute exactly as a
        // Work label does, or a rider gets their commute suggested on a Sunday.
        listOf("work", "office", "job", "workplace", "uni", "university", "campus", "college", "school")
            .forEach { label ->
                assertTrue(label in LabelSynonyms.commuteLabels, "\"$label\" should count as a commute")
            }
    }

    @Test
    fun `home and gym are never commute labels`() {
        listOf("home", "house", "gym").forEach { label ->
            assertFalse(label in LabelSynonyms.commuteLabels, "\"$label\" is not a commute")
        }
    }
}
