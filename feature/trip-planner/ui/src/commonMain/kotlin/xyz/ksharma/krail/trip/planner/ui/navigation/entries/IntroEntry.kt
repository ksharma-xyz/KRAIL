package xyz.ksharma.krail.trip.planner.ui.navigation.entries

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.trip.planner.ui.intro.v2.IntroFlowRoute
import xyz.ksharma.krail.trip.planner.ui.intro.v2.IntroFlowViewModel
import xyz.ksharma.krail.trip.planner.ui.navigation.IntroRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SavedTripsRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.TripPlannerNavigator
import kotlin.random.Random

/**
 * Intro Entry - Detail Screen in List-Detail pattern.
 *
 * The Intro's two steps, its view model and the platform glue are [IntroFlowRoute]. This only gives
 * it the two places it can go: Saved Trips, with the back stack cleared so the rider cannot go back
 * to the Intro, and the app's own stop search, which comes back through the result bus.
 *
 * The entries have no view model store of their own, so a view model made here is the Activity's and
 * would outlive the Intro. A key made once for each visit and saved with the entry gives the Intro a
 * new view model every time it is opened, such as from Settings, and the same one through a turn of
 * the screen and through the search being on screen.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun EntryProviderScope<NavKey>.IntroEntry(
    tripPlannerNavigator: TripPlannerNavigator,
) {
    entry<IntroRoute>(
        metadata = ListDetailSceneStrategy.detailPane(),
    ) {
        val visit = rememberSaveable { Random.nextLong().toString() }
        val viewModel = koinViewModel<IntroFlowViewModel>(key = "$INTRO_VIEW_MODEL_KEY-$visit")

        IntroFlowRoute(
            viewModel = viewModel,
            onNavigateToSavedTrips = {
                // Clear entire back stack - the rider shouldn't go back to the Intro
                tripPlannerNavigator.clearBackStackAndNavigate(SavedTripsRoute)
            },
            onNavigateToSearchStop = { fieldType -> tripPlannerNavigator.navigateToSearchStop(fieldType) },
        )
    }
}

private const val INTRO_VIEW_MODEL_KEY = "intro"
