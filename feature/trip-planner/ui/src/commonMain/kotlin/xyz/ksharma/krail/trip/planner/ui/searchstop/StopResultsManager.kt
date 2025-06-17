package xyz.ksharma.krail.trip.planner.ui.searchstop

import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState

interface StopResultsManager {

    suspend fun fetchStopResults(query: String): List<SearchStopState.StopResult>

    fun prioritiseStops(stopResults: List<SearchStopState.StopResult>): List<SearchStopState.StopResult>

    /**
     * Fetches the local stop name for a given stop ID.
     * If the stop ID is not found in the local database, it returns null.
     * @param stopId The ID of the stop for which to fetch the name.
     * @return The name of the stop if found, or null if not found.
     */
    fun fetchLocalStopName(stopId: String): String?
}
