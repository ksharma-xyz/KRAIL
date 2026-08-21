package xyz.ksharma.krail.trip.planner.ui.journeymap.business

import org.maplibre.compose.expressions.ast.Expression
import org.maplibre.compose.expressions.ast.FunctionCall
import org.maplibre.compose.expressions.ast.StringLiteral
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.StopType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * `JourneyMapFilters` builds the MapLibre filter expressions that decide which GeoJSON features
 * each journey-map layer draws. A wrong property key or a swapped `and`/`or` produces a layer that
 * silently renders nothing or renders everything — neither throws, and neither is visible in a
 * compile.
 *
 * The assertions walk the produced expression tree rather than comparing objects, because the
 * `FunctionCall` node carries a lambda and so does not compare structurally.
 */
class JourneyMapFiltersTest {

    @Test
    fun `GIVEN the leg filter WHEN built THEN it matches the journey_leg feature type`() {
        val filter = JourneyMapFilters.isJourneyLeg()

        assertEquals(listOf("type", "journey_leg"), filter.stringLiterals())
        assertEquals(listOf("==", "string", "get"), filter.functionNames())
    }

    @Test
    fun `GIVEN the stop filter WHEN built THEN it matches the journey_stop feature type`() {
        val filter = JourneyMapFilters.isJourneyStop()

        assertEquals(listOf("type", "journey_stop"), filter.stringLiterals())
        assertEquals(listOf("==", "string", "get"), filter.functionNames())
    }

    @Test
    fun `GIVEN a single stop type WHEN filtered THEN it must be a stop AND that type`() {
        val filter = JourneyMapFilters.isStopType(StopType.ORIGIN)

        assertEquals(listOf("type", "journey_stop", "stopType", "ORIGIN"), filter.stringLiterals())
        // "all" — a stop-type filter that dropped the journey_stop clause would match route
        // features too.
        assertEquals("all", filter.functionNames().first())
    }

    @Test
    fun `GIVEN several stop types WHEN filtered THEN the types are OR-ed inside the stop AND`() {
        val filter = JourneyMapFilters.isStopType(StopType.ORIGIN, StopType.DESTINATION)

        assertEquals(
            listOf("type", "journey_stop", "stopType", "ORIGIN", "stopType", "DESTINATION"),
            filter.stringLiterals(),
        )
        assertEquals(
            listOf("all", "==", "string", "get", "any", "==", "string", "get", "==", "string", "get"),
            filter.functionNames(),
        )
    }

    @Test
    fun `GIVEN no stop types WHEN filtered THEN it fails loudly instead of matching everything`() {
        val error = assertFailsWith<IllegalArgumentException> { JourneyMapFilters.isStopType() }

        assertEquals("At least one stop type must be provided", error.message)
    }

    private fun Expression<*>.stringLiterals(): List<String> = buildList {
        visit { node -> if (node is StringLiteral) add(node.value) }
    }

    private fun Expression<*>.functionNames(): List<String> = buildList {
        visit { node -> if (node is FunctionCall) add(node.name) }
    }
}
