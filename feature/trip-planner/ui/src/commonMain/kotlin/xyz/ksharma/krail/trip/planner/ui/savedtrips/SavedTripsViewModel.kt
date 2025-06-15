package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
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
import xyz.ksharma.krail.coroutines.ext.launchWithExceptionHandler
import xyz.ksharma.krail.park.ride.network.NswParkRideFacilityManager
import xyz.ksharma.krail.park.ride.network.model.NswParkRideFacility
import xyz.ksharma.krail.park.ride.network.service.ParkRideService
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource.SavedTrips
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SavedTrip
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.ParkRideStopsUiState
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
) : ViewModel() {

    /**
     * Will observe expanded park ride cards.
     * This is used to fetch park ride facilities for the expanded cards only.
     */
    private var expandedParkRideCardObserveJob: Job? = null

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

    private val lastApiCallTimeMap: MutableMap<String, Instant> = mutableMapOf()

    private val _uiState: MutableStateFlow<SavedTripsState> = MutableStateFlow(SavedTripsState())
    val uiState: StateFlow<SavedTripsState> = _uiState
        .onStart {
            analytics.trackScreenViewEvent(screen = AnalyticsScreen.SavedTrips)
            log("")
            observeSavedTrips()
            observeFacilitySpotsAvailability()
            pollParkRideFacilities()
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
     * TODO:
     * Think of solving edge case, where multiple stop Ids have same facility Ids.
     * {"stopId":"209325","parkRideFacilityId":"489","parkRideName":"Park&Ride - Manly Vale"},
     * {"stopId":"209324","parkRideFacilityId":"489","parkRideName":"Park&Ride - Manly Vale"},
     *
     * @param savedTrips List of saved trips to extract stop IDs from.
     */
    @VisibleForTesting // TODO - write unit tests for this function.
    suspend fun updateParkRideStopIdsInDb(savedTrips: List<SavedTrip>) {
        // 1. Collect all unique stop IDs from the saved trips (both fromStopId and toStopId)
        val uniqueSavedTripStopIds: Set<String> = savedTrips
            .flatMap { listOf(it.fromStopId, it.toStopId) }
            .toSet()

        // 2. Build a map from stopId to a list of associated park & ride facility IDs
        val facilityMap: Map<String, List<String>> = nswParkRideFacilityManager
            .getParkRideFacilities()
            .groupBy { it.stopId }
            .mapValues { entry -> entry.value.map { it.parkRideFacilityId } }

        // 3. For each unique stopId, pair it with all its facility IDs (if any), and collect as
        // a set of (stopId, facilityId) pairs
        val stopFacilityPairs: Set<Pair<String, String>> = uniqueSavedTripStopIds
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
     * - observe data inside the database for park ride facilities.,
     * it will tell how many spots are available for which facility id and how much %is full.
     * it will also have last updated time and location details if available.
     */
    private fun observeFacilitySpotsAvailability() {
        log("observeFacilitySpotsAvailability called")
        observeParkRideFacilityFromDatabaseJob?.cancel()
        observeParkRideFacilityFromDatabaseJob =
            viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {

                parkRideSandook.getAll()
                    .distinctUntilChanged()
                    .collectLatest { parkRides ->
                        val facilitiesByStop = parkRides.groupBy { it.stopId }
                        // For each stop, update the UI state individually

                        facilitiesByStop.forEach { (stopId, facilities) ->
                            updateUiState {
                                copy(
                                    parkRideUiState = ParkRideStopsUiState(
                                        stopId = stopId,
                                        stopName = facilities.firstOrNull()?.facilityName.orEmpty(),
                                        facilities = facilities.map { it.toParkRideState() }
                                            .toImmutableList(),
                                        isLoading = false,
                                        error = null,
                                    )
                                )
                            }
                        }
                    }
            }
    }

    private fun pollParkRideFacilities() {
        log("pollParkRideFacilities called")
        pollParkRideFacilitiesJob?.cancel()
        pollParkRideFacilitiesJob =
            viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {
                while (true) {
                    fetchParkRideFacilities()

                    delay(getApiCooldownDuration())
                }
            }
    }

    private suspend fun fetchParkRideFacilities() {
        val stopIdList =
            listOf("221710").toImmutableSet() // TODO uiState.value.observeParkRideStopIdSet
        if (stopIdList.isEmpty()) {
            log("No Park Ride stop IDs are expanded, therefore skipping API Call.")
            return
        }

        val facilityIdList = convertStopIdListToFacilityIdList(stopIdList)
        val cooldownDuration = getApiCooldownDuration()

        val facilitiesDueForRefresh = facilityIdList.filter { facilityId ->
            val lastCall = lastApiCallTimeMap[facilityId] ?: Instant.DISTANT_PAST
            System.now() - lastCall >= cooldownDuration
        }

        val facilitiesOnCooldown = facilityIdList - facilitiesDueForRefresh.toSet()
        facilitiesOnCooldown.forEach { facilityId ->
            val lastCall = lastApiCallTimeMap[facilityId] ?: Instant.DISTANT_PAST
            val timeLeft = cooldownDuration - (System.now() - lastCall)
            log("Facility $facilityId is on cooldown for another ${timeLeft.inWholeSeconds} seconds")
        }

        if (facilitiesDueForRefresh.isEmpty()) {
            log("API call skipped for all facilities due to cooldown.")
            return
        }

        log("Fetching Park Ride facilities for FacilityIdList: $facilitiesDueForRefresh")
        val parkRideDbList = facilitiesDueForRefresh.map { facilityId ->
            lastApiCallTimeMap[facilityId] = System.now()
            parkRideService
                .fetchCarParkFacilities(facilityId = facilityId)
                .toDbNSWParkRide()
        }.toImmutableList()
        log("Fetched Park Ride facilities: $parkRideDbList")

        parkRideSandook.insertOrReplaceAll(parkRideDbList)
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

    private fun getApiCooldownDuration(): Duration {
        return if (isNotPeakTime()) {
            5.minutes
        } else {
            2.minutes // TODO -  firebase controls this value.
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
        expandedParkRideCardObserveJob?.cancel()
        expandedParkRideCardObserveJob = null
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
