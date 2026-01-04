package xyz.ksharma.krail.trip.planner.ui.searchstop

import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

interface StopResultsManager {

    val selectedFromStop: StopItem?

    val selectedToStop: StopItem?

    suspend fun fetchStopResults(
        query: String,
        searchRoutesEnabled: Boolean = true,
    ): List<SearchStopState.SearchResult>

    fun prioritiseStops(stopResults: List<SearchStopState.SearchResult.Stop>): List<SearchStopState.SearchResult.Stop>

    /**
     * Fetches the local stop name for a given stop ID.
     * If the stop ID is not found in the local database, it returns null.
     * @param stopId The ID of the stop for which to fetch the name.
     * @return The name of the stop if found, or null if not found.
     */
    fun fetchLocalStopName(stopId: String): String?

    // Selected stop management methods
    fun setSelectedFromStop(stopItem: StopItem?)

    fun setSelectedToStop(stopItem: StopItem?)

    fun reverseSelectedStops()

    /**
     * Clears both selected 'from' and 'to' stops.
     */
    fun clearSelectedStops()

    // region RecentSearchStops
    /**
     * Fetches the list of recent search stops from local storage.
     */
    suspend fun recentSearchStops(): List<SearchStopState.StopResult>

    /**
     * Clears all recent search stops from local storage.
     */
    fun clearRecentSearchStops()

    // endregion
}
