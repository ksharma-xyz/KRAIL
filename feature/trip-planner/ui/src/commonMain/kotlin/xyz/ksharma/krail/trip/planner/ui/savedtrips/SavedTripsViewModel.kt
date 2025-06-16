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
import kotlinx.datetime.Clock.System
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.analytics.event.trackScreenViewEvent
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.coroutines.ext.launchWithExceptionHandler
import xyz.ksharma.krail.park.ride.network.NswParkRideFacilityManager
import xyz.ksharma.krail.park.ride.network.model.NswParkRideFacility
import xyz.ksharma.krail.park.ride.network.service.ParkRideService
import xyz.ksharma.krail.sandook.NSWParkRideFacilityDetail
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource.SavedTrips
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SavedTrip
import xyz.ksharma.krail.trip.planner.ui.searchstop.StopResultsManager
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class SavedTripsViewModel(
    private val sandook: Sandook,
    private val analytics: Analytics,
    private val ioDispatcher: CoroutineDispatcher,
    private val nswParkRideFacilityManager: NswParkRideFacilityManager,
    private val parkRideService: ParkRideService,
    private val parkRideSandook: NswParkRideSandook,
    private val stopResultsManager: StopResultsManager,
) : ViewModel() {

    /**
     * Will observe saved trips from the database.
     */
    private var observeSavedTripsJob: Job? = null

    /**
     * Will observe park ride facilities from the database.
     */
    private var observeParkRideFacilityFromDatabaseJob: Job? = null

    /**
     * Will fetch data from API every 60 seconds and update in DB.
     */
    private var pollParkRideFacilitiesJob: Job? = null

    private val _uiState: MutableStateFlow<SavedTripsState> = MutableStateFlow(SavedTripsState())
    val uiState: StateFlow<SavedTripsState> = _uiState
        .onStart {
            analytics.trackScreenViewEvent(screen = AnalyticsScreen.SavedTrips)
            log("")
            observeSavedTrips()
            observeFacilitySpotsAvailability()
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

            is SavedTripUiEvent.ExpandParkRideFacilityClick -> {
                log("Expand Park Ride : ${event.stopId}")
                updateUiState {
                    copy(
                        observeParkRideStopIdSet = (observeParkRideStopIdSet + event.stopId).toImmutableSet(),
                    )
                }
            }

            is SavedTripUiEvent.CollapseParkRideForTripClick -> {
                log("Collapse Park Ride : ${event.stopId}")
                updateUiState {
                    copy(
                        observeParkRideStopIdSet = (observeParkRideStopIdSet - event.stopId).toImmutableSet(),
                    )
                }
            }

            is SavedTripUiEvent.ParkRideCardClick -> onParkRideCardClick(
                parkRideState = event.parkRideState,
                isExpanded = event.isExpanded
            )
        }
    }

    // TODO - if card stays open, it won't refresh automatically. - okay for now.
    private fun onParkRideCardClick(
        parkRideState: ParkRideUiState,
        isExpanded: Boolean,
    ) {
        if (isExpanded) {
            updateUiState {
                copy(
                    observeParkRideStopIdSet = (observeParkRideStopIdSet + parkRideState.stopId).toImmutableSet(),
                    parkRideUiState = parkRideUiState.map { uiState ->
                        // Loading is dictated by totalspots being -1 because, when we do not have
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
                fetchAndSaveParkRideFacilityIfNeeded(stopId = parkRideState.stopId)
            }
        } else {
            updateUiState {
                copy(
                    observeParkRideStopIdSet = (observeParkRideStopIdSet - parkRideState.stopId).toImmutableSet(),
                    parkRideUiState = parkRideUiState.map { uiState ->
                        // Loading is dictated by totalspots being -1 because, when we do not have
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
        log("observeSavedTrips called")
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

        // 2. Build a map from stopId to a list of associated park & ride facility IDs
        val facilityMap: Map<String, List<String>> = nswParkRideFacilityManager
            .getParkRideFacilities()
            .groupBy { it.stopId }
            .mapValues { entry -> entry.value.map { it.parkRideFacilityId } }

        // 3. For each unique stopId, pair it with all its facility IDs (if any), and collect as
        // a set of (stopId, facilityId) pairs
        val stopFacilityPairs: Set<Pair<String, String>> = uniqueStopIds
            .flatMap { stopId ->
                facilityMap[stopId]?.map { facilityId -> stopId to facilityId }
                    ?: emptyList()
            }
            .toSet()

        log("StopId -> FacilityId Mapping for StopIds in SavedTrips: \n $stopFacilityPairs")
        // clear all existing saved park rides linked to saved trips for accuracy.
        parkRideSandook.clearAllSavedParkRidesBySource(source = SavedTrips)
        if (stopFacilityPairs.isNotEmpty()) {
            parkRideSandook.insertOrReplaceSavedParkRides(pairs = stopFacilityPairs)
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
    private fun observeFacilitySpotsAvailability() {
        log("observeFacilitySpotsAvailability called")
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

    suspend fun fetchAndSaveParkRideFacilityIfNeeded(stopId: String) {
        val facilityIds = convertStopIdListToFacilityIdList(setOf(stopId).toImmutableSet())
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
                    val detail = apiResult.toNSWParkRideFacilityDetail(
                        // TODO - handle edge case, where tsn is not present in stop results.
                        stopName = stopResultsManager.fetchStopResults(apiResult.tsn)
                            .firstOrNull()?.stopName ?: apiResult.tsn
                    )
                    parkRideSandook.insertOrReplaceAll(listOf(detail))
                    log("Fetched and saved facility $facilityId for stop $stopId")
                } else {
                    //  reset timestamp to DISTANT_PAST
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
     * Checks if the current time is not peak time.
     * Peak time is defined as 5am (05:00) to 10am (10:00), inclusive of 5am, exclusive of 10am.
     */
    private fun isNotPeakTime(): Boolean {
        val now = System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val hour = now.hour
        // Peak time: 5am (05:00) to 10am (10:00), inclusive of 5am, exclusive of 10am
        return hour < 5 || hour >= 10
    }

    // TODO - dictate these times from Firebase.
    private fun getApiCooldownDuration(): Duration {
        return if (isNotPeakTime()) {
            10.minutes
        } else {
            2.minutes
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
        pollParkRideFacilitiesJob?.cancel()
        pollParkRideFacilitiesJob = null
        observeParkRideFacilityFromDatabaseJob?.cancel()
        observeParkRideFacilityFromDatabaseJob = null
        pollParkRideFacilitiesJob?.cancel()
        pollParkRideFacilitiesJob = null
        log("Cleanup jobs in SavedTripsViewModel completed.")
    }
}

private fun SavedTrip.toTrip(): Trip = Trip(
    fromStopId = fromStopId,
    fromStopName = fromStopName,
    toStopId = toStopId,
    toStopName = toStopName,
)
