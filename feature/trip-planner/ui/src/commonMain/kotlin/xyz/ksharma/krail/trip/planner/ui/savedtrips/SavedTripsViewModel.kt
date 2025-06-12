package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
            val trips = mutableSetOf<Trip>()

            val savedTrips = sandook.selectAllTrips()
//            log("SavedTrips: $savedTrips")
            savedTrips.forEachIndexed { index, savedTrip ->
                trips.add(savedTrip.toTrip())
            }

            trips.addAll(savedTrips.map { savedTrip -> savedTrip.toTrip() })

            // log("SavedTrips: ${trips.size} number")
            trips.forEachIndexed { index, trip ->
                log("\t SavedTrip: #$index ${trip.fromStopName} -> ${trip.toStopName}")
            }

            updateUiState {
                copy(
                    savedTrips = trips.toImmutableList(),
                    isSavedTripsLoading = false
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
        // todo analytics
        updateUiState { copy(isParkRideLoading = true) }

        // if from stop id and toStopId are part of the park ride facility list
        // then call api for details for both stops else only the one that is part of the list.

        // If none is part of list then throw error as this method cannot be called for such stops in first place from the UI.
        // because the ui for expanding park ride facility is only shown when the stops are part of the park ride facility list.

        viewModelScope.launchWithExceptionHandler<SavedTripsViewModel>(ioDispatcher) {

            val parkRideFacilityList = nswParkRideFacilityManager.getParkRideFacilities()
            log("Park Ride Facilities: ${parkRideFacilityList.size}")

            val facilityIdList = parkRideFacilityList
                .filter { it.stopId == fromStopId || it.stopId == toStopId }
                .map { it.parkRideFacilityId }
                .toSet()

            val parkRideStates = facilityIdList.map { facilityId ->
                parkRideService
                    .getCarParkFacilities(facilityId = facilityId)
                    .toParkRideState()
            }.toImmutableList()

            updateUiState {
                copy(
                    parkRideList = parkRideStates,
                    isParkRideLoading = false,
                )
            }
        }
    }

    private fun CarParkFacilityDetailResponse.toParkRideState(): ParkRideState {
        val totalSpots = spots.toIntOrNull() ?: 0

        // Sum occupied spots from all zones (using loop sensor)
        val occupiedSpots = zones.sumOf { it.occupancy.transients?.toIntOrNull() ?: 0 }

        val spotsAvailable = totalSpots - occupiedSpots
        val percentFull = if (totalSpots > 0) {
            (occupiedSpots * 100) / totalSpots
        } else {
            0
        }

        log(
            "[$facilityName - $facilityId] \nTotal spots: $totalSpots, " +
                    "Occupied spots: $occupiedSpots, Spots available: $spotsAvailable, " +
                    "Percentage full: $percentFull%"
        )

        return ParkRideState(
            spotsAvailable = spotsAvailable,
            totalSpots = totalSpots,
            facilityName = facilityName,
            percentageFull = percentFull,
            stopId = tsn,
        )
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
