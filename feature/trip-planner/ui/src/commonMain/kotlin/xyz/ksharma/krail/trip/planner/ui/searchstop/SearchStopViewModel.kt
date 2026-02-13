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
import xyz.ksharma.krail.core.maps.data.model.NearbyStop
import xyz.ksharma.krail.core.maps.data.repository.NearbyStopsRepository
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.coroutines.ext.launchWithExceptionHandler
import xyz.ksharma.krail.sandook.utils.GeoUtils
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.LatLng
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.ListState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapDisplay
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopsConfig
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.RouteFeature
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchScreen
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.StopFeature
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.StopSelectionType
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class SearchStopViewModel(
    private val analytics: Analytics,
    private val stopResultsManager: StopResultsManager,
    private val nearbyStopsRepository: NearbyStopsRepository,
    val flag: Flag,
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
    private var nearbyStopsJob: Job? = null
    private var lastQueryCenter: LatLng? = null
    private var lastQueryTime: Long = 0

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
                        Dispatchers.IO,
                    ) {
                        fetchRecentStops()
                    }
            }

            is SearchStopUiEvent.StopSelectionTypeClicked -> {
                onStopSelectionTypeClicked(event.stopSelectionType)
            }

            is SearchStopUiEvent.ShowStopsHere -> {
                log("[NEARBY_STOPS] ShowStopsHere event received")
                loadNearbyStops()
            }
            is SearchStopUiEvent.MapCenterChanged -> {
                log("[NEARBY_STOPS] MapCenterChanged: lat=${event.center.latitude}, lon=${event.center.longitude}")
                onMapCenterChanged(event.center)
            }
            is SearchStopUiEvent.TransportModeFilterToggled -> {
                log("[NEARBY_STOPS] TransportModeFilterToggled: mode=${event.mode.name}")
                onModeToggled(event.mode)
            }
            is SearchStopUiEvent.NearbyStopClicked -> {
                log("[NEARBY_STOPS] NearbyStopClicked: ${event.stop.stopName}")
                onNearbyStopClicked(event.stop)
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

    @OptIn(ExperimentalTime::class)
    private fun loadNearbyStops() {
        log("[NEARBY_STOPS] loadNearbyStops() called")
        val currentState = _uiState.value
        val screen = currentState.screen as? SearchScreen.Map ?: run {
            log("[NEARBY_STOPS] ERROR: screen is not Map, it's ${currentState.screen}")
            return
        }
        val mapState = screen.mapUiState as? MapUiState.Ready ?: run {
            log("[NEARBY_STOPS] ERROR: mapUiState is not Ready, it's ${screen.mapUiState}")
            return
        }

        val center = mapState.mapDisplay.mapCenter
        log("[NEARBY_STOPS] Map center: lat=${center.latitude}, lon=${center.longitude}")

        // Check cache validity
        if (shouldUseCachedResults(center)) {
            log("[NEARBY_STOPS] Using cached nearby stops")
            return
        }

        // Cancel any ongoing query
        nearbyStopsJob?.cancel()

        // Show loading
        updateUiState { withMapState { copy(isLoadingNearbyStops = true) } }
        log("[NEARBY_STOPS] Loading state set to true")

        nearbyStopsJob = viewModelScope.launch(Dispatchers.IO) {
            delay(NearbyStopsConfig.QUERY_DEBOUNCE_MS) // Debounce
            log("[NEARBY_STOPS] Debounce complete, starting query...")

            runCatching {
                val selectedModes = mapState.mapDisplay.selectedTransportModes
                log("[NEARBY_STOPS] Query params: centerLat=${center.latitude}, centerLon=${center.longitude}, radiusKm=${NearbyStopsConfig.DEFAULT_RADIUS_KM}, productClasses=$selectedModes, maxResults=${NearbyStopsConfig.MAX_NEARBY_RESULTS}")

                val stops = nearbyStopsRepository.getStopsNearby(
                    centerLat = center.latitude,
                    centerLon = center.longitude,
                    radiusKm = NearbyStopsConfig.DEFAULT_RADIUS_KM,
                    productClasses = selectedModes,
                    maxResults = NearbyStopsConfig.MAX_NEARBY_RESULTS,
                )

                log("[NEARBY_STOPS] Query returned ${stops.size} stops")
                stops.take(5).forEach { stop ->
                    log("[NEARBY_STOPS] Stop: ${stop.stopName} (${stop.stopId}) - ${stop.distanceKm}km - modes=${stop.transportModes.map { it.name }}")
                }

                updateUiState {
                    withMapState {
                        copy(
                            mapDisplay = mapDisplay.copy(
                                nearbyStops = stops.map { it.toFeature() },
                            ),
                            isLoadingNearbyStops = false,
                        )
                    }
                }

                log("[NEARBY_STOPS] State updated with ${stops.size} stops, loading=false")

                // Update cache state
                lastQueryCenter = center
                lastQueryTime = Clock.System.now().toEpochMilliseconds()

                analytics.track(
                    AnalyticsEvent.NearbyStopsLoaded(
                        count = stops.size,
                        radiusKm = NearbyStopsConfig.DEFAULT_RADIUS_KM,
                        modes = selectedModes,
                    ),
                )
            }.getOrElse { error ->
                log("[NEARBY_STOPS] ERROR: Query failed - ${error.message}")
                error.printStackTrace()
                updateUiState { withMapState { copy(isLoadingNearbyStops = false) } }
                analytics.track(AnalyticsEvent.NearbyStopsError(error.message ?: "Unknown"))
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun shouldUseCachedResults(newCenter: LatLng): Boolean {
        val lastCenter = lastQueryCenter ?: return false

        // Check if cache expired
        val cacheAge = Clock.System.now().toEpochMilliseconds() - lastQueryTime
        if (cacheAge > NearbyStopsConfig.CACHE_EXPIRY_MS) return false

        // Check if center moved significantly
        val distance = GeoUtils.haversineDistance(
            lastCenter.latitude,
            lastCenter.longitude,
            newCenter.latitude,
            newCenter.longitude,
        )

        return distance < NearbyStopsConfig.MIN_DISTANCE_FOR_RELOAD_KM
    }

    private fun onMapCenterChanged(center: LatLng) {
        updateUiState {
            withMapState {
                copy(mapDisplay = mapDisplay.copy(mapCenter = center))
            }
        }
    }

    private fun onModeToggled(mode: TransportMode) {
        updateUiState {
            withMapState {
                val currentModes = mapDisplay.selectedTransportModes.toMutableSet()
                if (currentModes.contains(mode.productClass)) {
                    currentModes.remove(mode.productClass)
                } else {
                    currentModes.add(mode.productClass)
                }
                copy(
                    mapDisplay = mapDisplay.copy(
                        selectedTransportModes = currentModes,
                    ),
                )
            }
        }

        // Invalidate cache and reload
        lastQueryCenter = null
        loadNearbyStops()

        // Track analytics
        val currentState = _uiState.value
        val screen = currentState.screen as? SearchScreen.Map
        val mapState = screen?.mapUiState as? MapUiState.Ready
        val selectedModes = mapState?.mapDisplay?.selectedTransportModes ?: emptySet()
        val modeNames = selectedModes.mapNotNull { TransportMode.toTransportModeType(it)?.name }
        val resultCount = mapState?.mapDisplay?.nearbyStops?.size ?: 0

        analytics.track(
            AnalyticsEvent.ModeFilterChanged(
                selectedModes = modeNames,
                resultCount = resultCount,
            ),
        )
    }

    private fun onNearbyStopClicked(stop: NearbyStopFeature) {
        analytics.track(
            AnalyticsEvent.NearbyStopClicked(
                stopId = stop.stopId,
                distanceKm = stop.distanceKm,
                transportMode = stop.transportModes.firstOrNull()?.name ?: "Unknown",
            ),
        )

        // TODO: Show bottom sheet with stop details
    }

    // Helper to update map state safely
    private fun SearchStopState.withMapState(
        block: MapUiState.Ready.() -> MapUiState.Ready,
    ): SearchStopState {
        val currentScreen = screen as? SearchScreen.Map ?: return this
        val currentMapState = currentScreen.mapUiState as? MapUiState.Ready ?: return this

        return copy(
            screen = currentScreen.copy(
                mapUiState = currentMapState.block(),
            ),
        )
    }

    // Mapper
    private fun NearbyStop.toFeature() = NearbyStopFeature(
        stopId = stopId,
        stopName = stopName,
        position = LatLng(latitude, longitude),
        distanceKm = distanceKm,
        transportModes = transportModes,
    )

    // endregion

    private fun updateUiState(block: SearchStopState.() -> SearchStopState) {
        _uiState.update(block)
    }
}
