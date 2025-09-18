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

    // Track recent search stops for testing
    private val _recentSearchStops = mutableListOf<SearchStopState.StopResult>()

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
        // Add to recent search stops when a stop is selected (like the real implementation)
        if (stopItem != null) {
            addRecentSearchStopFromStopItem(stopItem)
        }
    }

    override fun setSelectedToStop(stopItem: StopItem?) {
        _selectedToStop = stopItem
        // Add to recent search stops when a stop is selected (like the real implementation)
        if (stopItem != null) {
            addRecentSearchStopFromStopItem(stopItem)
        }
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

    override suspend fun recentSearchStops(): List<SearchStopState.StopResult> {
        return _recentSearchStops.toList()
    }

    override fun clearRecentSearchStops() {
        _recentSearchStops.clear()
    }

    // Helper methods for testing
    fun reset() {
        _selectedFromStop = null
        _selectedToStop = null
        _recentSearchStops.clear()
        shouldThrowError = false
    }

    // Helper method to add recent stops for testing
    fun addRecentSearchStop(stopResult: SearchStopState.StopResult) {
        _recentSearchStops.removeAll { it.stopId == stopResult.stopId }
        _recentSearchStops.add(0, stopResult)
        if (_recentSearchStops.size > 5) {
            _recentSearchStops.removeAt(_recentSearchStops.size - 1)
        }
    }

    // Helper method to convert StopItem to StopResult and add to recent stops
    private fun addRecentSearchStopFromStopItem(stopItem: StopItem) {
        // Find the corresponding test stop result or create a basic one
        val stopResult = testStopResults.find { it.stopId == stopItem.stopId }
            ?: SearchStopState.StopResult(
                stopId = stopItem.stopId,
                stopName = stopItem.stopName,
                transportModeType = persistentListOf(TransportMode.Train()) // Default transport mode
            )

        addRecentSearchStop(stopResult)
    }
}
