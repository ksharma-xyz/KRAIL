package xyz.ksharma.krail.core.network

import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.asBoolean
import xyz.ksharma.krail.feature.debug.settings.state.FlagOverride
import xyz.ksharma.krail.feature.debug.settings.state.NetworkTarget
import xyz.ksharma.krail.feature.debug.settings.store.DebugNetworkConfigStore

/**
 * Resolves the base URL each `Real*Service` should use for an endpoint that
 * has both an NSW direct path and a KRAIL-BFF equivalent.
 *
 * One Firebase Remote Config flag, [FlagKeys.ENABLE_PROTO_BFF], drives the
 * decision. When the flag is `true`, BFF; when `false`, NSW direct. Debug
 * builds may override the flag via the Debug Config UI:
 *
 * ```
 * if (isDebug) {
 *     when (debugStore.flagOverride()) {
 *         FlagOverride.FOLLOW_RC -> remoteConfig flag value
 *         FlagOverride.FORCE_ON  -> true
 *         FlagOverride.FORCE_OFF -> false
 *     }
 * } else {
 *     remoteConfig flag value
 * }
 * ```
 *
 * When the result is BFF, [NetworkTarget] (also from the debug store) picks
 * which BFF deployment to hit. Empty BFF URLs fall back to [nswFallback] so
 * a misconfigured local-override or a not-yet-deployed BFF prod selection
 * cannot break the app.
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
        val useBff = useBff()
        return if (!useBff) {
            nswFallback
        } else {
            when (debugStore.networkTarget()) {
                NetworkTarget.BFF_LOCAL -> bffLocalBaseUrl.ifBlank { nswFallback }
                NetworkTarget.BFF_PROD -> bffProdBaseUrl.ifBlank { nswFallback }
            }
        }
    }

    /**
     * @return `true` when the caller should route through the BFF, `false`
     *   when it should hit NSW direct. Exposed for tests and for the Debug
     *   Config UI's "Currently: <value>" footer.
     */
    suspend fun useBff(): Boolean {
        val rcValue = flag.getFlagValue(FlagKeys.ENABLE_PROTO_BFF.key).asBoolean(fallback = false)
        return if (appInfoProvider.getAppInfo().isDebug) {
            when (debugStore.flagOverride()) {
                FlagOverride.FOLLOW_RC -> rcValue
                FlagOverride.FORCE_ON -> true
                FlagOverride.FORCE_OFF -> false
            }
        } else {
            rcValue
        }
    }
}
