package xyz.ksharma.krail.trip.planner.ui.settings.story

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.trip.planner.ui.navigation.OurStoryRoute
import xyz.ksharma.krail.trip.planner.ui.state.settings.story.OurStoryState

internal fun NavGraphBuilder.aboutUsDestination(navController: NavHostController) {
    composable<OurStoryRoute> {
        val viewModel: OurStoryViewModel = koinViewModel<OurStoryViewModel>()
        val ourStoryState: OurStoryState by viewModel.models.collectAsState()

        OurStoryScreen(
            state = ourStoryState,
            onBackClick = { navController.popBackStack() },
        )
    }
}
