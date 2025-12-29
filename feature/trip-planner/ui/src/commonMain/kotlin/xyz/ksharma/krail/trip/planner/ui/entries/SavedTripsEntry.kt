package xyz.ksharma.krail.trip.planner.ui.entries

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.core.navigation.ResultEffect
import xyz.ksharma.krail.trip.planner.ui.savedtrips.SavedTripsScreen
import xyz.ksharma.krail.trip.planner.ui.savedtrips.SavedTripsViewModel
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * Saved Trips Entry - List Screen in List-Detail pattern
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun EntryProviderScope<NavKey>.SavedTripsEntry(
    tripPlannerNavigator: TripPlannerNavigator
) {
    entry<SavedTripsRoute>(
        metadata = androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy.listPane()
    ) {
        // Scoped ViewModel that survives navigation
        val viewModel: SavedTripsViewModel = koinViewModel(key = "SavedTripsNav")
        val savedTripState by viewModel.uiState.collectAsStateWithLifecycle()

        // Listen for StopSelected results from SearchStop screen
        // This uses the singleton ResultEventBus to ensure results are received
        // even when screens are in different composition scopes (two-pane layout)
        ResultEffect<StopSelectedResult> { result ->
            val stopItem = StopItem(
                stopId = result.stopId,
                stopName = result.stopName
            )

            when (result.fieldType) {
                SearchStopFieldType.FROM -> {
                    viewModel.onEvent(SavedTripUiEvent.FromStopChanged(stopItem.toJsonString()))
                }
                SearchStopFieldType.TO -> {
                    viewModel.onEvent(SavedTripUiEvent.ToStopChanged(stopItem.toJsonString()))
                }
            }
        }

        SavedTripsScreen(
            savedTripsState = savedTripState,
            fromButtonClick = {
                viewModel.onEvent(SavedTripUiEvent.AnalyticsFromButtonClick)
                tripPlannerNavigator.navigateToSearchStop(SearchStopFieldType.FROM)
            },
            toButtonClick = {
                viewModel.onEvent(SavedTripUiEvent.AnalyticsToButtonClick)
                tripPlannerNavigator.navigateToSearchStop(SearchStopFieldType.TO)
            },
            onReverseButtonClick = {
                viewModel.onEvent(SavedTripUiEvent.ReverseStopClick)
            },
            onSavedTripCardClick = { fromStop, toStop ->
                if (fromStop?.stopId != null && toStop?.stopId != null) {
                    viewModel.onEvent(
                        SavedTripUiEvent.AnalyticsSavedTripCardClick(
                            fromStop.stopId,
                            toStop.stopId
                        )
                    )
                    tripPlannerNavigator.navigateToTimeTable(
                        fromStopId = fromStop.stopId,
                        fromStopName = fromStop.stopName,
                        toStopId = toStop.stopId,
                        toStopName = toStop.stopName
                    )
                }
            },
            onSearchButtonClick = {
                val fromStopItem = savedTripState.fromStop
                val toStopItem = savedTripState.toStop

                if (fromStopItem != null && toStopItem != null &&
                    fromStopItem.stopId != toStopItem.stopId) {
                    viewModel.onEvent(
                        SavedTripUiEvent.AnalyticsLoadTimeTableClick(
                            fromStopId = fromStopItem.stopId,
                            toStopId = toStopItem.stopId
                        )
                    )
                    tripPlannerNavigator.navigateToTimeTable(
                        fromStopId = fromStopItem.stopId,
                        fromStopName = fromStopItem.stopName,
                        toStopId = toStopItem.stopId,
                        toStopName = toStopItem.stopName
                    )
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
            onInviteFriendsTileDisplay = { viewModel.markInviteFriendsTileAsSeen() }
        )
    }
}

