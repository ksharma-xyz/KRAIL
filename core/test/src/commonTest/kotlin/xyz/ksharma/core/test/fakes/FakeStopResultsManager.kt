package xyz.ksharma.core.test.fakes

import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.trip.planner.ui.searchstop.StopResultsManager
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

class FakeStopResultsManager : StopResultsManager {
    // Add a flag to control whether fetchStopResults should throw an exception
    var shouldThrowError = false

    // Track selected stops
    private var _selectedFromStop: StopItem? = null
    private var _selectedToStop: StopItem? = null

    private val testStopResults = listOf(
        SearchStopState.StopResult(
            stopId = "10101",
            stopName = "Central Station",
            transportModeType = persistentListOf(TransportMode.Train(), TransportMode.Bus())
        ),
        SearchStopState.StopResult(
            stopId = "10102",
            stopName = "Town Hall",
            transportModeType = persistentListOf(TransportMode.Train())
        ),
        SearchStopState.StopResult(
            stopId = "10103",
            stopName = "Parramatta Station",
            transportModeType = persistentListOf(TransportMode.Train(), TransportMode.Bus())
        ),
        SearchStopState.StopResult(
            stopId = "10104",
            stopName = "Sydney Airport",
            transportModeType = persistentListOf(TransportMode.Train())
        )
    )

    override val selectedFromStop: StopItem?
        get() = _selectedFromStop

    override val selectedToStop: StopItem?
        get() = _selectedToStop

    override suspend fun fetchStopResults(query: String): List<SearchStopState.StopResult> {
        // Throw an exception if shouldThrowError is true
        if (shouldThrowError) {
            throw RuntimeException("Error fetching stop results")
        }

        return if (query.isBlank()) {
            testStopResults
        } else {
            testStopResults.filter {
                it.stopName.contains(query, ignoreCase = true)
            }
        }
    }

    override fun prioritiseStops(stopResults: List<SearchStopState.StopResult>): List<SearchStopState.StopResult> {
        return stopResults.sortedByDescending { it.transportModeType.size }
    }

    override fun fetchLocalStopName(stopId: String): String? {
        return testStopResults.firstOrNull { it.stopId == stopId }?.stopName
    }

    override fun setSelectedFromStop(stopItem: StopItem?) {
        _selectedFromStop = stopItem
    }

    override fun setSelectedToStop(stopItem: StopItem?) {
        _selectedToStop = stopItem
    }

    override fun reverseSelectedStops() {
        val tempFrom = _selectedFromStop
        _selectedFromStop = _selectedToStop
        _selectedToStop = tempFrom
    }

    override fun clearSelectedStops() {
        _selectedFromStop = null
        _selectedToStop = null
    }

    // Helper methods for testing
    fun reset() {
        _selectedFromStop = null
        _selectedToStop = null
        shouldThrowError = false
    }
}
