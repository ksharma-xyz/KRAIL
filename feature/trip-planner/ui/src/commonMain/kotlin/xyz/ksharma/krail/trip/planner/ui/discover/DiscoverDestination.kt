package xyz.ksharma.krail.trip.planner.ui.discover

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.trip.planner.ui.navigation.DiscoverRoute

internal fun NavGraphBuilder.discoverDestination(navController: NavHostController) {
    composable<DiscoverRoute> {
        val viewModel: DiscoverViewModel = koinViewModel<DiscoverViewModel>()
        val discoverState by viewModel.uiState.collectAsStateWithLifecycle()

        DiscoverScreen(
            state = discoverState,
            onBackClick = {
                navController.navigateUp()
            }
        )
    }
}
