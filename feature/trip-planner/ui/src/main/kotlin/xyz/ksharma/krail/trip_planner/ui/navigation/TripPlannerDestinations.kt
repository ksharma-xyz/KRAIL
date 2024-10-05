package xyz.ksharma.krail.trip_planner.ui.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable
import xyz.ksharma.krail.trip_planner.ui.savedtrips.SavedTripsScreen
import xyz.ksharma.krail.trip_planner.ui.savedtrips.SavedTripsViewModel
import xyz.ksharma.krail.trip_planner.ui.searchstop.SearchStopScreen
import xyz.ksharma.krail.trip_planner.ui.searchstop.SearchStopViewModel
import xyz.ksharma.krail.trip_planner.ui.timetable.TimeTableScreen
import xyz.ksharma.krail.trip_planner.ui.timetable.TimeTableViewModel

/**
 * Nested navigation graph for the trip planner feature.
 * It contains all the screens in the feature Trip Planner.
 */
fun NavGraphBuilder.tripPlannerDestinations(
   navController: NavHostController, // TODO -  do not wanna add NavController here, but moving all callbacks to app module is not scaleable.
) {
    navigation<TripPlannerNavRoute>(startDestination = SavedTripsRoute) {
        composable<SavedTripsRoute> {
            val viewModel = hiltViewModel<SavedTripsViewModel>()
            val savedTripState by viewModel.uiState.collectAsStateWithLifecycle()

            SavedTripsScreen(
                savedTripsState = savedTripState,
                fromButtonClick = {
                    navController.navigate(SearchStopRoute)
                },
                toButtonClick = {
                    navController.navigate(SearchStopRoute)
                },
                onSearchButtonClick = {

                },
            ) { event ->
                viewModel.onEvent(event)
            }
        }

        composable<TimeTableRoute> {
            val viewModel = hiltViewModel<TimeTableViewModel>()
            val timeTableState by viewModel.uiState.collectAsStateWithLifecycle()

            TimeTableScreen(timeTableState) { event ->
                viewModel.onEvent(event)
            }
        }

        composable<SearchStopRoute> {
            val viewModel = hiltViewModel<SearchStopViewModel>()
            val searchStopState by viewModel.uiState.collectAsStateWithLifecycle()

            SearchStopScreen(searchStopState = searchStopState) { event ->
                viewModel.onEvent(event)
            }
        }
    }
}

@Serializable
data object TripPlannerNavRoute

@Serializable
data object SavedTripsRoute

@Serializable
data object TimeTableRoute

@Serializable
data object SearchStopRoute
