package xyz.ksharma.krail.feature.debug.settings.state

/**
 * Where a given [EndpointScope] should send its traffic at runtime.
 *
 * Resolution lives in the network services (deferred to a follow-up commit on
 * this branch). Empty / missing prod URL falls back silently to NSW so an
 * accidental `BFF_PROD` selection cannot break the app before BFF deploys.
 */
enum class NetworkTarget {
    /** NSW transport.nsw.gov.au direct, current production behavior. */
    NSW_DIRECT,

    /**
     * Local KRAIL-BFF instance (set via `local.properties` `krail.bffBaseUrl`).
     * Default for new debug-build installs so the existing local opt-in keeps
     * working transparently.
     */
    BFF_LOCAL,

    /**
     * Production KRAIL-BFF (set via `local.properties` `krail.bffProdBaseUrl`).
     * Empty URL until BFF deploys; resolver falls back to NSW in that case.
     */
    BFF_PROD,
}
