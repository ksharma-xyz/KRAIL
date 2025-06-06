package xyz.ksharma.krail.trip.planner.ui.datetimeselector

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.trip.planner.ui.navigation.DateTimeSelectorRoute
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectionItem
import xyz.ksharma.krail.trip.planner.ui.state.datetimeselector.DateTimeSelectorEvent

internal fun NavGraphBuilder.dateTimeSelectorDestination(navController: NavHostController) {
    composable<DateTimeSelectorRoute> { backStackEntry ->
        val route: DateTimeSelectorRoute = backStackEntry.toRoute()
        val viewModel: DateTimeSelectorViewModel = koinViewModel<DateTimeSelectorViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        DateTimeSelectorScreen(
            dateTimeSelection = route.dateTimeSelectionItemJson?.let {
                DateTimeSelectionItem.fromJsonString(it)
            },
            onBackClick = {
                navController.popBackStack()
            },
            onDateTimeSelected = { dateTimeSelection ->
                navController.previousBackStackEntry?.savedStateHandle?.set(
                    key = DateTimeSelectorRoute.DATE_TIME_TEXT_KEY,
                    value = dateTimeSelection?.toJsonString(),
                )
                navController.popBackStack()
            },
            onResetClick = {
                viewModel.onEvent(DateTimeSelectorEvent.ResetDateTimeClick)
            },
        )
    }
}
