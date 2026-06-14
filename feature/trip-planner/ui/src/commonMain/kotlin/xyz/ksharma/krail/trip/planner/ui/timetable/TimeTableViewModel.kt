package xyz.ksharma.krail.trip.planner.ui.timetable

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone.Companion.currentSystemDefault
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.analytics.event.trackScreenViewEvent
import xyz.ksharma.krail.core.appinfo.KRAIL_WEBSITE_URL
import xyz.ksharma.krail.core.datetime.DateTimeHelper.isBefore
import xyz.ksharma.krail.core.datetime.DateTimeHelper.isFuture
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toApiDateString
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toApiTimeString
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toDepartureRelativeString
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toHHMM
import xyz.ksharma.krail.core.datetime.DateTimeHelper.utcToLocalDateTimeAEST
import xyz.ksharma.krail.core.festival.FestivalManager
import xyz.ksharma.krail.core.festival.model.NoFestival
import xyz.ksharma.krail.core.festival.model.greetingAndEmoji
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.asBoolean
import xyz.ksharma.krail.core.share.ShareManager
import xyz.ksharma.krail.coroutines.ext.launchWithExceptionHandler
import xyz.ksharma.krail.feature.track.TripDeepLinkEncoder
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SelectServiceAlertsByJourneyId
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.network.api.ratelimit.RateLimiter
import xyz.ksharma.krail.trip.planner.network.api.service.DepArr
import xyz.ksharma.krail.trip.planner.network.api.service.TripPlanningService
import xyz.ksharma.krail.trip.planner.ui.state.alerts.ServiceAlert
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.JourneyTimeOptions
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import xyz.ksharma.krail.trip.planner.ui.timetable.business.buildJourneyListWithRawData
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Suppress("LongParameterList", "LargeClass")
class TimeTableViewModel(
    private val tripPlanningService: TripPlanningService,
    private val rateLimiter: RateLimiter,
    private val sandook: Sandook,
    private val analytics: Analytics,
    private val shareManager: ShareManager,
    private val ioDispatcher: CoroutineDispatcher,
    private val festivalManager: FestivalManager,
    val flag: Flag,
    private val tripTrackingDebugOverride: Boolean = true,
) : ViewModel() {

    private val _uiState: MutableStateFlow<TimeTableState> = MutableStateFlow(TimeTableState())
    val uiState: StateFlow<TimeTableState> = _uiState

    private val _isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
        // Will start fetching the trip as soon as the screen is visible, which means if android-app goes
        // to background and come back up again, the API call will be made.
        // Probably good to have data up to date.
        .onStart {
            log("onStart: Fetching Trip")
            updateMapsAvailability()
            updateLoadingEmoji()
            fetchTrip()
            analytics.trackScreenViewEvent(screen = AnalyticsScreen.TimeTable)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(ANR_TIMEOUT), true)

    private val _isActive: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /**
     * Flow that updates time text every 10 seconds while the screen is visible.
     * Uses WhileSubscribed to automatically stop when no collectors are active.
     */
    val isActive: StateFlow<Boolean> = _isActive.onStart {
        while (true) {
            if (_uiState.value.journeyList.isEmpty().not()) {
                updateTimeText()
            }
            delay(REFRESH_TIME_TEXT_DURATION)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIME_TEXT_UPDATES_THRESHOLD.inWholeMilliseconds),
        initialValue = true,
    )

    private val _autoRefreshTimeTable: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /**
     * Flow that auto-refreshes timetable every 30 seconds while the screen is visible.
     * Uses WhileSubscribed to automatically stop when no collectors are active (screen not visible).
     * This ensures we don't make API calls in the background when the user has navigated away.
     */
    val autoRefreshTimeTable: StateFlow<Boolean> = _autoRefreshTimeTable.onStart {
        while (true) {
            val hasJourneys = _uiState.value.journeyList.isEmpty().not()
            val hasError = _uiState.value.isError
            val isFutureDate = dateTimeSelectionItem?.date.isFuture()

            if ((hasJourneys || hasError) && !isFutureDate) {
                rateLimiter.triggerEvent()
            }

            delay(AUTO_REFRESH_TIME_TABLE_DURATION)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIME_TEXT_UPDATES_THRESHOLD.inWholeMilliseconds),
        initialValue = true,
    )

    private val _expandedJourneyId: MutableStateFlow<String?> = MutableStateFlow(null)
    val expandedJourneyId: StateFlow<String?> = _expandedJourneyId

    // Track previous trip to determine if we need to reload data
    private var previousTripId: String? = null

    private var tripInfo: Trip? = null
    private val unselectedModes: MutableSet<Int> = mutableSetOf() // all are selected by default

    @VisibleForTesting
    var dateTimeSelectionItem: DateTimeSelectionItem? = null

    private var fetchTripJob: Job? = null

    private val greetingAndEmoji: Pair<String, String> by lazy {
        (festivalManager.festivalOnDate() ?: NoFestival()).greetingAndEmoji
    }

    private var lastInitializedRouteFromTo: Pair<String, String>? = null

    /**
     * Cache of trips. Key is [TimeTableState.JourneyCardInfo.journeyId] and value is
     * [TimeTableState.JourneyCardInfo].
     *
     * This list will be displayed in the UI.
     */
    @VisibleForTesting
    val journeys: MutableMap<String, TimeTableState.JourneyCardInfo> = mutableMapOf()

    /** Future trips fetched via "Load More". Survives auto-refresh; cleared on trip/time reset. */
    @VisibleForTesting
    val loadMoreJourneys: MutableMap<String, TimeTableState.JourneyCardInfo> = mutableMapOf()

    /** Past trips fetched via "Show Previous". Survives auto-refresh; cleared on trip/time reset. */
    @VisibleForTesting
    val previousJourneysCache: MutableMap<String, TimeTableState.JourneyCardInfo> = mutableMapOf()

    private var loadMoreCount: Int = 0
    private var loadMoreFetchJob: Job? = null
    private var loadPreviousFetchJob: Job? = null

    private val rawJourneyDataByJourneyId: MutableMap<String, TripResponse.Journey> = mutableMapOf()

    private val isMapsAvailable: Boolean by lazy {
        flag.getFlagValue(FlagKeys.JOURNEY_MAPS_AVAILABLE.key)
            .asBoolean(fallback = false)
    }

    init {
        // Single VM-lifetime observer so DB updates always reach the UI even if
        // the screen briefly unsubscribes across config changes. Mirrors
        // SearchStopViewModel's pattern; we deliberately skip the seed-on-empty
        // branch since SearchStopViewModel owns label seeding.
        observeStopLabels()
    }

    /**
     * Get raw journey data by ID for map visualization.
     */
    fun getRawJourneyById(journeyId: String): TripResponse.Journey? =
        rawJourneyDataByJourneyId[journeyId]

    /**
     * Initialize trip from entry.
     * Handles trip loading and ensures ViewModel state stays in sync.
     *
     * IMPORTANT: This method tracks the last initialized route to prevent
     * reinitialization when navigating back from child screens (like JourneyMapScreen).
     * This preserves state when user goes: TimeTable → Map → Back.
     */
    fun initializeTrip(
        fromStopId: String,
        fromStopName: String,
        toStopId: String,
        toStopName: String,
    ) {
        val currentRoute = Pair(fromStopId, toStopId)
        // Check if this is the EXACT SAME route we just initialized
        if (lastInitializedRouteFromTo == currentRoute) {
            log("🗺️ initializeTrip: SKIPPING - Same route already initialized, preserving state!")
            // Don't reinitialize - this is likely navigation back from map
            // State is already correct, no need to call onLoadTimeTable
            return
        }

        // Different route - update tracking and proceed with initialization
        lastInitializedRouteFromTo = currentRoute

        val trip = Trip(
            fromStopId = fromStopId,
            fromStopName = fromStopName,
            toStopId = toStopId,
            toStopName = toStopName,
        )

        log("🗺️ Previous trip: ${tripInfo?.let { "${it.fromStopId} → ${it.toStopId}" } ?: "null"}")
        log("🗺️ New trip: ${trip.fromStopId} → ${trip.toStopId}")
        log("🗺️ Trip changed: ${trip != tripInfo}")

        // Call LoadTimeTable - it will handle logic:
        // - If trip changed: Clear date/time, clear cache, fetch from API
        // - If same trip (rotation/nav back): Preserve state, skip API call
        onLoadTimeTable(trip)
    }

    fun onEvent(event: TimeTableUiEvent) {
        when (event) {
            is TimeTableUiEvent.LoadTimeTable -> onLoadTimeTable(event.trip)

            is TimeTableUiEvent.JourneyCardClicked -> onJourneyCardClicked(event.journeyId)

            TimeTableUiEvent.SaveTripButtonClicked -> onSaveTripButtonClicked()

            TimeTableUiEvent.ReverseTripButtonClicked -> onReverseTripButtonClicked()

            TimeTableUiEvent.RetryButtonClicked -> {
                analytics.track(
                    AnalyticsEvent.RetryApiEvent(
                        source = AnalyticsEvent.RetryApiEvent.Source.TIMETABLE,
                    ),
                )
                onRetry()
            }

            is TimeTableUiEvent.DateTimeSelectionChanged -> {
                onDateTimeSelectionChanged(item = event.dateTimeSelectionItem)
            }

            TimeTableUiEvent.AnalyticsDateTimeSelectorClicked -> {
                analytics.track(
                    AnalyticsEvent.PlanTripClickEvent(
                        fromStopId = tripInfo?.fromStopId ?: "NA",
                        toStopId = tripInfo?.toStopId ?: "NA",
                    ),
                )
            }

            is TimeTableUiEvent.AnalyticsJourneyLegClicked -> {
                analytics.track(
                    AnalyticsEvent.JourneyLegClickEvent(
                        expanded = event.expanded,
                        transportMode = event.transportMode,
                        lineName = event.lineName,
                    ),
                )
            }

            is TimeTableUiEvent.ModeSelectionChanged -> onModeSelectionChanged(event.unselectedModes)

            is TimeTableUiEvent.ModeClicked -> trackOnModeClickEvent(event.displayModeSelectionRow)

            TimeTableUiEvent.BackClick -> trackBackClickEvent()

            is TimeTableUiEvent.ShareJourneyClicked -> onShareJourneyClicked(event)

            TimeTableUiEvent.LoadMoreTrips -> onLoadMoreTrips()

            TimeTableUiEvent.LoadPreviousTrips -> onLoadPreviousTrips()

            is TimeTableUiEvent.OriginDestinationStopHeaderClicked -> {
                analytics.track(
                    AnalyticsEvent.TimeTableStopHeaderClickEvent(
                        stopId = event.stopId,
                        stopName = event.stopName,
                        isOrigin = event.isOrigin,
                        tripFromStopId = tripInfo?.fromStopId.orEmpty(),
                        tripToStopId = tripInfo?.toStopId.orEmpty(),
                    ),
                )
            }
        }
    }

    private fun observeStopLabels() {
        viewModelScope.launchWithExceptionHandler<TimeTableViewModel>(ioDispatcher) {
            sandook.observeStopLabels()
                .distinctUntilChanged()
                .collectLatest { rows ->
                    val labels = rows.map { row ->
                        StopLabel(
                            emoji = row.emoji,
                            label = row.label,
                            stopId = row.stop_id,
                            stopName = row.stop_name,
                        )
                    }.toImmutableList()
                    updateUiState { copy(stopLabels = labels) }
                }
        }
    }

    private fun trackOnModeClickEvent(displayModeSelectionRow: Boolean) {
        analytics.track(
            AnalyticsEvent.ModeClickEvent(
                fromStopId = tripInfo?.fromStopId ?: "NA",
                toStopId = tripInfo?.toStopId ?: "NA",
                displayModeSelectionRow = displayModeSelectionRow,
            ),
        )
    }

    private fun onModeSelectionChanged(unselectedModes: Set<Int>) {
        if (hasModeSelectionChanged(unselectedModes)) {
            this.unselectedModes.clear()
            this.unselectedModes.addAll(unselectedModes)

            // All three caches were built under the old filter. Clear them so no
            // old-mode journeys survive into the re-fetched list. journeys must also
            // be cleared here (not just in updateTripsCache) because startedJourneyList
            // is derived from journeys before the clear, and would re-introduce
            // old-mode trips into the new results otherwise.
            journeys.clear()
            loadMoreJourneys.clear()
            previousJourneysCache.clear()
            // Old-mode raw journey data must not survive into the re-fetched list
            // (this path clears caches inline instead of via resetPaginationCaches()).
            rawJourneyDataByJourneyId.clear()
            loadMoreCount = 0

            // call api
            rateLimiter.triggerEvent()
            updateUiState {
                copy(
                    isLoading = true,
                    unselectedModes = this@TimeTableViewModel.unselectedModes.toImmutableSet(),
                )
            }

            // analytics
            analytics.track(
                AnalyticsEvent.ModeSelectionDoneEvent(
                    fromStopId = tripInfo?.fromStopId ?: "NA",
                    toStopId = tripInfo?.toStopId ?: "NA",
                    unselectedProductClasses = this.unselectedModes,
                ),
            )
        } else {
            // do nothing.
            log("Mode selection not changed")
        }
    }

    private fun hasModeSelectionChanged(unselectedModes: Set<Int>): Boolean {
        log("hasModeSelectionChanged - OLD: ${this.unselectedModes} NEW: $unselectedModes")
        return this.unselectedModes != unselectedModes
    }

    @OptIn(ExperimentalTime::class)
    private fun onDateTimeSelectionChanged(item: DateTimeSelectionItem?) {
        log("DateTimeSelectionChanged: $item")
        // Verify if date time selection has actually changed, otherwise, api will be called unnecessarily.
        if (dateTimeSelectionItem != item) {
            updateUiState { copy(isLoading = true) }
            dateTimeSelectionItem = item
            journeys.clear() // Clear cache trips when date time selection changed.
            resetPaginationCaches()
            sandook.clearAlerts()
            rateLimiter.triggerEvent()

            analytics.track(
                AnalyticsEvent.DateTimeSelectEvent(
                    dayOfWeek = item?.date?.dayOfWeek?.name ?: Clock.System.now()
                        .toLocalDateTime(currentSystemDefault()).dayOfWeek.name,
                    time = item?.toHHMM() ?: Clock.System.now()
                        .toLocalDateTime(currentSystemDefault()).toHHMM(),
                    journeyOption = item?.option?.name ?: JourneyTimeOptions.LEAVE.name,
                    isReset = item == null,
                ),
            )
        }
    }

    @VisibleForTesting
    fun fetchTrip() {
        log("fetchTrip API Call")
        fetchTripJob?.cancel()
        updateUiState { copy(silentLoading = true) }
        fetchTripJob = viewModelScope.launch(ioDispatcher) {
            rateLimiter.rateLimitFlow {
                log("rateLimitFlow block obj:$rateLimiter and coroutine: $this")
                updateUiState { copy(silentLoading = true) }
                loadTrip()
            }.catch { e ->
                log("Error while fetching trip: $e")
            }.collectLatest { result ->
                handleTripResult(result)
            }
        }
    }

    /**
     * Applies a [loadTrip] result to UI state. Shared by the rate-limited [fetchTrip] auto-refresh
     * path and the immediate [onRetry] path.
     */
    private suspend fun handleTripResult(result: Result<TripResponse>) {
        updateUiState { copy(silentLoading = false) }
        result.onSuccess { response ->
            updateTripsCache(response)
            updateUiStateWithFilteredTrips()
        }.onFailure {
            // fetchTrip() runs on every screen-visible (onStart) and every auto-refresh.
            // A failure on one of those silent/background refreshes must NOT blow away
            // journeys already on screen, otherwise returning to the app or a flaky
            // 30s refresh flips the whole screen to the error state. Only surface the
            // full error screen when there is nothing to show.
            val hasData = _uiState.value.journeyList.isNotEmpty()
            // Track the error screen as a screen view, but only on the transition
            // into the error state (not on every failed auto-refresh while errored).
            if (!hasData && !_uiState.value.isError) {
                analytics.trackScreenViewEvent(screen = AnalyticsScreen.TimeTableError)
            }
            updateUiState { copy(isLoading = false, isError = !hasData) }
        }
    }

    @OptIn(ExperimentalTime::class)
    @VisibleForTesting
    suspend fun updateTripsCache(response: TripResponse) = withContext(ioDispatcher) {
        val (newJourneyList, newRawDataMap) = response.buildJourneyListWithRawData()

        // Update raw journey data map
        rawJourneyDataByJourneyId.putAll(newRawDataMap)

        val startedJourneyList = journeys.values
            .filter {
                // Find list of journeys that have started.
                it.hasJourneyStarted
            }
            .filterNot {
                // If a journey has ended then remove it from the cache.
                // This is to avoid displaying ended journeys.
                // The threshold time is set to 10 minutes.
                val thresholdTime = Clock.System.now().minus(JOURNEY_ENDED_CACHE_THRESHOLD_TIME)
                Instant.parse(it.destinationUtcDateTime).isBefore(thresholdTime)
            }
            .filterNot {
                // Those trips that are started / saved in cache but still part of new api data.
                newJourneyList?.any { newJourney -> newJourney.journeyId == it.journeyId } == true
            }
            .sortedBy {
                // Sort by chronological order, from earliest to latest
                Instant.parse(it.originUtcDateTime)
            }
            .takeLast(MAX_STARTED_JOURNEY_DISPLAY_THRESHOLD)

        // Clear all journeys and re-create using started trips(cache) and new api data.
        journeys.clear()
        newJourneyList?.associateBy { it.journeyId }?.let { journeys.putAll(it) }
        journeys.putAll(startedJourneyList.associateBy { it.journeyId })

        startedJourneyList.forEach {
            log(
                "TripsCache - Started tripCode:[${it.journeyId}], Time: ${
                    it.originUtcDateTime.utcToLocalDateTimeAEST().toHHMM()
                }",
            )
        }
        newJourneyList?.forEach {
            log(
                "TripsCache - New tripCode:[${it.journeyId}], Time: ${
                    it.originUtcDateTime.utcToLocalDateTimeAEST().toHHMM()
                }",
            )
        }

        // Prune load-more cache: remove any entry whose departure time falls on or before
        // the latest trip returned by this fresh auto-refresh. This guarantees that cancelled
        // or rescheduled future trips eventually fall off rather than haunting the list.
        // Trips still beyond the fresh window are untouched — they are genuinely future.
        // See: feature/trip-planner/ui/docs/timetable_cache_architecture.md § Staleness Pruning
        pruneStaleLoadMoreEntries()
    }

    @OptIn(ExperimentalTime::class)
    @VisibleForTesting
    fun pruneStaleLoadMoreEntries() {
        val latestFreshInstant = journeys.values
            .maxByOrNull { Instant.parse(it.originUtcDateTime) }
            ?.let { Instant.parse(it.originUtcDateTime) }
            ?: return

        val staleKeys = loadMoreJourneys
            .filterValues { Instant.parse(it.originUtcDateTime) <= latestFreshInstant }
            .keys
            .toList()

        staleKeys.forEach {
            log("TripsCache - Pruned stale load-more trip: $it")
            loadMoreJourneys.remove(it)
        }
    }

    /**
     * Drops journeys that use any de-selected transport mode.
     *
     * The NSW trip API does not reliably honour the `exclMOT` exclusion params we send — it
     * still returns excluded modes (e.g. de-selecting Train still yields train journeys). We
     * therefore filter defensively on the client so the user's mode selection is always
     * respected. A multi-modal journey is dropped if ANY of its legs uses an excluded mode.
     *
     * Footpath/walk (productClass 99) is not a selectable mode and never appears in
     * [TimeTableState.JourneyCardInfo.transportModeLines], so it is never excluded.
     */
    private fun List<TimeTableState.JourneyCardInfo>.excludeUnselectedModes(): List<TimeTableState.JourneyCardInfo> =
        if (unselectedModes.isEmpty()) {
            this
        } else {
            filterNot { journey ->
                journey.transportModeLines.any { it.transportMode.productClass in unselectedModes }
            }
        }

    private fun updateUiStateWithFilteredTrips() {
        // Main list = current-window journeys + load-more (future) journeys, sorted chronologically.
        val mergedJourneys = (journeys.values + loadMoreJourneys.values)
            .distinctBy { it.journeyId }
        val journeyList = updateJourneyCardInfoTimeText(mergedJourneys)
            .excludeUnselectedModes()
            .sortedBy { it.originUtcDateTime.utcToLocalDateTimeAEST() }
            .toImmutableList()

        // Previous list = past journeys fetched via "Show Previous".
        val previousJourneyList = updateJourneyCardInfoTimeText(previousJourneysCache.values.toList())
            .excludeUnselectedModes()
            .sortedBy { it.originUtcDateTime.utcToLocalDateTimeAEST() }
            .toImmutableList()

        val trackingEnabled = tripTrackingDebugOverride
        val deepLinkUrls = tripInfo?.let { trip ->
            journeyList.mapNotNull { journey ->
                val url = if (trackingEnabled) {
                    TripDeepLinkEncoder.encode(
                        fromStopId = trip.fromStopId,
                        fromStopName = trip.fromStopName,
                        toStopId = trip.toStopId,
                        toStopName = trip.toStopName,
                        departureUtcDateTime = journey.originUtcDateTime,
                        legs = journey.legs,
                        excludedProductClasses = unselectedModes,
                    ) ?: return@mapNotNull null
                } else {
                    KRAIL_WEBSITE_URL
                }
                journey.journeyId to url
            }.toMap().toImmutableMap()
        }
        log("[SHARE] deep link URLs computed — ${deepLinkUrls?.size ?: 0} journeys encoded for sharing")

        // The API returned journeys but the user's mode selection filtered them all out.
        // Distinct from "API returned nothing" so the UI can show a mode-specific hint
        // instead of the generic "no route found" message.
        val emptyDueToModeFilter = unselectedModes.isNotEmpty() &&
            mergedJourneys.isNotEmpty() && journeyList.isEmpty()

        updateUiState {
            copy(
                isLoading = false,
                journeyList = journeyList,
                previousJourneyList = previousJourneyList,
                isError = false,
                emptyDueToModeFilter = emptyDueToModeFilter,
                deepLinkUrls = deepLinkUrls ?: this.deepLinkUrls,
                canLoadMore = PAGINATION_ENABLED && journeyList.isNotEmpty() &&
                    loadMoreCount < MAX_LOAD_MORE_COUNT,
                paginationEnabled = PAGINATION_ENABLED,
            )
        }
    }

    private suspend fun loadTrip(): Result<TripResponse> = withContext(ioDispatcher) {
        log("loadTrip API Call")
        require(
            tripInfo != null && tripInfo!!.fromStopId.isNotEmpty() && tripInfo!!.toStopId.isNotEmpty(),
        ) { "Trip Info is null or empty" }

        runCatching {
            val tripResponse = tripPlanningService.trip(
                originStopId = tripInfo!!.fromStopId,
                destinationStopId = tripInfo!!.toStopId,
                date = dateTimeSelectionItem?.toYYYYMMDD(),
                time = dateTimeSelectionItem?.toHHMM(),
                depArr = when (dateTimeSelectionItem?.option) {
                    JourneyTimeOptions.LEAVE -> DepArr.DEP
                    JourneyTimeOptions.ARRIVE -> DepArr.ARR
                    else -> DepArr.DEP
                },
                excludeProductClassSet = unselectedModes,
            )
            Result.success(tripResponse)
        }.getOrElse { error ->
            Result.failure(error)
        }
    }

    /** Clears load-more and previous-trip caches and resets the pagination counter. */
    private fun resetPaginationCaches() {
        loadMoreJourneys.clear()
        previousJourneysCache.clear()
        // Raw journey data backs the journey map. Every caller of this also clears
        // `journeys` and triggers a re-fetch (which repopulates initial raw data via
        // updateTripsCache), so clearing here prevents the map from leaking or serving
        // stale load-more/previous journeys after a date-time or reverse-trip reset.
        rawJourneyDataByJourneyId.clear()
        loadMoreCount = 0
        loadMoreFetchJob?.cancel()
        loadPreviousFetchJob?.cancel()
    }

    /**
     * Fetches the next page of future trips starting just after the last shown departure.
     * Results are merged into [loadMoreJourneys] so auto-refresh cannot overwrite them.
     */
    @OptIn(ExperimentalTime::class)
    private fun onLoadMoreTrips() {
        trackLoadMoreClick()
        if (loadMoreCount >= MAX_LOAD_MORE_COUNT) return
        val allJourneys = (journeys.values + loadMoreJourneys.values)
        val lastInstant = allJourneys
            .maxByOrNull { Instant.parse(it.originUtcDateTime) }
            ?.let { Instant.parse(it.originUtcDateTime) }
            ?: return

        val fromInstant = lastInstant.plus(1.minutes)
        val date = fromInstant.toApiDateString()
        val time = fromInstant.toApiTimeString()

        updateUiState { copy(isLoadingMore = true) }
        loadMoreFetchJob?.cancel()
        loadMoreFetchJob = viewModelScope.launch(ioDispatcher) {
            loadTripAtTime(date = date, time = time).onSuccess { response ->
                val (newJourneys, newRawDataMap) = response.buildJourneyListWithRawData()
                // Keep raw journey data so the journey map can resolve coordinates for
                // load-more journeys via getRawJourneyById(). Without this the map is empty.
                rawJourneyDataByJourneyId.putAll(newRawDataMap)
                newJourneys?.forEach { journey ->
                    if (!loadMoreJourneys.containsKey(journey.journeyId) &&
                        !journeys.containsKey(journey.journeyId)
                    ) {
                        loadMoreJourneys[journey.journeyId] = journey
                    }
                }
                loadMoreCount++
                updateUiStateWithFilteredTrips()
            }.onFailure {
                logError("Load more trips failed", it)
            }
            updateUiState { copy(isLoadingMore = false) }
        }
    }

    /**
     * Fetches past trips in the [PREVIOUS_TRIPS_WINDOW_MINUTES] window before the earliest
     * currently shown departure. Results are stored in [previousJourneysCache].
     */
    @OptIn(ExperimentalTime::class)
    private fun onLoadPreviousTrips() {
        trackLoadPreviousClick()
        val allJourneys = (previousJourneysCache.values + journeys.values + loadMoreJourneys.values)
        val firstInstant = allJourneys
            .minByOrNull { Instant.parse(it.originUtcDateTime) }
            ?.let { Instant.parse(it.originUtcDateTime) }
            ?: return

        val windowStart = firstInstant.minus(PREVIOUS_TRIPS_WINDOW_MINUTES.minutes)
        val date = windowStart.toApiDateString()
        val time = windowStart.toApiTimeString()

        updateUiState { copy(isLoadingPrevious = true) }
        loadPreviousFetchJob?.cancel()
        loadPreviousFetchJob = viewModelScope.launch(ioDispatcher) {
            loadTripAtTime(date = date, time = time).onSuccess { response ->
                val (newJourneys, newRawDataMap) = response.buildJourneyListWithRawData()
                // Keep raw journey data so the journey map can resolve coordinates for
                // previous journeys via getRawJourneyById(). Without this the map is empty.
                rawJourneyDataByJourneyId.putAll(newRawDataMap)
                newJourneys
                    ?.filter { Instant.parse(it.originUtcDateTime).isBefore(firstInstant) }
                    ?.forEach { journey ->
                        previousJourneysCache[journey.journeyId] = journey
                    }
                updateUiStateWithFilteredTrips()
            }.onFailure {
                logError("Load previous trips failed", it)
            }
            updateUiState { copy(isLoadingPrevious = false) }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun trackLoadMoreClick() {
        val info = tripInfo ?: return
        val visibleList = _uiState.value.journeyList
        val now = Clock.System.now()
        val latestMinutes = visibleList
            .mapNotNull { runCatching { Instant.parse(it.originUtcDateTime) }.getOrNull() }
            .maxOrNull()
            ?.let { (it - now).inWholeMinutes.toInt() }
            ?: 0
        analytics.track(
            AnalyticsEvent.TimeTableLoadMoreClickEvent(
                fromStopId = info.fromStopId,
                toStopId = info.toStopId,
                loadMoreCount = loadMoreCount,
                isCustomDateTime = dateTimeSelectionItem != null,
                latestVisibleDepartureMinutesFromNow = latestMinutes,
                visibleJourneyCount = visibleList.size,
            ),
        )
    }

    @OptIn(ExperimentalTime::class)
    private fun trackLoadPreviousClick() {
        val info = tripInfo ?: return
        val visibleList = _uiState.value.journeyList
        val now = Clock.System.now()
        val earliestMinutes = visibleList
            .mapNotNull { runCatching { Instant.parse(it.originUtcDateTime) }.getOrNull() }
            .minOrNull()
            ?.let { (it - now).inWholeMinutes.toInt() }
            ?: 0
        analytics.track(
            AnalyticsEvent.TimeTableLoadPreviousClickEvent(
                fromStopId = info.fromStopId,
                toStopId = info.toStopId,
                isCustomDateTime = dateTimeSelectionItem != null,
                earliestVisibleDepartureMinutesFromNow = earliestMinutes,
                visibleJourneyCount = visibleList.size,
            ),
        )
    }

    /** Like [loadTrip] but uses explicit [date]/[time] params instead of [dateTimeSelectionItem]. */
    private suspend fun loadTripAtTime(
        date: String,
        time: String,
        depArr: DepArr = DepArr.DEP,
    ): Result<TripResponse> = withContext(ioDispatcher) {
        require(
            tripInfo != null && tripInfo!!.fromStopId.isNotEmpty() && tripInfo!!.toStopId.isNotEmpty(),
        ) { "Trip Info is null or empty" }
        runCatching {
            tripPlanningService.trip(
                originStopId = tripInfo!!.fromStopId,
                destinationStopId = tripInfo!!.toStopId,
                date = date,
                time = time,
                depArr = depArr,
                excludeProductClassSet = unselectedModes,
            )
        }.mapCatching { it }
    }

    private fun onSaveTripButtonClicked() {
        log("Save Trip Button Clicked")
        viewModelScope.launch(ioDispatcher) {
            analytics.track(
                AnalyticsEvent.SaveTripClickEvent(
                    fromStopId = tripInfo?.fromStopId ?: "NA",
                    toStopId = tripInfo?.toStopId ?: "NA",
                ),
            )

            tripInfo?.let { trip ->
                log("Toggle Save Trip: $trip")
                val savedTrip = sandook.selectTripById(tripId = trip.tripId)
                if (savedTrip != null) {
                    // Trip is already saved, so delete it
                    sandook.deleteTrip(tripId = trip.tripId)
                    log("Deleted Trip (Pref): $savedTrip")
                    updateUiState { copy(isTripSaved = false) }
                } else {
                    // Trip is not saved, so save it
                    sandook.insertOrReplaceTrip(
                        tripId = trip.tripId,
                        fromStopId = trip.fromStopId,
                        fromStopName = trip.fromStopName,
                        toStopId = trip.toStopId,
                        toStopName = trip.toStopName,
                    )
                    log("Saved Trip (Pref): $trip")
                    updateUiState { copy(isTripSaved = true) }
                }
            }
        }
    }

    private fun onJourneyCardClicked(journeyId: String) {
        val journey = journeys[journeyId]
        val hasJourneyStarted = journey?.hasJourneyStarted ?: false
        val legCount = journey?.legs?.size ?: 0
        val transportModes = journey?.transportModeLines
            ?.map { it.transportMode.productClass }
            ?.sorted()
            ?.joinToString(",")
            .orEmpty()
        val expandedJourneyId = _expandedJourneyId.value
        log("Journey Card Clicked(JourneyId): $journeyId")
        _expandedJourneyId.update { if (it == journeyId) null else journeyId }
        val expanding = expandedJourneyId != journeyId
        analytics.trackJourneyCardToggleEvent(
            expanded = expanding,
            hasStarted = hasJourneyStarted,
            legCount = legCount,
            transportModes = transportModes,
        )
    }

    /**
     * Re-runs the current request after a failure. Unlike [onLoadTimeTable], retry must NOT
     * reset trip params: it preserves the selected date/time (Leave at / Arrive by) and the
     * mode filter, then simply re-fetches. Routing retry through onLoadTimeTable used to null
     * dateTimeSelectionItem whenever the VM had no previousTripId yet (e.g. a fresh VM after
     * process death or re-navigation), so retry silently fell back to "now".
     */
    private fun onRetry() {
        if (tripInfo == null) return
        log("onRetry: immediate fetch, bypassing rate limiter")
        // Show the loading screen right away.
        updateUiState { copy(isLoading = true, isError = false) }
        // Retry must hit the API IMMEDIATELY. Routing through fetchTrip()'s rateLimitFlow
        // debounces the call against the shared 30s auto-refresh trigger, so the user could
        // wait up to a full refresh interval before anything happens. Call loadTrip() directly.
        fetchTripJob?.cancel()
        fetchTripJob = viewModelScope.launch(ioDispatcher) {
            updateUiState { copy(silentLoading = true) }
            handleTripResult(loadTrip())
        }
    }

    private fun onLoadTimeTable(trip: Trip) {
        log("🗺️ onLoadTimeTable -- fromStopId: ${trip.fromStopId}, toStopId: ${trip.toStopId}")

        // Check if this is the same trip or a different one
        val currentTripId = trip.tripId
        val isSameTrip = previousTripId == currentTripId

        log("🗺️ onLoadTimeTable -- previousTripId: $previousTripId")
        log("🗺️ onLoadTimeTable -- currentTripId: $currentTripId")
        log("🗺️ onLoadTimeTable -- isSameTrip: $isSameTrip")
        log("🗺️ onLoadTimeTable -- Current journey count: ${journeys.size}")

        tripInfo = trip
        val savedTrip = sandook.selectTripById(tripId = trip.tripId)

        if (!isSameTrip) {
            // Different trip - clear state and fetch new data
            log("🗺️ onLoadTimeTable -- Different trip, clearing cache and fetching")
            dateTimeSelectionItem = null
            journeys.clear()
            resetPaginationCaches()
            rawJourneyDataByJourneyId.clear()

            updateUiState {
                copy(
                    isLoading = true,
                    trip = trip,
                    isTripSaved = savedTrip != null,
                    isError = false,
                )
            }

            rateLimiter.triggerEvent()
        } else {
            // Same trip - preserve state, no API call
            log("🗺️ onLoadTimeTable -- Same trip, preserving state with ${journeys.size} journeys")
            updateUiState {
                copy(
                    trip = trip,
                    isTripSaved = savedTrip != null,
                )
            }
        }

        previousTripId = currentTripId
    }

    private fun onReverseTripButtonClicked() {
        log("Reverse Trip Button Clicked -- Trigger")
        require(tripInfo != null) { "Trip Info is null" }
        val reverseTrip = Trip(
            fromStopId = tripInfo!!.toStopId,
            fromStopName = tripInfo!!.toStopName,
            toStopId = tripInfo!!.fromStopId,
            toStopName = tripInfo!!.fromStopName,
        )
        tripInfo = reverseTrip
        journeys.clear() // Clear cache trips when reverse trip is clicked.
        resetPaginationCaches()
        sandook.clearAlerts() // Clear alerts cache when reverse trip is clicked.

        analytics.track(
            AnalyticsEvent.ReverseTimeTableClickEvent(
                fromStopId = tripInfo!!.fromStopId,
                toStopId = tripInfo!!.toStopId,
            ),
        )

        val savedTrip = sandook.selectTripById(tripId = reverseTrip.tripId)
        updateUiState {
            copy(
                trip = reverseTrip,
                isTripSaved = savedTrip != null,
                isLoading = true,
                isError = false,
            )
        }
        rateLimiter.triggerEvent()
    }

    /**
     * As the clock is progressing, the value [TimeTableState.JourneyCardInfo.timeText] of the
     * journey card should be updated.
     */
    private fun updateTimeText() = viewModelScope.launch {
        val updatedJourneyList = withContext(ioDispatcher) {
            updateJourneyCardInfoTimeText(_uiState.value.journeyList).toImmutableList()
        }
        val updatedPreviousJourneyList = withContext(ioDispatcher) {
            updateJourneyCardInfoTimeText(_uiState.value.previousJourneyList).toImmutableList()
        }
        updateUiState {
            copy(
                journeyList = updatedJourneyList,
                previousJourneyList = updatedPreviousJourneyList,
            )
        }
    }

    /**
     * Update the [TimeTableState.JourneyCardInfo.timeText] of the journey card.
     * This will be called when the screen is visible and the time is progressing and also when the
     * API returned with new data.
     */
    @OptIn(ExperimentalTime::class)
    private fun updateJourneyCardInfoTimeText(
        journeyList: List<TimeTableState.JourneyCardInfo>,
    ): List<TimeTableState.JourneyCardInfo> {
        return journeyList.map { journeyCardInfo ->
            journeyCardInfo.copy(
                timeText = journeyCardInfo.originUtcDateTime.toDepartureRelativeString(),
            )
        }
    }

    private inline fun updateUiState(block: TimeTableState.() -> TimeTableState) {
        _uiState.update(block)
    }

    fun fetchAlertsForJourney(journeyId: String, onResult: (List<ServiceAlert>) -> Unit) {
        viewModelScope.launch {
            val alerts = withContext(ioDispatcher) {
                runCatching {
                    _uiState.value.journeyList.find { it.journeyId == journeyId }?.let { journey ->
                        getAlertsFromJourney(journey)
                    }
                }.getOrElse {
                    logError("Error while fetching alerts for journey: $journeyId", it)
                    emptyList()
                }
            }
            if (alerts?.isNotEmpty() == true) {
                analytics.track(
                    AnalyticsEvent.JourneyAlertClickEvent(
                        fromStopId = tripInfo?.fromStopId ?: "NA",
                        toStopId = tripInfo?.toStopId ?: "NA",
                    ),
                )
            }
            onResult(alerts ?: emptyList())
        }
    }

    private fun getAlertsFromJourney(journey: TimeTableState.JourneyCardInfo): List<ServiceAlert> {
        val alerts =
            journey.legs.filterIsInstance<TimeTableState.JourneyCardInfo.Leg.TransportLeg>()
                .flatMap { it.serviceAlertList.orEmpty() }

        sandook.insertAlerts(
            journeyId = journey.journeyId,
            alerts = alerts.map { it.toSelectServiceAlertsByJourneyId(journey.journeyId) },
        )
        return alerts
    }

    @Suppress("MagicNumber")
    @OptIn(ExperimentalTime::class)
    private fun onShareJourneyClicked(event: TimeTableUiEvent.ShareJourneyClicked) {
        viewModelScope.launch {
            journeys[event.journeyId]?.let { journey ->
                val originInstant = Instant.parse(journey.originUtcDateTime)
                val destinationInstant = Instant.parse(journey.destinationUtcDateTime)
                val deepLinkUrl = uiState.value.deepLinkUrls[event.journeyId]
                log(
                    "[SHARE] journey share clicked — " +
                        "journeyId=${event.journeyId}, " +
                        "from=${tripInfo?.fromStopName} → to=${tripInfo?.toStopName}, " +
                        "depUtc=${journey.originUtcDateTime}, " +
                        "modes=${journey.transportModeLines.joinToString { it.lineName }}, " +
                        "legs=${journey.legs.size}, " +
                        "deepLinkUrl=${deepLinkUrl ?: "null (not yet encoded)"}, " +
                        "shareText=${event.shareText.take(80)}…",
                )
                analytics.trackShareJourneyClickEvent(
                    transportModeLines = journey.transportModeLines,
                    legCount = journey.legs.size,
                    originInstant = originInstant,
                    travelDuration = destinationInstant - originInstant,
                    isPastDeparture = event.isPastDeparture,
                )
            } ?: log("[SHARE] journey share clicked — journeyId=${event.journeyId} not found in cache")
            shareManager.shareImage(bitmap = event.bitmap, text = event.shareText)
                .onFailure { error ->
                    logError("error while sharing image", error)
                }
        }
    }

    private fun trackBackClickEvent() {
        analytics.track(
            AnalyticsEvent.BackClickEvent(fromScreen = AnalyticsScreen.TimeTable),
        )
    }

    private fun updateMapsAvailability() {
        updateUiState { copy(isMapsAvailable = this@TimeTableViewModel.isMapsAvailable) }
    }

    private fun updateLoadingEmoji() {
        updateUiState {
            copy(
                loadingEmoji = TimeTableState.LoadingEmoji(
                    emoji = greetingAndEmoji.second,
                    greeting = greetingAndEmoji.first,
                ),
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        log("TimeTableViewModel cleared")
        cleanupJobs()
    }

    /**
     * Cleanup jobs and resources.
     * Called when ViewModel is cleared (onCleared).
     *
     * Note: This is NOT called when user just navigates away from the screen.
     * The flows (isActive, autoRefreshTimeTable) use WhileSubscribed to automatically
     * stop when the screen is not visible, so they don't make API calls in background.
     */
    @VisibleForTesting
    fun cleanupJobs() {
        sandook.clearAlerts()
        fetchTripJob?.cancel()
        fetchTripJob = null
        loadMoreFetchJob?.cancel()
        loadMoreFetchJob = null
        loadPreviousFetchJob?.cancel()
        loadPreviousFetchJob = null
        log("Cleanup jobs completed")
    }

    companion object {
        private const val ANR_TIMEOUT = 5000L

        @VisibleForTesting
        val REFRESH_TIME_TEXT_DURATION = 10.seconds

        @VisibleForTesting
        val AUTO_REFRESH_TIME_TABLE_DURATION = 30.seconds
        private val STOP_TIME_TEXT_UPDATES_THRESHOLD = 3.seconds

        /**
         * Maximum number of started journeys to display.
         */
        // TODO - UT - at-least these many should remain in past all the time once initial
        //  past trips are starting to show.
        private const val MAX_STARTED_JOURNEY_DISPLAY_THRESHOLD = 2

        /**
         * How long to keep displaying a past journey after it has ended.
         */
        @VisibleForTesting
        val JOURNEY_ENDED_CACHE_THRESHOLD_TIME = 10.minutes

        /** How many "Load More" taps are allowed per session before the button is hidden. */
        @VisibleForTesting
        const val MAX_LOAD_MORE_COUNT = 3

        /** How many minutes before the first shown trip the "Show Previous" window covers. */
        @VisibleForTesting
        val PREVIOUS_TRIPS_WINDOW_MINUTES = 60L

        /**
         * Set to false to hide Show-Previous / Load-More UI globally.
         * Replace with a remote flag lookup when ready.
         */
        const val PAGINATION_ENABLED = true
    }
}

private fun ServiceAlert.toSelectServiceAlertsByJourneyId(journeyId: String) =
    SelectServiceAlertsByJourneyId(
        journeyId = journeyId,
        heading = heading,
        message = message,
    )
