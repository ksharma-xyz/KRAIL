// kotlin
package xyz.ksharma.krail.trip.planner.ui.state.searchstop

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode

/**
 * List states for the Search Stop screen:
 * - Recent: show recent stops (query is blank)
 * - Results: search has results (may also be loading)
 * - NoMatch: search completed with no results (after delay)
 * - Error: show error UI
 */
sealed interface ListState {
    data object Recent : ListState
    data class Results(
        val results: ImmutableList<SearchStopState.SearchResult> = persistentListOf(),
        val isLoading: Boolean = false,
        val isError: Boolean = false,
    ) : ListState

    data object NoMatch : ListState
    data object Error : ListState
}

/**
 * Root UI state for the Search Stop screen.
 * List and map states are independent - both can be active simultaneously (dual-pane mode)
 * or individually (single-pane mode).
 *
 * - listState: current state of the list view
 * - mapUiState: current state of the map view (null if not initialized or maps disabled)
 * - isMapsAvailable: whether maps feature is enabled
 * - searchQuery: current text in the search field
 * - searchResults: backing data for search results
 * - recentStops: backing data for recent stops
 */
data class SearchStopState(
    val listState: ListState = ListState.Recent,
    val mapUiState: MapUiState? = null,
    val isMapsAvailable: Boolean = false,
    val searchQuery: String = "",
    val searchResults: ImmutableList<SearchResult> = persistentListOf(),
    val recentStops: ImmutableList<StopResult> = persistentListOf(),
) {
    /**
     * Internal sealed classes describing results / recent items.
     * Keep shape compatible with your existing code (StopResult / Trip / Stop).
     */
    sealed class SearchResult {
        data class Stop(
            val stopName: String,
            val stopId: String,
            val transportModeType: ImmutableList<TransportMode> = persistentListOf(),
        ) : SearchResult()

        data class Trip(
            val tripId: String,
            val routeShortName: String,
            val headsign: String,
            val stops: ImmutableList<TripStop> = persistentListOf(),
            val transportMode: TransportMode,
        ) : SearchResult()
    }

    data class TripStop(
        val stopId: String,
        val stopName: String,
        val stopSequence: Int,
        val transportModeType: ImmutableList<TransportMode> = persistentListOf(),
    )

    data class StopResult(
        val stopName: String,
        val stopId: String,
        val transportModeType: ImmutableList<TransportMode> = persistentListOf(),
    )
}
