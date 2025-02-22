package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.trip.planner.ui.navigation.SavedTripsRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopFieldType
import xyz.ksharma.krail.trip.planner.ui.navigation.SearchStopRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SettingsRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.TimeTableRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.animComposable
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.SavedTripUiEvent
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem.Companion.fromJsonString

@Suppress("LongMethod")
internal fun NavGraphBuilder.savedTripsDestination(navController: NavHostController) {
    animComposable<SavedTripsRoute> { backStackEntry ->
        val viewModel: SavedTripsViewModel = koinViewModel<SavedTripsViewModel>()
        val savedTripState by viewModel.uiState.collectAsStateWithLifecycle()

        val fromArg: String? =
            backStackEntry.savedStateHandle.get<String>(SearchStopFieldType.FROM.key)
        val toArg: String? =
            backStackEntry.savedStateHandle.get<String>(SearchStopFieldType.TO.key)

        LaunchedEffect(Unit) {
            viewModel.onEvent(SavedTripUiEvent.LoadSavedTrips)
        }

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
                navController.navigate(SearchStopRoute(fieldTypeKey = SearchStopFieldType.FROM.key))
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
                } else {
                    // TODO - show message - to select both stops
                    // logError(("Select both stops")
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
                } else {
                    // TODO - show message - to select both stops
                    // logError(("Select both stops")
                }
            },
            onSettingsButtonClick = {
                viewModel.onEvent(SavedTripUiEvent.AnalyticsSettingsButtonClick)
                navController.navigate(
                    route = SettingsRoute,
                    navOptions = NavOptions.Builder().setLaunchSingleTop(true).build(),
                )
            },
            onEvent = { event -> viewModel.onEvent(event) },
        )
    }
}
