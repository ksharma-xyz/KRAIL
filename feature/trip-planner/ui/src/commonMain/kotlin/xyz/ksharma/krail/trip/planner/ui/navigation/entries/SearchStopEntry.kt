package xyz.ksharma.krail.trip.planner.ui.navigation.entries

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.core.navigation.LocalResultEventBusObj
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.StopSelectedResult
import xyz.ksharma.krail.trip.planner.ui.navigation.TripPlannerNavigator
import xyz.ksharma.krail.trip.planner.ui.searchstop.SearchStopScreen
import xyz.ksharma.krail.trip.planner.ui.searchstop.SearchStopViewModel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent

/**
 * Search Stop Entry - Detail Screen in List-Detail pattern
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun EntryProviderScope<NavKey>.SearchStopEntry(
    tripPlannerNavigator: TripPlannerNavigator,
) {
    entry<SearchStopRoute>(
        metadata = ListDetailSceneStrategy.listPane(),
    ) { key ->
        // ViewModel is scoped to this NavEntry and will be recreated each time
        // This ensures recent stops are refreshed when the screen is opened
        val viewModel: SearchStopViewModel = koinViewModel()
        val searchStopState by viewModel.uiState.collectAsStateWithLifecycle()

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
            onStopSelect = { stopItem ->
                // Send result using captured bus reference
                val result = StopSelectedResult(
                    fieldType = key.fieldType,
                    stopId = stopItem.stopId,
                    stopName = stopItem.stopName,
                )

                resultEventBus.sendResult(result = result)

                // Navigate back
                tripPlannerNavigator.goBack()
            },
            goBack = {
                tripPlannerNavigator.goBack()
            },
            onEvent = { event -> viewModel.onEvent(event) },
        )
    }
}
