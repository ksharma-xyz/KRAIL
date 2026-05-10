package xyz.ksharma.krail.feature.debug.settings.state

/**
 * Snapshot of the runtime debug-network configuration.
 *
 * Persisted by `DebugNetworkConfigStore` in the `:feature:debug-settings:store`
 * module. Read once per network call by the four `Real*Service` classes (wired
 * in a follow-up commit on this branch).
 *
 * @property targetsByScope current selection per [EndpointScope]. Default is
 *   `BFF_LOCAL` for every scope so the existing `local.properties` opt-in
 *   keeps working transparently until the developer flips a row to
 *   `NSW_DIRECT` via the PR 2b UI.
 * @property killSwitchEnabled when `true`, every scope resolves to
 *   `NSW_DIRECT` regardless of [targetsByScope]. Mirrors the production
 *   `bff_kill_switch` Firebase RC flag.
 * @property compareModeEnabled when `true`, debug builds may issue both NSW
 *   and BFF requests in parallel and log the diff. Off by default; PR 2b
 *   wires it.
 */
data class DebugSettingsState(
    val targetsByScope: Map<EndpointScope, NetworkTarget>,
    val killSwitchEnabled: Boolean,
    val compareModeEnabled: Boolean,
) {
    /**
     * Effective target for [scope], folding the kill switch in. Callers
     * should prefer this over reading [targetsByScope] directly.
     */
    fun effectiveTarget(scope: EndpointScope): NetworkTarget =
        if (killSwitchEnabled) {
            NetworkTarget.NSW_DIRECT
        } else {
            targetsByScope[scope] ?: DEFAULT_TARGET
        }

    companion object {
        /**
         * Default network target for every [EndpointScope]. `BFF_LOCAL` keeps
         * the existing `local.properties` `krail.bffBaseUrl` opt-in working
         * with no UI interaction; the resolver falls back to NSW when the
         * local URL is empty.
         */
        val DEFAULT_TARGET: NetworkTarget = NetworkTarget.BFF_LOCAL

        /**
         * Initial state for a fresh install (or after [DebugSettingsEvent.Reset]).
         */
        fun default(): DebugSettingsState = DebugSettingsState(
            targetsByScope = EndpointScope.entries.associateWith { DEFAULT_TARGET },
            killSwitchEnabled = false,
            compareModeEnabled = false,
        )
    }
}
