package xyz.ksharma.krail.feature.debug.settings.state

/**
 * Snapshot of the runtime debug-network configuration.
 *
 * Persisted by `DebugNetworkConfigStore` in the `:feature:debug-settings:store`
 * module. Read by `BffEndpointResolver` (in `:core:network`) once per network
 * call to decide whether the call goes to BFF or NSW, and which BFF deployment.
 *
 * @property networkTarget which BFF deployment to hit when the resolver picks
 *   the BFF path. Default is [DEFAULT_NETWORK_TARGET] so a developer's existing
 *   `local.properties` opt-in keeps working.
 * @property flagOverride debug-only override for the single Firebase RC flag
 *   `enable_proto_bff`. Default is [DEFAULT_FLAG_OVERRIDE] so debug builds
 *   match the production cohort until a developer flips it. Ignored entirely
 *   in release builds.
 */
data class DebugSettingsState(
    val networkTarget: NetworkTarget,
    val flagOverride: FlagOverride,
) {
    companion object {
        /** Default BFF deployment for fresh installs. Preserves the local opt-in. */
        val DEFAULT_NETWORK_TARGET: NetworkTarget = NetworkTarget.BFF_LOCAL

        /** Default flag override for fresh installs. Matches the release cohort. */
        val DEFAULT_FLAG_OVERRIDE: FlagOverride = FlagOverride.FOLLOW_RC

        /**
         * Initial state for a fresh install (or after [DebugSettingsEvent.Reset]).
         */
        fun default(): DebugSettingsState = DebugSettingsState(
            networkTarget = DEFAULT_NETWORK_TARGET,
            flagOverride = DEFAULT_FLAG_OVERRIDE,
        )
    }
}
