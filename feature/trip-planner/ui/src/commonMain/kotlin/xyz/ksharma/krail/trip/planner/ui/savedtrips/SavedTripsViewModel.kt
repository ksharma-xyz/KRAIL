package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
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
import xyz.ksharma.krail.park.ride.network.service.ParkRideService
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SavedTrip
import xyz.ksharma.krail.trip.planner.ui.state.parkride.ParkRideState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip
import kotlin.time.Duration.Companion.seconds

class SavedTripsViewModel(
    private val sandook: Sandook,
    private val analytics: Analytics,
    private val ioDispatcher: CoroutineDispatcher,
    private val nswParkRideFacilityManager: NswParkRideFacilityManager,
    private val parkRideService: ParkRideService,
    private val parkRideSandook: NswParkRideSandook,
) : ViewModel() {

    private var expandedParkRideCardObserveJob: Job? = null

    private val _uiState: MutableStateFlow<SavedTripsState> = MutableStateFlow(SavedTripsState())
    val uiState: StateFlow<SavedTripsState> = _uiState
        .onStart {
            analytics.trackScreenViewEvent(screen = AnalyticsScreen.SavedTrips)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SavedTripsState())

    // 1. Use StateFlow for expanded cards
    private val _expandedParkRideCards = MutableStateFlow<Set<String>>(emptySet())
    val expandedParkRideCards: StateFlow<Set<String>> = _expandedParkRideCards

    fun onEvent(event: SavedTripUiEvent) {
        when (event) {
            is SavedTripUiEvent.DeleteSavedTrip -> onDeleteSavedTrip(event.trip)

            SavedTripUiEvent.LoadSavedTrips -> loadSavedTrips()

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

            is SavedTripUiEvent.DisplayParkRideFacilitiesClick -> {
                log("Expand Park Ride for trip: ${event.fromStopId} to ${event.toStopId}")
                _expandedParkRideCards.value =
                    _expandedParkRideCards.value + event.fromStopId + event.toStopId
                log("After expand expandedParkRideCards: $expandedParkRideCards in obj:${this@SavedTripsViewModel}")
            }

            is SavedTripUiEvent.CollapseParkRideForTripClick -> {
                log("Collapse Park Ride for trip: ${event.fromStopId} to ${event.toStopId}")
                _expandedParkRideCards.value =
                    _expandedParkRideCards.value - event.fromStopId - event.toStopId
                log("After collapse expandedParkRideCards: $expandedParkRideCards in obj:${this@SavedTripsViewModel}")
            }

            SavedTripUiEvent.LifecycleStarted -> {
                onLifecycleStarted()
            }

            SavedTripUiEvent.LifecycleStopped -> {
                log("Lifecycle stopped, cancelling expandedParkRideCardObserveJob")
                expandedParkRideCardObserveJob?.cancel()
                expandedParkRideCardObserveJob = null
            }
        }
    }

    private fun loadSavedTrips() {
        viewModelScope.launch(ioDispatcher) {
            updateUiState { copy(isSavedTripsLoading = true) }

            val savedTrips = sandook.selectAllTrips()
            val facilities = nswParkRideFacilityManager.getParkRideFacilities()
            val parkRideStopIds = facilities.map { it.stopId }.toSet()

            val updatedTrips = savedTrips.map { savedTrip ->
                val trip = savedTrip.toTrip()
                val hasParkRide =
                    trip.fromStopId in parkRideStopIds || trip.toStopId in parkRideStopIds
                trip.copy(
                    parkRideUiState = if (hasParkRide) {
                        ParkRideUiState.Available
                    } else {
                        ParkRideUiState.NotAvailable
                    },
                )
            }

            updateUiState {
                copy(
                    savedTrips = updatedTrips.toImmutableList(),
                    isSavedTripsLoading = false,
                )
            }
        }
    }

    private fun onDeleteSavedTrip(savedTrip: Trip) {
        log("onDeleteSavedTrip: $savedTrip")
        viewModelScope.launch(context = Dispatchers.IO) {
            sandook.deleteTrip(tripId = savedTrip.tripId)
            loadSavedTrips()
            analytics.trackDeleteSavedTrip(
                fromStopId = savedTrip.fromStopId,
                toStopId = savedTrip.toStopId,
            )
        }
    }

    private suspend fun fetchParkRideFacilities(stopIdList: Set<String>) {
        val facilityIdList = getFacilityIdListToObserve(stopIdList)
        log("Fetch Park Ride for stopIDs: $stopIdList and facilityIDs: $facilityIdList")

        val parkRideList: ImmutableList<ParkRideState> =
            facilityIdList.map { facilityId ->
                log("Fetching Park Ride facility for ID: $facilityId")
                parkRideService
                    .getCarParkFacilities(facilityId = facilityId)
            }
                .toParkRideStates()
                .toImmutableList()

        updateUiState {
            copy(
                savedTrips = savedTrips.map { trip ->
                    if (trip.fromStopId in stopIdList || trip.toStopId in stopIdList) {
                        // Only show facilities relevant to this trip
                        val relevantFacilities = parkRideList.filter { facility ->
                            facility.stopId == trip.fromStopId || facility.stopId == trip.toStopId
                        }.toImmutableList()
                        trip.copy(parkRideUiState = ParkRideUiState.Loaded(relevantFacilities))
                    } else trip
                }.toImmutableList()
            )
        }
    }

    fun getFacilityIdListToObserve(stopIdList: Set<String>): Set<String> {
        val parkRideFacilityList = nswParkRideFacilityManager.getParkRideFacilities()

        val facilityIdList = parkRideFacilityList
            .filter { it.stopId in stopIdList }
            .map { it.parkRideFacilityId }
            .toSet()

        return facilityIdList
    }

    private fun onLifecycleStarted() {
        observeExpandedParkRideCards()
    }

    private fun observeExpandedParkRideCards() {
        var parkRidePollingJob: Job? = null
        expandedParkRideCardObserveJob?.cancel()
        expandedParkRideCardObserveJob =
            viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {
                expandedParkRideCards.collectLatest { expandedIds ->
                    parkRidePollingJob?.cancel()

                    log("Expanded Park Ride Cards: $expandedIds")
                    if (expandedIds.isNotEmpty()) {
                        log("Observing Park Ride Facilities for expanded cards: $expandedParkRideCards")
                        parkRidePollingJob = launch {
                            while (true) {
                                fetchParkRideFacilities(expandedIds)
                                delay(60.seconds)
                            }
                        }
                    } else {
                        log("No expanded Park Ride cards to observe. Cancelling parkRidePollingJob.")
                        parkRidePollingJob?.cancel()
                    }
                }
            }
    }

    private fun updateUiState(block: SavedTripsState.() -> SavedTripsState) {
        _uiState.update(block)
    }

    override fun onCleared() {
        super.onCleared()
        expandedParkRideCardObserveJob?.cancel()
    }
}

private fun SavedTrip.toTrip(): Trip = Trip(
    fromStopId = fromStopId,
    fromStopName = fromStopName,
    toStopId = toStopId,
    toStopName = toStopName,
)
