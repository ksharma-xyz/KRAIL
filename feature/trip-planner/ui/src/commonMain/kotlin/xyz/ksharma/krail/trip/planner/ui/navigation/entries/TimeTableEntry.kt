package xyz.ksharma.krail.trip.planner.ui.navigation.entries

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.core.adaptiveui.rememberAdaptiveLayoutInfo
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.taj.components.ModalBottomSheet
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.alerts.ServiceAlertScreen
import xyz.ksharma.krail.trip.planner.ui.datetimeselector.DateTimeSelectorScreen
import xyz.ksharma.krail.trip.planner.ui.journeymap.JourneyMap
import xyz.ksharma.krail.trip.planner.ui.journeymap.business.JourneyMapMapper.toJourneyMapState
import xyz.ksharma.krail.trip.planner.ui.navigation.TimeTableRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.TripPlannerNavigator
import xyz.ksharma.krail.trip.planner.ui.navigation.savers.dateTimeSelectionSaver
import xyz.ksharma.krail.trip.planner.ui.navigation.savers.serviceAlertSaver
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyMapUiState
import xyz.ksharma.krail.trip.planner.ui.state.timetable.TimeTableUiEvent
import xyz.ksharma.krail.trip.planner.ui.timetable.TimeTableScreen
import xyz.ksharma.krail.trip.planner.ui.timetable.TimeTableViewModel

/**
 * Renders full-width on every form factor. On tablets / foldable-unfolded / phone
 * landscape (≥ 600 dp width), the screen splits internally: journey list on the left
 * (capped width), persistent JourneyMap on the right. The per-card "Maps" button is
 * suppressed in dual-pane because the map is always visible.
 *
 * See docs/TABLET_FOLDABLE_UX.md §3.
 */
@Suppress("UNUSED_VARIABLE", "LongMethod")
@Composable
internal fun EntryProviderScope<NavKey>.TimeTableEntry(
    tripPlannerNavigator: TripPlannerNavigator,
) {
    entry<TimeTableRoute> { key ->
        LaunchedEffect(key) {
            log("🗺️ TimeTableEntry - key changed: from-${key.fromStopId}, to: ${key.toStopId}")
        }
        val viewModel: TimeTableViewModel = koinViewModel()
        log("🗺️ TimeTableEntry - ViewModel instance: ${viewModel.hashCode()}")

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
            log("🗺️ TimeTableEntry - LaunchedEffect triggered, initializing trip")
            viewModel.initializeTrip(
                fromStopId = key.fromStopId,
                fromStopName = key.fromStopName,
                toStopId = key.toStopId,
                toStopName = key.toStopName,
            )
        }

        // Adaptive layout — dual-pane on ≥ 600 dp width
        val adaptiveLayoutInfo = rememberAdaptiveLayoutInfo()
        val dualPane = adaptiveLayoutInfo.shouldShowDualPane

        // Map state for the currently expanded journey. produceState recomputes whenever
        // expandedJourneyId changes — the JourneyMap composable stays mounted in the right
        // pane, only its data layer re-renders.
        val journeyMapState by produceState<JourneyMapUiState?>(
            initialValue = null,
            key1 = expandedJourneyId,
        ) {
            value = expandedJourneyId?.let { id ->
                withContext(Dispatchers.Default) {
                    viewModel.getRawJourneyById(id)?.toJourneyMapState()
                }
            }
        }

        val timeTableScreen: @Composable (Modifier, Boolean) -> Unit = { mod, hideMapBtn ->
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
                onJourneyLegClick = { expanded, transportMode, lineName ->
                    viewModel.onEvent(
                        TimeTableUiEvent.AnalyticsJourneyLegClicked(
                            expanded = expanded,
                            transportMode = transportMode,
                            lineName = lineName,
                        ),
                    )
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
                onMapClick = { journeyId ->
                    tripPlannerNavigator.navigateToJourneyMap(journeyId)
                },
                hideMapButton = hideMapBtn,
                modifier = mod,
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (dualPane) {
                Row(modifier = Modifier.fillMaxSize()) {
                    timeTableScreen(
                        Modifier
                            .widthIn(max = TIMETABLE_PANE_MAX_WIDTH)
                            .fillMaxHeight(),
                        true,
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(color = KrailTheme.colors.surface),
                    ) {
                        when (val mapState = journeyMapState) {
                            is JourneyMapUiState.Ready -> {
                                JourneyMap(
                                    journeyMapState = mapState,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            else -> {
                                Text(
                                    text = "Tap a journey to see the route",
                                    style = KrailTheme.typography.bodyLarge,
                                    color = KrailTheme.colors.onSurface,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(KrailTheme.dimensions.spacingXL)
                                        .fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            } else {
                timeTableScreen(Modifier.fillMaxSize(), false)
            }

            // Service Alerts Modal
            if (showAlertsModal) {
                ModalBottomSheet(
                    onDismissRequest = { showAlertsModal = false },
                    containerColor = KrailTheme.colors.bottomSheetBackground,
                    modifier = Modifier,
                    contentWindowInsets = {
                        WindowInsets(0, 0, 0, 0)
                    },
                ) {
                    ServiceAlertScreen(
                        serviceAlerts = alertsToDisplay,
                    )
                }
            }

            // Date/Time Selector Modal
            if (showDateTimeSelectorModal) {
                ModalBottomSheet(
                    onDismissRequest = { showDateTimeSelectorModal = false },
                    containerColor = KrailTheme.colors.bottomSheetBackground,
                    modifier = Modifier,
                    contentWindowInsets = {
                        WindowInsets(0, 0, 0, 0)
                    },
                ) {
                    DateTimeSelectorScreen(
                        dateTimeSelection = dateTimeSelectionItem,
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

// Left-pane width cap in dual-pane. Keeps journey cards at phone-width proportions
// (≈ 520 dp) so they don't stretch awkwardly on tablets; map fills the remainder.
private val TIMETABLE_PANE_MAX_WIDTH = 520.dp
