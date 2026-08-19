package xyz.ksharma.krail.trip.planner.ui.testfakes

import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.core.transport.nsw.NswTransportMode
import xyz.ksharma.krail.trip.planner.ui.searchstop.StopResultsManager
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

internal class FakeStopResultsManager : StopResultsManager {
    // Add a flag to control whether fetchStopResults should throw an exception
    var shouldThrowError = false

    /** Matches "townhall" against "Town Hall" the way the real fuzzy search does. */
    var spaceInsensitiveSearch = false

    // Track selected stops
    private var _selectedFromStop: StopItem? = null
    private var _selectedToStop: StopItem? = null

    // Track recent search stops for testing
    private val _recentSearchStops = mutableListOf<SearchStopState.StopResult>()

    private val testStopResults = listOf(
        SearchStopState.SearchResult.Stop(
            stopId = "10101",
            stopName = "Central Station",
            transportModeType = persistentListOf(NswTransportMode.Train, NswTransportMode.Bus),
        ),
        SearchStopState.SearchResult.Stop(
            stopId = "10102",
            stopName = "Town Hall",
            transportModeType = persistentListOf(NswTransportMode.Train),
        ),
        // A real stop, kept because it is the one that made "work" resolve to a bus stop on
        // the northern beaches: "work" appears inside "Powderworks".
        SearchStopState.SearchResult.Stop(
            stopId = "10104",
            stopName = "70 Powderworks Rd",
            transportModeType = persistentListOf(NswTransportMode.Bus),
        ),
        SearchStopState.SearchResult.Stop(
            stopId = "10103",
            stopName = "Parramatta Station",
            transportModeType = persistentListOf(NswTransportMode.Train, NswTransportMode.Bus),
        ),
        SearchStopState.SearchResult.Stop(
            stopId = "10104",
            stopName = "Sydney Airport",
            transportModeType = persistentListOf(NswTransportMode.Train),
        ),
    )

    override val selectedFromStop: StopItem?
        get() = _selectedFromStop

    override val selectedToStop: StopItem?
        get() = _selectedToStop

    override suspend fun fetchStopResults(
        query: String,
        searchRoutesEnabled: Boolean,
    ): List<SearchStopState.SearchResult> {
        // Throw an exception if shouldThrowError is true
        if (shouldThrowError) {
            error("Error fetching stop results")
        }

        return if (query.isBlank()) {
            testStopResults
        } else {
            testStopResults.filter {
                // With spaceInsensitiveSearch on, the fake finds "Town Hall" for "townhall"
                // the way the real fuzzy search does. Off by default: tests that assert on
                // zero-result queries would otherwise start matching things.
                it.stopName.contains(query, ignoreCase = true) ||
                    (
                        spaceInsensitiveSearch &&
                            it.stopName.withoutSpaces().contains(query.withoutSpaces(), ignoreCase = true)
                        )
            }
        }
    }

    private fun String.withoutSpaces(): String = replace(" ", "")

    override fun prioritiseStops(stopResults: List<SearchStopState.SearchResult.Stop>): List<SearchStopState.SearchResult.Stop> {
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
        // Find the corresponding test stop result and convert to legacy StopResult format
        val searchResultStop = testStopResults.find { it.stopId == stopItem.stopId }
        val stopResult = if (searchResultStop != null) {
            SearchStopState.StopResult(
                stopId = searchResultStop.stopId,
                stopName = searchResultStop.stopName,
                transportModeType = searchResultStop.transportModeType,
            )
        } else {
            SearchStopState.StopResult(
                stopId = stopItem.stopId,
                stopName = stopItem.stopName,
                transportModeType = persistentListOf(NswTransportMode.Train), // Default transport mode
            )
        }

        addRecentSearchStop(stopResult)
    }
}
