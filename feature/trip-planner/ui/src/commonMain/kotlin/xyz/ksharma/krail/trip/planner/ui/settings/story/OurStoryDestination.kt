package xyz.ksharma.krail.trip.planner.ui.settings.story

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.trip.planner.ui.navigation.OurStoryRoute

internal fun NavGraphBuilder.aboutUsDestination(navController: NavHostController) {
    composable<OurStoryRoute> {

        val viewModel: OurStoryViewModel = koinViewModel<OurStoryViewModel>()
        val ourStoryState by viewModel.uiState.collectAsStateWithLifecycle()

        OurStoryScreen(
            state = ourStoryState,
            onBackClick = { navController.popBackStack() }
        )
    }
}
