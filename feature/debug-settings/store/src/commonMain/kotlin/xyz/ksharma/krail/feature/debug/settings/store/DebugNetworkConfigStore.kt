package xyz.ksharma.krail.feature.debug.settings.store

import kotlinx.coroutines.flow.Flow
import xyz.ksharma.krail.feature.debug.settings.state.DebugSettingsEvent
import xyz.ksharma.krail.feature.debug.settings.state.DebugSettingsState

/**
 * Single source of truth for the runtime debug-network configuration.
 *
 * Reads + writes the user's per-scope `NetworkTarget` selections, the kill
 * switch, and the compare-mode toggle. Backed by `SandookPreferences`
 * (KRAIL's KMP key-value store, SQLDelight under the hood) so state survives
 * process death and is shared across both Android and iOS targets.
 *
 * Debug-only by convention. The eventual `Real*Service` resolver only reads
 * this store when `AppInfo.isDebug` is `true`; release builds ignore it
 * entirely and fall through to `NetworkBuildKonfig` defaults (or to Firebase
 * Remote Config once Phase B-prod lands).
 */
interface DebugNetworkConfigStore {

    /**
     * Hot flow of the current settings snapshot. Emits the latest value on
     * subscription and on every successful [set].
     */
    val state: Flow<DebugSettingsState>

    /** Apply a [DebugSettingsEvent] and persist the resulting state. */
    suspend fun set(event: DebugSettingsEvent)
}
