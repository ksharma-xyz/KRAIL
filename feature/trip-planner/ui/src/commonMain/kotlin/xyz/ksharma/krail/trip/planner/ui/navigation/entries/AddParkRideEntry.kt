package xyz.ksharma.krail.trip.planner.ui.navigation.entries

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.trip.planner.ui.navigation.AddParkRideRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.TripPlannerNavigator
import xyz.ksharma.krail.trip.planner.ui.parkride.AddParkRideScreen
import xyz.ksharma.krail.trip.planner.ui.parkride.AddParkRideViewModel

@Composable
internal fun EntryProviderScope<NavKey>.AddParkRideEntry(
    tripPlannerNavigator: TripPlannerNavigator,
) {
    entry<AddParkRideRoute> {
        val viewModel: AddParkRideViewModel = koinViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        AddParkRideScreen(
            state = state,
            onBackClick = { tripPlannerNavigator.goBack() },
            onEvent = viewModel::onEvent,
        )
    }
}
