package xyz.ksharma.krail.trip.planner.ui.entries

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.trip.planner.ui.intro.IntroScreen
import xyz.ksharma.krail.trip.planner.ui.intro.IntroViewModel
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroUiEvent

/**
 * Intro Entry - Detail Screen in List-Detail pattern
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun EntryProviderScope<NavKey>.IntroEntry(
    tripPlannerNavigator: TripPlannerNavigator,
) {
    entry<IntroRoute>(
        metadata = androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy.detailPane(),
    ) {
        val viewModel = koinViewModel<IntroViewModel>()
        val introState by viewModel.uiState.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()

        IntroScreen(
            state = introState,
            onIntroComplete = { pageType, pageNumber ->
                scope.launch {
                    viewModel.onEvent(IntroUiEvent.Complete(pageType, pageNumber))
                    // Clear entire back stack - user shouldn't go back to intro
                    tripPlannerNavigator.clearBackStackAndNavigate(SavedTripsRoute)
                }
            },
        ) { event -> viewModel.onEvent(event) }
    }
}
