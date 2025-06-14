package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
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
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.analytics.event.trackScreenViewEvent
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.coroutines.ext.launchWithExceptionHandler
import xyz.ksharma.krail.park.ride.network.NswParkRideFacilityManager
import xyz.ksharma.krail.park.ride.network.model.NswParkRideFacility
import xyz.ksharma.krail.park.ride.network.service.ParkRideService
import xyz.ksharma.krail.sandook.NSWParkRide
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SavedTrip
import xyz.ksharma.krail.trip.planner.ui.state.parkride.ParkRideState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
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

    private val _uiState: MutableStateFlow<SavedTripsState> = MutableStateFlow(SavedTripsState())
    val uiState: StateFlow<SavedTripsState> = _uiState
        .onStart {
            analytics.trackScreenViewEvent(screen = AnalyticsScreen.SavedTrips)
            // TODO - call api once and save data in db.
            observeSavedTrips()
            observeParkRideFacilityDatabase()
            pollParkRideFacilities()
        }
        .onCompletion {
            expandedParkRideCardObserveJob?.cancel()
            expandedParkRideCardObserveJob = null
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SavedTripsState())

    // 1. Use StateFlow for expanded cards
    private val _expandedParkRideCards = MutableStateFlow<Set<String>>(emptySet())
    val expandedParkRideCards: StateFlow<Set<String>> = _expandedParkRideCards

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

    private fun observeParkRideFacilityDatabase() {
        observeParkRideFacilityFromDatabaseJob?.cancel()
        observeParkRideFacilityFromDatabaseJob =
            viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {
                parkRideSandook
                    .observeSavedParkRides()
                    .distinctUntilChanged()
                    .collectLatest {
                        // TODO - Update UI.
                }
            }
    }

    private fun pollParkRideFacilities() {
        pollParkRideFacilitiesJob?.cancel()
        pollParkRideFacilitiesJob = viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {
            while (true) {
                fetchParkRideFacilities()
                delay(2.minutes)
            }
        }
    }

    private suspend fun fetchParkRideFacilities() {
        val facilityIdList = getFacilityIdListToObserve()

        val parkRideList: ImmutableList<ParkRideState> =
            facilityIdList.map { facilityId ->
                log("Fetching Park Ride facility for ID: $facilityId")
                parkRideService.fetchCarParkFacilities(facilityId = facilityId)
            }
                .toParkRideStates()
                .toImmutableList()

        // Save to database
        parkRideSandook.insertOrReplaceAll(parkRideList.map { it.toParkRide() })
    }

    private fun getFacilityIdListToObserve(): Set<String> {
        val stopIdList = uiState.value.observeParkRideStopIdSet
        val parkRideFacilityList: List<NswParkRideFacility> = nswParkRideFacilityManager.getParkRideFacilities()

        val facilityIdList = parkRideFacilityList
            .filter { it.stopId in stopIdList }
            .map { it.parkRideFacilityId }
            .toSet()

        return facilityIdList
    }

    private fun updateUiState(block: SavedTripsState.() -> SavedTripsState) {
        _uiState.update(block)
    }

    override fun onCleared() {
        super.onCleared()
        expandedParkRideCardObserveJob?.cancel()
        expandedParkRideCardObserveJob = null
        observeSavedTripsJob?.cancel()
        observeSavedTripsJob = null
        observeParkRideFacilityFromDatabaseJob?.cancel()
        observeParkRideFacilityFromDatabaseJob = null
    }
}


private fun SavedTrip.toTrip(): Trip = Trip(
    fromStopId = fromStopId,
    fromStopName = fromStopName,
    toStopId = toStopId,
    toStopName = toStopName,
)
