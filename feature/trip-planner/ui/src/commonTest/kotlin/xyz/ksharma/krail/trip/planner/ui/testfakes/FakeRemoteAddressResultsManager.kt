package xyz.ksharma.krail.trip.planner.ui.testfakes

import xyz.ksharma.krail.trip.planner.ui.searchstop.RemoteAddressResultsManager
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState

class FakeRemoteAddressResultsManager : RemoteAddressResultsManager {
    var results: List<SearchStopState.SearchResult.Address> = emptyList()
    var shouldThrowError = false

    override suspend fun fetchAddressResults(query: String): List<SearchStopState.SearchResult.Address> {
        if (shouldThrowError) {
            throw RuntimeException("Error fetching address results")
        }
        return results
    }
}
