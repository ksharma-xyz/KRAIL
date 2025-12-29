package xyz.ksharma.krail.navigation.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.module
import xyz.ksharma.krail.core.appversion.AppUpgradeScreen
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.navigation.AppUpgradeRoute
import xyz.ksharma.krail.core.navigation.EntryBuilderDescriptor
import xyz.ksharma.krail.core.navigation.EntryBuilderQualifiers
import xyz.ksharma.krail.core.navigation.SplashRoute
import xyz.ksharma.krail.navigation.Navigator
import xyz.ksharma.krail.splash.SplashScreen
import xyz.ksharma.krail.splash.SplashUiEvent
import xyz.ksharma.krail.splash.SplashViewModel
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme

/**
 * Koin module for app-level navigation entries (Splash, App Upgrade, etc.)
 *
 * This module provides entry builders following the Google multibinding approach.
 * Each entry builder is provided as an EntryBuilderDescriptor that can be collected
 * into a Set by the app's navigation system.
 */
val appNavigationModule = module {
    // Provide Splash entry builder
    factory<EntryBuilderDescriptor>(qualifier = EntryBuilderQualifiers.SPLASH) {
        EntryBuilderDescriptor(
            name = EntryBuilderQualifiers.Names.SPLASH,
            builder = { navigator ->
                splashEntry(navigator as Navigator)
            }
        )
    }

    // Provide App Upgrade entry builder
    factory<EntryBuilderDescriptor>(qualifier = EntryBuilderQualifiers.APP_UPGRADE) {
        EntryBuilderDescriptor(
            name = EntryBuilderQualifiers.Names.APP_UPGRADE,
            builder = { _ ->
                appUpgradeEntry()
            }
        )
    }
}

/**
 * Splash screen navigation entry.
 * Takes Navigator as parameter to handle navigation and theme updates.
 */
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
                navigator.replaceCurrent(destination)
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

/**
 * App upgrade screen navigation entry.
 */
@Composable
private fun EntryProviderScope<NavKey>.appUpgradeEntry() {
    entry<AppUpgradeRoute> { key ->
        AppUpgradeScreen()
    }
}

