package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopRoute
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.SearchStopUiEvent

fun NavGraphBuilder.searchStopDestination(navController: NavHostController) {
    composable<SearchStopRoute> { backStackEntry ->
        val viewModel: SearchStopViewModel = koinViewModel<SearchStopViewModel>()
        val searchStopState by viewModel.uiState.collectAsStateWithLifecycle()
        val route: SearchStopRoute = backStackEntry.toRoute()

        SearchStopScreen(
            searchStopState = searchStopState,
            onStopSelect = { stopItem ->
                // log(("onStopSelected: fieldTypeKey=${route.fieldType.key} and stopItem: $stopItem")
                viewModel.onEvent(SearchStopUiEvent.StopSelected(stopItem))

                navController.previousBackStackEntry?.savedStateHandle?.set(
                    route.fieldType.key,
                    stopItem.toJsonString(),
                )
                navController.popBackStack()
            },
            goBack = {
                navController.popBackStack()
            },
            onEvent = { event -> viewModel.onEvent(event) }
        )
    }
}
