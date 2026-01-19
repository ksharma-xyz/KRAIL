package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.ClearRecentSearchClickEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.StopSelectedEvent
import xyz.ksharma.krail.core.analytics.event.trackScreenViewEvent
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.asBoolean
import xyz.ksharma.krail.coroutines.ext.launchWithExceptionHandler
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.LatLng
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.ListState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapDisplay
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.RouteFeature
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchScreen
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.StopFeature
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.StopSelectionType

class SearchStopViewModel(
    private val analytics: Analytics,
    private val stopResultsManager: StopResultsManager,
    private val flag: Flag,
) : ViewModel() {

    private val _uiState: MutableStateFlow<SearchStopState> = MutableStateFlow(SearchStopState())
    val uiState: StateFlow<SearchStopState> = _uiState
        .onStart {
            analytics.trackScreenViewEvent(screen = AnalyticsScreen.SearchStop)
            checkMapsAvailability()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchStopState())

    private val isMapsAvailable: Boolean by lazy {
        flag.getFlagValue(FlagKeys.SEARCH_STOP_MAPS_AVAILABLE.key)
            .asBoolean(fallback = false)
    }

    private var searchJob: Job? = null
    private var fetchRecentStopsJob: Job? = null

    fun onEvent(event: SearchStopUiEvent) {
        when (event) {
            is SearchStopUiEvent.SearchTextChanged -> onSearchTextChanged(event.query)

            is SearchStopUiEvent.TrackStopSelected -> {
                analytics.track(
                    StopSelectedEvent(
                        stopId = event.stopItem.stopId,
                        isRecentSearch = event.isRecentSearch,
                    ),
                )
            }

            is SearchStopUiEvent.ClearRecentSearchStops -> {
                analytics.track(
                    ClearRecentSearchClickEvent(
                        recentSearchCount = event.recentSearchCount,
                    ),
                )
                stopResultsManager.clearRecentSearchStops()
                // Refresh the state with empty recent stops
                updateUiState {
                    copy(recentStops = persistentListOf())
                }
            }

            SearchStopUiEvent.RefreshRecentStopsList -> {
                fetchRecentStopsJob?.cancel()
                fetchRecentStopsJob =
                    viewModelScope.launchWithExceptionHandler<SearchStopViewModel>(
                        Dispatchers.IO,
                    ) {
                        fetchRecentStops()
                    }
            }

            is SearchStopUiEvent.StopSelectionTypeClicked -> {
                onStopSelectionTypeClicked(event.stopSelectionType)
            }
        }
    }

    private fun onSearchTextChanged(query: String) {
        // update query in state first
        updateUiState { copy(searchQuery = query) }

        // If map is selected -> do not run search
        val current = _uiState.value
        if (current.selectionType == StopSelectionType.MAP) return

        // Blank query -> show recent stops and cancel any ongoing search
        if (query.isBlank()) {
            searchJob?.cancel()
            updateUiState { copy(screen = SearchScreen.List(ListState.Recent)) }

            // ensure recentStops are loaded
            fetchRecentStopsJob?.cancel()
            fetchRecentStopsJob =
                viewModelScope.launchWithExceptionHandler<SearchStopViewModel>(Dispatchers.IO) {
                    fetchRecentStops()
                }
            return
        }

        // Non-empty query -> set loading and start search
        updateUiState { displayLoading() }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(100) // debounce / small throttle on VM side
            runCatching {
                val stopResults = stopResultsManager.fetchStopResults(query)
                updateUiState { displayData(stopResults) }
                analytics.track(
                    AnalyticsEvent.SearchStopQuery(
                        query = query,
                        resultsCount = stopResults.size,
                    ),
                )
            }.getOrElse {
                updateUiState { displayError() }
                analytics.track(AnalyticsEvent.SearchStopQuery(query = query, isError = true))
            }
        }
    }

    private fun onStopSelectionTypeClicked(stopSelectionType: StopSelectionType) {
        // update selection toggle
        updateUiState { copy(selectionType = stopSelectionType) }

        // Decide screen to show:
        if (stopSelectionType == StopSelectionType.MAP && isMapsAvailable) {
            loadStaticMapData()
        } else {
            // LIST selected: if query is blank show recent, else show results (trigger a search)
            val currentQuery = _uiState.value.searchQuery
            if (currentQuery.isBlank()) {
                updateUiState { copy(screen = SearchScreen.List(ListState.Recent)) }
                // ensure recentStops are loaded (trigger fetch if needed)
                fetchRecentStopsJob?.cancel()
                fetchRecentStopsJob =
                    viewModelScope.launchWithExceptionHandler<SearchStopViewModel>(Dispatchers.IO) {
                        fetchRecentStops()
                    }
            } else {
                // trigger the search flow: reuse onSearchTextChanged to ensure same behaviour
                onSearchTextChanged(currentQuery)
            }
        }
    }

    private suspend fun fetchRecentStops() {
        val recentStops = stopResultsManager.recentSearchStops().toImmutableList()
        log("fetchRecentStops: ${recentStops.map { it.stopName }}")
        updateUiState { copy(recentStops = recentStops) }
    }

    private fun checkMapsAvailability() {
        updateUiState { copy(isMapsAvailable = this@SearchStopViewModel.isMapsAvailable) }
    }

    private fun SearchStopState.displayData(stopsResult: List<SearchStopState.SearchResult>) = copy(
        searchResults = stopsResult.toImmutableList(),
        screen = if (stopsResult.isEmpty()) {
            SearchScreen.List(ListState.NoMatch)
        } else {
            SearchScreen.List(
                ListState.Results(
                    results = stopsResult.toImmutableList(),
                    isLoading = false,
                    isError = false,
                ),
            )
        },
    )

    private fun SearchStopState.displayLoading() =
        copy(
            // keep existing results (if any) but mark loading
            screen = SearchScreen.List(
                ListState.Results(
                    results = searchResults,
                    isLoading = true,
                    isError = false,
                ),
            ),
        )

    private fun SearchStopState.displayError() = copy(
        searchResults = persistentListOf(),
        screen = SearchScreen.List(ListState.Error),
    )

    // region Maps related methods

    @Suppress("MagicNumber")
    private fun loadStaticMapData() {
        // Build platform-agnostic route & stop data here (all static data comes from VM)
        val route = RouteFeature(
            id = "T1",
            colorHex = "#0055FF",
            points = listOf(
                LatLng(-33.875, 151.200),
                LatLng(-33.873, 151.206),
                LatLng(-33.870, 151.212),
                LatLng(-33.867, 151.218),
            ),
        )

        val stops = listOf(
            StopFeature(
                stopId = "stop_1",
                stopName = "Central",
                lineId = "T1",
                position = LatLng(-33.873, 151.206),
            ),
            StopFeature(
                stopId = "stop_2",
                stopName = "Town Hall",
                lineId = "T1",
                position = LatLng(-33.870, 151.212),
            ),
            StopFeature(
                stopId = "stop_3",
                stopName = "Wynyard",
                lineId = "T1",
                position = LatLng(-33.867, 151.218),
            ),
        )

        // Create MapDisplay and MapUiState.Ready with the new payload
        val ready = MapUiState.Ready(
            mapDisplay = MapDisplay(
                routes = listOf(route),
                stops = stops,
                selectedStop = null,
            ),
        )

        _uiState.update { current ->
            val newScreen = when (val s = current.screen) {
                is SearchScreen.Map -> s.copy(mapUiState = ready)
                else -> SearchScreen.Map(mapUiState = ready)
            }
            current.copy(screen = newScreen, selectionType = StopSelectionType.MAP)
        }
    }

    // endregion

    private fun updateUiState(block: SearchStopState.() -> SearchStopState) {
        _uiState.update(block)
    }
}
