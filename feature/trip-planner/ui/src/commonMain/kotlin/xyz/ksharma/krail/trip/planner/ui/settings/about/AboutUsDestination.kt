package xyz.ksharma.krail.trip.planner.ui.settings.about

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.trip.planner.ui.navigation.AboutUsRoute

internal fun NavGraphBuilder.aboutUsDestination(navController: NavHostController) {
    composable<AboutUsRoute> {

        val viewModel: AboutUsViewModel = koinViewModel<AboutUsViewModel>()
        val aboutUsState by viewModel.uiState.collectAsStateWithLifecycle()

        AboutUsScreen(
            state = aboutUsState,
            onBackClick = { navController.popBackStack() }
        )
    }
}
