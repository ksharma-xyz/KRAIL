package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.trip.planner.ui.navigation.IntroRoute

fun NavGraphBuilder.introDestination(navController: NavHostController) {
    composable<IntroRoute> { backStackEntry ->
        val viewModel = koinViewModel<IntroViewModel>()
        val introState by viewModel.uiState.collectAsStateWithLifecycle()
        val route: IntroRoute = backStackEntry.toRoute()

        IntroScreen(
            state = introState,
        ) { event -> viewModel.onEvent(event) }
    }
}
