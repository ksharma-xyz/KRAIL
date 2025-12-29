package xyz.ksharma.krail.trip.planner.ui.entries

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.trip.planner.ui.settings.story.OurStoryScreen
import xyz.ksharma.krail.trip.planner.ui.settings.story.OurStoryViewModel

/**
 * Our Story Entry - Detail Screen in List-Detail pattern
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun EntryProviderScope<NavKey>.OurStoryEntry(
    tripPlannerNavigator: TripPlannerNavigator,
) {
    entry<OurStoryRoute>(
        metadata = androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy.detailPane(),
    ) {
        val viewModel: OurStoryViewModel = koinViewModel()
        val ourStoryState by viewModel.models.collectAsStateWithLifecycle()

        OurStoryScreen(
            state = ourStoryState,
            onBackClick = { tripPlannerNavigator.goBack() },
        )
    }
}
