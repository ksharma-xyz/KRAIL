package xyz.ksharma.krail.feature.debug.settings.state

/**
 * UI / caller-issued mutations to the debug-network configuration.
 * Consumed by `DebugNetworkConfigStore.set(...)` in the store module.
 */
sealed interface DebugSettingsEvent {

    /** Pick the [NetworkSource] used by the resolver. */
    data class SetSource(val source: NetworkSource) : DebugSettingsEvent

    /** Force KRAIL Pro on or off for UI testing without real IAP. */
    data class TogglePro(val enabled: Boolean) : DebugSettingsEvent

    /** Restore the entire state to [DebugSettingsState.default]. */
    data object Reset : DebugSettingsEvent
}
