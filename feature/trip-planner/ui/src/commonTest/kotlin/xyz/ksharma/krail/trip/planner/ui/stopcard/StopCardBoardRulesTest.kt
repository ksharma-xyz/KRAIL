package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.departures.ui.state.model.StopDeparture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What a stop card's departure board shows in each state, and the Lines chip it earns. Each rule is
 * driven on its own, because a single real board cannot reach a stop with one line, a failed first
 * load or a refresh in flight.
 */
class StopCardBoardRulesTest {

    private fun row(line: String, mode: String = "Train", color: String = "#F99D1C") = StopDeparture(
        lineNumber = line,
        lineColorCode = color,
        transportModeName = mode,
        destinationName = "Anywhere",
        departureTimeText = "11:30 AM",
        departureUtcDateTime = "2026-11-22T01:30:00Z",
    )

    private fun content(vararg lines: String, silent: Boolean = false) = DeparturesState(
        isLoading = false,
        silentLoading = silent,
        departures = lines.map { row(it) }.toImmutableList(),
    )

    // region the phase

    private class PhaseCase(
        val name: String,
        val state: DeparturesState,
        val expanded: Boolean,
        val phase: BoardPhase,
    )

    @Test
    fun `the board is in the phase its state and the chevron say`() {
        val cases = listOf(
            PhaseCase("closed means nothing was asked", DeparturesState(), false, BoardPhase.Collapsed),
            PhaseCase("closed with rows still collapsed", content("T1"), false, BoardPhase.Collapsed),
            PhaseCase("open and first load", DeparturesState(isLoading = true), true, BoardPhase.Loading),
            PhaseCase(
                "open and failed with nothing",
                DeparturesState(isLoading = false, isError = true),
                true,
                BoardPhase.Failed,
            ),
            PhaseCase("open and nothing running", DeparturesState(isLoading = false), true, BoardPhase.Empty),
            PhaseCase("open with rows", content("T1"), true, BoardPhase.Content),
            PhaseCase(
                "loading wins over a stale error",
                DeparturesState(isLoading = true, isError = true),
                true,
                BoardPhase.Loading,
            ),
            PhaseCase(
                "a failed refresh that kept its rows is still content",
                content("T1", "T2").copy(isError = false),
                true,
                BoardPhase.Content,
            ),
        )
        cases.forEach { case ->
            assertEquals(case.phase, stopCardBoard(case.state, case.expanded).phase, case.name)
        }
    }

    @Test
    fun `an open board holds its height floor in every phase and a closed one holds none`() {
        listOf(
            DeparturesState(isLoading = true),
            DeparturesState(isLoading = false, isError = true),
            DeparturesState(isLoading = false),
            content("T1"),
        ).forEach { state ->
            assertTrue(stopCardBoard(state, expanded = true).holdsBodyFloor, "open $state")
            assertFalse(stopCardBoard(state, expanded = false).holdsBodyFloor, "closed $state")
        }
    }

    @Test
    fun `a refresh in flight leaves the rows and says so`() {
        val refreshing = stopCardBoard(content("T1", "T2", silent = true), expanded = true)
        assertEquals(BoardPhase.Content, refreshing.phase)
        assertTrue(refreshing.refreshing)
        assertFalse(stopCardBoard(content("T1", "T2"), expanded = true).refreshing)
        assertFalse(stopCardBoard(DeparturesState(isLoading = true, silentLoading = true), expanded = true).refreshing)
    }

    // endregion

    // region the Lines chip

    private class ChipCase(val name: String, val state: DeparturesState, val chip: Boolean, val reserved: Boolean)

    @Test
    fun `the lines chip shows only for a stop with more than one line and keeps its room while loading`() {
        val cases = listOf(
            ChipCase("one line leaving once", content("T1"), chip = false, reserved = false),
            ChipCase(
                "one line leaving three times is still one",
                content("T1", "T1", "T1"),
                chip = false,
                reserved = false,
            ),
            ChipCase("two lines", content("T1", "T2"), chip = true, reserved = false),
            ChipCase("many lines", content("T1", "T2", "T4", "333", "M50"), chip = true, reserved = false),
            ChipCase(
                "loading holds the chips room and shows none",
                DeparturesState(isLoading = true),
                chip = false,
                reserved = true,
            ),
            ChipCase(
                "failed has no chip and no reserved room",
                DeparturesState(isLoading = false, isError = true),
                false,
                false,
            ),
            ChipCase("empty has no chip and no reserved room", DeparturesState(isLoading = false), false, false),
        )
        cases.forEach { case ->
            val board = stopCardBoard(case.state, expanded = true)
            assertEquals(case.chip, board.showsLinesChip, "chip, ${case.name}")
            assertEquals(case.reserved, board.reservesLinesChip, "reserved, ${case.name}")
        }
    }

    @Test
    fun `a closed board shows no chip even when rows are held`() {
        val board = stopCardBoard(content("T1", "T2"), expanded = false)
        assertFalse(board.showsLinesChip)
        assertFalse(board.reservesLinesChip)
        assertEquals(0, board.lineCount)
    }

    @Test
    fun `lines are counted by line and not by departure`() {
        assertEquals(0, distinctLineCount(emptyList()))
        assertEquals(1, distinctLineCount(listOf(row("T1"), row("T1"))))
        assertEquals(3, distinctLineCount(listOf(row("T1"), row("T2"), row("T1"), row("333", "Bus"))))
    }

    @Test
    fun `the chip appears at two lines and not at one`() {
        assertFalse(showsLinesChip(0))
        assertFalse(showsLinesChip(1))
        assertTrue(showsLinesChip(2))
        assertTrue(showsLinesChip(40))
    }

    // endregion

    // region the badges

    @Test
    fun `badges list each line once in the apps mode order and keep the board's order inside a mode`() {
        val departures = listOf(
            row("333", "Bus", "#00B5EF"),
            row("T2", "Train", "#0098CD"),
            row("309", "Bus", "#00B5EF"),
            row("T1", "Train", "#F99D1C"),
            row("T2", "Train", "#0098CD"),
            row("L1", "Light Rail", "#E4022D"),
            row("X1", "Mystery", "#000000"),
        )
        assertEquals(
            listOf("T2", "T1", "333", "309", "L1", "X1"),
            lineBadges(departures).map { it.lineNumber },
        )
    }

    @Test
    fun `a badge carries its colour and mode`() {
        val badge = lineBadges(listOf(row("T4", "Train", "#005AA3"))).single()
        assertEquals(LineBadge("T4", "#005AA3", "Train"), badge)
    }

    @Test
    fun `no departures give no badges`() {
        assertEquals(persistentListOf(), lineBadges(emptyList()))
    }

    @Test
    fun `tapping a badge selects it and tapping the selected one clears it`() {
        assertEquals("T1", lineFilterAfterTap(selected = null, tapped = "T1"))
        assertEquals("T2", lineFilterAfterTap(selected = "T1", tapped = "T2"))
        assertNull(lineFilterAfterTap(selected = "T1", tapped = "T1"))
    }

    // endregion

    // region the floor

    @Test
    fun `the board floor is the failed message's height at normal text and grows with the text`() {
        assertEquals(296.dp, boardFloorHeight(1f))
        assertEquals(592.dp, boardFloorHeight(2f))
        assertEquals(296.dp * 1.3f, boardFloorHeight(1.3f))
    }

    @Test
    fun `text smaller than normal does not shrink the floor`() {
        assertEquals(296.dp, boardFloorHeight(0.85f))
        assertEquals(296.dp, boardFloorHeight(0f))
    }

    // endregion
}
