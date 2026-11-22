package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingCommand
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.analytics.event.trackScreenViewEvent
import xyz.ksharma.krail.core.appreview.AppReviewManager
import xyz.ksharma.krail.core.appreview.DelightMoment
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.core.maps.data.repository.NearbyStopsRepository
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.asBoolean
import xyz.ksharma.krail.core.remoteconfig.flag.asNumber
import xyz.ksharma.krail.coroutines.ext.launchWithExceptionHandler
import xyz.ksharma.krail.feature.track.TrackedJourney
import xyz.ksharma.krail.feature.track.TrackingManager
import xyz.ksharma.krail.info.tile.network.api.InfoTileManager
import xyz.ksharma.krail.park.ride.network.NswParkRideFacilityManager
import xyz.ksharma.krail.park.ride.network.model.NswParkRideFacility
import xyz.ksharma.krail.park.ride.network.model.ParkingStopBatchResponse
import xyz.ksharma.krail.park.ride.network.service.ParkRideService
import xyz.ksharma.krail.platform.ops.PlatformOps
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource.SavedTrips
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource.UserAdded
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.sandook.db.NSWParkRideFacilityDetail
import xyz.ksharma.krail.sandook.db.SavedTrip
import xyz.ksharma.krail.trip.planner.ui.searchstop.SearchSessionStore
import xyz.ksharma.krail.trip.planner.ui.searchstop.StopResultsManager
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.LabelChipTap
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ReorderSession
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopState
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import kotlin.time.Clock.System
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Suppress("LongParameterList")
class SavedTripsViewModel(
    private val sandook: Sandook,
    internal val analytics: Analytics,
    internal val ioDispatcher: CoroutineDispatcher,
    private val nswParkRideFacilityManager: NswParkRideFacilityManager,
    private val parkRideService: ParkRideService,
    private val parkRideSandook: NswParkRideSandook,
    private val stopResultsManager: StopResultsManager,
    private val searchSessionStore: SearchSessionStore,
    private val flag: Flag,
    private val preferences: SandookPreferences,
    internal val infoTileManager: InfoTileManager,
    internal val platformOps: PlatformOps,
    private val inviteFriendsTileManager: InviteFriendsTileManager,
    private val trackingManager: TrackingManager,
    private val appReviewManager: AppReviewManager,
    private val nearbyStopsRepository: NearbyStopsRepository,
) : ViewModel() {

    init {
        log("StopItem - SavedTripsViewModel created")
    }

    private val nonPeakTimeCooldownSeconds: Long by lazy {
        flag.getFlagValue(FlagKeys.NSW_PARK_RIDE_NON_PEAK_TIME_COOLDOWN.key)
            .asNumber(600)
    }

    private val peakTimeCooldownSeconds: Long by lazy {
        flag.getFlagValue(FlagKeys.NSW_PARK_RIDE_PEAK_TIME_COOLDOWN.key)
            .asNumber(fallback = 120)
    }

    private val isDiscoverAvailable: Boolean by lazy {
        flag.getFlagValue(FlagKeys.DISCOVER_AVAILABLE.key).asBoolean(false)
    }

    /**
     * Will observe saved trips from the database.
     */
    private var observeSavedTripsJob: Job? = null

    /**
     * Will observe park ride facilities from the database.
     */
    private var observeParkRideFacilityFromDatabaseJob: Job? = null

    // Keeps the numbers on open Park & Ride cards moving. See pollExpandedParkRideStops.
    private var parkRidePollJob: Job? = null

    // Asks for the numbers of every saved station once when home opens. See refreshSavedParkRidesOnOpen.
    private var parkRideOpenRefreshJob: Job? = null

    // One fetch pass at a time. Opening home and tapping a tile can land together, and the cooldown
    // stamp is only written when a reading comes back, so without this both would see the same
    // stale stamp and both would call the API for the same facility.
    private val parkRideFetchMutex = Mutex()

    val trackedJourney: StateFlow<TrackedJourney?> = trackingManager.tracked

    // How many collectors uiState has RIGHT NOW, with no grace period.
    //
    // stateIn keeps its own subscriber count to itself, and the count that is reachable —
    // _uiState.subscriptionCount — reads 1 for a further SUBSCRIPTION_GRACE_MS after the last
    // collector has gone. That padding is deliberate and correct for keeping the database
    // observation warm across a rotation; it is the wrong answer for "is the rider looking at
    // Saved Trips", which is what the review moment below has to know.
    private val screenCollectors = MutableStateFlow(0)

    // WhileSubscribed with the real subscriber count mirrored into screenCollectors on its way
    // past. The counting stream never emits a command of its own; merging it just keeps it
    // collected for as long as the sharing coroutine lives.
    private val whileScreenSubscribed = SharingStarted { subscriptionCount ->
        merge(
            SharingStarted.WhileSubscribed(SUBSCRIPTION_GRACE_MS).command(subscriptionCount),
            subscriptionCount.transform<Int, SharingCommand> { screenCollectors.value = it },
        )
    }

    private val _uiState: MutableStateFlow<SavedTripsState> = MutableStateFlow(SavedTripsState())
    val uiState: StateFlow<SavedTripsState> = _uiState
        .onStart {
            analytics.trackScreenViewEvent(screen = AnalyticsScreen.SavedTrips)
            observeSavedTrips()
            observeStopLabels()
            seedDefaultLabelsIfEmpty()
            observeFacilityDetailsFromDb()
            refreshSavedParkRidesOnOpen()
            pollExpandedParkRideStops()
            updateDiscoverState()
            updateInfoTilesUiState()
            updateSelectedStops()
            // A chip tap, or a reorder, left over from before the screen was last away is stale and
            // was never saved: the rider backgrounded the app mid edit, and that is a cancel.
            updateUiState { copy(labelChipTap = LabelChipTap.Idle, reorder = null) }
            updateInviteFriendsTileSeenState()
            loadReorderTipSeenState()
            maybeRequestReview()
        }
        .onCompletion {
            cleanupJobs()
        }
        .stateIn(viewModelScope, whileScreenSubscribed, SavedTripsState())

    private fun updateSelectedStops() {
        updateUiState {
            copy(
                fromStop = stopResultsManager.selectedFromStop,
                toStop = stopResultsManager.selectedToStop,
            )
        }
    }

    fun markInviteFriendsTileAsSeen() {
        // Manager handles both saving to preferences and updating the cached value
        // State is not updated immediately to prevent animation during current session
        // State will be loaded from cache (which reflects the preference) on next app launch
        inviteFriendsTileManager.markAsSeen()
    }

    fun onEvent(event: SavedTripUiEvent) {
        when (event) {
            is SavedTripUiEvent.DeleteSavedTrip -> onDeleteSavedTrip(event.trip)
            is SavedTripUiEvent.AnalyticsSavedTripCardClick -> onSavedTripCardClick(event)
            SavedTripUiEvent.ReverseStopClick -> {
                stopResultsManager.reverseSelectedStops()
                updateSelectedStops()
                analytics.track(AnalyticsEvent.ReverseStopClickEvent)
            }
            is SavedTripUiEvent.AnalyticsLoadTimeTableClick ->
                analytics.trackLoadTimeTableClick(
                    fromStopId = event.fromStopId,
                    toStopId = event.toStopId,
                    searchSessionId = searchSessionStore.consumeFor(
                        fromStopId = event.fromStopId,
                        toStopId = event.toStopId,
                    ),
                )
            SavedTripUiEvent.AnalyticsSettingsButtonClick ->
                analytics.track(AnalyticsEvent.SettingsClickEvent)
            SavedTripUiEvent.AnalyticsFromButtonClick ->
                analytics.track(AnalyticsEvent.FromFieldClickEvent)
            SavedTripUiEvent.AnalyticsToButtonClick ->
                analytics.track(AnalyticsEvent.ToFieldClickEvent)
            is SavedTripUiEvent.ParkRideCardClick -> onParkRideCardClick(
                parkRideState = event.parkRideState,
                isExpanded = event.isExpanded,
            )
            is SavedTripUiEvent.RemoveParkRideStation -> removeParkRideStation(event.parkRideState)
            is SavedTripUiEvent.ParkRideDirectionsClick -> platformOps.openMapDirections(
                latitude = event.latitude,
                longitude = event.longitude,
                label = event.label,
            )
            SavedTripUiEvent.AnalyticsDiscoverButtonClick -> {
                analytics.track(AnalyticsEvent.DiscoverButtonClick)
                preferences.markDiscoverAsClicked()
                updateUiState { copy(displayDiscoverBadge = false) }
            }
            is SavedTripUiEvent.DismissInfoTile -> onDismissInfoTile(event.infoTile)
            is SavedTripUiEvent.InfoTileCtaClick -> onInfoTileCtaClick(event.infoTile)
            is SavedTripUiEvent.InfoTileExpand -> onInfoTileExpand(key = event.key)
            is SavedTripUiEvent.FromStopChanged -> onFromStopChanged(event.fromJson)
            is SavedTripUiEvent.ToStopChanged -> onToStopChanged(event.toJson)
            is SavedTripUiEvent.DateTimeSelectionChanged -> updateUiState {
                copy(dateTimeSelectionItem = event.dateTimeJson?.let(DateTimeSelectionItem::fromJsonString))
            }
            SavedTripUiEvent.StopTracking -> trackingManager.stop()
            is SavedTripUiEvent.MoveSavedTripToIndex -> onMoveSavedTrip(event.tripId, event.targetIndex)
            SavedTripUiEvent.MarkReorderTipSeen -> onMarkReorderTipSeen()
            is SavedTripUiEvent.EnterReorder -> updateReorder { it ?: beginReorder(event.liftedKey) }
            is SavedTripUiEvent.ReorderMove -> updateReorder { it?.move(event.fromKey, event.toKey) }
            is SavedTripUiEvent.ReorderRemove -> updateReorder { it?.remove(event.tripId) }
            SavedTripUiEvent.ReorderDone -> onReorderDone()
            SavedTripUiEvent.ReorderCancel -> updateReorder { null }
            is SavedTripUiEvent.LabelChipClick -> onLabelChipClick(event.labelKey)
            is SavedTripUiEvent.LabelChipLocated -> onLabelChipLocated(event)
            SavedTripUiEvent.LabelChipTapHandled -> updateUiState { copy(labelChipTap = LabelChipTap.Idle) }
            is SavedTripUiEvent.LabelStopAssigned -> onLabelStopAssigned(event)
        }
    }

    private fun onSavedTripCardClick(event: SavedTripUiEvent.AnalyticsSavedTripCardClick) {
        analytics.trackSavedTripCardClick(event.fromStopId, event.toStopId)
    }

    /**
     * A Home or Work chip was tapped. One tap at a time: while one is still being worked out a
     * second changes nothing, which is also what keeps it to a single location fix.
     */
    private fun onLabelChipClick(labelKey: String) {
        if (_uiState.value.labelChipTap != LabelChipTap.Idle) return
        val label = homeLabelFor(labelKey, _uiState.value.stopLabels) ?: return
        updateUiState { copy(labelChipTap = startLabelChipTap(label)) }
    }

    /**
     * The one fix a set chip's tap asked for has come back, or there was none. Looks up the stops
     * near it in the local database, once, and decides. Anything that goes wrong in the lookup is
     * the same as no stop near, so the tap always ends in a decision and never stays pending.
     */
    private fun onLabelChipLocated(event: SavedTripUiEvent.LabelChipLocated) {
        val tap = _uiState.value.labelChipTap
        if (tap !is LabelChipTap.Locating || tap.labelKey != event.labelKey) return
        val destination = homeLabelFor(event.labelKey, _uiState.value.stopLabels)?.toStopItem()
        if (destination == null) {
            updateUiState { copy(labelChipTap = LabelChipTap.Idle) }
            return
        }
        val rider = event.latitude?.let { lat -> event.longitude?.let { lon -> LatLng(lat, lon) } }
        viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {
            val candidates = rider?.let { stopsNear(it) }.orEmpty()
            updateUiState {
                copy(labelChipTap = LabelChipTap.Decided(decideLabelTap(destination, rider, candidates)))
            }
        }
    }

    private suspend fun stopsNear(rider: LatLng) = runCatching {
        nearbyStopsRepository.getStopsNearby(
            centerLat = rider.latitude,
            centerLon = rider.longitude,
            radiusKm = LABEL_TAP_RADIUS_KM,
            maxResults = LABEL_TAP_CANDIDATES,
        )
    }.onFailure { logError("Home chip stops lookup failed", it) }.getOrDefault(emptyList())

    /**
     * Stop search returned a stop for a label the rider is setting from the home chips. A stop is
     * on one label only, so a stop another label already holds is left alone. A label with no row
     * (Work, after it was deleted in Manage) is created by the assignment.
     */
    private fun onLabelStopAssigned(event: SavedTripUiEvent.LabelStopAssigned) {
        val labels = _uiState.value.stopLabels
        val row = labels.firstOrNull { it.label.equals(event.labelKey, ignoreCase = true) }
        if (labels.any { it.stopId == event.stopId && it != row }) return
        val builtIn = homeLabelFor(event.labelKey, labels) ?: return
        viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {
            if (row != null) {
                sandook.updateStopLabelStop(row.label, event.stopId, event.stopName)
            } else {
                sandook.upsertStopLabel(
                    label = builtIn.label,
                    emoji = builtIn.emoji,
                    stopId = event.stopId,
                    stopName = event.stopName,
                    sortOrder = labels.size.toLong(),
                )
            }
        }
    }

    /**
     * Saved Trips is the one calm screen the review sheet is allowed to land on. If a delight
     * moment (a clean timetable view, a save, a park-and-ride add) was armed elsewhere, this
     * is where it fires; the manager owns every gate and no-ops when nothing is armed. On the
     * IO dispatcher because the eligibility check reads the saved-trip count from the database.
     */
    private fun maybeRequestReview() {
        viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {
            appReviewManager.onSavedTripsScreenShown()
        }
    }

    /**
     * Expanding a Park and Ride card is a delight moment, but it happens on the Saved Trips
     * screen itself rather than on a screen we navigate away from. So we arm and fire in one
     * place, after a short delay so the sheet is not requested in the same frame as the tap.
     */
    private fun armParkRideCardReviewMoment() {
        viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {
            delay(PARK_RIDE_CARD_REVIEW_DELAY_MS)
            // Those few seconds are long enough for the rider to open a timetable, settings or
            // the stop search, and the review sheet is a system dialog that will open over any
            // of them — the one thing the calm-screen rule above exists to prevent. If they
            // have left, the moment is simply dropped: it is not worth holding onto, since
            // expanding a card again is all it takes to earn another one.
            if (screenCollectors.value == 0) {
                log("Park & Ride review moment dropped — Saved Trips is no longer on screen")
                return@launchWithExceptionHandler
            }
            appReviewManager.onDelightMoment(DelightMoment.PARK_RIDE_CARD_TAPPED)
            appReviewManager.onSavedTripsScreenShown()
        }
    }

    private fun onToStopChanged(toJson: String) {
        log("StopItem - onToStopChanged: $toJson")
        StopItem.fromJsonString(toJson)?.let { stop ->
            stopResultsManager.setSelectedToStop(stop)
            updateUiState { copy(toStop = stopResultsManager.selectedToStop) }
        } ?: run {
            log("StopItem - Failed to parse toStop from JSON: $toJson")
        }
    }

    private fun onFromStopChanged(fromJson: String) {
        log("StopItem - onFromStopChanged: $fromJson")
        StopItem.fromJsonString(fromJson)?.let { stop ->
            stopResultsManager.setSelectedFromStop(stop)
            updateUiState { copy(fromStop = stopResultsManager.selectedFromStop) }
        } ?: run {
            log("StopItem - Failed to parse fromStop from JSON: $fromJson")
        }
    }

    /**
     * Removes the car parks the rider added at this station, one row each, and nothing else. The home card
     * and its `P` pin follow from the table the screen already observes, so nothing is set here by hand.
     * A row a saved trip holds is left alone, because the delete names the source.
     */
    private fun removeParkRideStation(parkRideState: ParkRideUiState) {
        viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {
            parkRideState.facilities.forEach { facility ->
                parkRideSandook.deleteSavedParkRide(
                    stopId = facility.stopId,
                    facilityId = facility.facilityId,
                    source = UserAdded,
                )
            }
        }
    }

    private fun onParkRideCardClick(
        parkRideState: ParkRideUiState,
        isExpanded: Boolean,
    ) {
        analytics.track(
            event = AnalyticsEvent.ParkRideCardClickEvent(
                stopId = parkRideState.stopId,
                expand = isExpanded,
                // Ids only: joining the facility objects themselves stringified the whole
                // data class, which is both unreadable and long enough that Firebase
                // rejected the param outright. Sorted and comma-joined matches how
                // multi-value params are encoded elsewhere (e.g. transportModes).
                facilityId = parkRideState.facilities
                    .map { it.facilityId }
                    .sorted()
                    .joinToString(separator = ","),
            ),
        )

        if (isExpanded) {
            onParkRideCardExpanded(parkRideState)
        } else {
            onParkRideCardCollapsed(parkRideState)
        }
    }

    private fun onParkRideCardExpanded(parkRideState: ParkRideUiState) {
        log(
            "Park Ride card expanded for stopId: ${parkRideState.stopId} , facilities: ${parkRideState.facilities.joinToString()}",
        )
        updateUiState {
            copy(
                observeParkRideStopIdSet = (observeParkRideStopIdSet + parkRideState.stopId).toImmutableSet(),
                parkRideUiState = parkRideUiState.map { uiState ->
                    // Loading is dictated by totalSpots being -1 because, when we do not have
                    // any facility detail data for first time, we set totalSpots to -1.
                    if (uiState.stopId == parkRideState.stopId && parkRideState.facilities.any {
                            it.totalSpots == -1 || it.spotsAvailable == -1
                        }
                    ) {
                        uiState.copy(isLoading = true)
                    } else if (parkRideState.facilities.any { it.totalSpots >= 0 }) {
                        uiState.copy(isLoading = false)
                    } else {
                        uiState
                    }
                }.toImmutableList(),
            )
        }
        viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {
            log("Fetching Park Ride facility for stopId: ${parkRideState.stopId}")
            fetchAndSaveParkRideFacilityIfNeeded(stopId = parkRideState.stopId)
        }
        armParkRideCardReviewMoment()
        log("Park Ride card expanded done")
    }

    // Deliberately not the expanded branch with the sign flipped: collapsing has no
    // "spots have arrived, stop showing loading" arm, because nothing is fetched here.
    private fun onParkRideCardCollapsed(parkRideState: ParkRideUiState) {
        log(
            "Park Ride card collapsed for stopId: ${parkRideState.stopId} , facilities: ${parkRideState.facilities.joinToString()}",
        )
        updateUiState {
            copy(
                observeParkRideStopIdSet = (observeParkRideStopIdSet - parkRideState.stopId).toImmutableSet(),
                parkRideUiState = parkRideUiState.map { uiState ->
                    // Loading is dictated by totalSpots being -1 because, when we do not have
                    // any facility detail data for first time, we set totalSpots to -1.
                    if (uiState.stopId == parkRideState.stopId && parkRideState.facilities.any {
                            it.totalSpots == -1 || it.spotsAvailable == -1
                        }
                    ) {
                        uiState.copy(isLoading = true)
                    } else {
                        uiState
                    }
                }.toImmutableList(),
            )
        }
    }

    private fun observeStopLabels() {
        viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {
            sandook.observeStopLabels()
                .distinctUntilChanged()
                .collect { rows ->
                    val labels = rows.toStopLabels()
                    updateUiState { copy(stopLabels = labels) }
                }
        }
    }

    private fun seedDefaultLabelsIfEmpty() {
        viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {
            sandook.observeStopLabels().take(1).collect { labels ->
                if (labels.isEmpty()) {
                    StopLabel.defaults.forEachIndexed { index, label ->
                        sandook.upsertStopLabel(label.label, label.emoji, null, null, index.toLong())
                    }
                }
            }
        }
    }

    private fun observeSavedTrips() {
        log("onStart - observeSavedTrips called")
        observeSavedTripsJob?.cancel()
        observeSavedTripsJob =
            viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {
                // Read with the trips, before the first list is drawn, so the row never jumps.
                val rowIndex = preferences.getLong(SandookPreferences.KEY_PARK_RIDE_ROW_INDEX)?.toInt()
                // The labels are read with the trips for the same reason: a card draws a label whenever its
                // stop is the labelled stop, so one drawn before the labels landed would grow when they did
                // (`docs/MAP_HOME_UX.md` rules 22 and 37). The labels' own observer keeps them current after.
                val labels = sandook.observeStopLabels().first().toStopLabels()
                updateUiState {
                    copy(isSavedTripsLoading = true, parkRideRowIndex = rowIndex, stopLabels = labels)
                }
                sandook.observeAllTrips()
                    .distinctUntilChanged()
                    .collectLatest { savedTrips ->
                        log("Saved trips updated: $savedTrips")
                        val trips = savedTrips.map { it.toTrip() }.toImmutableList()
                        updateUiState { copy(savedTrips = trips, isSavedTripsLoading = false) }

                        syncSavedTripParkRides(
                            savedTrips = savedTrips,
                            facilityManager = nswParkRideFacilityManager,
                            stopResultsManager = stopResultsManager,
                            parkRideSandook = parkRideSandook,
                            analytics = analytics,
                        )
                    }
            }
    }

    private fun onMoveSavedTrip(tripId: String, targetIndex: Int) {
        val trips = _uiState.value.savedTrips.toMutableList()
        val fromIndex = trips.indexOfFirst { it.tripId == tripId }
        if (fromIndex == -1 || targetIndex !in trips.indices) return
        if (fromIndex == targetIndex) return
        val moved = trips.removeAt(fromIndex)
        trips.add(targetIndex, moved)
        updateUiState { copy(savedTrips = trips.toImmutableList()) }
        analytics.track(
            AnalyticsEvent.SavedTripCardReorderedEvent(
                fromStopId = moved.fromStopId,
                toStopId = moved.toStopId,
                previousIndex = fromIndex,
                newIndex = targetIndex,
                totalCount = trips.size,
            ),
        )
        viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {
            trips.forEachIndexed { index, trip ->
                sandook.updateSavedTripSortOrder(tripId = trip.tripId, sortOrder = index.toLong())
            }
        }
    }

    private fun onDeleteSavedTrip(savedTrip: Trip) {
        log("onDeleteSavedTrip: $savedTrip")
        viewModelScope.launch(context = ioDispatcher) {
            sandook.deleteTrip(tripId = savedTrip.tripId)
            analytics.trackDeleteSavedTrip(
                fromStopId = savedTrip.fromStopId,
                toStopId = savedTrip.toStopId,
            )
        }
    }

    private suspend fun loadReorderTipSeenState() {
        val hasSeen = preferences.getBoolean(SandookPreferences.KEY_HAS_SEEN_SAVED_TRIP_CARD_REORDER_TIP) ?: false
        if (!hasSeen) updateUiState { copy(hasSeenReorderTip = false) }
    }

    private fun onMarkReorderTipSeen() {
        viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {
            preferences.setBoolean(SandookPreferences.KEY_HAS_SEEN_SAVED_TRIP_CARD_REORDER_TIP, true)
            // No UI state update, the tip stays visible for the current session, hidden on next launch.
        }
    }

    /** Changes the open reorder session, or starts or drops it. Nothing is written until Done. */
    private fun updateReorder(change: SavedTripsState.(ReorderSession?) -> ReorderSession?) {
        updateUiState { copy(reorder = change(reorder)) }
    }

    /**
     * Done. The finished order is applied to the state at once and written once, off the main thread:
     * the trips and the ones taken off in one transaction, the row's place in one preference.
     */
    private fun onReorderDone() {
        val state = _uiState.value
        val session = state.reorder ?: return
        updateUiState { withReorderApplied(session.result()) }
        viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {
            persistReorder(sandook, preferences, analytics, session, state.savedTrips)
        }
    }

    /**
     * Observes and merges Park & Ride facility mapping and detailed data, updating the UI state.
     *
     * This method combines two sources:
     * 1. The mapping of stopId to facilityId (from `SavedParkRide`), which is always available for all saved trips.
     * 2. The detailed facility data (from `NSWParkRide`), which is only available after an API call for that facility.
     *
     * For each update:
     * - If no facilities are found in the mapping, the Park & Ride UI state is cleared.
     * - For each mapped facility, if detailed data is available, it is used; otherwise, a placeholder is created with only mapping info.
     * - This ensures the UI always lists all Park & Ride facilities relevant to the user's saved trips, even if detailed data is not yet fetched.
     *
     * Edge case:
     * - If a facility mapping exists (stopId ↔ facilityId) but no detailed data is present (because the user hasn't interacted with the card),
     *   a placeholder entry is shown in the UI. The detailed data will appear automatically once fetched via API.
     *
     * The resulting UI state always reflects the latest merged facility information for display.
     */
    @OptIn(ExperimentalTime::class)
    private fun observeFacilityDetailsFromDb() {
        log("onStart - observeFacilitySpotsAvailability called")
        observeParkRideFacilityFromDatabaseJob?.cancel()
        observeParkRideFacilityFromDatabaseJob =
            viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {
                combine(
                    flow = parkRideSandook.observeSavedParkRides().distinctUntilChanged(),
                    flow2 = parkRideSandook.getAllParkRideFacilityDetail().distinctUntilChanged(),
                ) { savedParkRides, facilityDetails ->

                    log(" - ID Mapping Table: ${savedParkRides.map { it.facilityId }.toSet()}")
                    log(
                        " - Facility Detail Table: ${
                            facilityDetails.map { it.facilityName }.toSet()
                        }",
                    )

                    // Map details by facilityId for quick lookup
                    val detailsByFacilityId = facilityDetails.associateBy { it.facilityId }
                    // Merge: for every mapping, fill with details if present, else use minimal info
                    savedParkRides.map { mapping ->
                        val detail: NSWParkRideFacilityDetail? =
                            detailsByFacilityId[mapping.facilityId]

                        detail ?: NSWParkRideFacilityDetail(
                            facilityId = mapping.facilityId,
                            facilityName = nswParkRideFacilityManager.getParkRideFacilityById(
                                facilityId = mapping.facilityId,
                            )?.parkRideName.orEmpty(),
                            stopId = mapping.stopId,
                            spotsAvailable = -1,
                            totalSpots = -1,
                            percentageFull = -1,
                            timeText = "",
                            suburb = "",
                            address = "",
                            latitude = 0.0,
                            longitude = 0.0,
                            // The mapping row already carries a resolved stop name, written
                            // by the same priority chain syncSavedTripParkRides uses. Fall
                            // back to it before the raw stop ID, or a card whose stop is not
                            // in the local search index shows "2153478" instead of
                            // "Bella Vista" until its first availability fetch lands.
                            stopName = stopResultsManager.fetchStopResults(mapping.stopId)
                                .filterIsInstance<SearchStopState.SearchResult.Stop>()
                                .firstOrNull()?.stopName
                                ?: mapping.stopName.ifBlank { mapping.stopId },
                            timestamp = Instant.DISTANT_PAST.epochSeconds,
                        )
                    }
                }.collectLatest { mergedList ->
                    if (mergedList.isEmpty()) {
                        log("No Park Ride facilities found in the database.")
                        updateUiState { copy(parkRideUiState = persistentListOf()) }
                    } else {
                        log("Merged Park Ride data: ${mergedList.size}")
                        updateUiState {
                            copy(
                                parkRideUiState = mergedList
                                    .toParkRideUiState()
                                    .toImmutableList(),
                            )
                        }
                    }
                }
            }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun fetchAndSaveParkRideFacilityIfNeeded(stopId: String): Unit = parkRideFetchMutex.withLock {
        val facilityIds = convertStopIdListToFacilityIdList(setOf(stopId).toImmutableSet())
        if (facilityIds.isEmpty()) {
            logError("No Park Ride facilities found for stopId: $stopId")
            return@withLock
        }
        val now = System.now().epochSeconds
        val refreshable = filterRefreshableFacilities(
            parkRideSandook = parkRideSandook,
            facilityIds = facilityIds,
            cooldown = getApiCooldownDuration(),
            nowInstant = Instant.fromEpochSeconds(now),
        )
        if (refreshable.isEmpty()) return@withLock

        // BFF batch path: one HTTP call resolves stop to facilities and returns
        // them all. NSW path returns null, the caller falls back to per-facility
        // fan-out.
        val batched: ParkingStopBatchResponse? =
            parkRideService.fetchAvailabilityForStops(stopIds = listOf(stopId))

        if (batched != null) {
            applyBatchResults(parkRideSandook, stopId, refreshable, batched, now)
        } else {
            fanOutPerFacility(parkRideSandook, parkRideService, stopId, refreshable, now)
        }
    }

    private fun convertStopIdListToFacilityIdList(stopIdList: ImmutableSet<String>): Set<String> {
        val parkRideFacilityList: List<NswParkRideFacility> =
            nswParkRideFacilityManager.getParkRideFacilities()

        val facilityIdList = parkRideFacilityList
            .filter { it.stopId in stopIdList }
            .map { it.parkRideFacilityId }
            .toSet()

        return facilityIdList
    }

    /**
     * Asks for the numbers of every saved Park & Ride station when home opens or comes back to the
     * front, so a tile has a current number without a tap. Cost: at most one call per saved
     * facility, and only for the ones [isOffCooldown] lets through, which is the same decision the
     * tap and the poll make. It runs beside the first frame and never ahead of it, a failure leaves
     * the cached reading as it was, and nothing repeats: only an open tile is polled.
     */
    private fun refreshSavedParkRidesOnOpen() {
        parkRideOpenRefreshJob?.cancel()
        parkRideOpenRefreshJob = viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {
            val savedStopIds = parkRideSandook.observeSavedParkRides().first().map { it.stopId }
            stopsToRefreshOnOpen(savedStopIds, _uiState.value.observeParkRideStopIdSet).forEach { stopId ->
                log("Park & Ride on open for stopId: $stopId")
                fetchAndSaveParkRideFacilityIfNeeded(stopId = stopId)
            }
        }
    }

    /** Keeps open Park & Ride cards fresh; the rules live on [pollWhileCardsAreOpen]. */
    private fun pollExpandedParkRideStops() {
        parkRidePollJob?.cancel()
        parkRidePollJob = viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {
            pollWhileCardsAreOpen(
                screenCollectors = screenCollectors,
                subscriptionGraceMillis = SUBSCRIPTION_GRACE_MS,
                openStopIds = _uiState.map { it.observeParkRideStopIdSet },
                interval = ::getApiCooldownDuration,
                refresh = ::refreshFacilityDetails,
            )
        }
    }

    /**
     * Refreshes Park & Ride facility data for all currently expanded stops.
     *
     * Called on arrival at the screen, on the tap that expands a card, and on every tick of
     * [pollExpandedParkRideStops]. Each of those is only a trigger: the per-facility cooldown
     * inside decides which facilities are actually worth an API call, so the three entry points
     * share one budget rather than each having their own.
     *
     * Cooldowns are shorter during peak hours, when parking fills fastest and a stale number is
     * most misleading, and longer off-peak.
     */
    private suspend fun refreshFacilityDetails() {
        log("onStart - refreshFacilityDetails called")

        // Retrieve the set of stop IDs with currently expanded UI cards
        val expandedStopIds = _uiState.value.observeParkRideStopIdSet
        log("Refreshing Park Ride facilities for expanded stops: $expandedStopIds")

        // Early return if no stops are currently expanded
        if (expandedStopIds.isEmpty()) {
            log("No expanded Park Ride stops to refresh.")
            return
        }

        // For each expanded stop, fetch and update its facility data if needed
        // (respecting cooldown periods and handling API failures)
        expandedStopIds.forEach { stopId ->
            log("Checking Park Ride facility for stopId: $stopId")
            fetchAndSaveParkRideFacilityIfNeeded(stopId = stopId)
        }
    }

    private fun getApiCooldownDuration(): Duration = parkRideApiCooldown(
        peakTimeCooldownSeconds = peakTimeCooldownSeconds,
        nonPeakTimeCooldownSeconds = nonPeakTimeCooldownSeconds,
    )

    private suspend fun updateDiscoverState() {
        log("onStart - updateDiscoverState called")
        // Delay to ensure savedTrips is inited before checking discover availability
        delay(200.milliseconds)
        updateUiState {
            copy(
                isDiscoverAvailable = this@SavedTripsViewModel.isDiscoverAvailable && savedTrips.isNotEmpty(),
                displayDiscoverBadge = !preferences.hasDiscoverBeenClicked(),
            )
        }
    }

    private suspend fun updateInviteFriendsTileSeenState() {
        log("onStart - updateInviteFriendsTileSeenState called")
        // Manager caches the value, so this will only read from preferences once
        updateUiState {
            copy(hasSeenInviteFriendsTile = inviteFriendsTileManager.hasSeenInviteFriendsTile())
        }
    }

    internal fun updateUiState(block: SavedTripsState.() -> SavedTripsState) {
        _uiState.update(block)
    }

    override fun onCleared() {
        super.onCleared()
        log("StopItem - onCleared called in SavedTripsViewModel")
        cleanupJobs()
    }

    @VisibleForTesting
    fun cleanupJobs() {
        observeSavedTripsJob?.cancel()
        observeSavedTripsJob = null
        observeParkRideFacilityFromDatabaseJob?.cancel()
        observeParkRideFacilityFromDatabaseJob = null
        parkRidePollJob?.cancel()
        parkRidePollJob = null
        parkRideOpenRefreshJob?.cancel()
        parkRideOpenRefreshJob = null
        log("Cleanup jobs in SavedTripsViewModel completed.")
    }
}

// Delay before requesting review after a Park and Ride card tap, so it never fires in the
// same frame as the tap.
private const val PARK_RIDE_CARD_REVIEW_DELAY_MS = 3_000L

// How long the screen's data keeps loading after the last collector leaves, so a rotation or
// a brief detach does not restart it.
private const val SUBSCRIPTION_GRACE_MS = 5_000L

private fun SavedTrip.toTrip(): Trip = Trip(
    fromStopId = fromStopId,
    fromStopName = fromStopName,
    toStopId = toStopId,
    toStopName = toStopName,
)
