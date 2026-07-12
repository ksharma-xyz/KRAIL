package xyz.ksharma.krail.trip.planner.ui.managestoplabels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ResolveRenameSaveActionTest {

    @Test
    fun `blank typed name collapses only`() {
        val action = resolveRenameSaveAction(
            typedName = "   ",
            currentName = "Work",
            existingLabelNames = listOf("Home", "Work"),
        )
        assertIs<RenameSaveAction.CollapseOnly>(action)
    }

    @Test
    fun `unchanged name collapses only`() {
        val action = resolveRenameSaveAction(
            typedName = "Work",
            currentName = "Work",
            existingLabelNames = listOf("Home", "Work"),
        )
        assertIs<RenameSaveAction.CollapseOnly>(action)
    }

    @Test
    fun `name matching another existing label is a duplicate`() {
        val action = resolveRenameSaveAction(
            typedName = "home",
            currentName = "Work",
            existingLabelNames = listOf("Home", "Work"),
        )
        val duplicate = assertIs<RenameSaveAction.Duplicate>(action)
        assertEquals("home", duplicate.cleanedName)
    }

    @Test
    fun `case-only change against the row's own current name is not a duplicate`() {
        // "work" vs current "Work" fails the case-sensitive unchanged-check, so it's
        // a real edit — and it must not be blocked as colliding with itself.
        val action = resolveRenameSaveAction(
            typedName = "  work  ",
            currentName = "Work",
            existingLabelNames = listOf("Home", "Work"),
        )
        val save = assertIs<RenameSaveAction.Save>(action)
        assertEquals("work", save.trimmedName)
    }

    @Test
    fun `genuinely new name resolves to Save with the trimmed value`() {
        val action = resolveRenameSaveAction(
            typedName = "  Gym  ",
            currentName = "Work",
            existingLabelNames = listOf("Home", "Work"),
        )
        val save = assertIs<RenameSaveAction.Save>(action)
        assertEquals("Gym", save.trimmedName)
    }
}
