package xyz.ksharma.krail.trip.planner.ui.testfakes

import xyz.ksharma.krail.trip.planner.ui.searchstop.RemoteAddressResultsManager
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState

class FakeRemoteAddressResultsManager : RemoteAddressResultsManager {
    var results: List<SearchStopState.SearchResult.Address> = emptyList()
    var shouldThrowError = false
    var callCount: Int = 0
        private set
    var lastQuery: String? = null
        private set

    override suspend fun fetchAddressResults(query: String): List<SearchStopState.SearchResult.Address> {
        callCount++
        lastQuery = query
        if (shouldThrowError) {
            throw RuntimeException("Error fetching address results")
        }
        return results
    }
}
