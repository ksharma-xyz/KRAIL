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
    val isProEnabled: Boolean = false,
) {
    companion object {
        /** Default source for fresh installs. Matches the release cohort. */
        val DEFAULT_SOURCE: NetworkSource = NetworkSource.FOLLOW_RC

        /**
         * Initial state for a fresh install (or after [DebugSettingsEvent.Reset]).
         */
        fun default(): DebugSettingsState = DebugSettingsState(source = DEFAULT_SOURCE)
    }
}
