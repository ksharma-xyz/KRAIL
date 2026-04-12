package xyz.ksharma.krail.trip.planner.ui.searchstop

import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.test.runTest
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.core.transport.nsw.NswTransportMode
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import kotlin.test.Test
import kotlin.test.assertEquals

/* TODO -  add test when this functionality is added in future
class StopFilterByProductClassTest {

    @Test
    fun `should return stops excluding given product classes`() = runTest {
        // Given
        val testCases = listOf(
            TestCase(
                excludedClasses = listOf(1),
                expectedStopIds = listOf("10101101", "10101105", "12349", "12356")
            ),
            TestCase(
                excludedClasses = listOf(1, 2),
                expectedStopIds = listOf("12349", "12356")
            ),
            TestCase(
                excludedClasses = listOf(5),
                expectedStopIds = listOf("10101101", "10101100", "10101105", "12349")
            ),
            // All product classes are excluded
            TestCase(
                excludedClasses = listOf(1, 2, 5),
                expectedStopIds = listOf()
            )
        )

        testCases.forEach { testCase ->
            // When
            val actualResults = filterProductClasses(
                stopResults = stopResults,
                excludedProductClasses = testCase.excludedClasses
            )

            val actualStopIds = actualResults.map { it.stopId }

            // Then
            assertEquals(testCase.expectedStopIds.sorted(), actualStopIds.sorted())
        }
    }

    private data class TestCase(
        val excludedClasses: List<Int>,
        val expectedStopIds: List<String>,
    )

    companion object {
        private val stopResults = listOf(
            SearchStopState.StopResult(
                stopName = "Town Hall Station",
                stopId = "10101101",
                transportModeType = listOf(
                    NswTransportMode.Train,
                    NswTransportMode.Metro
                ).toImmutableList(),
            ),
            SearchStopState.StopResult(
                stopName = "Wynyard Station",
                stopId = "10101100",
                transportModeType = listOf(NswTransportMode.Train).toImmutableList(),
            ),
            SearchStopState.StopResult(
                stopName = "Metro Only Station",
                stopId = "10101105",
                transportModeType = listOf(NswTransportMode.Metro).toImmutableList(),
            ),
            SearchStopState.StopResult(
                stopName = "Schofields Station",
                stopId = "12349",
                transportModeType = listOf(
                    NswTransportMode.Bus,
                    NswTransportMode.Train
                ).toImmutableList(),
            ),
            SearchStopState.StopResult(
                stopName = "103 ABC Rd, Hallway",
                stopId = "12356",
                transportModeType = listOf(NswTransportMode.Bus).toImmutableList(),
            ),
        )
    }
}
*/
