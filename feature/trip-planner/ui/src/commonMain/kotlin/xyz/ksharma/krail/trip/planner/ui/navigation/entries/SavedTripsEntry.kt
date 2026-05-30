package xyz.ksharma.krail.trip.planner.ui.navigation.entries

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.navigation.ResultEffect
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.mapstopselection.MapStopSelectionPane
import xyz.ksharma.krail.trip.planner.ui.navigation.SavedTripsRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.navigation.StopSelectedResult
import xyz.ksharma.krail.trip.planner.ui.navigation.TripPlannerNavigator
import xyz.ksharma.krail.trip.planner.ui.savedtrips.DepartureBoardViewModel
import xyz.ksharma.krail.trip.planner.ui.savedtrips.SavedTripsScreen
import xyz.ksharma.krail.trip.planner.ui.savedtrips.SavedTripsViewModel
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
        val savedTripState by viewModel.uiState.collectAsStateWithLifecycle()

        val departureBoardViewModel: DepartureBoardViewModel = koinViewModel()
        val departureBoardEntries by departureBoardViewModel.entries.collectAsStateWithLifecycle()
        val expandedDepartureBoardStopId by departureBoardViewModel.expandedStopId.collectAsStateWithLifecycle()

        val trackedJourney by viewModel.trackedJourney.collectAsStateWithLifecycle()

        LaunchedEffect(savedTripState.savedTrips) {
            departureBoardViewModel.setTrips(savedTripState.savedTrips)
        }

        // Listen for StopSelected results from SearchStop screen
        // This uses the singleton ResultEventBus to ensure results are received
        // even when screens are in different composition scopes (two-pane layout)
        ResultEffect<StopSelectedResult> { result ->
            val stopItem = StopItem(
                stopId = result.stopId,
                stopName = result.stopName,
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
            onSettingsButtonClick = {
                viewModel.onEvent(SavedTripUiEvent.AnalyticsSettingsButtonClick)
                tripPlannerNavigator.navigateToSettings()
            },
            onDiscoverButtonClick = {
                viewModel.onEvent(SavedTripUiEvent.AnalyticsDiscoverButtonClick)
                tripPlannerNavigator.navigateToDiscover()
            },
            onEvent = { event -> viewModel.onEvent(event) },
            departureBoardEntries = departureBoardEntries,
            expandedDepartureBoardStopId = expandedDepartureBoardStopId,
            onDepartureBoardEvent = departureBoardViewModel::onEvent,
            onCompactHeightPlanTrip = {
                viewModel.onEvent(SavedTripUiEvent.AnalyticsFromButtonClick)
                tripPlannerNavigator.navigateToSearchStop(SearchStopFieldType.FROM)
            },
            onCompactHeightSetDestination = {
                viewModel.onEvent(SavedTripUiEvent.AnalyticsToButtonClick)
                tripPlannerNavigator.navigateToSearchStop(SearchStopFieldType.TO)
            },
            onCompactHeightFindTrip = {
                triggerTripSearch(
                    fromStop = savedTripState.fromStop,
                    toStop = savedTripState.toStop,
                    viewModel = viewModel,
                    tripPlannerNavigator = tripPlannerNavigator,
                )
            },
            rightPane = {
                log("[MAP_STOP_SEL] SavedTrips rightPane slot invoked")
                MapStopSelectionPane(
                    onStopSelected = { stopItem ->
                        // Two-tap From / To flow. First unset slot wins; once both
                        // are set, the next pick replaces From so the user can start
                        // a fresh trip without explicit clear.
                        when {
                            savedTripState.fromStop == null -> {
                                viewModel.onEvent(
                                    SavedTripUiEvent.FromStopChanged(stopItem.toJsonString()),
                                )
                            }
                            savedTripState.toStop == null -> {
                                viewModel.onEvent(
                                    SavedTripUiEvent.ToStopChanged(stopItem.toJsonString()),
                                )
                            }
                            else -> {
                                viewModel.onEvent(
                                    SavedTripUiEvent.FromStopChanged(stopItem.toJsonString()),
                                )
                            }
                        }
                    },
                    topOverlay = {
                        SavedTripsMapBanner(
                            fromStop = savedTripState.fromStop,
                            toStop = savedTripState.toStop,
                            onSearchTrip = {
                                triggerTripSearch(
                                    fromStop = savedTripState.fromStop,
                                    toStop = savedTripState.toStop,
                                    viewModel = viewModel,
                                    tripPlannerNavigator = tripPlannerNavigator,
                                )
                            },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(KrailTheme.dimensions.spacingM),
                        )
                    },
                )
            },
        )
    }
}

/**
 * Contextual pill on the SavedTrips map. Styling matches LocationPermissionBanner
 * (rounded surface pill, onSurface text). Three states walk the user through the
 * two-tap From / To pick + the "see timetable" action when both are set.
 */
@Composable
private fun SavedTripsMapBanner(
    fromStop: StopItem?,
    toStop: StopItem?,
    onSearchTrip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val (label, onTap) = when {
        fromStop == null -> "Tap a stop to set your starting point" to null
        toStop == null -> "Tap a stop to set your destination" to null
        else -> "Tap to see timetable" to onSearchTrip
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(SAVED_TRIPS_BANNER_CORNER_RADIUS))
            .background(
                color = KrailTheme.colors.surface.copy(alpha = SAVED_TRIPS_BANNER_BG_ALPHA),
            )
            .then(if (onTap != null) Modifier.klickable(onClick = onTap) else Modifier)
            .padding(horizontal = dim.spacingL, vertical = dim.spacingM),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = KrailTheme.typography.bodySmall,
            color = KrailTheme.colors.onSurface,
        )
    }
}

private val SAVED_TRIPS_BANNER_CORNER_RADIUS = 24.dp
private const val SAVED_TRIPS_BANNER_BG_ALPHA = 0.9f

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
