package xyz.ksharma.krail.trip.planner.ui.searchstop

import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState

/**
 * Address/POI search via the NSW `stop_finder` API — additive to, and fully independent
 * of, local transit-stop search ([StopResultsManager]). Transit stops are never sourced
 * from here; only `singlehouse` / `street` / `poi` results surface.
 */
interface RemoteAddressResultsManager {

    suspend fun fetchAddressResults(query: String): List<SearchStopState.SearchResult.Address>
}
