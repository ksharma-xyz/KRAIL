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
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
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

    var expandedParkRideCards: Set<String> = mutableSetOf()

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
                expandedParkRideCards = expandedParkRideCards +
                    event.fromStopId + event.toStopId
            }

            is SavedTripUiEvent.CollapseParkRideForTripClick ->  {
                expandedParkRideCards = expandedParkRideCards - event.fromStopId - event.toStopId
            }

            SavedTripUiEvent.LifecycleStarted ->  {
                onLifecycleStarted()
            }

            SavedTripUiEvent.LifecycleStopped -> {
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

/*    private fun onLoadParkRideFacilities() {
        // TODO - analytics event for loading Park&Ride facilities

        // Cancel any existing parkRideAvailabilityJob to avoid multiple concurrent jobs
        parkRideAvailabilityJob?.cancel()

        // Set the relevant trips' parkRideUiState to Loading for all expanded cards
        updateUiState {
            copy(
                savedTrips = savedTrips.map { trip ->
                    if (trip.fromStopId in expandedParkRideCards || trip.toStopId in expandedParkRideCards) {
                        trip.copy(parkRideUiState = ParkRideUiState.Loading)
                    } else trip
                }.toImmutableList()
            )
        }

        val expandedStopIds = _uiState.value.expandedParkRideCards
        parkRideAvailabilityJob =
            viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {
                observeParkRideFacilities(stopIdList = expandedStopIds)
            }
    }*/

    private suspend fun observeParkRideFacilities(stopIdList: Set<String>) {
        val facilityIdList = getFacilityIdListToObserve(stopIdList)
        while (true) { // TODO will go away when flow  is used instead to observe changes from local DB.

            log("Observing Park Ride Facilities for stopIDs: $stopIdList and facilityIDs: $facilityIdList")

            val parkRideList: ImmutableList<ParkRideState> =
                facilityIdList.map { facilityId ->
                    parkRideService
                        .getCarParkFacilities(facilityId = facilityId)
                        .toParkRideState()
                }.toImmutableList()

            updateUiState {
                // TODO - haptic feedback when parkRideUiState is updated
                copy(
                    savedTrips = savedTrips.map { trip ->
                        // Update the parkRideUiState for those trips that exist in stopIdList
                        if (trip.fromStopId in stopIdList || trip.toStopId in stopIdList) {
                            trip.copy(parkRideUiState = ParkRideUiState.Loaded(parkRideList))
                        } else trip
                    }.toImmutableList()
                )
            }

            delay(10.seconds) // Execute update every minute.
        }
    }

    fun getFacilityIdListToObserve(stopIdList: Set<String>): Set<String> {
        val parkRideFacilityList = nswParkRideFacilityManager.getParkRideFacilities()
        log("All Park Ride Facilities: ${parkRideFacilityList.size}")

        val facilityIdList = parkRideFacilityList
            .filter { it.stopId in stopIdList }
            .map { it.parkRideFacilityId }
            .toSet()

        log("Facility IDs to observe: $facilityIdList")
        return facilityIdList
    }

    private fun onLifecycleStarted() {
        expandedParkRideCardObserveJob?.cancel()
        expandedParkRideCardObserveJob = viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {



            _uiState
                .map { it.expandedParkRideCards }
                .distinctUntilChanged()
                .collect { expandedStopIds ->
                    if (expandedStopIds.isNotEmpty()) {
                        // Set relevant trips to Loading
                        updateUiState {
                            copy(
                                savedTrips = savedTrips.map { trip ->
                                    if (trip.fromStopId in expandedStopIds || trip.toStopId in expandedStopIds) {
                                        trip.copy(parkRideUiState = ParkRideUiState.Loading)
                                    } else trip
                                }.toImmutableList()
                            )
                        }
                        observeParkRideFacilities(expandedStopIds)
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
