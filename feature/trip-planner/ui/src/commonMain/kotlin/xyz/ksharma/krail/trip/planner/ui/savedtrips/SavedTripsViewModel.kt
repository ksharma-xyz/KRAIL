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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.analytics.event.trackScreenViewEvent
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.appversion.AppVersionManager
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.core.remote_config.flag.Flag
import xyz.ksharma.krail.core.remote_config.flag.FlagKeys
import xyz.ksharma.krail.core.remote_config.flag.asBoolean
import xyz.ksharma.krail.core.remote_config.flag.asNumber
import xyz.ksharma.krail.coroutines.ext.launchWithExceptionHandler
import xyz.ksharma.krail.park.ride.network.NswParkRideFacilityManager
import xyz.ksharma.krail.park.ride.network.model.NswParkRideFacility
import xyz.ksharma.krail.park.ride.network.service.ParkRideService
import xyz.ksharma.krail.sandook.NSWParkRideFacilityDetail
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource.SavedTrips
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.sandook.SavedParkRide
import xyz.ksharma.krail.sandook.SavedTrip
import xyz.ksharma.krail.taj.components.InfoTileCta
import xyz.ksharma.krail.taj.components.InfoTileData
import xyz.ksharma.krail.trip.planner.ui.searchstop.StopResultsManager
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import kotlin.time.Clock.System
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class SavedTripsViewModel(
    private val sandook: Sandook,
    private val analytics: Analytics,
    private val ioDispatcher: CoroutineDispatcher,
    private val nswParkRideFacilityManager: NswParkRideFacilityManager,
    private val parkRideService: ParkRideService,
    private val parkRideSandook: NswParkRideSandook,
    private val stopResultsManager: StopResultsManager,
    private val flag: Flag,
    private val preferences: SandookPreferences,
    private val appVersionManager: AppVersionManager,
    private val appInfoProvider: AppInfoProvider,
) : ViewModel() {

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

    private val _uiState: MutableStateFlow<SavedTripsState> = MutableStateFlow(SavedTripsState())
    val uiState: StateFlow<SavedTripsState> = _uiState
        .onStart {
            analytics.trackScreenViewEvent(screen = AnalyticsScreen.SavedTrips)
            observeSavedTrips()
            observeFacilityDetailsFromDb()
            refreshFacilityDetails()
            updateDiscoverState()
            checkAppVersion()
        }
        .onCompletion {
            cleanupJobs()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SavedTripsState())

    fun onEvent(event: SavedTripUiEvent) {
        when (event) {
            is SavedTripUiEvent.DeleteSavedTrip -> onDeleteSavedTrip(event.trip)

            is SavedTripUiEvent.AnalyticsSavedTripCardClick -> {
                analytics.trackSavedTripCardClick(
                    fromStopId = event.fromStopId,
                    toStopId = event.toStopId,
                )
            }

            SavedTripUiEvent.AnalyticsReverseSavedTrip -> {
                analytics.track(AnalyticsEvent.ReverseStopClickEvent)
            }

            is SavedTripUiEvent.AnalyticsLoadTimeTableClick -> {
                analytics.trackLoadTimeTableClick(
                    fromStopId = event.fromStopId,
                    toStopId = event.toStopId,
                )
            }

            SavedTripUiEvent.AnalyticsSettingsButtonClick -> {
                analytics.track(AnalyticsEvent.SettingsClickEvent)
            }

            SavedTripUiEvent.AnalyticsFromButtonClick -> {
                analytics.track(AnalyticsEvent.FromFieldClickEvent)
            }

            SavedTripUiEvent.AnalyticsToButtonClick -> {
                analytics.track(AnalyticsEvent.ToFieldClickEvent)
            }

            is SavedTripUiEvent.ParkRideCardClick -> onParkRideCardClick(
                parkRideState = event.parkRideState,
                isExpanded = event.isExpanded
            )

            SavedTripUiEvent.AnalyticsDiscoverButtonClick -> {
                analytics.track(AnalyticsEvent.DiscoverButtonClick)
                preferences.markDiscoverAsClicked()
                updateUiState { copy(displayDiscoverBadge = false) }
            }

            is SavedTripUiEvent.DismissInfoTile -> {} //onDismissInfoTile(event.infoTileState)

            is SavedTripUiEvent.InfoTileCtaClick -> {} // onInfoTileCtaClick(event.infoTileState)
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
                facilityId = parkRideState.facilities.joinToString(),
            )
        )

        if (isExpanded) {
            log("Park Ride card expanded for stopId: ${parkRideState.stopId} , facilities: ${parkRideState.facilities.joinToString()}")
            updateUiState {
                copy(
                    observeParkRideStopIdSet = (observeParkRideStopIdSet + parkRideState.stopId).toImmutableSet(),
                    parkRideUiState = parkRideUiState.map { uiState ->
                        // Loading is dictated by totalSpots being -1 because, when we do not have
                        // any facility detail data for first time, we set totalSpots to -1.
                        if (uiState.stopId == parkRideState.stopId && parkRideState.facilities.any { it.totalSpots == -1 || it.spotsAvailable == -1 }) {
                            uiState.copy(isLoading = true)
                        } else if (parkRideState.facilities.any { it.totalSpots >= 0 }) {
                            uiState.copy(isLoading = false)
                        } else {
                            uiState
                        }
                    }.toImmutableList()
                )
            }
            viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {
                log("Fetching Park Ride facility for stopId: ${parkRideState.stopId}")
                fetchAndSaveParkRideFacilityIfNeeded(stopId = parkRideState.stopId)
            }
            log("Park Ride card expanded done")
        } else {
            log("Park Ride card collapsed for stopId: ${parkRideState.stopId} , facilities: ${parkRideState.facilities.joinToString()}")
            updateUiState {
                copy(
                    observeParkRideStopIdSet = (observeParkRideStopIdSet - parkRideState.stopId).toImmutableSet(),
                    parkRideUiState = parkRideUiState.map { uiState ->
                        // Loading is dictated by totalSpots being -1 because, when we do not have
                        // any facility detail data for first time, we set totalSpots to -1.
                        if (uiState.stopId == parkRideState.stopId && parkRideState.facilities.any { it.totalSpots == -1 || it.spotsAvailable == -1 }) {
                            uiState.copy(isLoading = true)
                        } else {
                            uiState
                        }
                    }.toImmutableList()
                )
            }
        }
    }

    private fun observeSavedTrips() {
        log("onStart - observeSavedTrips called")
        observeSavedTripsJob?.cancel()
        observeSavedTripsJob = viewModelScope.launch(ioDispatcher) {
            updateUiState { copy(isSavedTripsLoading = true) }
            sandook.observeAllTrips()
                .distinctUntilChanged()
                .collectLatest { savedTrips ->
                    log("Saved trips updated: $savedTrips")
                    val trips = savedTrips.map { it.toTrip() }.toImmutableList()
                    updateUiState { copy(savedTrips = trips, isSavedTripsLoading = false) }

                    updateParkRideStopIdsInDb(savedTrips)
                }
        }
    }

    /**
     * Handles the logic for updating Park & Ride stop IDs in the database based on saved trips.
     * Updates the database with (stopId, facilityId) pairs for all saved trips.
     *
     * For each unique stop ID found in the list of saved trips (from both `fromStopId` and `toStopId`),
     * this method finds all associated Park & Ride facility IDs using the facility manager.
     *
     * Only stop IDs that have at least one facility are included.
     * The resulting set of (stopId, facilityId) pairs is then inserted or replaced in the database.
     *
     * In short:
     *      StopId -> FacilityId Mapping for StopIds in SavedTrips
     *
     * edge case solved, where multiple stop Ids have same facility Ids.
     * {"stopId":"209325","parkRideFacilityId":"489","parkRideName":"Park&Ride - Manly Vale"},
     * {"stopId":"209324","parkRideFacilityId":"489","parkRideName":"Park&Ride - Manly Vale"},
     *
     * @param savedTrips List of saved trips to extract stop IDs from.
     */
    @VisibleForTesting // TODO - write unit tests for this function.
    suspend fun updateParkRideStopIdsInDb(savedTrips: List<SavedTrip>) {
        // 1. Collect all unique stop IDs from the saved trips (both fromStopId and toStopId)
        val uniqueStopIds: Set<String> = uniqueSavedTripStopIds(savedTrips)
        log("Unique Stop IDs from Saved Trips: $uniqueStopIds")

        // 2. Build a map from stopId to a list of associated park & ride facilities
        val facilityDetailMap: Map<String, List<NswParkRideFacility>> = nswParkRideFacilityManager
            .getParkRideFacilities()
            .groupBy { it.stopId }
        log("Facility Detail Map: $facilityDetailMap")

        // 3. For each unique stopId, create SavedParkRide objects with all required data
        val savedParkRideList: Set<SavedParkRide> = uniqueStopIds
            .flatMap { stopId ->
                // Try to resolve stopName for each stopId in priority order:
                // 1. Use local DB (should always succeed for saved trips).
                // 2. If not found, use Park & Ride facility name from facility mapping - remote config.
                //    This can happen if the stopId in the saved trip is different from the one in
                //    the park ride api call response. In that case, the remote config mapping
                //    will provide the correct facility name for those stopId's.
                // 3. If still not found, fallback to stopId itself (should rarely happen).
                val stopName = stopResultsManager.fetchLocalStopName(stopId)
                    ?: facilityDetailMap[stopId]?.firstOrNull()?.parkRideName
                    ?: stopId

                facilityDetailMap[stopId]?.map { facility ->
                    SavedParkRide(
                        stopId = stopId,
                        facilityId = facility.parkRideFacilityId,
                        stopName = stopName,
                        facilityName = facility.parkRideName,
                        source = SavedTrips.value,
                    )
                } ?: emptyList()
            }
            .toSet()

        savedParkRideList.map {
            it.stopId to it.facilityId + " (${it.stopName})"
        }.let {
            log("StopId -> FacilityId Mapping: \n${it.joinToString("\n")}")
        }

        // Clear all existing saved park rides linked to saved trips for accuracy
        parkRideSandook.clearAllSavedParkRidesBySource(source = SavedTrips)

        if (savedParkRideList.isNotEmpty()) {
            parkRideSandook.insertOrReplaceSavedParkRides(
                parkRideInfoList = savedParkRideList,
                source = SavedTrips
            )
        }
    }

    private fun uniqueSavedTripStopIds(savedTrips: List<SavedTrip>): Set<String> {
        val uniqueSavedTripStopIds: Set<String> = savedTrips
            .flatMap { listOf(it.fromStopId, it.toStopId) }
            .toSet()
        return uniqueSavedTripStopIds
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
                        }"
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
                                facilityId = mapping.facilityId
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
                            stopName = stopResultsManager.fetchStopResults(mapping.stopId)
                                .firstOrNull()?.stopName ?: mapping.stopId,
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
                                    .toImmutableList()
                            )
                        }
                    }
                }
            }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun fetchAndSaveParkRideFacilityIfNeeded(stopId: String) {
        val facilityIds = convertStopIdListToFacilityIdList(setOf(stopId).toImmutableSet())
        if (facilityIds.isEmpty()) {
            logError("No Park Ride facilities found for stopId: $stopId")
            return
        }
        val cooldown = getApiCooldownDuration()
        val now = System.now().epochSeconds

        facilityIds.forEach { facilityId ->
            val lastCallEpoch = parkRideSandook.getLastApiCallTimestamp(facilityId)
                ?: Instant.DISTANT_PAST.epochSeconds
            val lastCall = Instant.fromEpochSeconds(lastCallEpoch)
            val nowInstant = Instant.fromEpochSeconds(now)
            if (nowInstant - lastCall >= cooldown) {
                log("Fetching facility $facilityId for stop $stopId")
                val apiResult = parkRideService.fetchCarParkFacilities(facilityId).getOrNull()

                if (apiResult != null) {
                    parkRideSandook.updateApiCallTimestamp(facilityId, now)

                    // Get the stop name from the saved park ride mapping for this facility
                    val stopName =
                        parkRideSandook.getSavedParkRideByFacilityId(facilityId)?.stopName
                            ?: stopId // Fall back to stopId if mapping is somehow missing

                    val detail = apiResult.toNSWParkRideFacilityDetail(
                        stopName = stopName,
                        stopId = stopId,
                    )

                    parkRideSandook.insertOrReplaceAll(listOf(detail))
                    log("Fetched and saved facility $facilityId for stop $stopId - $detail")
                } else {
                    // reset timestamp to DISTANT_PAST as API call failed, so we can retry
                    // earlier than cooldown would end.
                    parkRideSandook.updateApiCallTimestamp(
                        facilityId = facilityId,
                        timestamp = Instant.DISTANT_PAST.epochSeconds,
                    )
                    logError("API call failed for facility $facilityId")
                }
            } else {
                val timeLeft = cooldown - (nowInstant - lastCall)
                log("Facility $facilityId is on cooldown for another ${timeLeft.inWholeSeconds} seconds")
            }
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
     * Refreshes Park & Ride facility data for all currently expanded stops.
     *
     * Business Logic:
     * 1. Ensures fresh data is displayed for stops that users are actively viewing (expanded cards)
     * 2. Provides automatic background updates without requiring user interaction
     * 3. Handles several important scenarios:
     *    - When a card is already expanded and the API cooldown period has elapsed
     *    - When previous API calls failed and need to be retried
     *    - When users navigate between different screens and return to expanded cards
     *
     * This method complements the on-demand updates triggered by card expansion, creating
     * a dual refresh strategy:
     * - Immediate refresh on user interaction (card expansion)
     * - Background refresh for already-expanded cards
     *
     * API calls are managed with cooldown periods to prevent excessive network requests,
     * with shorter cooldowns during peak hours and longer ones during off-peak times.
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

    /**
     * Checks if the current time is not peak time.
     * Peak time is defined as 5am (05:00) to 10am (10:00), inclusive of 5am, exclusive of 10am.
     */
    @OptIn(ExperimentalTime::class)
    private fun isNotPeakTime(): Boolean {
        val now = System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val hour = now.hour
        // Peak time: 5am (05:00) to 10am (10:00), inclusive of 5am, exclusive of 10am
        return hour < 5 || hour >= 10
    }

    private fun getApiCooldownDuration(): Duration {
        return if (isNotPeakTime()) {
            nonPeakTimeCooldownSeconds.seconds
        } else {
            peakTimeCooldownSeconds.seconds
        }
    }

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

    private suspend fun checkAppVersion() {
        log("onStart - checkAppVersion called")
        val appUpdateCopy = appVersionManager.getUpdateCopy()
        if (appUpdateCopy == null) {
            log("App update copy is null, no update available.")
            return
        }

        updateUiState {
            val updateTile = InfoTileData(
                title = appUpdateCopy.title,
                description = appUpdateCopy.description,
                type = InfoTileData.InfoTileType.APP_UPDATE,
                primaryCta = InfoTileCta(
                    text = appUpdateCopy.ctaText,
                    url = appInfoProvider.getAppInfo().appStoreUrl,
                )
            )
            copy(
                infoTiles = (infoTiles ?: persistentListOf())
                    .plus(updateTile)
                    .toSet()
                    .sortedBy { it.type.priority }
                    // TODO - [Visual] [Enhancement] At max 2 tiles should be displayed.
                    //  Unless a Stack UI, in which case, all can be displayed.
                    .take(2)
                    .toImmutableList()
            )
        }
    }

    private fun updateUiState(block: SavedTripsState.() -> SavedTripsState) {
        _uiState.update(block)
    }

    override fun onCleared() {
        super.onCleared()
        cleanupJobs()
    }

    @VisibleForTesting
    fun cleanupJobs() {
        observeSavedTripsJob?.cancel()
        observeSavedTripsJob = null
        observeParkRideFacilityFromDatabaseJob?.cancel()
        observeParkRideFacilityFromDatabaseJob = null
        log("Cleanup jobs in SavedTripsViewModel completed.")
    }
}

private fun SavedTrip.toTrip(): Trip = Trip(
    fromStopId = fromStopId,
    fromStopName = fromStopName,
    toStopId = toStopId,
    toStopName = toStopName,
)
