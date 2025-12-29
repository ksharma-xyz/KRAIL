package xyz.ksharma.krail.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

/**
 * Custom multibinding implementation for Koin.
 * Similar to Dagger's @IntoSet, this allows multiple modules to contribute
 * entry builders that get collected into a single Set.
 */

/**
 * Type alias for entry builder functions.
 * Each feature receives its specific navigator implementation (cast from Any).
 */
typealias FeatureEntryBuilder = @Composable EntryProviderScope<NavKey>.(navigator: Any) -> Unit

/**
 * Wrapper class to hold entry builder metadata.
 * This allows us to identify and organize entry builders from different modules.
 *
 * Each feature module provides an instance of this class through Koin,
 * which is then collected and invoked by the app's navigation system.
 */
data class EntryBuilderDescriptor(
    val name: String,
    val builder: FeatureEntryBuilder
)
