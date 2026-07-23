package xyz.ksharma.krail.trip.planner.ui.parkride

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.AnalyticsScreen
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.analytics.event.trackScreenViewEvent
import xyz.ksharma.krail.core.appreview.AppReviewManager
import xyz.ksharma.krail.core.appreview.DelightMoment
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.coroutines.ext.launchWithExceptionHandler
import xyz.ksharma.krail.platform.ops.PlatformOps
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource.SavedTrips
import xyz.ksharma.krail.sandook.NswParkRideSandook.Companion.SavedParkRideSource.UserAdded
import xyz.ksharma.krail.sandook.SavedParkRide
import xyz.ksharma.krail.trip.planner.ui.savedtrips.toParkRideState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ErrorKind
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideState.ParkRideStationPickerItem
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.AddParkRideUiEvent

/**
 * Backs the Park & Ride picker.
 *
 * The catalogue of stations comes from Remote Config via [NswParkRideFacilityManager], so
 * stations can be added without an app release. Raw entries are collapsed into stations by
 * [groupIntoStations] — see that file for why neither ID alone is a safe grouping key.
 *
 * Added state is read back from the `UserAdded` rows in [NswParkRideSandook] rather than held
 * locally, so the toggle reflects what is actually stored and stays correct if the same
 * station is added from elsewhere.
 *
 * This screen only writes the stop-to-facility mapping. It never fetches availability —
 * that stays owned by the home cards' expand-and-visible lifecycle
 * (see `docs/POLLING_LIFECYCLE.md`).
 */
@Suppress("LongParameterList")
class AddParkRideViewModel(
    private val catalogue: ParkRideCatalogue,
    private val parkRideSandook: NswParkRideSandook,
    private val availabilityLoader: ParkRideAvailabilityLoader,
    private val platformOps: PlatformOps,
    private val analytics: Analytics,
    private val ioDispatcher: CoroutineDispatcher,
    private val appReviewManager: AppReviewManager,
) : ViewModel() {

    private var observeUserAddedJob: Job? = null
    private var observeSelectedDetailsJob: Job? = null

    private val _uiState: MutableStateFlow<AddParkRideState> = MutableStateFlow(AddParkRideState())
    val uiState: StateFlow<AddParkRideState> = _uiState
        .onStart {
            analytics.trackScreenViewEvent(screen = AnalyticsScreen.AddParkRide)
            observeStations()
        }
        .onCompletion { cleanupJobs() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AddParkRideState())

    fun onEvent(event: AddParkRideUiEvent) {
        when (event) {
            is AddParkRideUiEvent.SearchQueryChanged -> updateUiState { copy(query = event.query) }
            is AddParkRideUiEvent.ToggleStation -> onToggleStation(event.item)
            is AddParkRideUiEvent.StationSelected -> onStationSelected(event.item)
            AddParkRideUiEvent.StationDismissed -> onStationDismissed()
            is AddParkRideUiEvent.DirectionsClicked -> platformOps.openMapDirections(
                latitude = event.position.latitude,
                longitude = event.position.longitude,
                label = event.stationName,
            )
            AddParkRideUiEvent.Retry -> onRetry()
        }
    }

    /** Merges the Remote Config catalogue with the rider's stored `UserAdded` rows. */
    private fun observeStations() {
        observeUserAddedJob?.cancel()
        observeUserAddedJob = viewModelScope.launchWithExceptionHandler<AddParkRideViewModel>(ioDispatcher) {
            updateUiState { copy(loadingEmoji = catalogue.loadingEmoji()) }
            val stations = catalogue.stations()

            if (stations.isEmpty()) {
                // Remote Config missing or unparseable: there is nothing to offer, and it can
                // resolve on its own, so this is a retryable error rather than an empty list.
                log("Park & Ride catalogue is empty")
                updateUiState { copy(isLoading = false, error = ErrorKind.NoFacilities) }
                return@launchWithExceptionHandler
            }

            // Both sources, so a station reached by a saved trip reads as added too — its
            // card is on the home screen either way, and claiming otherwise here would
            // contradict what the rider can see.
            combine(
                parkRideSandook.observeSavedParkRidesBySource(UserAdded).distinctUntilChanged(),
                parkRideSandook.observeSavedParkRidesBySource(SavedTrips).distinctUntilChanged(),
            ) { userAdded, savedTrips ->
                userAdded.map { it.facilityId }.toSet() to savedTrips.map { it.facilityId }.toSet()
            }.collectLatest { (userAddedIds, savedTripIds) ->
                updateUiState {
                    copy(
                        stations = stations.map { station ->
                            station.copy(
                                isUserAdded = station.holdsAll(userAddedIds),
                                isFromSavedTrip = station.holdsAny(savedTripIds),
                            )
                        }.toImmutableList(),
                        isLoading = false,
                        error = null,
                    )
                }
            }
        }
    }

    /**
     * Opens a station's sheet, showing whatever is already cached straight away and fetching
     * only when the facility is off cooldown.
     *
     * This is an on-demand fetch tied to an explicit tap, exactly like expanding a home card —
     * not a background poll. Nothing keeps running once the sheet closes, so the rules in
     * `docs/POLLING_LIFECYCLE.md` still hold.
     */
    private fun onStationSelected(station: ParkRideStationPickerItem) {
        updateUiState { copy(selectedStation = station, isLoadingSelectedStation = true) }

        observeSelectedDetailsJob?.cancel()
        observeSelectedDetailsJob = viewModelScope.launchWithExceptionHandler<AddParkRideViewModel>(ioDispatcher) {
            val facilityIds = station.mappings.map { it.facilityId }.toSet()

            // Cached rows first, so re-opening a recently loaded station is instant.
            launch {
                parkRideSandook.getAllParkRideFacilityDetail()
                    .distinctUntilChanged()
                    .collectLatest { details ->
                        updateUiState {
                            copy(
                                selectedStationDetails = details
                                    .filter { it.facilityId in facilityIds }
                                    .map { it.toParkRideState() }
                                    .toImmutableList(),
                            )
                        }
                    }
            }

            availabilityLoader.refreshIfNeeded(station.mappings)
            updateUiState { copy(isLoadingSelectedStation = false) }
        }
    }

    private fun onStationDismissed() {
        observeSelectedDetailsJob?.cancel()
        observeSelectedDetailsJob = null
        updateUiState {
            copy(
                selectedStation = null,
                selectedStationDetails = persistentListOf(),
                isLoadingSelectedStation = false,
            )
        }
    }

    private fun onRetry() {
        updateUiState { copy(isLoading = true, error = null) }
        observeStations()
    }

    /**
     * Adding stores every car park at the station, so the home card lists them together;
     * removing clears all of them, and only the `UserAdded` rows, leaving any saved-trip
     * row for the same station untouched.
     */
    private fun onToggleStation(station: ParkRideStationPickerItem) {
        // A saved trip owns this station's card, so there is nothing here to remove. The row
        // explains why rather than silently ignoring the tap.
        if (station.isLockedBySavedTrip) {
            log("${station.stationName} is held by a saved trip; not removable from the picker")
            return
        }

        viewModelScope.launchWithExceptionHandler<AddParkRideViewModel>(ioDispatcher) {
            if (station.isUserAdded) {
                log("Removing ${station.mappings.size} user-added mappings for ${station.stationName}")
                station.mappings.forEach { mapping ->
                    parkRideSandook.deleteSavedParkRide(
                        stopId = mapping.stopId,
                        facilityId = mapping.facilityId,
                        source = UserAdded,
                    )
                }
            } else {
                log("Adding ${station.mappings.size} user-added mappings for ${station.stationName}")
                parkRideSandook.insertOrReplaceSavedParkRides(
                    parkRideInfoList = station.mappings.map { mapping ->
                        SavedParkRide(
                            stopId = mapping.stopId,
                            facilityId = mapping.facilityId,
                            stopName = station.stopName.ifBlank { station.stationName },
                            facilityName = mapping.facilityName,
                            source = UserAdded.value,
                        )
                    }.toSet(),
                    source = UserAdded,
                )
                // Adding a facility is a delight moment; it fires once the user returns to
                // the Saved Trips screen. Removing one is not, so this stays in the add branch.
                appReviewManager.onDelightMoment(DelightMoment.PARK_RIDE_ADDED)
            }

            trackToggle(station)
        }
    }

    private fun trackToggle(station: ParkRideStationPickerItem) {
        analytics.track(
            AnalyticsEvent.ParkRideUserFacilityEvent(
                facilityId = station.mappings.joinToString { it.facilityId },
                stopId = station.stationId,
                action = if (station.isUserAdded) {
                    AnalyticsEvent.ParkRideUserFacilityEvent.Action.REMOVE
                } else {
                    AnalyticsEvent.ParkRideUserFacilityEvent.Action.ADD
                },
                source = AnalyticsEvent.ParkRideUserFacilityEvent.Source.ADD_PARK_RIDE_SCREEN,
            ),
        )
    }

    private fun updateUiState(block: AddParkRideState.() -> AddParkRideState) {
        _uiState.update(block)
    }

    override fun onCleared() {
        super.onCleared()
        cleanupJobs()
    }

    private fun cleanupJobs() {
        observeUserAddedJob?.cancel()
        observeUserAddedJob = null
        observeSelectedDetailsJob?.cancel()
        observeSelectedDetailsJob = null
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5000L
    }
}

/**
 * User-added only counts when every car park at the station is stored — anything less is a
 * partial write, which should still offer the add rather than claim to be done.
 */
private fun ParkRideStationPickerItem.holdsAll(facilityIds: Set<String>): Boolean =
    mappings.isNotEmpty() && mappings.all { it.facilityId in facilityIds }

/**
 * Saved trips only ever map the car parks belonging to the stops on that trip, so a single
 * match is enough to put the station's card on the home screen.
 */
private fun ParkRideStationPickerItem.holdsAny(facilityIds: Set<String>): Boolean =
    mappings.any { it.facilityId in facilityIds }
