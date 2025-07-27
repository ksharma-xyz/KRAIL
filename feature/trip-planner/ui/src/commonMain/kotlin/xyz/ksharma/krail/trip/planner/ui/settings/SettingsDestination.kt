package xyz.ksharma.krail.trip.planner.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import xyz.ksharma.krail.social.network.api.model.KrailSocialType
import androidx.navigation.compose.composable
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.trip.planner.ui.navigation.IntroRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.OurStoryRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.SettingsRoute
import xyz.ksharma.krail.trip.planner.ui.navigation.ThemeSelectionRoute
import xyz.ksharma.krail.trip.planner.ui.state.settings.SettingsEvent

internal fun NavGraphBuilder.settingsDestination(navController: NavHostController) {
    composable<SettingsRoute> {
        val viewModel: SettingsViewModel = koinViewModel<SettingsViewModel>()
        val settingsState by viewModel.uiState.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()

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
            onIntroClick = {
                scope.launch {
                    viewModel.onIntroClick()
                    navController.navigate(
                        route = IntroRoute,
                        navOptions = NavOptions.Builder().setLaunchSingleTop(true).build(),
                    )
                }
            },
            onAboutUsClick = {
                scope.launch {
                    viewModel.onOurStoryClick()
                    navController.navigate(
                        route = OurStoryRoute,
                        navOptions = NavOptions.Builder().setLaunchSingleTop(true).build(),
                    )
                }
            },
            onSocialLinkClick = { socialType ->
                viewModel.onEvent(SettingsEvent.SocialLinkClick(socialType))
            }
        )
    }
}
