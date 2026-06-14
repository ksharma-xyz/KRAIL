package xyz.ksharma.krail.feature.debug.settings.state

/**
 * Snapshot of the runtime debug-network configuration.
 *
 * Persisted by `DebugNetworkConfigStore` in the `:feature:debug-settings:store`
 * module. Read by `BffEndpointResolver` (in `:core:network`) once per network
 * call to decide where the call should go.
 *
 * @property source the single knob driving the resolver. Default is
 *   [DEFAULT_SOURCE] so debug builds match the production cohort until a
 *   developer flips a row in the Network screen. Ignored entirely in
 *   release builds (which always behave as [NetworkSource.FOLLOW_RC]).
 */
data class DebugSettingsState(
    val source: NetworkSource = DEFAULT_SOURCE,
    val tripTrackingEnabled: Boolean = DEFAULT_TRIP_TRACKING_ENABLED,
) {
    companion object {
        val DEFAULT_SOURCE: NetworkSource = NetworkSource.FOLLOW_RC
        const val DEFAULT_TRIP_TRACKING_ENABLED: Boolean = true

        fun default(): DebugSettingsState = DebugSettingsState(
            source = DEFAULT_SOURCE,
            tripTrackingEnabled = DEFAULT_TRIP_TRACKING_ENABLED,
        )
    }
}
