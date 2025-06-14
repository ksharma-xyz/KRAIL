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
            observeParkRideFacilityDatabase()
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

                    // TODO - only insert the stopId - facilityId mapping.
                    //   filter saved trip stops by the ones that have park ride facilities.
                    //   stopsWithParkRideFacilities

                    val uniqueSavedTripStopIds: Set<String> = savedTrips
                        .flatMap { listOf(it.fromStopId, it.toStopId) }
                        .toSet()

                    val facilityMap: Map<String, List<String>> = nswParkRideFacilityManager
                        .getParkRideFacilities()
                        .groupBy { it.stopId }
                        .mapValues { entry -> entry.value.map { it.parkRideFacilityId } }

                    val stopFacilityPairs: Set<Pair<String, String>> = uniqueSavedTripStopIds
                        .flatMap { stopId ->
                            facilityMap[stopId]?.map { facilityId -> stopId to facilityId }
                                ?: emptyList()
                        }
                        .toSet()

                    log("These are uniquely saved trip stops filtered by which have park ride facility and their facility ids : \n $stopFacilityPairs")

//                    parkRideSandook.insertOrReplaceSavedParkRides(pairs = stopFacilityPairs)
                }
        }
    }

    /**
     * Inserts or replaces saved park ride facilities for the given trips.
     *
     * For each trip, this method creates all possible pairs of stop IDs and facility IDs
     * (handling both from and to stops). It then inserts these pairs into the database.
     *
     * Edge cases handled:
     * - Multiple facilities for a single stop: All facility IDs for a stop are paired and inserted.
     * - A single facility mapped to multiple stops: Each (stopId, facilityId) pair is inserted separately.
     *
     * This ensures that all valid stop-facility associations are stored, covering one-to-many
     * and many-to-one relationships between stops and facilities.
     */
    private suspend fun insertSavedParkRideFacilitiesForTrips(
        savedTrips: List<SavedTrip>,
        facilityList: List<NswParkRideFacility>
    ) {
        val stopFacilityPairs = mutableSetOf<Pair<String, String>>()
        savedTrips.forEach { trip ->
            listOf(trip.fromStopId, trip.toStopId).forEach { stopId ->
                val facilityIds = facilityList
                    .filter { it.stopId == stopId }
                    .map { it.parkRideFacilityId }
                facilityIds.forEach { facilityId ->
                    stopFacilityPairs.add(Pair(stopId, facilityId))
                }
            }
        }

        // Delete all trip-linked Park&Ride pairs, then insert new ones.
        parkRideSandook.clearAllSavedParkRidesBySource(source = SavedTrips)

        log("Inserting/Updating Park Ride facilities for trips: $stopFacilityPairs")
        parkRideSandook.insertOrReplaceSavedParkRides(
            pairs = stopFacilityPairs,
            source = SavedTrips,
        )
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

    private fun observeParkRideFacilityDatabase() {
        log("observeParkRideFacilityDatabase called")
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
