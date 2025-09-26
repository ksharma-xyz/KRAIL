package xyz.ksharma.krail.trip.planner.ui.timetable

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.trip.planner.ui.navigation.DateTimeSelectorRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.ServiceAlertRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.TimeTableRoute
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem.Companion.fromJsonString
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.timetable.Trip

internal fun NavGraphBuilder.timeTableDestination(navController: NavHostController) {
    composable<TimeTableRoute> { backStackEntry ->
        val viewModel: TimeTableViewModel = koinViewModel<TimeTableViewModel>()
        val timeTableState by viewModel.uiState.collectAsStateWithLifecycle()
        val route: TimeTableRoute = backStackEntry.toRoute()
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        if (isLoading) {
            viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip = route.toTrip()))
        }
        // Subscribe to the isActive state flow - for updating the TimeText periodically.
        val isActive by viewModel.isActive.collectAsStateWithLifecycle()
        val autoRefreshTimeTable by viewModel.autoRefreshTimeTable.collectAsStateWithLifecycle()
        val expandedJourneyId: String? by viewModel.expandedJourneyId.collectAsStateWithLifecycle()

        // Arguments
        // Cannot use 'rememberSaveable' here because DateTimeSelectionItem is not Parcelable.
        // But it's saved in backStackEntry.savedStateHandle as json, so it's able to
        // handle config changes properly.
        val dateTimeSelectionJson: String? =
            backStackEntry.savedStateHandle.get(key = DateTimeSelectorRoute.DATE_TIME_TEXT_KEY)
        var dateTimeSelectionItem: DateTimeSelectionItem? by remember {
            mutableStateOf(dateTimeSelectionJson?.let { fromJsonString(it) })
        }

        // Lookout for new updates
        LaunchedEffect(dateTimeSelectionJson) {
            log("Changed dateTimeSelectionItem: $dateTimeSelectionItem")
            dateTimeSelectionItem = dateTimeSelectionJson?.let { fromJsonString(it) }
            viewModel.onEvent(TimeTableUiEvent.DateTimeSelectionChanged(dateTimeSelectionItem))
        }

        TimeTableScreen(
            timeTableState = timeTableState,
            expandedJourneyId = expandedJourneyId,
            onEvent = { viewModel.onEvent(it) },
            onBackClick = {
                viewModel.onEvent(
                    TimeTableUiEvent.BackClick(
                        isPreviousBackStackEntryNull = navController.previousBackStackEntry == null
                    )
                )

                navController.navigateUp()
            },
            onAlertClick = { journeyId ->
                log("AlertClicked for journeyId: $journeyId")
                viewModel.fetchAlertsForJourney(journeyId) { alerts ->
                    if (alerts.isNotEmpty()) {
                        navController.navigate(
                            route = ServiceAlertRoute(journeyId = journeyId),
                            navOptions = NavOptions.Builder().setLaunchSingleTop(singleTop = true)
                                .build(),
                        )
                    }
                }
            },
            dateTimeSelectionItem = dateTimeSelectionItem,
            dateTimeSelectorClicked = {
                viewModel.onEvent(TimeTableUiEvent.AnalyticsDateTimeSelectorClicked)
                navController.navigate(
                    route = DateTimeSelectorRoute(dateTimeSelectionItem?.toJsonString()),
                    navOptions = NavOptions.Builder().setLaunchSingleTop(singleTop = true).build(),
                )
            },
            onJourneyLegClick = { journeyId ->
                viewModel.onEvent(TimeTableUiEvent.JourneyLegClicked(journeyId))
            },
            onModeSelectionChanged = { unselectedModes ->
                log("onModeSelectionChanged Exclude :$unselectedModes")
                viewModel.onEvent(TimeTableUiEvent.ModeSelectionChanged(unselectedModes))
            },
            onModeClick = { displayModeSelectionRow ->
                viewModel.onEvent(TimeTableUiEvent.ModeClicked(displayModeSelectionRow))
            }
        )
    }
}

private fun TimeTableRoute.toTrip() = Trip(
    fromStopId = fromStopId,
    fromStopName = fromStopName,
    toStopId = toStopId,
    toStopName = toStopName,
)
