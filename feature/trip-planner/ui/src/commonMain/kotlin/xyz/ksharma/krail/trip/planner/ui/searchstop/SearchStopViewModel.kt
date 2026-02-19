package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
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
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.coroutines.ext.launchWithExceptionHandler
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.MapStateHelper
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.NearbyStopsManager
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.ListState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent

@Suppress("TooManyFunctions")
class SearchStopViewModel(
    private val analytics: Analytics,
    private val stopResultsManager: StopResultsManager,
    private val nearbyStopsManager: NearbyStopsManager,
    val flag: Flag,
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState: MutableStateFlow<SearchStopState> = MutableStateFlow(SearchStopState())
    val uiState: StateFlow<SearchStopState> = _uiState
        .onStart {
            analytics.trackScreenViewEvent(screen = AnalyticsScreen.SearchStop)
            checkMapsAvailability()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchStopState())

    private val isMapsAvailable: Boolean = true
    /*
        by lazy {
            flag.getFlagValue(FlagKeys.SEARCH_STOP_MAPS_AVAILABLE.key)
                .asBoolean(fallback = false)
        }
     */

    private var searchJob: Job? = null
    private var fetchRecentStopsJob: Job? = null

    @Suppress("LongMethod")
    fun onEvent(event: SearchStopUiEvent) {
        when (event) {
            is SearchStopUiEvent.SearchTextChanged -> onSearchTextChanged(event.query)

            is SearchStopUiEvent.TrackStopSelected -> {
                analytics.track(
                    StopSelectedEvent(
                        stopId = event.stopItem.stopId,
                        isRecentSearch = event.isRecentSearch,
                        searchQuery = event.searchQuery,
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
                        ioDispatcher,
                    ) {
                        fetchRecentStops()
                    }
            }

            SearchStopUiEvent.InitializeMap -> {
                if (isMapsAvailable && _uiState.value.mapUiState == null) {
                    log("[NEARBY_STOPS] Initializing map state")
                    updateUiState {
                        copy(mapUiState = MapUiState.Ready())
                    }
                    // Note: Auto-fetch of user location happens in UI layer
                    // via LaunchedEffect observing permission status
                }
            }

            is SearchStopUiEvent.MapCenterChanged -> {
                log("[NEARBY_STOPS] MapCenterChanged: lat=${event.center.latitude}, lon=${event.center.longitude}")
                onMapCenterChanged(event.center)
            }

            is SearchStopUiEvent.UserLocationUpdated -> {
                val locStr = event.location?.let { "lat=${it.latitude}, lon=${it.longitude}" } ?: "null"
                log("[NEARBY_STOPS] UserLocationUpdated: $locStr")
                onUserLocationUpdated(event.location)
            }

            is SearchStopUiEvent.TransportModeFilterToggled -> {
                log("[NEARBY_STOPS] TransportModeFilterToggled: mode=${event.mode.name}")
                onModeToggled(event.mode)
            }

            is SearchStopUiEvent.NearbyStopClicked -> {
                log("[NEARBY_STOPS] NearbyStopClicked: ${event.stop.stopName}")
                onNearbyStopClicked(event.stop)
            }

            SearchStopUiEvent.MapOptionsClicked -> {
                // No-op: Bottom sheet visibility is handled in UI layer
            }

            is SearchStopUiEvent.SearchRadiusChanged -> {
                log("[NEARBY_STOPS] SearchRadiusChanged: ${event.radiusKm}km")
                onSearchRadiusChanged(event.radiusKm)
            }

            is SearchStopUiEvent.ShowDistanceScaleToggled -> {
                log("[NEARBY_STOPS] ShowDistanceScaleToggled: ${event.enabled}")
                onShowDistanceScaleToggled(event.enabled)
            }

            is SearchStopUiEvent.ShowCompassToggled -> {
                log("[NEARBY_STOPS] ShowCompassToggled: ${event.enabled}")
                onShowCompassToggled(event.enabled)
            }
        }
    }

    private fun onSearchTextChanged(query: String) {
        // update query in state first
        updateUiState { copy(searchQuery = query) }

        // Blank query -> show recent stops and cancel any ongoing search
        if (query.isBlank()) {
            searchJob?.cancel()
            updateUiState { copy(listState = ListState.Recent) }

            // ensure recentStops are loaded
            fetchRecentStopsJob?.cancel()
            fetchRecentStopsJob =
                viewModelScope.launchWithExceptionHandler<SearchStopViewModel>(ioDispatcher) {
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

    private suspend fun fetchRecentStops() {
        val recentStops = stopResultsManager.recentSearchStops().toImmutableList()
        log("fetchRecentStops: ${recentStops.map { it.stopName }}")
        updateUiState { copy(recentStops = recentStops) }
    }

    private fun checkMapsAvailability() {
        updateUiState { copy(isMapsAvailable = this@SearchStopViewModel.isMapsAvailable) }
    }

    // endregion

    private fun SearchStopState.displayData(stopsResult: List<SearchStopState.SearchResult>) = copy(
        searchResults = stopsResult.toImmutableList(),
        listState = if (stopsResult.isEmpty()) {
            ListState.NoMatch
        } else {
            ListState.Results(
                results = stopsResult.toImmutableList(),
                isLoading = false,
                isError = false,
            )
        },
    )

    private fun SearchStopState.displayLoading() =
        copy(
            // keep existing results (if any) but mark loading
            listState = ListState.Results(
                results = searchResults,
                isLoading = true,
                isError = false,
            ),
        )

    private fun SearchStopState.displayError() = copy(
        searchResults = persistentListOf(),
        listState = ListState.Error,
    )

    // region Maps related methods

    private fun loadNearbyStops() {
        log("[NEARBY_STOPS] loadNearbyStops() called")

        val currentState = _uiState.value
        val mapState = MapStateHelper.getReadyMapState(currentState)

        if (mapState == null) {
            log("[NEARBY_STOPS] ERROR: Cannot load - map state is not Ready")
            return
        }

        val center = mapState.mapDisplay.mapCenter
        log("[NEARBY_STOPS] Loading stops for center: lat=${center.latitude}, lon=${center.longitude}")

        nearbyStopsManager.loadNearbyStops(
            mapState = mapState,
            center = center,
            scope = viewModelScope,
            onLoadingStateChanged = { isLoading ->
                log("[NEARBY_STOPS] Loading state changed: $isLoading")
                updateUiState { MapStateHelper.setLoadingState(this, isLoading) }
            },
            onStopsLoaded = { stops ->
                log("[NEARBY_STOPS] Loaded ${stops.size} stops, updating state")
                updateUiState {
                    MapStateHelper.updateNearbyStops(this, stops, isLoading = false)
                }
                log("[NEARBY_STOPS] State updated with ${stops.size} stops")
            },
            onError = { error ->
                log("[NEARBY_STOPS] Error handled: ${error.message}")
                // analytics.track(AnalyticsEvent.NearbyStopsError(error.message ?: "Unknown"))
            },
        )
    }

    private fun onMapCenterChanged(center: LatLng) {
        log("[NEARBY_STOPS] Map center changed to: lat=${center.latitude}, lon=${center.longitude}")

        // Update the center in state
        updateUiState { MapStateHelper.updateMapCenter(this, center) }

        // Load nearby stops for the new center
        // This is the primary trigger for loading stops
        loadNearbyStops()
    }

    private fun onModeToggled(mode: TransportMode) {
        updateUiState { MapStateHelper.toggleTransportMode(this, mode) }

        // Invalidate cache and reload
        nearbyStopsManager.invalidateCache()
        loadNearbyStops()

        // Track analytics
       /* val currentState = _uiState.value
        val screen = currentState.screen as? SearchScreen.Map
        val mapState = screen?.mapUiState as? MapUiState.Ready
        val selectedModes = mapState?.mapDisplay?.selectedTransportModes ?: emptySet()
        val modeNames = selectedModes.mapNotNull { TransportMode.toTransportModeType(it)?.name }
        val resultCount = mapState?.mapDisplay?.nearbyStops?.size ?: 0*/
    }

    private fun onNearbyStopClicked(stop: NearbyStopFeature) {
        // Show bottom sheet with stop details
        log("stop: $stop")
    }

    private fun onUserLocationUpdated(location: LatLng?) {
        updateUiState { MapStateHelper.updateUserLocation(this, location) }
    }

    private fun onSearchRadiusChanged(radiusKm: Double) {
        updateUiState { MapStateHelper.updateSearchRadius(this, radiusKm) }

        // Invalidate cache and reload with new radius
        nearbyStopsManager.invalidateCache()
        loadNearbyStops()
    }

    private fun onShowDistanceScaleToggled(enabled: Boolean) {
        updateUiState { MapStateHelper.toggleDistanceScale(this, enabled) }
    }

    private fun onShowCompassToggled(enabled: Boolean) {
        updateUiState { MapStateHelper.toggleCompass(this, enabled) }
    }

    // endregion

    private fun updateUiState(block: SearchStopState.() -> SearchStopState) {
        _uiState.update(block)
    }
}
