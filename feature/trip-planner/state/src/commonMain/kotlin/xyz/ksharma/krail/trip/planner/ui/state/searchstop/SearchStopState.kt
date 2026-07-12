// kotlin
package xyz.ksharma.krail.trip.planner.ui.state.searchstop

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel

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
    val showMapOptionsOnOpen: Boolean = false,
    val stopLabels: ImmutableList<StopLabel> = persistentListOf(),
    /**
     * Address/POI hits from the NSW `stop_finder` API (`type_sf=any`, `stop`-typed
     * results filtered out — transit stops always come from [searchResults] / local DB
     * only). Deliberately separate from [listState] so this section's visibility never
     * depends on the local search outcome: it renders whenever non-empty, even if
     * [listState] is [ListState.NoMatch] for the same query.
     */
    val addressResults: ImmutableList<SearchResult.Address> = persistentListOf(),
    val isAddressSearchLoading: Boolean = false,
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

        /**
         * An address/POI hit from `stop_finder`. [addressId] is the API's location `id`
         * (e.g. a `streetID:...` value) — never a local transit `stopId`.
         */
        data class Address(
            val addressId: String,
            val displayName: String,
            val addressType: String,
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
