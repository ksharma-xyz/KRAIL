package xyz.ksharma.krail.core.network

import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.asBoolean
import xyz.ksharma.krail.feature.debug.settings.state.NetworkSource
import xyz.ksharma.krail.feature.debug.settings.store.DebugNetworkConfigStore

/**
 * Master rollout-arming switch, sitting ABOVE the `enable_proto_bff`
 * Firebase Remote Config flag.
 *
 * While `false`, [BffEndpointResolver.resolveBaseUrl] ignores the RC flag
 * entirely: the FOLLOW_RC path always resolves to NSW direct regardless of
 * the live flag value, in both release and debug builds. Explicit Debug
 * Config picks (`BFF_LOCAL` / `BFF_PROD`) are unaffected so a developer can
 * still test the BFF path on demand.
 *
 * This is a deliberate compile-time gate so a premature or accidental
 * Firebase RC flip cannot route real users to a BFF that is not yet
 * deployed / not yet ready. Flip to `true` in a release build ONLY when
 * the production cohort rollout is intentionally being armed.
 */
internal const val BFF_ROLLOUT_ARMED: Boolean = false

/**
 * Resolves the base URL each `Real*Service` should use for an endpoint that
 * has both an NSW direct path and a KRAIL-BFF equivalent.
 *
 * One Firebase Remote Config flag, [FlagKeys.ENABLE_PROTO_BFF], drives the
 * production decision. Debug builds may steer the resolver via a single
 * [NetworkSource] picked in the Debug Config UI:
 *
 * | Build   | Source       | Behaviour                                            |
 * |---------|--------------|------------------------------------------------------|
 * | release | n/a          | FOLLOW_RC; BFF Prod URL when RC on AND rollout armed |
 * | debug   | FOLLOW_RC    | RC value (BFF Local URL) only when rollout armed     |
 * | debug   | NSW_DIRECT   | NSW direct, RC ignored                               |
 * | debug   | BFF_LOCAL    | local BFF URL, RC + arming ignored                   |
 * | debug   | BFF_PROD     | prod BFF URL, RC + arming ignored                    |
 *
 * The FOLLOW_RC rows are additionally gated by [BFF_ROLLOUT_ARMED]; while
 * disarmed (current state) FOLLOW_RC always resolves to NSW. Explicit
 * BFF_LOCAL / BFF_PROD picks bypass the arming gate.
 *
 * Empty BFF URLs are masked by [nswFallback] so a misconfigured local
 * opt-in or a not-yet-deployed prod URL cannot break the app.
 *
 * Replaces the build-time [IS_BFF_LOCAL_OVERRIDE_SET] check that earlier
 * services branched on. The constant remains for backward-compat (some
 * logging defaults still reference it) but new service code should call
 * [resolveBaseUrl] instead.
 */
class BffEndpointResolver(
    private val appInfoProvider: AppInfoProvider,
    private val flag: Flag,
    private val debugStore: DebugNetworkConfigStore,
    private val bffLocalBaseUrl: String = KRAIL_BFF_BASE_URL,
    private val bffProdBaseUrl: String = KRAIL_BFF_PROD_BASE_URL,
    private val rolloutArmed: Boolean = BFF_ROLLOUT_ARMED,
) {

    /**
     * @return the base URL for a BFF-eligible endpoint. The caller appends
     *   the path (e.g. `/v1/tp/trip`) onto whatever comes back. Empty BFF
     *   URLs are masked by [nswFallback] so callers never see an empty
     *   string.
     */
    suspend fun resolveBaseUrl(nswFallback: String = NSW_TRANSPORT_BASE_URL): String {
        val isDebug = appInfoProvider.getAppInfo().isDebug
        val source: NetworkSource = if (isDebug) debugStore.source() else NetworkSource.FOLLOW_RC
        return when (source) {
            NetworkSource.FOLLOW_RC -> {
                // Master arming gate sits above the RC flag: while disarmed,
                // FOLLOW_RC always resolves to NSW regardless of the live
                // flag value. Explicit BFF_LOCAL / BFF_PROD picks below
                // intentionally bypass this gate.
                val rcEnabled = rolloutArmed &&
                    flag.getFlagValue(FlagKeys.ENABLE_PROTO_BFF.key).asBoolean(false)
                if (!rcEnabled) {
                    nswFallback
                } else if (isDebug) {
                    // Debug devs follow the rollout but keep hitting their own BFF.
                    bffLocalBaseUrl.ifBlank { nswFallback }
                } else {
                    bffProdBaseUrl.ifBlank { nswFallback }
                }
            }

            NetworkSource.NSW_DIRECT -> nswFallback
            NetworkSource.BFF_LOCAL -> bffLocalBaseUrl.ifBlank { nswFallback }
            NetworkSource.BFF_PROD -> bffProdBaseUrl.ifBlank { nswFallback }
        }
    }

    /**
     * @return `true` when the live `enable_proto_bff` Firebase RC flag is on.
     *   Exposed for the Debug Config UI's "Maps to" hint under FOLLOW_RC.
     *   Independent of the developer's [NetworkSource] selection.
     */
    fun bffEnabled(): Boolean =
        flag.getFlagValue(FlagKeys.ENABLE_PROTO_BFF.key).asBoolean(fallback = false)
}
