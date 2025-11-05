package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.trip.planner.ui.navigation.DiscoverRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SavedTripsRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SettingsRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.TimeTableRoute
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent

@Suppress("LongMethod")
internal fun NavGraphBuilder.savedTripsDestination(navController: NavHostController) {
    composable<SavedTripsRoute> { backStackEntry ->
        // Use a stable key to ensure ViewModel survives navigation within the nav graph
        val viewModel: SavedTripsViewModel =
            koinViewModel<SavedTripsViewModel>(key = "SavedTripsNav")
        val savedTripState by viewModel.uiState.collectAsStateWithLifecycle()

        val fromArg: String? =
            backStackEntry.savedStateHandle.get<String>(SearchStopFieldType.FROM.key)
        val toArg: String? =
            backStackEntry.savedStateHandle.get<String>(SearchStopFieldType.TO.key)

        LaunchedEffect(fromArg) {
            log("StopItem - fromArg changed: $fromArg")
            fromArg?.let { json ->
                viewModel.onEvent(SavedTripUiEvent.FromStopChanged(json))
                // Clear after processing
                backStackEntry.savedStateHandle.remove<String>(SearchStopFieldType.FROM.key)
            }
        }

        LaunchedEffect(toArg) {
            log("StopItem - toArg changed: $toArg")
            toArg?.let { json ->
                viewModel.onEvent(SavedTripUiEvent.ToStopChanged(json))
                // Clear after processing
                backStackEntry.savedStateHandle.remove<String>(SearchStopFieldType.TO.key)
            }
        }

        SavedTripsScreen(
            savedTripsState = savedTripState,
            fromButtonClick = {
                viewModel.onEvent(SavedTripUiEvent.AnalyticsFromButtonClick)
                navController.navigate(
                    route = SearchStopRoute(fieldTypeKey = SearchStopFieldType.FROM.key),
                    navOptions = NavOptions.Builder().setLaunchSingleTop(true).build(),
                )
            },
            toButtonClick = {
                viewModel.onEvent(SavedTripUiEvent.AnalyticsToButtonClick)
                navController.navigate(
                    route = SearchStopRoute(fieldTypeKey = SearchStopFieldType.TO.key),
                    navOptions = NavOptions.Builder().setLaunchSingleTop(true).build(),
                )
            },
            onReverseButtonClick = {
                viewModel.onEvent(SavedTripUiEvent.ReverseStopClick)
            },
            onSavedTripCardClick = { fromStop, toStop ->
                if (fromStop?.stopId != null && toStop?.stopId != null) {
                    viewModel.onEvent(
                        SavedTripUiEvent.AnalyticsSavedTripCardClick(fromStop.stopId, toStop.stopId),
                    )
                    navController.navigate(
                        route = TimeTableRoute(
                            fromStopId = fromStop.stopId,
                            fromStopName = fromStop.stopName,
                            toStopId = toStop.stopId,
                            toStopName = toStop.stopName,
                        ),
                        navOptions = NavOptions.Builder().setLaunchSingleTop(true).build(),
                    )
                }
            },
            onSearchButtonClick = {
                val fromStopItem = savedTripState.fromStop
                val toStopItem = savedTripState.toStop

                if (fromStopItem != null && toStopItem != null && fromStopItem.stopId != toStopItem.stopId) {
                    viewModel.onEvent(
                        SavedTripUiEvent.AnalyticsLoadTimeTableClick(
                            fromStopId = fromStopItem.stopId,
                            toStopId = toStopItem.stopId,
                        ),
                    )
                    navController.navigate(
                        route = TimeTableRoute(
                            fromStopId = fromStopItem.stopId,
                            fromStopName = fromStopItem.stopName,
                            toStopId = toStopItem.stopId,
                            toStopName = toStopItem.stopName,
                        ),
                        navOptions = NavOptions.Builder().setLaunchSingleTop(true).build(),
                    )
                }
            },
            onSettingsButtonClick = {
                viewModel.onEvent(SavedTripUiEvent.AnalyticsSettingsButtonClick)
                navController.navigate(
                    route = SettingsRoute,
                    navOptions = NavOptions.Builder().setLaunchSingleTop(true).build(),
                )
            },
            onDiscoverButtonClick = {
                viewModel.onEvent(SavedTripUiEvent.AnalyticsDiscoverButtonClick)
                navController.navigate(
                    route = DiscoverRoute,
                    navOptions = NavOptions.Builder().setLaunchSingleTop(true).build(),
                )
            },
            onEvent = { event -> viewModel.onEvent(event) },
            onInviteFriendsTileDisplayed = { viewModel.markInviteFriendsTileAsSeen() },
        )
    }
}
