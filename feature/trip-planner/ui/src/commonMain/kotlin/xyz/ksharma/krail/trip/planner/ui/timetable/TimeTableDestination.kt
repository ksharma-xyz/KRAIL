package xyz.ksharma.krail.trip.planner.ui.timetable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.core.deeplink.DeepLinkManager
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.trip.planner.ui.navigation.DateTimeSelectorRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SavedTripsRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.ServiceAlertRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.TimeTableRoute
import xyz.ksharma.krail.trip.planner.ui.searchstop.StopResultsManager
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

        // Log initial navigation
        LogNavigationDetails(route, isLoading)

        // Handle deep link stop name resolution
        val resolvedRoute = useStopNameResolution(route, viewModel)

        // Handle deep link lifecycle tracking
        UseDeepLinkTracking(route)

        // Handle initial loading
        UseInitialLoading(isLoading, resolvedRoute, viewModel)

        // Collect additional UI state
        val expandedJourneyId by viewModel.expandedJourneyId.collectAsStateWithLifecycle()

        // Handle date time selection
        val dateTimeSelectionItem = useDateTimeSelection(backStackEntry, viewModel)

        log("TimeTableDestination: Rendering TimeTableScreen with state isError=${timeTableState.isError}, journeyListSize=${timeTableState.journeyList.size}")

        TimeTableScreen(
            timeTableState = timeTableState,
            expandedJourneyId = expandedJourneyId,
            onEvent = { viewModel.onEvent(it) },
            onBackClick = { handleBackNavigation(navController, viewModel) },
            onAlertClick = { journeyId -> handleAlertClick(journeyId, navController, viewModel) },
            dateTimeSelectionItem = dateTimeSelectionItem,
            dateTimeSelectorClicked = { handleDateTimeSelectorClick(dateTimeSelectionItem, navController, viewModel) },
            onJourneyLegClick = { journeyId -> viewModel.onEvent(TimeTableUiEvent.JourneyLegClicked(journeyId)) },
            onModeSelectionChanged = { unselectedModes -> handleModeSelectionChanged(unselectedModes, viewModel) },
            onModeClick = { displayModeSelectionRow -> viewModel.onEvent(TimeTableUiEvent.ModeClicked(displayModeSelectionRow)) }
        )
    }
}

@Composable
private fun LogNavigationDetails(route: TimeTableRoute, isLoading: Boolean) {
    log("TimeTableDestination: Navigated to TimeTable screen with route: $route")
    log("TimeTableDestination: fromStopId=${route.fromStopId}, toStopId=${route.toStopId}")
    log("TimeTableDestination: fromStopName='${route.fromStopName}', toStopName='${route.toStopName}'")
    log("TimeTableDestination: isLoading=$isLoading")
}

@Composable
private fun useStopNameResolution(
    route: TimeTableRoute,
    viewModel: TimeTableViewModel
): TimeTableRoute {
    val stopResultsManager: StopResultsManager = org.koin.compose.koinInject()
    var resolvedRoute by remember(route) { mutableStateOf(route) }

    LaunchedEffect(route) {
        var needsUpdate = false
        var updatedFromStopName = route.fromStopName
        var updatedToStopName = route.toStopName

        // Check if fromStopName is missing and resolve it
        if (route.fromStopName.isBlank() && route.fromStopId.isNotBlank()) {
            log("TimeTableDestination: Resolving missing fromStopName for stopId: ${route.fromStopId}")
            stopResultsManager.fetchLocalStopName(route.fromStopId)?.let { stopName ->
                log("TimeTableDestination: Resolved fromStopName: $stopName")
                updatedFromStopName = stopName
                needsUpdate = true
            } ?: log("TimeTableDestination: Could not resolve fromStopName for stopId: ${route.fromStopId}")
        }

        // Check if toStopName is missing and resolve it
        if (route.toStopName.isBlank() && route.toStopId.isNotBlank()) {
            log("TimeTableDestination: Resolving missing toStopName for stopId: ${route.toStopId}")
            stopResultsManager.fetchLocalStopName(route.toStopId)?.let { stopName ->
                log("TimeTableDestination: Resolved toStopName: $stopName")
                updatedToStopName = stopName
                needsUpdate = true
            } ?: log("TimeTableDestination: Could not resolve toStopName for stopId: ${route.toStopId}")
        }

        // Update the resolved route if any names were found
        if (needsUpdate) {
            resolvedRoute = route.copy(
                fromStopName = updatedFromStopName,
                toStopName = updatedToStopName
            )
            log("TimeTableDestination: Updated route with resolved stop names: $resolvedRoute")

            // Update the ViewModel with the resolved trip information
            log("TimeTableDestination: Updating ViewModel with resolved trip")
            viewModel.onEvent(TimeTableUiEvent.UpdateTripFromDeepLink(trip = resolvedRoute.toTrip()))
        }
    }

    return resolvedRoute
}

@Composable
private fun UseDeepLinkTracking(route: TimeTableRoute) {
    val deepLinkManager: DeepLinkManager = org.koin.compose.koinInject()
    val routeKey = "TimeTable_${route.fromStopId}_${route.toStopId}"

    DisposableEffect(routeKey) {
        onDispose {
            log("TimeTableDestination: Screen disposed, notifying DeepLinkManager")
            deepLinkManager.onNavigatedAwayFromDeepLinkedScreen(routeKey)
        }
    }
}

@Composable
private fun UseInitialLoading(
    isLoading: Boolean,
    resolvedRoute: TimeTableRoute,
    viewModel: TimeTableViewModel
) {
    if (isLoading) {
        log("TimeTableDestination: Loading timetable data for trip: ${resolvedRoute.toTrip()}")
        viewModel.onEvent(TimeTableUiEvent.LoadTimeTable(trip = resolvedRoute.toTrip()))
    }
}

@Composable
private fun useDateTimeSelection(
    backStackEntry: NavBackStackEntry,
    viewModel: TimeTableViewModel
): DateTimeSelectionItem? {
    val dateTimeSelectionJson: String? =
        backStackEntry.savedStateHandle.get(key = DateTimeSelectorRoute.DATE_TIME_TEXT_KEY)

    var dateTimeSelectionItem: DateTimeSelectionItem? by remember {
        mutableStateOf(dateTimeSelectionJson?.let { fromJsonString(it) })
    }

    LaunchedEffect(dateTimeSelectionJson) {
        log("TimeTableDestination: DateTimeSelection changed: $dateTimeSelectionItem")
        dateTimeSelectionItem = dateTimeSelectionJson?.let { fromJsonString(it) }
        viewModel.onEvent(TimeTableUiEvent.DateTimeSelectionChanged(dateTimeSelectionItem))
    }

    return dateTimeSelectionItem
}

private fun handleBackNavigation(
    navController: NavHostController,
    viewModel: TimeTableViewModel
) {
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
        navController.navigate(SavedTripsRoute)
    }
}

private fun handleAlertClick(
    journeyId: String,
    navController: NavHostController,
    viewModel: TimeTableViewModel
) {
    log("AlertClicked for journeyId: $journeyId")
    viewModel.fetchAlertsForJourney(journeyId) { alerts ->
        if (alerts.isNotEmpty()) {
            navController.navigate(ServiceAlertRoute(journeyId = journeyId))
        }
    }
}

private fun handleDateTimeSelectorClick(
    dateTimeSelectionItem: DateTimeSelectionItem?,
    navController: NavHostController,
    viewModel: TimeTableViewModel
) {
    viewModel.onEvent(TimeTableUiEvent.AnalyticsDateTimeSelectorClicked)
    navController.navigate(DateTimeSelectorRoute(dateTimeSelectionItem?.toJsonString()))
}

private fun handleModeSelectionChanged(
    unselectedModes: Set<Int>,
    viewModel: TimeTableViewModel
) {
    log("onModeSelectionChanged Exclude :$unselectedModes")
    viewModel.onEvent(TimeTableUiEvent.ModeSelectionChanged(unselectedModes))
}

private fun TimeTableRoute.toTrip() = Trip(
    fromStopId = fromStopId,
    fromStopName = fromStopName,
    toStopId = toStopId,
    toStopName = toStopName,
)
