package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.trip.planner.ui.navigation.DiscoverRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SavedTripsRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SettingsRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.TimeTableRoute
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem.Companion.fromJsonString

@Suppress("LongMethod")
internal fun NavGraphBuilder.savedTripsDestination(navController: NavHostController) {
    composable<SavedTripsRoute> { backStackEntry ->
        val viewModel: SavedTripsViewModel = koinViewModel<SavedTripsViewModel>()
        val savedTripState by viewModel.uiState.collectAsStateWithLifecycle()

        val fromArg: String? =
            backStackEntry.savedStateHandle.get<String>(SearchStopFieldType.FROM.key)
        val toArg: String? =
            backStackEntry.savedStateHandle.get<String>(SearchStopFieldType.TO.key)

/*        LifecycleStartEffect(Unit, backStackEntry) {
            viewModel.onEvent(SavedTripUiEvent.LifecyleStarted)
            onStopOrDispose {
                viewModel.onEvent(SavedTripUiEvent.LifecycleStopped)
            }
        }*/

        // Cannot use 'rememberSaveable' here because StopItem is not Parcelable.
        // But it's saved in backStackEntry.savedStateHandle as json, so it's able to
        // handle config changes properly.
        var fromStopItem: StopItem? by remember {
            mutableStateOf(fromArg?.let { fromJsonString(it) })
        }
        var toStopItem: StopItem? by remember { mutableStateOf(toArg?.let { fromJsonString(it) }) }

        LaunchedEffect(fromArg) {
            fromArg?.let { fromStopItem = fromJsonString(it) }
//            log(("Change fromStopItem: $fromStopItem")
        }

        LaunchedEffect(toArg) {
            toArg?.let { toStopItem = fromJsonString(it) }
//            log(("Change toStopItem: $toStopItem")
        }

        SavedTripsScreen(
            savedTripsState = savedTripState,
            fromStopItem = fromStopItem,
            toStopItem = toStopItem,
            fromButtonClick = {
                viewModel.onEvent(SavedTripUiEvent.AnalyticsFromButtonClick)
                //              Timber.d("fromButtonClick - nav: ${SearchStopRoute(fieldType = SearchStopFieldType.FROM)}")
                navController.navigate(
                    route = SearchStopRoute(fieldTypeKey = SearchStopFieldType.FROM.key),
                    navOptions = NavOptions.Builder().setLaunchSingleTop(true).build(),
                )
            },
            toButtonClick = {
                viewModel.onEvent(SavedTripUiEvent.AnalyticsToButtonClick)
                //              Timber.d("toButtonClick - nav: ${SearchStopRoute(fieldType = SearchStopFieldType.TO)}")
                navController.navigate(
                    route = SearchStopRoute(fieldTypeKey = SearchStopFieldType.TO.key),
                    navOptions = NavOptions.Builder().setLaunchSingleTop(true).build(),
                )
            },
            onReverseButtonClick = {
                //              log(("onReverseButtonClick:")
                val bufferStop = fromStopItem
                backStackEntry.savedStateHandle[SearchStopFieldType.FROM.key] =
                    toStopItem?.toJsonString()
                backStackEntry.savedStateHandle[SearchStopFieldType.TO.key] =
                    bufferStop?.toJsonString()

                fromStopItem = toStopItem
                toStopItem = bufferStop
                viewModel.onEvent(SavedTripUiEvent.AnalyticsReverseSavedTrip)
            },
            onSavedTripCardClick = { fromStop, toStop ->
                if (fromStop?.stopId != null && toStop?.stopId != null) {
                    val fromStopId = fromStop.stopId
                    val toStopId = toStop.stopId
                    viewModel.onEvent(
                        SavedTripUiEvent.AnalyticsSavedTripCardClick(fromStopId, toStopId)
                    )
                    navController.navigate(
                        route = TimeTableRoute(
                            fromStopId = fromStop.stopId,
                            fromStopName = fromStop.stopName,
                            toStopId = toStop.stopId,
                            toStopName = toStop.stopName,
                        ),
                        navOptions = NavOptions.Builder().setLaunchSingleTop(true).build(),
                    )
                }
            },
            onSearchButtonClick = {
                if (fromStopItem != null && toStopItem != null) {
                    val fromStopId = fromStopItem!!.stopId
                    val toStopId = toStopItem!!.stopId

                    viewModel.onEvent(
                        SavedTripUiEvent.AnalyticsLoadTimeTableClick(
                            fromStopId = fromStopId,
                            toStopId = toStopId,
                        )
                    )
                    navController.navigate(
                        route = TimeTableRoute(
                            fromStopId = fromStopId,
                            fromStopName = fromStopItem!!.stopName,
                            toStopId = toStopId,
                            toStopName = toStopItem!!.stopName,
                        ),
                        navOptions = NavOptions.Builder().setLaunchSingleTop(true).build(),
                    )
                }
            },
            onSettingsButtonClick = {
                viewModel.onEvent(SavedTripUiEvent.AnalyticsSettingsButtonClick)
                navController.navigate(
                    route = SettingsRoute,
                    navOptions = NavOptions.Builder().setLaunchSingleTop(true).build(),
                )
            },
            onDiscoverButtonClick = {
               viewModel.onEvent(SavedTripUiEvent.AnalyticsDiscoverButtonClick)
                navController.navigate(
                    route = DiscoverRoute,
                    navOptions = NavOptions.Builder().setLaunchSingleTop(true).build(),
                )
            },
            onEvent = { event -> viewModel.onEvent(event) },
        )
    }
}
