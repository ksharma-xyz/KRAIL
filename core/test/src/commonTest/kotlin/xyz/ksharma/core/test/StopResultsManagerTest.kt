package xyz.ksharma.core.test

import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.core.test.fakes.FakeFlag
import xyz.ksharma.core.test.fakes.FakeSandook
import xyz.ksharma.krail.trip.planner.ui.searchstop.RealStopResultsManager
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState

// TODO Write UTs separately
class RealStopResultsManagerTest {
    private lateinit var sandook: FakeSandook
    private lateinit var flag: FakeFlag
    private lateinit var stopResultsManager: RealStopResultsManager

    fun setUp() {
        sandook = FakeSandook()
        flag = FakeFlag()
        stopResultsManager = RealStopResultsManager(sandook, flag)
    }
    /*

        @Test
        fun `fetchStopResults should fetch from local stops when enabled`() = runTest {
            setUp()

            // Set flag values
            flag.setFlagValue(FlagKeys.LOCAL_STOPS_ENABLED.key, FlagValue.BooleanValue(true))
            flag.setFlagValue(
                FlagKeys.HIGH_PRIORITY_STOP_IDS.key,
                FlagValue.JsonValue("[\"200060\", \"200070\"]")
            )

            // Set Sandook response
            sandook.insertNswStop(
                stopId = "200060",
                stopName = "Stop A",
                stopLat = 1.0,
                stopLon = 1.0,
                isParent = true,
            )

            // Call the method
            val results = stopResultsManager.fetchStopResults("query")

            // Verify the results
            assertEquals(1, results.size)
            assertEquals("200060", results[0].stopId)
            assertEquals("Stop A", results[0].stopName)
        }


        @Test
        fun `prioritiseStops should prioritize stops correctly`() {
            setUp()

            // Set flag values
            flag.setFlagValue(
                FlagKeys.HIGH_PRIORITY_STOP_IDS.key,
                FlagValue.JsonValue("[\"200060\", \"200070\"]")
            )

            // Create test data
            val stopResults = listOf(
                createStopResult("200060", "Stop A", listOf(TransportMode.Train())),
                createStopResult("200080", "Stop B", listOf(TransportMode.Bus())),
                createStopResult("200070", "Stop C", listOf(TransportMode.Ferry())),
                createStopResult("200090", "Stop D", listOf(TransportMode.Train(), TransportMode.Bus()))
            )

            val expectedResults = listOf(
                createStopResult("200060", "Stop A", listOf(TransportMode.Train())),
                createStopResult("200070", "Stop C", listOf(TransportMode.Ferry())),
                createStopResult(
                    "200090",
                    "Stop D",
                    listOf(TransportMode.Train(), TransportMode.Bus())
                ),
                createStopResult("200080", "Stop B", listOf(TransportMode.Bus()))
            )

            // Call the method
            val actualResults = stopResultsManager.prioritiseStops(stopResults)

            // Verify the results
            assertEquals(expectedResults, actualResults)
        }
    */

    private fun createStopResult(
        stopId: String,
        stopName: String,
        transportModes: List<TransportMode>,
    ): SearchStopState.StopResult {
        return SearchStopState.StopResult(
            stopId = stopId,
            stopName = stopName,
            transportModeType = transportModes.toImmutableList()
        )
    }
}
