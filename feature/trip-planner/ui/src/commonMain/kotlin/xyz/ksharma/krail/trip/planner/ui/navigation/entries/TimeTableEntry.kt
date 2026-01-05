package xyz.ksharma.krail.trip.planner.ui.navigation.entries

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.alerts.ServiceAlertScreen
import xyz.ksharma.krail.trip.planner.ui.datetimeselector.DateTimeSelectorScreen
import xyz.ksharma.krail.trip.planner.ui.navigation.TimeTableRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.TripPlannerNavigator
import xyz.ksharma.krail.trip.planner.ui.navigation.savers.dateTimeSelectionSaver
import xyz.ksharma.krail.trip.planner.ui.navigation.savers.serviceAlertSaver
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableUiEvent
import xyz.ksharma.krail.trip.planner.ui.timetable.TimeTableScreen
import xyz.ksharma.krail.trip.planner.ui.timetable.TimeTableViewModel

/**
 * TimeTable Entry - Detail Screen in List-Detail pattern
 *
 * Handles modal state for alerts and date/time selection.
 * Uses custom savers to preserve state across configuration changes.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Suppress("UNUSED_VARIABLE")
@Composable
internal fun EntryProviderScope<NavKey>.TimeTableEntry(
    tripPlannerNavigator: TripPlannerNavigator,
) {
    entry<TimeTableRoute>(
        metadata = ListDetailSceneStrategy.detailPane(),
    ) { key ->
        LaunchedEffect(key) {
            log("TimeTableRoute key: from-${key.fromStopId}, to: ${key.toStopId}")
        }
        val viewModel: TimeTableViewModel = koinViewModel()
        val timeTableState by viewModel.uiState.collectAsStateWithLifecycle()
        val expandedJourneyId by viewModel.expandedJourneyId.collectAsStateWithLifecycle()

        // CRITICAL: Must collect these to trigger flows' onStart blocks
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        val isActive by viewModel.isActive.collectAsStateWithLifecycle()
        val autoRefreshTimeTable by viewModel.autoRefreshTimeTable.collectAsStateWithLifecycle()

        // Modal visibility state
        var showAlertsModal by rememberSaveable { mutableStateOf(false) }
        var showDateTimeSelectorModal by rememberSaveable { mutableStateOf(false) }

        // Trip ID for saveable state scoping
        val tripId = remember(key.fromStopId, key.toStopId) {
            "${key.fromStopId}->${key.toStopId}"
        }

        // State for service alerts - survives rotation
        var alertsToDisplay by rememberSaveable(stateSaver = serviceAlertSaver()) {
            mutableStateOf(persistentSetOf())
        }

        // State for date/time selection - survives rotation but clears when trip changes
        var dateTimeSelectionItem by rememberSaveable(
            tripId,
            stateSaver = dateTimeSelectionSaver(),
        ) {
            mutableStateOf<DateTimeSelectionItem?>(null)
        }

        // Sync date/time selection with ViewModel on first composition
        LaunchedEffect(Unit) {
            viewModel.onEvent(TimeTableUiEvent.DateTimeSelectionChanged(dateTimeSelectionItem))
        }

        // Initialize trip when route changes
        LaunchedEffect(key.fromStopId, key.toStopId) {
            viewModel.initializeTrip(
                fromStopId = key.fromStopId,
                fromStopName = key.fromStopName,
                toStopId = key.toStopId,
                toStopName = key.toStopName,
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Main TimeTable Screen
            TimeTableScreen(
                timeTableState = timeTableState,
                expandedJourneyId = expandedJourneyId,
                dateTimeSelectionItem = dateTimeSelectionItem,
                onEvent = { event -> viewModel.onEvent(event) },
                onAlertClick = { journeyId ->
                    log("AlertClicked for journeyId: $journeyId")
                    viewModel.fetchAlertsForJourney(journeyId) { alerts ->
                        if (alerts.isNotEmpty()) {
                            alertsToDisplay = alerts.toPersistentSet()
                            showAlertsModal = true
                        }
                    }
                },
                onBackClick = { tripPlannerNavigator.goBack() },
                onJourneyLegClick = { expanded ->
                    // Track analytics when user expands/collapses journey legs
                    viewModel.onEvent(TimeTableUiEvent.AnalyticsJourneyLegClicked(expanded))
                },
                dateTimeSelectorClicked = {
                    // Track analytics for date/time selector
                    viewModel.onEvent(TimeTableUiEvent.AnalyticsDateTimeSelectorClicked)
                    showDateTimeSelectorModal = true
                },
                onModeSelectionChanged = { modes ->
                    viewModel.onEvent(TimeTableUiEvent.ModeSelectionChanged(modes))
                },
                onModeClick = { isVisible ->
                    viewModel.onEvent(TimeTableUiEvent.ModeClicked(isVisible))
                },
            )

            // Service Alerts Modal
            if (showAlertsModal) {
                ModalBottomSheet(
                    onDismissRequest = { showAlertsModal = false },
                    containerColor = KrailTheme.colors.bottomSheetBackground,
                    contentWindowInsets = {
                        WindowInsets(0, 0, 0, 0)
                    },
                ) {
                    ServiceAlertScreen(
                        serviceAlerts = alertsToDisplay,
                        onBackClick = { showAlertsModal = false },
                    )
                }
            }

            // Date/Time Selector Modal
            if (showDateTimeSelectorModal) {
                ModalBottomSheet(
                    onDismissRequest = { showDateTimeSelectorModal = false },
                    containerColor = KrailTheme.colors.bottomSheetBackground,
                    contentWindowInsets = {
                        WindowInsets(0, 0, 0, 0)
                    },
                ) {
                    DateTimeSelectorScreen(
                        dateTimeSelection = dateTimeSelectionItem,
                        onBackClick = {
                            showDateTimeSelectorModal = false
                        },
                        onDateTimeSelected = { selection ->
                            dateTimeSelectionItem = selection
                            viewModel.onEvent(TimeTableUiEvent.DateTimeSelectionChanged(selection))
                            showDateTimeSelectorModal = false
                        },
                        onResetClick = {
                            dateTimeSelectionItem = null
                            viewModel.onEvent(TimeTableUiEvent.DateTimeSelectionChanged(null))
                            showDateTimeSelectorModal = false
                        },
                    )
                }
            }
        }
    }
}
