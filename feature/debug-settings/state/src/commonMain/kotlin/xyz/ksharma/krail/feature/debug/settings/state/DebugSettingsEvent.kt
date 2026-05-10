package xyz.ksharma.krail.feature.debug.settings.state

/**
 * UI / caller-issued mutations to the debug-network configuration.
 * Consumed by `DebugNetworkConfigStore.set(...)` in the store module.
 */
sealed interface DebugSettingsEvent {

    /** Pick the [NetworkTarget] used when the BFF path is active. */
    data class SetNetworkTarget(val target: NetworkTarget) : DebugSettingsEvent

    /** Pick the [FlagOverride] for the single `enable_proto_bff` flag. */
    data class SetFlagOverride(val override: FlagOverride) : DebugSettingsEvent

    /** Restore the entire state to [DebugSettingsState.default]. */
    data object Reset : DebugSettingsEvent
}
