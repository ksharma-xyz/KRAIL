package xyz.ksharma.krail.trip.planner.ui.settings.about


import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import xyz.ksharma.krail.trip.planner.ui.navigation.AboutUsRoute

internal fun NavGraphBuilder.aboutUsDestination(navController: NavHostController) {
    composable<AboutUsRoute> {

        AboutUsScreen(
            onBackClick = { navController.popBackStack() }
        )
    }
}
