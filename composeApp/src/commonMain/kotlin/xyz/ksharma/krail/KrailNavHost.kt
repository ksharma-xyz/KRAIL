package xyz.ksharma.krail

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import xyz.ksharma.krail.navigation.*
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.LocalThemeContentColor
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.taj.toHex
import xyz.ksharma.krail.trip.planner.ui.entries.LocalResultEventBus
import xyz.ksharma.krail.trip.planner.ui.entries.ResultEventBus

/**
 * Main navigation host using Navigation 3 with List-Detail adaptive layout.
 *
 * Navigation 3 solves the previous issues:
 * - No circular dependencies (routes defined in :ui:api modules)
 * - Direct back stack control
 * - Better modularity and scalability
 * - Supports adaptive layouts (list-detail for tablets/foldables)
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun KrailNavHost(modifier: Modifier = Modifier) {
    // Navigation state - start with SplashRoute
    val navigationState = rememberNavigationState(
        startRoute = SplashRoute,
        topLevelRoutes = setOf(SplashRoute)
    )

    val navigator = rememberNavigator(navigationState)
    val tripPlannerNavigator = remember { TripPlannerNavigatorImpl(navigator) }

    // Get the singleton ResultEventBus instance for passing results between screens
    // Using singleton ensures the same instance is shared across list and detail panes
    val resultEventBus = remember { ResultEventBus.getInstance() }

    // Use Navigator's theme color instead of local state
    val themeContentColor = getForegroundColor(
        backgroundColor = navigator.themeColor.hexToComposeColor()
    ).toHex()

    // Entry provider
    val entryProvider = krailEntryProvider(
        navigator = navigator,
        tripPlannerNavigator = tripPlannerNavigator
    )

    // List-Detail scene strategy for adaptive layout
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    CompositionLocalProvider(
        LocalThemeColor provides mutableStateOf(navigator.themeColor),
        LocalThemeContentColor provides mutableStateOf(themeContentColor),
        LocalTextColor provides KrailTheme.colors.onSurface,
        LocalResultEventBus provides resultEventBus,
    ) {
        NavDisplay<NavKey>(
            entries = navigationState.toEntries(entryProvider),
            onBack = { navigator.goBack() },
            sceneStrategy = listDetailStrategy,
            modifier = modifier.fillMaxSize()
        )
    }
}
