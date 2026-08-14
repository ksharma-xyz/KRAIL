package xyz.ksharma.krail.trip.planner.ui.navigation.entries

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import xyz.ksharma.krail.core.maps.data.location.rememberUserLocationManager
import xyz.ksharma.krail.core.navigation.ResultEffect
import xyz.ksharma.krail.trip.planner.ui.mapstopselection.MapStopSelectionPane
import xyz.ksharma.krail.trip.planner.ui.mapstopselection.MapStopSelectionViewModel
import xyz.ksharma.krail.trip.planner.ui.navigation.SavedTripsRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.navigation.StopSelectedResult
import xyz.ksharma.krail.trip.planner.ui.navigation.TripPlannerNavigator
import xyz.ksharma.krail.trip.planner.ui.savedtrips.SavedTripsScreen
import xyz.ksharma.krail.trip.planner.ui.savedtrips.SavedTripsViewModel
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputEvent
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputPhase
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputViewModel
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * Saved Trips Entry - List Screen in List-Detail pattern.
 *
 * Right-pane content on tablet / foldable / phone-landscape is supplied as a slot
 * (`rightPane`). Pending follow-up: plug in MapStopSelectionPane backed by a shared
 * Koin singleton ViewModel.
 */
@Composable
internal fun EntryProviderScope<NavKey>.SavedTripsEntry(
    tripPlannerNavigator: TripPlannerNavigator,
) {
    entry<SavedTripsRoute> {
        // Scoped ViewModel that survives navigation
        val viewModel: SavedTripsViewModel = koinViewModel(key = "SavedTripsNav")
        val mapStopSelectionViewModel: MapStopSelectionViewModel = koinViewModel()
        val mapUiState by mapStopSelectionViewModel.mapUiState.collectAsStateWithLifecycle()
        val savedTripState by viewModel.uiState.collectAsStateWithLifecycle()

        val trackedJourney by viewModel.trackedJourney.collectAsStateWithLifecycle()

        // Drives the AI search sheet the home screen opens over itself (see AiSearchSheet).
        // userLocationManager only exists as a @Composable factory (its permission/location
        // controllers need Activity context tied to composition), so it's built here and
        // passed in via koinViewModel's parametersOf, matching TrackTripViewModel's own
        // Composable-supplied param.
        val userLocationManager = rememberUserLocationManager()
        val aiSearchInputViewModel: AiSearchInputViewModel = koinViewModel(
            parameters = { parametersOf(suspend { userLocationManager.getCurrentLocation().getOrNull() }) },
        )
        val aiSearchInputState by aiSearchInputViewModel.uiState.collectAsStateWithLifecycle()

        // Whatever the AI resolved is written into SavedTripsState the same way a manual
        // SearchStop pick does (FromStopChanged/ToStopChanged) — per field, so a half-resolved
        // trip still fills the field it did find and leaves the other for a normal tap.
        //
        // The sheet fills the fields and stops there. Loading a timetable is the rider's call
        // and always has been: they read the two fields, decide the AI got it right, and press
        // Search themselves. Doing it for them takes that check away at the one moment it
        // matters most.
        LaunchedEffect(aiSearchInputState.phase, aiSearchInputState.resolved) {
            val resolved = aiSearchInputState.resolved
            if (aiSearchInputState.phase == AiSearchInputPhase.RESOLVED && resolved != null) {
                resolved.fromStopItem?.let {
                    viewModel.onEvent(SavedTripUiEvent.FromStopChanged(it.toJsonString()))
                }
                resolved.toStopItem?.let {
                    viewModel.onEvent(SavedTripUiEvent.ToStopChanged(it.toJsonString()))
                }
                aiSearchInputViewModel.onEvent(AiSearchInputEvent.StartOver)
            }
        }

        // Listen for StopSelected results from SearchStop screen
        // This uses the singleton ResultEventBus to ensure results are received
        // even when screens are in different composition scopes (two-pane layout)
        ResultEffect<StopSelectedResult> { result ->
            val stopItem = StopItem(
                stopId = result.stopId,
                stopName = result.stopName,
                locationKind = result.locationKind,
                addressType = result.addressType,
            )
            when (result.fieldType) {
                SearchStopFieldType.FROM ->
                    viewModel.onEvent(SavedTripUiEvent.FromStopChanged(stopItem.toJsonString()))
                SearchStopFieldType.TO ->
                    viewModel.onEvent(SavedTripUiEvent.ToStopChanged(stopItem.toJsonString()))
                SearchStopFieldType.LABEL -> Unit
            }
        }

        SavedTripsScreen(
            savedTripsState = savedTripState,
            trackedJourney = trackedJourney,
            onTrackingCardClick = { tripPlannerNavigator.navigateToTrackTrip() },
            onStopTracking = { viewModel.onEvent(SavedTripUiEvent.StopTracking) },
            onAddParkRideClick = { tripPlannerNavigator.navigateToAddParkRide() },
            fromButtonClick = {
                viewModel.onEvent(SavedTripUiEvent.AnalyticsFromButtonClick)
                tripPlannerNavigator.navigateToSearchStop(SearchStopFieldType.FROM)
            },
            toButtonClick = {
                viewModel.onEvent(SavedTripUiEvent.AnalyticsToButtonClick)
                tripPlannerNavigator.navigateToSearchStop(SearchStopFieldType.TO)
            },
            onSavedTripCardClick = { fromStop, toStop ->
                if (fromStop?.stopId != null && toStop?.stopId != null) {
                    viewModel.onEvent(
                        SavedTripUiEvent.AnalyticsSavedTripCardClick(
                            fromStop.stopId,
                            toStop.stopId,
                        ),
                    )
                    tripPlannerNavigator.navigateToTimeTable(
                        fromStopId = fromStop.stopId,
                        fromStopName = fromStop.stopName,
                        toStopId = toStop.stopId,
                        toStopName = toStop.stopName,
                    )
                }
            },
            onSearchButtonClick = {
                triggerTripSearch(
                    fromStop = savedTripState.fromStop,
                    toStop = savedTripState.toStop,
                    viewModel = viewModel,
                    tripPlannerNavigator = tripPlannerNavigator,
                )
            },
            aiState = aiSearchInputState,
            onAiEvent = aiSearchInputViewModel::onEvent,
            // A label chip is the no-speech path through the sheet: it names a destination the
            // rider chose themselves. Same rule as a resolved intent — it fills Destination and
            // closes, and the rider presses Search when they are ready.
            onAiLabelClick = { label ->
                label.toStopItem()?.let { stop ->
                    viewModel.onEvent(SavedTripUiEvent.ToStopChanged(stop.toJsonString()))
                    aiSearchInputViewModel.onEvent(AiSearchInputEvent.CloseSheet)
                }
            },
            onSettingsButtonClick = {
                viewModel.onEvent(SavedTripUiEvent.AnalyticsSettingsButtonClick)
                tripPlannerNavigator.navigateToSettings()
            },
            onDiscoverButtonClick = {
                viewModel.onEvent(SavedTripUiEvent.AnalyticsDiscoverButtonClick)
                tripPlannerNavigator.navigateToDiscover()
            },
            onEvent = { event -> viewModel.onEvent(event) },
            rightPane = {
                MapStopSelectionPane(
                    mapUiState = mapUiState,
                    onEvent = mapStopSelectionViewModel::onEvent,
                    onStopSelected = { stop ->
                        viewModel.onEvent(SavedTripUiEvent.ToStopChanged(stop.toJsonString()))
                    },
                )
            },
        )
    }
}

private fun triggerTripSearch(
    fromStop: StopItem?,
    toStop: StopItem?,
    viewModel: SavedTripsViewModel,
    tripPlannerNavigator: TripPlannerNavigator,
) {
    if (fromStop != null && toStop != null && fromStop.stopId != toStop.stopId) {
        viewModel.onEvent(
            SavedTripUiEvent.AnalyticsLoadTimeTableClick(
                fromStopId = fromStop.stopId,
                toStopId = toStop.stopId,
            ),
        )
        tripPlannerNavigator.navigateToTimeTable(
            fromStopId = fromStop.stopId,
            fromStopName = fromStop.stopName,
            toStopId = toStop.stopId,
            toStopName = toStop.stopName,
        )
    }
}
