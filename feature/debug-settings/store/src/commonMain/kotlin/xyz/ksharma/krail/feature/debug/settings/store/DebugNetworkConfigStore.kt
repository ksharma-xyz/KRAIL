package xyz.ksharma.krail.feature.debug.settings.store

import kotlinx.coroutines.flow.Flow
import xyz.ksharma.krail.feature.debug.settings.state.DebugSettingsEvent
import xyz.ksharma.krail.feature.debug.settings.state.DebugSettingsState
import xyz.ksharma.krail.feature.debug.settings.state.FlagOverride
import xyz.ksharma.krail.feature.debug.settings.state.NetworkTarget

/**
 * Single source of truth for the runtime debug-network configuration.
 *
 * Reads + writes the developer's [NetworkTarget] selection (BFF Local vs
 * BFF Prod) and their [FlagOverride] for the single `enable_proto_bff`
 * Firebase RC flag. Backed by `SandookPreferences` (KRAIL's KMP key-value
 * store, SQLDelight under the hood) so state survives process death and is
 * shared across both Android and iOS targets.
 *
 * Debug-only by convention. The `BffEndpointResolver` (in `:core:network`)
 * only consults this store's [flagOverride] and [networkTarget] when
 * `AppInfo.isDebug` is `true`; release builds resolve straight from
 * Firebase RC.
 */
interface DebugNetworkConfigStore {

    /**
     * Hot flow of the current settings snapshot. Emits the latest value on
     * subscription and on every successful [set].
     */
    val state: Flow<DebugSettingsState>

    /** Current [FlagOverride] for the `enable_proto_bff` flag. */
    suspend fun flagOverride(): FlagOverride

    /** Current [NetworkTarget] selection for the BFF path. */
    suspend fun networkTarget(): NetworkTarget

    /** Apply a [DebugSettingsEvent] and persist the resulting state. */
    suspend fun set(event: DebugSettingsEvent)
}
