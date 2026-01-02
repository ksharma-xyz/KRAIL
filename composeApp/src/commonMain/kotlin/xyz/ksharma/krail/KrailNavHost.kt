package xyz.ksharma.krail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.core.navigation.LocalResultEventBusObj
import xyz.ksharma.krail.core.navigation.ResultEventBus
import xyz.ksharma.krail.core.navigation.SplashRoute
import xyz.ksharma.krail.core.navigation.rememberNavigationState
import xyz.ksharma.krail.core.navigation.toEntries
import xyz.ksharma.krail.navigation.Navigator
import xyz.ksharma.krail.navigation.di.collectEntryProviders
import xyz.ksharma.krail.navigation.krailNavSerializationConfig
import xyz.ksharma.krail.navigation.rememberNavigator
import xyz.ksharma.krail.navigation.rememberPreviewNavigator
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.LocalThemeContentColor
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.taj.toHex

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
@Suppress("RememberMissing")
@Composable
fun KrailNavHost(modifier: Modifier = Modifier) {
    // Navigation state - start with SplashRoute
    val navigationState = rememberNavigationState(
        startRoute = SplashRoute,
        topLevelRoutes = setOf(SplashRoute),
        serializationConfig = krailNavSerializationConfig,
    )

    val navigator = rememberNavigator(navigationState)

    // Get the singleton ResultEventBus instance for passing results between screens
    // Using singleton ensures the same instance is shared across list and detail panes
    val resultEventBus = remember { ResultEventBus.getInstance() }

    // Use Navigator's theme color instead of local state
    val themeContentColor = getForegroundColor(
        backgroundColor = navigator.themeColor.hexToComposeColor(),
    ).toHex()

    // Entry provider using multibinding approach
    // Collects all entry builders from Koin modules
    val entryProvider = collectEntryProviders(navigator = navigator)

    // Calculate directive with custom spacing at top level
    // Override the defaults so that there isn't a horizontal space between the panes.
    // See b/418201867
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo)
            .copy(horizontalPartitionSpacerSize = 0.dp)
    }

    // List-Detail scene strategy for adaptive layout with custom directive
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

    // Calculate entries explicitly to check for emptiness
    val entries = navigationState.toEntries(entryProvider)

    CompositionLocalProvider(
        LocalThemeColor provides mutableStateOf(navigator.themeColor),
        LocalThemeContentColor provides mutableStateOf(themeContentColor),
        LocalTextColor provides KrailTheme.colors.onSurface,
        LocalResultEventBusObj provides resultEventBus,
    ) {
        if (entries.isNotEmpty()) {
            NavDisplay(
                entries = navigationState.toEntries(entryProvider),
                onBack = { navigator.pop() },
                sceneStrategy = listDetailStrategy,
                modifier = modifier.fillMaxSize(),
            )
        } else {
            NoEntriesUI(navigator)
        }
    }
}

/**
 * Fallback UI: This handles the crash gracefully.
 * Since navigator works independently of NavDisplay, clicking the button
 * will update the state, trigger recomposition, and fix the entries list.
 */
@Composable
private fun NoEntriesUI(navigator: Navigator) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Something went wrong",
                style = KrailTheme.typography.displayMedium,
                modifier = Modifier.padding(top = 100.dp, start = 24.dp, end = 24.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    // Navigate to a known safe route (e.g., Splash or SavedTrips)
                    navigator.resetRoot(SplashRoute)
                },
                modifier = Modifier.systemBarsPadding().padding(20.dp),
            ) {
                Text(text = "Let's KRAIL")
            }
        }
    }
}

@Preview
@Composable
private fun NoEntriesUIPreview() {
    PreviewTheme {
        val navigationState = rememberNavigationState(
            startRoute = SplashRoute,
            topLevelRoutes = setOf(SplashRoute),
            serializationConfig = krailNavSerializationConfig,
        )
        // Koin is not available in previews, so we can't use rememberNavigator, which uses
        // koinInject. Instead, we create a dummy Navigator that doesn't use Koin.
        NoEntriesUI(navigator = rememberPreviewNavigator(navigationState))
    }
}
