package xyz.ksharma.krail.trip.planner.ui.navigation.entries

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.core.navigation.LocalResultEventBusObj
import xyz.ksharma.krail.trip.planner.ui.mapstopselection.MapStopSelectionViewModel
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.StopSelectedResult
import xyz.ksharma.krail.trip.planner.ui.navigation.TimetableStopChangedResult
import xyz.ksharma.krail.trip.planner.ui.navigation.TripPlannerNavigator
import xyz.ksharma.krail.trip.planner.ui.searchstop.SearchStopScreen
import xyz.ksharma.krail.trip.planner.ui.searchstop.SearchStopViewModel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent

/**
 * Renders full-width on every form factor. SearchStopScreen owns its own
 * list + map split internally via AdaptiveScreenContent, so this entry must
 * not declare ListDetailSceneStrategy.listPane() — doing so would halve the
 * window before the screen's own dual-pane halves it again, producing the
 * cramped ~25 % / ~25 % layout on tablets and phone landscape.
 * See docs/TABLET_FOLDABLE_UX.md §2.
 */
@Composable
internal fun EntryProviderScope<NavKey>.SearchStopEntry(
    tripPlannerNavigator: TripPlannerNavigator,
) {
    entry<SearchStopRoute> { key ->
        // ViewModel is scoped to this NavEntry and will be recreated each time
        // This ensures recent stops are refreshed when the screen is opened
        val viewModel: SearchStopViewModel = koinViewModel()
        val searchStopState by viewModel.uiState.collectAsStateWithLifecycle()

        // Shared singleton ViewModel for the dual-pane right-pane map. Injected here
        // (entry level) so the composable tree never touches Koin directly.
        val mapStopSelectionViewModel: MapStopSelectionViewModel = koinViewModel()
        val mapUiState by mapStopSelectionViewModel.mapUiState.collectAsStateWithLifecycle()

        // Refresh recent stops every time screen opens.
        // Note: Cannot rely on StateFlow's onStart because WhileSubscribed(5000) keeps the
        // flow alive for 5 seconds after screen closes. If user returns within that window,
        // onStart won't run again and recent stops won't refresh.
        LaunchedEffect(Unit) {
            viewModel.onEvent(SearchStopUiEvent.RefreshRecentStopsList)
        }

        // Capture ResultEventBus in composable scope for use in callbacks
        // This uses the singleton instance to ensure results reach SavedTrips
        // even in two-pane layouts where screens are in different composition scopes
        val resultEventBus = LocalResultEventBusObj.current

        SearchStopScreen(
            searchStopState = searchStopState,
            fieldType = key.fieldType,
            editTripLeg = key.editTripLeg,
            onStopSelect = { stopItem ->
                if (key.editTripLeg) {
                    // Leg-scoped edit from the timetable header — deliver on a
                    // dedicated channel so SavedTrips' StopSelectedResult
                    // listener never consumes it.
                    resultEventBus.sendResult(
                        result = TimetableStopChangedResult(
                            isOrigin = key.fieldType == SearchStopFieldType.FROM,
                            stopId = stopItem.stopId,
                            stopName = stopItem.stopName,
                        ),
                    )
                } else {
                    // Send result using captured bus reference
                    resultEventBus.sendResult(
                        result = StopSelectedResult(
                            fieldType = key.fieldType,
                            stopId = stopItem.stopId,
                            stopName = stopItem.stopName,
                            labelKey = key.labelKey,
                            locationKind = stopItem.locationKind,
                            addressType = stopItem.addressType,
                        ),
                    )
                }

                // Navigate back
                tripPlannerNavigator.goBack()
            },
            goBack = {
                tripPlannerNavigator.goBack()
            },
            onManageLabelsClick = { tripPlannerNavigator.navigateToManageStopLabels() },
            onEvent = { event -> viewModel.onEvent(event) },
            dualPaneMapUiState = mapUiState,
            onDualPaneMapEvent = mapStopSelectionViewModel::onEvent,
        )
    }
}
