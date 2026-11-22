package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.departures.ui.state.DeparturesState
import xyz.ksharma.krail.departures.ui.state.model.StopDeparture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What a stop card's departure board shows in each state. Each rule is driven on its own, because a
 * single real board cannot reach a failed first load or a refresh in flight.
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
