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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.ClearRecentSearchClickEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.StopLabelCreatedEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.StopLabelRemovedEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.StopLabelStopAssignedEvent
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.StopSelectedEvent
import xyz.ksharma.krail.core.analytics.event.trackScreenViewEvent
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.asBoolean
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.coroutines.ext.launchWithExceptionHandler
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.trip.planner.ui.components.normaliseLabelName
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.MapStateHelper
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.NearbyStopsManager
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.ListState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent

@Suppress("TooManyFunctions", "LongParameterList")
class SearchStopViewModel(
    private val analytics: Analytics,
    private val stopResultsManager: StopResultsManager,
    private val nearbyStopsManager: NearbyStopsManager,
    val flag: Flag,
    private val ioDispatcher: CoroutineDispatcher,
    private val preferences: SandookPreferences,
    private val sandook: Sandook,
) : ViewModel() {

    private val _uiState: MutableStateFlow<SearchStopState> = MutableStateFlow(SearchStopState())
    val uiState: StateFlow<SearchStopState> = _uiState
        .onStart {
            analytics.trackScreenViewEvent(screen = AnalyticsScreen.SearchStop)
            checkMapsAvailability()
            val hasSeenOptions = preferences.getBoolean(
                SandookPreferences.KEY_HAS_SEEN_MAP_OPTIONS_SHEET,
            ) == true
            if (!hasSeenOptions) {
                updateUiState { copy(showMapOptionsOnOpen = true) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchStopState())

    init {
        // Single VM-lifetime observer so DB updates always reach the UI even if
        // the screen briefly unsubscribes (e.g. across config changes).
        observeStopLabels()
    }

    private val isMapsAvailable: Boolean by lazy {
        flag.getFlagValue(FlagKeys.SEARCH_STOP_MAPS_AVAILABLE.key)
            .asBoolean(fallback = false)
    }

    private var searchJob: Job? = null
    private var fetchRecentStopsJob: Job? = null

    @Suppress("LongMethod", "ReturnCount")
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
                onMapCenterChanged(event.center)
            }

            is SearchStopUiEvent.UserLocationUpdated -> {
                onUserLocationUpdated(event.location)
            }

            is SearchStopUiEvent.TransportModeFilterToggled -> {
                log("[NEARBY_STOPS] TransportModeFilterToggled: mode=${NswTransportConfig.nameFor(event.mode)}")
                onModeToggled(event.mode)
            }

            is SearchStopUiEvent.NearbyStopClicked -> {
                onNearbyStopClicked(event.stop)
            }

            SearchStopUiEvent.MapOptionsClicked -> {
                // No-op: Bottom sheet visibility is handled in UI layer
            }

            SearchStopUiEvent.MapOptionsFirstTimeShown -> {
                preferences.setBoolean(SandookPreferences.KEY_HAS_SEEN_MAP_OPTIONS_SHEET, true)
                updateUiState { copy(showMapOptionsOnOpen = false) }
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

            is SearchStopUiEvent.MapToggleClicked -> {
                analytics.track(AnalyticsEvent.MapToggleClickEvent(selected = event.selected))
            }

            SearchStopUiEvent.MapOptionsButtonClicked -> {
                analytics.track(AnalyticsEvent.MapOptionsOpenedEvent)
            }

            is SearchStopUiEvent.LocationButtonClicked -> {
                analytics.track(
                    AnalyticsEvent.MapLocationButtonClickEvent(
                        isLocationActive = event.hadLocation,
                        source = AnalyticsEvent.MapLocationButtonClickEvent.Source.SEARCH_STOP_MAP,
                    ),
                )
            }

            SearchStopUiEvent.LocationPermissionSettingsClicked -> {
                analytics.track(
                    AnalyticsEvent.LocationPermissionSettingsClickEvent(
                        source = AnalyticsEvent.LocationPermissionSettingsClickEvent.Source.SEARCH_STOP_MAP,
                    ),
                )
            }

            is SearchStopUiEvent.MapOptionsSaved -> {
                analytics.track(
                    AnalyticsEvent.MapOptionsSavedEvent(
                        radiusKm = event.radiusKm,
                        transportModes = event.transportModes,
                        showDistanceScale = event.showDistanceScale,
                        showCompass = event.showCompass,
                        radiusChanged = event.radiusChanged,
                        modesChanged = event.modesChanged,
                    ),
                )
            }

            is SearchStopUiEvent.TrackStopSelectedFromMap -> {
                analytics.track(
                    AnalyticsEvent.StopSelectedFromMapEvent(
                        stopId = event.stopId,
                        searchRadiusKm = event.searchRadiusKm,
                        enabledModesCount = event.enabledModesCount,
                        nearbyStopsCount = event.nearbyStopsCount,
                        hadUserLocation = event.hadUserLocation,
                    ),
                )
            }

            is SearchStopUiEvent.AssignLabelStop -> {
                // Snapshot pre-state so isReassignment is captured against the value
                // *before* the optimistic update mutates it.
                val previousStopId = _uiState.value.stopLabels
                    .firstOrNull { it.label == event.labelKey }
                    ?.stopId
                val isReassignment = previousStopId != null && previousStopId != event.stopItem.stopId

                // Optimistic: update state immediately so the pill row reflects the
                // assignment before the IO write completes. The DB observer re-emits
                // the same shape and the second update is a no-op.
                updateUiState {
                    val updated = stopLabels.map { label ->
                        if (label.label == event.labelKey) {
                            label.copy(stopId = event.stopItem.stopId, stopName = event.stopItem.stopName)
                        } else {
                            label
                        }
                    }.toImmutableList()
                    copy(stopLabels = updated)
                }
                analytics.track(
                    StopLabelStopAssignedEvent(
                        labelName = event.labelKey,
                        stopId = event.stopItem.stopId,
                        stopName = event.stopItem.stopName,
                        isReassignment = isReassignment,
                        isProtectedLabel = event.labelKey
                            .equals(StopLabel.PROTECTED_LABEL, ignoreCase = true),
                    ),
                )
                viewModelScope.launchWithExceptionHandler<SearchStopViewModel>(ioDispatcher) {
                    sandook.updateStopLabelStop(event.labelKey, event.stopItem.stopId, event.stopItem.stopName)
                    // Stops the user pins to a label are also stops they "interacted
                    // with", so surface them in Recents next time the screen opens.
                    sandook.insertOrReplaceRecentSearchStop(stopId = event.stopItem.stopId)
                    fetchRecentStops()
                }
            }

            is SearchStopUiEvent.CreateLabel -> {
                // Strip emoji and whitespace from the typed name so `🏠 Home` and `Home`
                // resolve to the same canonical label. Dedupe case-insensitively against
                // existing labels — duplicates silently no-op (the UI blocks them earlier
                // with an inline error).
                val cleanedName = normaliseLabelName(event.name)
                if (cleanedName.isNotBlank()) {
                    val alreadyExists = _uiState.value.stopLabels.any {
                        normaliseLabelName(it.label).equals(cleanedName, ignoreCase = true)
                    }
                    if (!alreadyExists) {
                        val sortOrder = _uiState.value.stopLabels.size.toLong()
                        updateUiState {
                            copy(
                                stopLabels = (stopLabels + StopLabel(emoji = event.emoji, label = cleanedName))
                                    .toImmutableList(),
                            )
                        }
                        analytics.track(
                            StopLabelCreatedEvent(
                                labelName = cleanedName,
                                emoji = event.emoji,
                                totalLabelsCountAfter = _uiState.value.stopLabels.size,
                            ),
                        )
                        viewModelScope.launchWithExceptionHandler<SearchStopViewModel>(ioDispatcher) {
                            sandook.upsertStopLabel(cleanedName, event.emoji, null, null, sortOrder)
                        }
                    }
                }
            }

            is SearchStopUiEvent.ClearLabelStop -> {
                val hadStop = _uiState.value.stopLabels
                    .firstOrNull { it.label == event.labelKey }
                    ?.stopId != null
                updateUiState {
                    val updated = stopLabels.map { label ->
                        if (label.label == event.labelKey) {
                            label.copy(stopId = null, stopName = null)
                        } else {
                            label
                        }
                    }.toImmutableList()
                    copy(stopLabels = updated)
                }
                analytics.track(
                    StopLabelRemovedEvent(
                        labelName = event.labelKey,
                        action = StopLabelRemovedEvent.Action.CLEAR,
                        hadStop = hadStop,
                    ),
                )
                viewModelScope.launchWithExceptionHandler<SearchStopViewModel>(ioDispatcher) {
                    sandook.updateStopLabelStop(event.labelKey, null, null)
                }
            }

            is SearchStopUiEvent.DeleteLabel -> {
                // Home is non-deletable. Defence in depth: the UI also hides the ✕ on
                // protected pills.
                if (event.labelKey.equals(StopLabel.PROTECTED_LABEL, ignoreCase = true)) {
                    return
                }
                val hadStop = _uiState.value.stopLabels
                    .firstOrNull { it.label == event.labelKey }
                    ?.stopId != null
                updateUiState {
                    val updated = stopLabels.filterNot { it.label == event.labelKey }.toImmutableList()
                    copy(stopLabels = updated)
                }
                analytics.track(
                    StopLabelRemovedEvent(
                        labelName = event.labelKey,
                        action = StopLabelRemovedEvent.Action.DELETE,
                        hadStop = hadStop,
                    ),
                )
                viewModelScope.launchWithExceptionHandler<SearchStopViewModel>(ioDispatcher) {
                    sandook.deleteStopLabel(event.labelKey)
                }
            }

            is SearchStopUiEvent.MoveLabelToIndex -> {
                val current = _uiState.value.stopLabels.toMutableList()
                val sourceIndex = current.indexOfFirst { it.label == event.labelKey }
                if (sourceIndex == -1) return
                val target = event.targetIndex.coerceIn(0, current.size - 1)
                if (target == sourceIndex) return
                val moved = current.removeAt(sourceIndex)
                current.add(target, moved)
                val reordered = current.toImmutableList()
                updateUiState { copy(stopLabels = reordered) }
                viewModelScope.launchWithExceptionHandler<SearchStopViewModel>(ioDispatcher) {
                    reordered.forEachIndexed { index, label ->
                        sandook.upsertStopLabel(
                            label.label,
                            label.emoji,
                            label.stopId,
                            label.stopName,
                            index.toLong(),
                        )
                    }
                }
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

    private fun observeStopLabels() {
        viewModelScope.launchWithExceptionHandler<SearchStopViewModel>(ioDispatcher) {
            sandook.observeStopLabels()
                .distinctUntilChanged()
                .collectLatest { rows ->
                    if (rows.isEmpty()) {
                        StopLabel.defaults.forEachIndexed { index, label ->
                            sandook.upsertStopLabel(label.label, label.emoji, null, null, index.toLong())
                        }
                        return@collectLatest
                    }
                    val labels = rows.map { row ->
                        StopLabel(emoji = row.emoji, label = row.label, stopId = row.stop_id, stopName = row.stop_name)
                    }.toImmutableList()
                    updateUiState { copy(stopLabels = labels) }
                }
        }
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
        log("[NEARBY_STOPS] Loading nearby stops")

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
    }

    private fun onNearbyStopClicked(stop: NearbyStopFeature) {
        analytics.track(
            AnalyticsEvent.NearbyStopClickEvent(
                stopId = stop.stopId,
                transportModesCount = stop.transportModes.size,
            ),
        )
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
