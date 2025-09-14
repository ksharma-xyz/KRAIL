package xyz.ksharma.krail.trip.planner.ui.timetable

import androidx.compose.runtime.DisposableEffect
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
import xyz.ksharma.krail.core.deeplink.DeepLinkManager
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.trip.planner.ui.navigation.DateTimeSelectorRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SavedTripsRoute
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

        log("TimeTableDestination: Navigated to TimeTable screen with route: $route")
        log("TimeTableDestination: fromStopId=${route.fromStopId}, toStopId=${route.toStopId}")
        log("TimeTableDestination: fromStopName='${route.fromStopName}', toStopName='${route.toStopName}'")
        log("TimeTableDestination: isLoading=$isLoading")

        // Track navigation state for deep link management
        val routeKey = "TimeTable_${route.fromStopId}_${route.toStopId}"

        // Inject DeepLinkManager to track navigation state
        val deepLinkManager: DeepLinkManager = org.koin.compose.koinInject()

        // Notify when leaving this screen
        DisposableEffect(routeKey) {
            onDispose {
                log("TimeTableDestination: Screen disposed, notifying DeepLinkManager")
                deepLinkManager.onNavigatedAwayFromDeepLinkedScreen(routeKey)
            }
        }

        if (isLoading) {
            log("TimeTableDestination: Loading timetable data for trip: ${route.toTrip()}")
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
            log("TimeTableDestination: DateTimeSelection changed: $dateTimeSelectionItem")
            dateTimeSelectionItem = dateTimeSelectionJson?.let { fromJsonString(it) }
            viewModel.onEvent(TimeTableUiEvent.DateTimeSelectionChanged(dateTimeSelectionItem))
        }

        log("TimeTableDestination: Rendering TimeTableScreen with state isError=${timeTableState.isError}, journeyListSize=${timeTableState.journeyList.size}")

        TimeTableScreen(
            timeTableState = timeTableState,
            expandedJourneyId = expandedJourneyId,
            onEvent = { viewModel.onEvent(it) },
            onBackClick = {
                log("TimeTableDestination: Back button clicked")
                viewModel.onEvent(
                    TimeTableUiEvent.BackClick(
                        isPreviousBackStackEntryNull = navController.previousBackStackEntry == null
                    )
                )

                // Use proper back navigation instead of forcing SavedTripsRoute
                if (navController.previousBackStackEntry != null) {
                    log("TimeTableDestination: Navigating back to previous screen")
                    navController.popBackStack()
                } else {
                    log("TimeTableDestination: No previous entry, navigating to SavedTripsRoute")
                    navController.navigate(
                        route = SavedTripsRoute,
                        navOptions = NavOptions.Builder().setLaunchSingleTop(true).build(),
                    )
                }
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
