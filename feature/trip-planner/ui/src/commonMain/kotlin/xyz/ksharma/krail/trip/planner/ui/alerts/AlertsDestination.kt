package xyz.ksharma.krail.trip.planner.ui.alerts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.trip.planner.ui.navigation.ServiceAlertRoute

internal fun NavGraphBuilder.alertsDestination(navController: NavHostController) {
    composable<ServiceAlertRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ServiceAlertRoute>()
        val viewModel: ServiceAlertsViewModel = koinViewModel<ServiceAlertsViewModel>()
        val serviceAlertState by viewModel.uiState.collectAsStateWithLifecycle()
        val alerts by remember(route.journeyId) {
            mutableStateOf(
                viewModel.fetchAlerts(journeyId = route.journeyId),
            )
        }
        ServiceAlertScreen(
            serviceAlerts = alerts?.toImmutableSet() ?: persistentSetOf(),
            onBackClick = {
                navController.popBackStack()
            },
        )
    }
}
