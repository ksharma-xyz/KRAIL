package xyz.ksharma.krail.feature.location.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import xyz.ksharma.krail.feature.location.LocationScreen

/**
 * Location screen entry provider.
 */
@Composable
internal fun EntryProviderScope<NavKey>.LocationEntry() {
    entry<LocationRoute> {
        LocationScreen()
    }
}

