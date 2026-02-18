package xyz.ksharma.krail.navigation.di

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import org.koin.compose.koinInject
import xyz.ksharma.krail.core.navigation.EntryBuilderDescriptor
import xyz.ksharma.krail.core.navigation.EntryBuilderQualifiers
import xyz.ksharma.krail.navigation.Navigator

/**
 * Provides a unified entry provider by collecting all entry builders from Koin modules.
 *
 * This approach mimics Google's multibinding pattern:
 * - Each feature module provides its entry builders through Koin modules
 * - This function collects all entry builders and invokes them
 * - Navigator is passed to all entry builders
 * - Feature modules handle their own navigator wrapping internally
 * - No direct dependencies on feature implementations
 *
 * ## Build Performance Benefit
 * When navigation logic changes in a feature module, only that feature module
 * is recompiled. The app module is NOT recompiled because it has no direct
 * dependency on feature-specific navigator implementations.
 *
 * ## Usage
 * ```kotlin
 * val entryProvider = collectEntryProviders(navigator = navigator)
 * ```
 */
@Composable
fun collectEntryProviders(
    navigator: Navigator,
): (NavKey) -> NavEntry<NavKey> {
    // Inject all entry builders from Koin using centralized qualifiers
    val splashEntryBuilder: EntryBuilderDescriptor = koinInject(EntryBuilderQualifiers.SPLASH)
    val appUpgradeEntryBuilder: EntryBuilderDescriptor = koinInject(EntryBuilderQualifiers.APP_UPGRADE)
    val tripPlannerEntryBuilder: EntryBuilderDescriptor = koinInject(EntryBuilderQualifiers.TRIP_PLANNER)
    val locationEntryBuilder: EntryBuilderDescriptor = koinInject(EntryBuilderQualifiers.LOCATION)

    return entryProvider {
        // Invoke all entry builders with Navigator
        // Each feature module wraps the Navigator in its own implementation if needed
        splashEntryBuilder.builder(this, navigator)
        appUpgradeEntryBuilder.builder(this, navigator)
        tripPlannerEntryBuilder.builder(this, navigator)
        locationEntryBuilder.builder(this, navigator)
    }
}
