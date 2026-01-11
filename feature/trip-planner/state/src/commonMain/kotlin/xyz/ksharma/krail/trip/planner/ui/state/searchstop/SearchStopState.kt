// kotlin
package xyz.ksharma.krail.trip.planner.ui.state.searchstop

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode

/**
 * High level screen mode:
 * - Map: render the map UI
 * - List: render a list UI whose inner state is expressed by ListState
 */
sealed interface SearchScreen {
    data class Map(val mapUiState: MapUiState = MapUiState.Ready()) : SearchScreen
    data class List(val listState: ListState) : SearchScreen
}

/**
 * Inner list states for the List screen:
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
 * Selection toggle exposed to UI (Radio/segmented control)
 */
enum class StopSelectionType { LIST, MAP }

/**
 * Root UI state for the Search Stop screen.
 * - selectionType: what the user has chosen (LIST/MAP)
 * - screen: derived/explicit screen to render
 * - searchQuery: current text in the text field
 * - recentStops / searchResults: backing data
 *
 * ViewModel updates these fields; composables read them and render.
 */
data class SearchStopState(
    val selectionType: StopSelectionType = StopSelectionType.LIST,
    val screen: SearchScreen = SearchScreen.List(ListState.Recent),
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
