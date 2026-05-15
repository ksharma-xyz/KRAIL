package xyz.ksharma.krail.core.network

import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.asBoolean
import xyz.ksharma.krail.feature.debug.settings.state.NetworkSource
import xyz.ksharma.krail.feature.debug.settings.store.DebugNetworkConfigStore

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
 * | release | n/a          | always FOLLOW_RC, BFF Prod URL when RC says enabled  |
 * | debug   | FOLLOW_RC    | RC value, BFF Local URL when enabled (so debug       |
 * |         |              | devs hit their local BFF when "following" rollout)   |
 * | debug   | NSW_DIRECT   | NSW direct, RC ignored                               |
 * | debug   | BFF_LOCAL    | local BFF URL, RC ignored                            |
 * | debug   | BFF_PROD     | prod BFF URL, RC ignored                             |
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
                val rcEnabled = flag.getFlagValue(FlagKeys.ENABLE_PROTO_BFF.key).asBoolean(false)
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
