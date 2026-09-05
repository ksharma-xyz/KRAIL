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
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent.StopLabelReorderedEvent
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
import xyz.ksharma.krail.coroutines.ext.suspendSafeResult
import xyz.ksharma.krail.sandook.RecentSearchLocation
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.trip.planner.ui.components.normaliseLabelName
import xyz.ksharma.krail.trip.planner.ui.searchstop.address.AddressSearchEligibility
import xyz.ksharma.krail.trip.planner.ui.searchstop.address.AddressSearchGate
import xyz.ksharma.krail.trip.planner.ui.searchstop.address.DEFAULT_ADDRESS_SEARCH_MIN_QUERY_LENGTH
import xyz.ksharma.krail.trip.planner.ui.searchstop.address.normalizeAddressQuery
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.MapStateHelper
import xyz.ksharma.krail.trip.planner.ui.searchstop.map.NearbyStopsManager
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.ListState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.NearbyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.LocationKind
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import kotlin.math.abs
import kotlin.random.Random

@Suppress("TooManyFunctions", "LongParameterList")
class SearchStopViewModel(
    private val analytics: Analytics,
    private val stopResultsManager: StopResultsManager,
    private val remoteAddressResultsManager: RemoteAddressResultsManager,
    private val nearbyStopsManager: NearbyStopsManager,
    val flag: Flag,
    private val ioDispatcher: CoroutineDispatcher,
    private val preferences: SandookPreferences,
    private val sandook: Sandook,
    private val searchSessionStore: SearchSessionStore,
    private val isAddressSearchEnabled: () -> Boolean = { false },
    private val addressSearchMinQueryLength: () -> Int = { DEFAULT_ADDRESS_SEARCH_MIN_QUERY_LENGTH },
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
    private var addressSearchJob: Job? = null
    private var fetchRecentStopsJob: Job? = null

    // Staleness guard for the address pipeline only — local stop search has no equivalent
    // because it has no remote round-trip to protect. There is deliberately no result
    // cache: a cached "no results" is indistinguishable on screen from a suppressed call,
    // and hiding a result the rider asked for costs more than the saved request.
    private var addressSearchRequestToken = 0

    // Random ID minted here on every query change, not once per search. Sent on search
    // analytics events so funnels can be joined without carrying the typed text (which can
    // be a street address — see SearchQueryAnalyticsRedaction).
    //
    // Only the firing that outlives SEARCH_ANALYTICS_QUIET_MS reports one, so a typing burst
    // yields a single reported id even though it minted several. Do not restore a per-firing
    // report without also giving a burst its own key: an id that changes per keystroke, on an
    // event that fires per keystroke, makes every rate derived from it count keystrokes.
    private var currentSearchSessionId: String = newSearchSessionId()

    @Suppress("LongMethod", "ReturnCount")
    fun onEvent(event: SearchStopUiEvent) {
        when (event) {
            is SearchStopUiEvent.SearchTextChanged -> onSearchTextChanged(event.query)

            is SearchStopUiEvent.TrackStopSelected -> {
                // Selection context only makes sense mid-search: for recents,
                // empty-state stops, and map picks the displayed result counts
                // belong to a previous (or no) query, so send nothing.
                val state = _uiState.value
                val fromLiveQuery = state.searchQuery.isNotBlank()
                // Hand the session to whatever loads a trip next, so the funnel can run
                // past selection to "departure times were actually seen". Recents and
                // empty-state stops record null, which also clears any earlier pending
                // session rather than letting it attach to an unrelated trip.
                searchSessionStore.recordSelection(
                    searchSessionId = currentSearchSessionId.takeIf { fromLiveQuery },
                    stopId = event.stopItem.stopId,
                )
                analytics.track(
                    StopSelectedEvent(
                        stopId = event.stopItem.stopId,
                        isRecentSearch = event.isRecentSearch,
                        locationKind = when (event.stopItem.locationKind) {
                            LocationKind.TRANSIT_STOP -> StopSelectedEvent.LocationKind.TRANSIT_STOP
                            LocationKind.ADDRESS -> StopSelectedEvent.LocationKind.ADDRESS
                        },
                        addressType = event.stopItem.addressType?.let(StopSelectedEvent.AddressType::from),
                        searchSessionId = currentSearchSessionId.takeIf { fromLiveQuery },
                        displayedLocalCount = StopSelectedEvent.CountBucket
                            .from(state.searchResults.size)
                            .takeIf { fromLiveQuery },
                        displayedAddressCount = StopSelectedEvent.CountBucket
                            .from(state.addressResults.size)
                            .takeIf { fromLiveQuery },
                        resultIndex = state.resultIndexOf(event.stopItem.stopId)
                            .takeIf { fromLiveQuery },
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

            SearchStopUiEvent.SelectOnMapButtonClicked -> {
                analytics.track(AnalyticsEvent.SelectOnMapButtonClickEvent)
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
                // A map pick belongs to no query. Recording null clears any session left
                // pending by an earlier search so the trip that follows is not credited
                // to a search the rider abandoned.
                searchSessionStore.recordSelection(searchSessionId = null, stopId = event.stopId)
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
                        assignmentSurface = event.surface.toAnalyticsSurface(),
                        assignmentMode = if (event.isNewLabel) {
                            StopLabelStopAssignedEvent.AssignmentMode.NEW_LABEL
                        } else {
                            StopLabelStopAssignedEvent.AssignmentMode.EXISTING_LABEL
                        },
                        locationKind = when (event.stopItem.locationKind) {
                            LocationKind.TRANSIT_STOP -> StopSelectedEvent.LocationKind.TRANSIT_STOP
                            LocationKind.ADDRESS -> StopSelectedEvent.LocationKind.ADDRESS
                        },
                        labelKind = labelKindOf(event.labelKey),
                        isReassignment = isReassignment,
                    ),
                )
                viewModelScope.launchWithExceptionHandler<SearchStopViewModel>(ioDispatcher) {
                    sandook.updateStopLabelStop(event.labelKey, event.stopItem.stopId, event.stopItem.stopName)
                    // Stops the user pins to a label are also stops they "interacted
                    // with", so surface them in Recents next time the screen opens.
                    sandook.upsertRecentSearchLocation(
                        RecentSearchLocation(
                            locationId = event.stopItem.stopId,
                            displayName = event.stopItem.stopName,
                            kind = event.stopItem.locationKind.name,
                            addressType = event.stopItem.addressType,
                            productClasses = event.stopItem.productClasses(),
                        ),
                    )
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
                                creationSurface = event.surface.toAnalyticsSurface(),
                                labelCountBucket = AnalyticsEvent.StopLabelCountBucket
                                    .from(_uiState.value.stopLabels.size),
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
                        action = StopLabelRemovedEvent.Action.CLEAR,
                        hadStop = hadStop,
                        labelKind = labelKindOf(event.labelKey),
                    ),
                )
                viewModelScope.launchWithExceptionHandler<SearchStopViewModel>(ioDispatcher) {
                    sandook.updateStopLabelStop(event.labelKey, null, null)
                }
            }

            is SearchStopUiEvent.RenameLabel -> {
                // Home can't be renamed — isProtected is name-derived, so renaming it
                // away from "Home" would silently un-protect it. Defence in depth: the
                // UI also hides the rename field for protected labels.
                if (event.labelKey.equals(StopLabel.PROTECTED_LABEL, ignoreCase = true)) {
                    return
                }
                val cleanedName = normaliseLabelName(event.newName)
                val currentLabels = _uiState.value.stopLabels
                val target = currentLabels.firstOrNull { it.label == event.labelKey }
                val duplicate = currentLabels.any {
                    it.label != event.labelKey && normaliseLabelName(it.label).equals(cleanedName, ignoreCase = true)
                }
                if (cleanedName.isBlank() || target == null) return
                if (duplicate || cleanedName == target.label) return
                updateUiState {
                    val updated = stopLabels.map { label ->
                        if (label.label == event.labelKey) label.copy(label = cleanedName) else label
                    }.toImmutableList()
                    copy(stopLabels = updated)
                }
                viewModelScope.launchWithExceptionHandler<SearchStopViewModel>(ioDispatcher) {
                    sandook.renameStopLabel(event.labelKey, cleanedName)
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
                        action = StopLabelRemovedEvent.Action.DELETE,
                        hadStop = hadStop,
                        labelKind = labelKindOf(event.labelKey),
                    ),
                )
                viewModelScope.launchWithExceptionHandler<SearchStopViewModel>(ioDispatcher) {
                    sandook.deleteStopLabel(event.labelKey)
                }
            }

            is SearchStopUiEvent.LabelReorderDragCompleted -> {
                // Only a drag whose final order differs is a reorder; a drag that
                // returns to its start position is a no-op, matching the plan's
                // "fire once per completed changed drag" rule.
                if (event.fromIndex != event.toIndex) {
                    analytics.track(
                        StopLabelReorderedEvent(
                            labelKind = labelKindOf(event.labelKey),
                            moveDistanceBucket = StopLabelReorderedEvent.MoveDistanceBucket
                                .from(abs(event.fromIndex - event.toIndex)),
                            setLabelCountBucket = AnalyticsEvent.StopLabelCountBucket
                                .from(_uiState.value.stopLabels.count { it.isSet }),
                        ),
                    )
                }
            }

            SearchStopUiEvent.ManageLabelsScreenViewed -> {
                analytics.trackScreenViewEvent(screen = AnalyticsScreen.ManageStopLabels)
            }

            is SearchStopUiEvent.MoveLabelToIndex -> {
                val current = _uiState.value.stopLabels.toMutableList()
                val sourceIndex = current.indexOfFirst { it.label == event.labelKey }
                if (sourceIndex == -1) return
                // Resolve the target's index against the list *before* removing the
                // dragged item — inserting at that raw index afterwards is what makes
                // adjacent-item drags swap correctly, and it's immune to any
                // non-draggable rows (banner, headers) the caller's LazyColumn mixes
                // in, since we key off the target label rather than a raw list index.
                val target = current.indexOfFirst { it.label == event.targetLabelKey }
                if (target == -1 || target == sourceIndex) return
                val moved = current.removeAt(sourceIndex)
                current.add(target, moved)
                val reordered = current.toImmutableList()
                updateUiState { copy(stopLabels = reordered) }
                // No analytics here: MoveLabelToIndex fires once per swap mid-drag.
                // LabelReorderDragCompleted reports the finished gesture instead.
                viewModelScope.launchWithExceptionHandler<SearchStopViewModel>(ioDispatcher) {
                    // One transaction per firing, not one write per label — onMove fires
                    // per swap mid-drag, not just once on drop, so an unbatched loop here
                    // is a DB write storm during a single drag gesture.
                    sandook.insertTransaction {
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
    }

    private fun onSearchTextChanged(query: String) {
        // update query in state first
        updateUiState { copy(searchQuery = query) }

        // Blank query -> show recent stops and cancel any ongoing search
        if (query.isBlank()) {
            searchJob?.cancel()
            addressSearchJob?.cancel()
            updateUiState {
                copy(
                    listState = ListState.Recent,
                    addressResults = persistentListOf(),
                    isAddressSearchLoading = false,
                )
            }

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

        currentSearchSessionId = newSearchSessionId()
        val searchSessionId = currentSearchSessionId

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)

            // suspendSafeResult, not runCatching: every keystroke cancels this job, and
            // runCatching swallows the CancellationException it throws - which reported a
            // cancelled keystroke as a failed search, and would now send its text too.
            val stopResults = suspendSafeResult(ioDispatcher) {
                stopResultsManager.fetchStopResults(query)
            }.getOrNull()

            if (stopResults != null) {
                updateUiState { displayData(stopResults) }
            } else {
                updateUiState { displayError() }
            }

            // The screen is now up to date; analytics deliberately is not yet. Sitting out
            // a quiet period first means a burst of typing reports the query the rider
            // finished, not one row per prefix - "4", "4 f", "4 fu" are keystrokes, not
            // searches, and counting them as failed searches skewed every read of the
            // address gate. Cancellation does the work: the next keystroke cancels this
            // job while it waits here.
            delay(SEARCH_ANALYTICS_QUIET_MS)

            if (stopResults != null) {
                analytics.trackLocalSearchResolved(
                    query = query,
                    searchSessionId = searchSessionId,
                    localResultsCount = stopResults.size,
                    addressSearchGate = currentAddressSearchGate(normalizeAddressQuery(query)),
                )
            } else {
                analytics.trackLocalSearchFailed(
                    query = query,
                    searchSessionId = searchSessionId,
                )
            }
        }

        // Independent pipeline: own debounce, own job, own state fields. Local search
        // above is untouched by any of this — a slow/failed remote call can never
        // delay or affect it. When the flag is off, no job is launched at all.
        onAddressSearchTextChanged(query)
    }

    private fun onAddressSearchTextChanged(query: String) {
        addressSearchJob?.cancel()
        val normalizedQuery = normalizeAddressQuery(query)
        val searchSessionId = currentSearchSessionId

        if (currentAddressSearchGate(normalizedQuery) != AddressSearchGate.ELIGIBLE) {
            updateUiState { copy(addressResults = persistentListOf(), isAddressSearchLoading = false) }
            return
        }

        val requestToken = ++addressSearchRequestToken
        addressSearchJob = viewModelScope.launch {
            delay(ADDRESS_SEARCH_DEBOUNCE_MS)

            // A flag flip during the debounce must not fire a now-stale request - re-check
            // the same gate the caller already passed. Clears the section on the way out: a
            // previous query's addresses left on screen under a newly-suppressed query
            // would be worse than showing none.
            if (currentAddressSearchGate(normalizedQuery) != AddressSearchGate.ELIGIBLE) {
                updateUiState {
                    copy(addressResults = persistentListOf(), isAddressSearchLoading = false)
                }
                return@launch
            }

            updateUiState { copy(isAddressSearchLoading = true) }
            // suspendSafeResult, not runCatching: a cancelled coroutine (e.g. this job
            // getting replaced by the next keystroke) must propagate CancellationException
            // rather than have it swallowed into a Result.failure.
            val fetchResult = suspendSafeResult(ioDispatcher) {
                remoteAddressResultsManager.fetchAddressResults(normalizedQuery)
            }
            val addressResults = fetchResult.getOrElse {
                log("Address search failed for query of length ${normalizedQuery.length}: ${it.message}")
                emptyList()
            }

            // A newer keystroke may have started (and even completed) another request
            // while this one was in flight; only the most recent request may still win.
            if (requestToken != addressSearchRequestToken) return@launch
            updateUiState {
                copy(
                    addressResults = addressResults.toImmutableList(),
                    isAddressSearchLoading = false,
                )
            }
            analytics.trackAddressSearchResolved(
                normalizedQuery = normalizedQuery,
                searchSessionId = searchSessionId,
                localResultsCount = _uiState.value.searchResults.size,
                addressResults = if (fetchResult.isSuccess) addressResults else null,
            )
        }
    }

    private fun currentAddressSearchGate(normalizedQuery: String): AddressSearchGate =
        AddressSearchEligibility.evaluate(
            normalizedQuery = normalizedQuery,
            isAddressSearchEnabled = isAddressSearchEnabled(),
            minQueryLength = addressSearchMinQueryLength(),
        )

    private fun StopItem.productClasses(): String =
        when (locationKind) {
            LocationKind.TRANSIT_STOP -> sandook.selectStopsByIds(
                listOf(stopId),
            )
                .firstOrNull()
                ?.productClasses
                .orEmpty()
            LocationKind.ADDRESS -> ""
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

    private companion object {
        /** How long typing must pause before the local stop list is refreshed. */
        const val SEARCH_DEBOUNCE_MS = 100L

        // Longer than local search's 100ms debounce - network round-trip is inherently
        // slower, and there's no benefit to firing it as eagerly as the local DB query.
        const val ADDRESS_SEARCH_DEBOUNCE_MS = 350L

        /**
         * Extra quiet time after the results are on screen before `search_stop_query`
         * fires. Deliberately far longer than [SEARCH_DEBOUNCE_MS]: the screen must keep
         * up with each keystroke, but an event per keystroke is a different thing - it
         * inflates the failed-search count with prefixes of searches that then succeeded,
         * and from 1.27 it would send each of those prefixes' text as well. Long enough to
         * outlast a typing pause, short enough that a rider who types and reads still
         * reports before they move on.
         */
        const val SEARCH_ANALYTICS_QUIET_MS = 600L

        fun newSearchSessionId(): String = Random.nextLong().toULong().toString(radix = 16)
    }
}

/**
 * Zero-based row the selected stop was sitting at, within its own section: the local
 * list for transit stops, the address section below it for addresses. Null when the
 * stop is not on screen at all, which is every selection that did not come from a
 * live query.
 *
 * A stop tapped inside an expanded trip card reports the trip's own row, because that
 * is the row the rider had to reach before the stop was reachable at all.
 */
private fun SearchStopState.resultIndexOf(stopId: String): Int? {
    val localIndex = searchResults.indexOfFirst { result ->
        when (result) {
            is SearchStopState.SearchResult.Stop -> result.stopId == stopId
            is SearchStopState.SearchResult.Trip -> result.stops.any { it.stopId == stopId }
            is SearchStopState.SearchResult.Address -> result.addressId == stopId
        }
    }
    return if (localIndex >= 0) {
        localIndex
    } else {
        addressResults.indexOfFirst { it.addressId == stopId }.takeIf { it >= 0 }
    }
}
