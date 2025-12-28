package xyz.ksharma.krail.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AdaptStrategy
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldAdaptStrategies
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey

/**
 * Creates a Material 3 Adaptive ListDetailSceneStrategy for Navigation 3.
 *
 * This strategy automatically handles:
 * - List + Detail side-by-side on wide screens (tablets, foldables)
 * - Single pane on narrow screens (phones)
 * - Smooth transitions between layouts
 *
 * Usage: Mark entries with metadata:
 * - ListDetailSceneStrategy.listPane() for list screens
 * - ListDetailSceneStrategy.detailPane() for detail screens
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun <T : NavKey> rememberListDetailSceneStrategy(): ListDetailSceneStrategy<T> {
    val adaptiveInfo = currentWindowAdaptiveInfo()

    // Calculate the pane scaffold directive based on window size
    val directive = calculatePaneScaffoldDirective(adaptiveInfo)

    // Debug: Log the window size class to see what's being detected
    println("📱 ADAPTIVE | Window size: ${adaptiveInfo.windowSizeClass}")
    println("📱 ADAPTIVE | Directive maxHorizontalPartitions: ${directive.maxHorizontalPartitions}")
    println("📱 ADAPTIVE | Window width dp: ${adaptiveInfo.windowSizeClass.minWidthDp}")

    return remember(directive) {
        ListDetailSceneStrategy(
            backNavigationBehavior = BackNavigationBehavior.PopLatest,
            directive = directive,
            adaptStrategies = ThreePaneScaffoldAdaptStrategies(
                // For list-detail layout, we want to show both panes side-by-side on wide screens
                // Use AdaptStrategy.Hide to collapse to single pane on narrow screens
                primaryPaneAdaptStrategy = AdaptStrategy.Hide,
                secondaryPaneAdaptStrategy = AdaptStrategy.Hide,
                tertiaryPaneAdaptStrategy = AdaptStrategy.Hide
            )
        )
    }
}

