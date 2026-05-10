package xyz.ksharma.krail.feature.debug.settings.state

/**
 * UI / caller-issued mutations to the debug-network configuration.
 * Consumed by `DebugNetworkConfigStore.set(...)` in the store module.
 */
sealed interface DebugSettingsEvent {

    /** Pick a [NetworkTarget] for one [EndpointScope]. */
    data class SetTarget(
        val scope: EndpointScope,
        val target: NetworkTarget,
    ) : DebugSettingsEvent

    /**
     * Toggle the global kill switch. When enabled, every scope resolves to
     * `NSW_DIRECT` regardless of [SetTarget] selections (selections are kept,
     * not cleared, so disabling the kill switch restores prior intent).
     */
    data class SetKillSwitch(val enabled: Boolean) : DebugSettingsEvent

    /**
     * Toggle compare-mode logging. PR 2b consumes this; the store just
     * persists the bit until then.
     */
    data class SetCompareMode(val enabled: Boolean) : DebugSettingsEvent

    /** Restore the entire state to [DebugSettingsState.default]. */
    data object Reset : DebugSettingsEvent
}
