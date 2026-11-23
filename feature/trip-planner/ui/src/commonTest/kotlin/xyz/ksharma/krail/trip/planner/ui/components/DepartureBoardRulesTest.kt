package xyz.ksharma.krail.trip.planner.ui.components

import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.departures.ui.state.model.StopDeparture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** The departure board's decisions that need no layout. */
class DepartureBoardRulesTest {

    private val departures = persistentListOf(row("T1"), row("T2"), row("T1"), row("T4"))

    private fun row(line: String) = StopDeparture(
        lineNumber = line,
        lineColorCode = "#F99D1C",
        transportModeName = "Train",
        destinationName = "Anywhere",
        departureTimeText = "11:30 AM",
        departureUtcDateTime = "2026-11-22T01:30:00Z",
    )

    @Test
    fun `no chosen line keeps every departure, as the same list`() {
        assertSame(departures, departuresOnLine(departures, null))
    }

    @Test
    fun `a chosen line keeps only its departures, in order`() {
        val onT1 = departuresOnLine(departures, "T1")
        assertEquals(listOf(departures[0], departures[2]), onT1)
    }

    @Test
    fun `a line with no departures leaves none`() {
        assertTrue(departuresOnLine(departures, "T9").isEmpty())
        assertTrue(departuresOnLine(persistentListOf(), "T1").isEmpty())
    }

    @Test
    fun `previous departures are asked for only when the panel opens with none and no fetch running`() {
        assertTrue(previousDeparturesWanted(showPrevious = true, hasPrevious = false, previousLoading = false))
        assertFalse(previousDeparturesWanted(showPrevious = false, hasPrevious = false, previousLoading = false))
        assertFalse(previousDeparturesWanted(showPrevious = true, hasPrevious = true, previousLoading = false))
        assertFalse(previousDeparturesWanted(showPrevious = true, hasPrevious = false, previousLoading = true))
    }
}
