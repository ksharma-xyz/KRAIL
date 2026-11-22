package xyz.ksharma.krail.trip.planner.ui.navigation.entries

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.trip.planner.ui.intro.IntroFlowRoute
import xyz.ksharma.krail.trip.planner.ui.intro.IntroFlowViewModel
import xyz.ksharma.krail.trip.planner.ui.intro.finale.IntroFinaleSignal
import xyz.ksharma.krail.trip.planner.ui.navigation.IntroRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SavedTripsRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.TripPlannerNavigator
import kotlin.random.Random

/**
 * Intro Entry - Detail Screen in List-Detail pattern.
 *
 * The Intro's screens, its view model and the platform glue are [IntroFlowRoute]. This only gives it
 * the places it can go: Saved Trips, with the back stack cleared so the rider cannot go back to the
 * Intro, and the app's theme.
 *
 * The entries have no view model store of their own, so a view model made here is the Activity's and
 * would outlive the Intro. A key made once for each visit and saved with the entry gives the Intro a
 * new view model every time it is opened, such as from Settings, and the same one through a turn of
 * the screen.
 *
 * The finale's signal is the app's singleton, injected here at the entry as the other entries inject what they need, and
 * the Intro starts it when it finishes. The entry only says where Saved Trips is. The finale layer is at the app root.
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
        val finale = koinInject<IntroFinaleSignal>()

        IntroFlowRoute(
            viewModel = viewModel,
            finale = finale,
            onNavigateToSavedTrips = {
                // Clear entire back stack - the rider shouldn't go back to the Intro
                tripPlannerNavigator.clearBackStackAndNavigate(SavedTripsRoute)
            },
            // The same call the Settings theme screen makes to recolour the whole app.
            onApplyTheme = { hexColorCode -> tripPlannerNavigator.updateTheme(hexColorCode) },
        )
    }
}

private const val INTRO_VIEW_MODEL_KEY = "intro"
