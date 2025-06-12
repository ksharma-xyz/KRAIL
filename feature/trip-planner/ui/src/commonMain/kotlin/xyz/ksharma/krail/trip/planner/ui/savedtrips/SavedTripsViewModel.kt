package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
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
import xyz.ksharma.krail.park.ride.network.model.CarParkFacilityDetailResponse
import xyz.ksharma.krail.park.ride.network.service.ParkRideService
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SavedTrip
import xyz.ksharma.krail.trip.planner.ui.state.parkride.ParkRideState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripsState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.ParkRideUiState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

class SavedTripsViewModel(
    private val sandook: Sandook,
    private val analytics: Analytics,
    private val ioDispatcher: CoroutineDispatcher,
    private val nswParkRideFacilityManager: NswParkRideFacilityManager,
    private val parkRideService: ParkRideService
) : ViewModel() {

    private val _uiState: MutableStateFlow<SavedTripsState> = MutableStateFlow(SavedTripsState())
    val uiState: StateFlow<SavedTripsState> = _uiState
        .onStart {
            analytics.trackScreenViewEvent(screen = AnalyticsScreen.SavedTrips)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SavedTripsState())

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

            is SavedTripUiEvent.LoadParkRideFacilities -> {
                onLoadParkRideFacilities(event.fromStopId, event.toStopId)
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

    private fun onLoadParkRideFacilities(fromStopId: String, toStopId: String) {
        log("onLoadParkRideFacilities: fromStopId=$fromStopId, toStopId=$toStopId")
        // TODO - analytics event for loading Park&Ride facilities

        // Set the relevant trip's parkRideUiState to Loading
        updateUiState {
            copy(
                savedTrips = savedTrips.map { trip ->
                    if (trip.fromStopId == fromStopId && trip.toStopId == toStopId) {
                        trip.copy(parkRideUiState = ParkRideUiState.Loading)
                    } else trip
                }.toImmutableList()
            )
        }

        viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {
            try {
                val parkRideFacilityList = nswParkRideFacilityManager.getParkRideFacilities()
                log("Park Ride Facilities: ${parkRideFacilityList.size}")

                val facilityIdList = parkRideFacilityList
                    .filter { it.stopId == fromStopId || it.stopId == toStopId }
                    .map { it.parkRideFacilityId }
                    .toSet()

                val parkRideList: ImmutableList<ParkRideState> = facilityIdList.map { facilityId ->
                    parkRideService
                        .getCarParkFacilities(facilityId = facilityId)
                        .toParkRideState()
                }.toImmutableList()

                updateUiState {
                    copy(
                        savedTrips = savedTrips.map { trip ->
                            if (trip.fromStopId == fromStopId && trip.toStopId == toStopId) {
                                trip.copy(parkRideUiState = ParkRideUiState.Loaded(parkRideList))
                            } else trip
                        }.toImmutableList()
                    )
                }
            } catch (e: Exception) {
                updateUiState {
                    copy(
                        savedTrips = savedTrips.map { trip ->
                            if (trip.fromStopId == fromStopId && trip.toStopId == toStopId) {
                                trip.copy(
                                    parkRideUiState = ParkRideUiState.Error(
                                        e.message ?: "Unknown error"
                                    )
                                )
                            } else trip
                        }.toImmutableList()
                    )
                }
            }
        }
    }

    private fun updateUiState(block: SavedTripsState.() -> SavedTripsState) {
        _uiState.update(block)
    }
}

private fun SavedTrip.toTrip(): Trip = Trip(
    fromStopId = fromStopId,
    fromStopName = fromStopName,
    toStopId = toStopId,
    toStopName = toStopName,
)
