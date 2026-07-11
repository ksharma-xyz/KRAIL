package xyz.ksharma.krail.feature.debug.settings.state

/**
 * UI / caller-issued mutations to the debug-network configuration.
 * Consumed by `DebugNetworkConfigStore.set(...)` in the store module.
 */
sealed interface DebugSettingsEvent {

    /** Pick the [NetworkSource] used by the resolver. */
    data class SetSource(val source: NetworkSource) : DebugSettingsEvent

    /** Override the TRIP_TRACKING_ENABLED RC flag locally for this debug build. */
    data class SetTripTrackingEnabled(val enabled: Boolean) : DebugSettingsEvent

    /** Override the SEARCH_STOP_ADDRESS_SEARCH_ENABLED RC flag locally for this debug build. */
    data class SetAddressSearchEnabled(val enabled: Boolean) : DebugSettingsEvent

    /** Restore the entire state to [DebugSettingsState.default]. */
    data object Reset : DebugSettingsEvent
}
