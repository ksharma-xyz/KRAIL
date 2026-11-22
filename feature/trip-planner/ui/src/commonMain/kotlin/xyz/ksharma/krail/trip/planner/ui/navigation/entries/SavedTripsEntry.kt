package xyz.ksharma.krail.trip.planner.ui.navigation.entries

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.data.location.rememberUserLocationManager
import xyz.ksharma.krail.core.navigation.ResultEffect
import xyz.ksharma.krail.trip.planner.ui.homemap.HomeMapActions
import xyz.ksharma.krail.trip.planner.ui.homemap.HomeMapEntryContent
import xyz.ksharma.krail.trip.planner.ui.homemap.HomeMapEvent
import xyz.ksharma.krail.trip.planner.ui.homemap.HomeMapInputs
import xyz.ksharma.krail.trip.planner.ui.homemap.HomeMapViewModel
import xyz.ksharma.krail.trip.planner.ui.mapstopselection.MapStopSelectionPane
import xyz.ksharma.krail.trip.planner.ui.mapstopselection.MapStopSelectionViewModel
import xyz.ksharma.krail.trip.planner.ui.navigation.SavedTripsRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.navigation.StopSelectedResult
import xyz.ksharma.krail.trip.planner.ui.navigation.TripPlannerNavigator
import xyz.ksharma.krail.trip.planner.ui.parkride.suggestion.ParkRideSuggestionEvent
import xyz.ksharma.krail.trip.planner.ui.parkride.suggestion.ParkRideSuggestionViewModel
import xyz.ksharma.krail.trip.planner.ui.parkride.suggestion.rememberParkRideSuggestionSlot
import xyz.ksharma.krail.trip.planner.ui.savedtrips.LabelChipTapEffect
import xyz.ksharma.krail.trip.planner.ui.savedtrips.SavedTripsScreen
import xyz.ksharma.krail.trip.planner.ui.savedtrips.SavedTripsViewModel
import xyz.ksharma.krail.trip.planner.ui.savedtrips.currentLatLng
import xyz.ksharma.krail.trip.planner.ui.savedtrips.onLabelStopSelected
import xyz.ksharma.krail.trip.planner.ui.savedtrips.toTile
import xyz.ksharma.krail.trip.planner.ui.search.ai.AiSearchInputViewModel
import xyz.ksharma.krail.trip.planner.ui.search.ai.isHandoffActionable
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.MapUiState
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
        // The map home's pins and offline notice. With the maps flag off it does no work and the classic home
        // below is drawn, so a build without the flag keeps today's screen.
        val homeMapViewModel: HomeMapViewModel = koinViewModel()
        val homeMapState by homeMapViewModel.uiState.collectAsStateWithLifecycle()
        // The one-time Park & Ride suggestion. It does no work unless the map home is on, and it reads local data only.
        val suggestionViewModel: ParkRideSuggestionViewModel = koinViewModel()
        val suggestionState by suggestionViewModel.uiState.collectAsStateWithLifecycle()
        val savedTripState by viewModel.uiState.collectAsStateWithLifecycle()

        val trackedJourney by viewModel.trackedJourney.collectAsStateWithLifecycle()

        // Drives the AI search sheet the home screen opens over itself (see AiSearchSheet).
        // userLocationManager only exists as a @Composable factory (its permission/location
        // controllers need Activity context tied to composition), so it's built here and
        // passed in via koinViewModel's parametersOf, matching TrackTripViewModel's own
        // Composable-supplied param.
        val userLocationManager = rememberUserLocationManager()
        val aiSearchInputViewModel: AiSearchInputViewModel = koinViewModel(
            parameters = {
                parametersOf(
                    suspend {
                        // The Result is unwrapped here and nowhere else, so this is the only
                        // place that can say WHY a location was not available. Downstream all
                        // three causes (denied, restricted, timed out) arrive as the same null
                        // and produce the same blank From field, which is what made the
                        // reported bug impossible to tell apart from "no stops nearby".
                        userLocationManager.getCurrentLocation()
                            .onFailure { log("[AI_ORIGIN] location unavailable: $it") }
                            .getOrNull()
                    },
                )
            },
        )
        val aiSearchInputState by aiSearchInputViewModel.uiState.collectAsStateWithLifecycle()

        // Whatever the AI resolved is written into SavedTripsState the same way a manual
        // SearchStop pick does (FromStopChanged/ToStopChanged) — per field, so a half-resolved
        // trip still fills the field it did find and leaves the other for a normal tap.
        //
        // One-shot per resolve, enforced by the phase: this effect re-launches every time this
        // entry recomposes (coming back from the stop-search screen included), so it must only
        // act while the resolve is live. closeAfterHandoff steps the phase to IDLE as the
        // dialog closes; without that, a back-navigation replayed the AI's stops over the
        // rider's own later picks.
        //
        // Loading a timetable is still the rider's call and always has been: they read the two
        // stops, decide the AI got it right, and press the button themselves. Doing it for them
        // takes that check away at the one moment it matters most. What changed is only where
        // they read them — on the surface that found them, rather than on the row behind it.
        LaunchedEffect(aiSearchInputState.phase, aiSearchInputState.resolved) {
            val resolved = aiSearchInputState.resolved
            if (aiSearchInputState.isHandoffActionable && resolved != null) {
                resolved.fromStopItem?.let {
                    viewModel.onEvent(SavedTripUiEvent.FromStopChanged(it.toJsonString()))
                }
                resolved.toStopItem?.let {
                    viewModel.onEvent(SavedTripUiEvent.ToStopChanged(it.toJsonString()))
                }
                // "by 6pm" used to be understood and then dropped on the floor here. It is
                // held with the two stops and travels with them when the rider presses
                // Search. Sent even when null, so a fresh sentence with no time in it clears
                // a time left over from the previous one.
                viewModel.onEvent(
                    SavedTripUiEvent.DateTimeSelectionChanged(
                        resolved.dateTimeSelectionItem?.toJsonString(),
                    ),
                )
                // Nothing closes the AI screen from here any more. It shows the rider what it
                // found, including the field it missed, and they leave it themselves: by its
                // own button or by back. These writes happen either way, so whichever route
                // they take lands on a row that already agrees with the card.
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
                // A start Search Stop filled in by itself never replaces one that is already set.
                SearchStopFieldType.FROM ->
                    if (!result.onlyIfEmpty || viewModel.uiState.value.fromStop == null) {
                        viewModel.onEvent(SavedTripUiEvent.FromStopChanged(stopItem.toJsonString()))
                    }
                SearchStopFieldType.TO ->
                    viewModel.onEvent(SavedTripUiEvent.ToStopChanged(stopItem.toJsonString()))
                SearchStopFieldType.LABEL -> onLabelStopSelected(result, viewModel::onEvent)
            }
        }

        // A Home or Work chip tap, played out from the ViewModel's state so a rotation mid tap
        // loses nothing. The location manager is built here because it needs the screen.
        LabelChipTapEffect(
            tap = savedTripState.labelChipTap,
            locate = userLocationManager::currentLatLng,
            onEvent = viewModel::onEvent,
            tripPlannerNavigator = tripPlannerNavigator,
        )

        val actions = HomeMapActions(
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
                    dateTimeSelectionJson = savedTripState.dateTimeSelectionItem?.toJsonString(),
                )
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
            onTrackingCardClick = { tripPlannerNavigator.navigateToTrackTrip() },
            onStopTracking = { viewModel.onEvent(SavedTripUiEvent.StopTracking) },
            onAddParkRideClick = { tripPlannerNavigator.navigateToAddParkRide() },
            onLabelChipClick = { labelKey -> viewModel.onEvent(SavedTripUiEvent.LabelChipClick(labelKey)) },
        )

        if (homeMapState.isMapsAvailable) {
            // The pins are drawn from the labels and the saved stations this screen already holds.
            val tiles = remember(savedTripState.parkRideUiState) {
                savedTripState.parkRideUiState.map { it.toTile() }.toImmutableList()
            }
            LaunchedEffect(savedTripState.stopLabels, tiles) {
                homeMapViewModel.onEvent(
                    HomeMapEvent.InputsChanged(HomeMapInputs(labels = savedTripState.stopLabels, tiles = tiles)),
                )
            }
            // The suggestion is measured from Home, or from the position the map already tracks before Home is set.
            val position = (mapUiState as? MapUiState.Ready)?.mapDisplay?.userLocation
            LaunchedEffect(position) {
                suggestionViewModel.onEvent(ParkRideSuggestionEvent.LocationChanged(position))
            }
            val suggestion = rememberParkRideSuggestionSlot(suggestionViewModel, suggestionState)
            HomeMapEntryContent(
                savedTripsState = savedTripState,
                homeMapState = homeMapState,
                mapUiState = mapUiState,
                userLocationManager = userLocationManager,
                onMapEvent = mapStopSelectionViewModel::onEvent,
                trackedJourney = trackedJourney,
                aiState = aiSearchInputState,
                onAiEvent = aiSearchInputViewModel::onEvent,
                aiVoiceLevel = { aiSearchInputViewModel.voiceLevel.value },
                actions = actions,
                suggestion = suggestion,
            )
        } else {
            SavedTripsScreen(
                savedTripsState = savedTripState,
                trackedJourney = trackedJourney,
                onTrackingCardClick = actions.onTrackingCardClick,
                onStopTracking = actions.onStopTracking,
                onAddParkRideClick = actions.onAddParkRideClick,
                onLabelChipClick = actions.onLabelChipClick,
                fromButtonClick = actions.fromButtonClick,
                toButtonClick = actions.toButtonClick,
                onSavedTripCardClick = actions.onSavedTripCardClick,
                onSearchButtonClick = actions.onSearchButtonClick,
                aiState = aiSearchInputState,
                onAiEvent = aiSearchInputViewModel::onEvent,
                aiVoiceLevel = { aiSearchInputViewModel.voiceLevel.value },
                onSettingsButtonClick = actions.onSettingsButtonClick,
                onDiscoverButtonClick = actions.onDiscoverButtonClick,
                onEvent = actions.onEvent,
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
}

private fun triggerTripSearch(
    fromStop: StopItem?,
    toStop: StopItem?,
    viewModel: SavedTripsViewModel,
    tripPlannerNavigator: TripPlannerNavigator,
    dateTimeSelectionJson: String? = null,
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
            dateTimeSelectionJson = dateTimeSelectionJson,
        )
    }
}
