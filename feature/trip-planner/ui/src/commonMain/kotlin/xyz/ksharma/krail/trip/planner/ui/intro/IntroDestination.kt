package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.trip.planner.ui.navigation.IntroRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SavedTripsRoute

fun NavGraphBuilder.introDestination(navController: NavHostController) {
    composable<IntroRoute> { backStackEntry ->
        val viewModel = koinViewModel<IntroViewModel>()
        val introState by viewModel.uiState.collectAsStateWithLifecycle()

        IntroScreen(
            state = introState,
            onComplete = {
                navController.navigate(
                    route = SavedTripsRoute,
                    navOptions = NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setPopUpTo<SavedTripsRoute>(inclusive = false)
                        .build(),
                )
            },
        ) { event -> viewModel.onEvent(event) }
    }
}
