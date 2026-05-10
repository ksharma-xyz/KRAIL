package xyz.ksharma.krail.feature.debug.settings.state

/**
 * Which BFF deployment a debug build targets when the BFF path is active.
 *
 * Only consulted when `BffEndpointResolver.useBff()` is true (either because
 * Firebase RC `enable_proto_bff` is on, or because the developer set
 * [FlagOverride.FORCE_ON] in the Debug Config UI). When neither is true the
 * resolver routes to NSW direct and ignores this value entirely.
 *
 * Default for new installs is [BFF_LOCAL] so the existing `local.properties`
 * `krail.bffBaseUrl` opt-in keeps working transparently for developers who
 * already wired it up.
 */
enum class NetworkTarget {
    /**
     * Local KRAIL-BFF instance, sourced from `local.properties`
     * `krail.bffBaseUrl`. Empty URL falls back to NSW direct so a
     * misconfigured local opt-in cannot break the app.
     */
    BFF_LOCAL,

    /**
     * Production KRAIL-BFF, sourced from `local.properties`
     * `krail.bffProdBaseUrl`. Empty until BFF deploys; resolver falls back
     * to NSW direct in that case so a `BFF_PROD` selection cannot break
     * the app before the BFF actually ships.
     */
    BFF_PROD,
}
