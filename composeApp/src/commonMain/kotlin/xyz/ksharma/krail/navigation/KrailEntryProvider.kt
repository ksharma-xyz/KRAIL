package xyz.ksharma.krail.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import org.koin.compose.viewmodel.koinViewModel
import xyz.ksharma.krail.core.appversion.AppUpgradeScreen
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.splash.SplashScreen
import xyz.ksharma.krail.splash.SplashUiEvent
import xyz.ksharma.krail.splash.SplashViewModel
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.entries.tripPlannerEntries

/**
 * Main entry provider for KRAIL app.
 * Aggregates all feature entry providers.
 */
@Composable
fun krailEntryProvider(
    navigator: Navigator,
    tripPlannerNavigator: TripPlannerNavigatorImpl
): (NavKey) -> NavEntry<NavKey> {
    return entryProvider {
        // App-level entries
        splashEntry(navigator)
        appUpgradeEntry()

        // Trip planner feature entries
        tripPlannerEntries(tripPlannerNavigator)
    }
}

@Composable
private fun EntryProviderScope<NavKey>.splashEntry(
    navigator: Navigator
) {
    entry<SplashRoute> { key ->
        val viewModel: SplashViewModel = koinViewModel()
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        val splashState by viewModel.uiState.collectAsStateWithLifecycle()

        // Set theme in Navigator
        LaunchedEffect(splashState.themeStyle) {
            navigator.updateTheme(splashState.themeStyle.hexColorCode)
        }

        // Navigate when destination is determined
        LaunchedEffect(splashState.navigationDestination) {
            splashState.navigationDestination?.let { destination ->
                log("Splash complete. Navigating to: $destination")
                navigator.navigateAndReplace(destination)
            }
        }

        SplashScreen(
            splashState = splashState,
            logoColor = navigator.themeColor.hexToComposeColor(),
            backgroundColor = KrailTheme.colors.surface,
            onSplashAnimationComplete = {
                viewModel.onUiEvent(SplashUiEvent.SplashAnimationComplete)
            }
        )
    }
}

@Composable
private fun EntryProviderScope<NavKey>.appUpgradeEntry() {
    entry<AppUpgradeRoute> { key ->
        AppUpgradeScreen()
    }
}

