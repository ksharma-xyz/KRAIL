package xyz.ksharma.krail.trip.planner.ui.settings

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.trip.planner.ui.navigation.AboutUsRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SettingsRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.ThemeSelectionRoute

internal fun NavGraphBuilder.settingsDestination(navController: NavHostController) {
    composable<SettingsRoute> {

        val viewModel: SettingsViewModel = koinViewModel<SettingsViewModel>()
        val settingsState by viewModel.uiState.collectAsStateWithLifecycle()

        SettingsScreen(
            appVersion = settingsState.appVersion,
            onChangeThemeClick = {
                navController.navigate(
                    route = ThemeSelectionRoute,
                    navOptions = NavOptions.Builder().setLaunchSingleTop(true).build(),
                )
            },
            onBackClick = {
                navController.popBackStack()
            },
            onReferFriendClick = {
                viewModel.onReferFriendClick()
            },
            onAboutUsClick = {
                navController.navigate(
                    route = AboutUsRoute,
                    navOptions = NavOptions.Builder().setLaunchSingleTop(true).build(),
                )
            },
        )
    }
}
