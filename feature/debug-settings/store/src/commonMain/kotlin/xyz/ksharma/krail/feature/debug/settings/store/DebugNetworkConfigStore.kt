package xyz.ksharma.krail.feature.debug.settings.store

import kotlinx.coroutines.flow.Flow
import xyz.ksharma.krail.feature.debug.settings.state.DebugSettingsEvent
import xyz.ksharma.krail.feature.debug.settings.state.DebugSettingsState
import xyz.ksharma.krail.feature.debug.settings.state.NetworkSource

/**
 * Single source of truth for the runtime debug-network configuration.
 *
 * Reads + writes the developer's [NetworkSource] selection. Backed by
 * `SandookPreferences` (KRAIL's KMP key-value store, SQLDelight under the
 * hood) so state survives process death and is shared across both Android
 * and iOS targets.
 *
 * Debug-only by convention. The `BffEndpointResolver` (in `:core:network`)
 * only consults this store's [source] when `AppInfo.isDebug` is `true`;
 * release builds resolve straight from Firebase RC.
 */
interface DebugNetworkConfigStore {

    /**
     * Hot flow of the current settings snapshot. Emits the latest value on
     * subscription and on every successful [set].
     */
    val state: Flow<DebugSettingsState>

    /** Current [NetworkSource] selection. */
    suspend fun source(): NetworkSource

    /** Apply a [DebugSettingsEvent] and persist the resulting state. */
    suspend fun set(event: DebugSettingsEvent)
}
