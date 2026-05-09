package xyz.ksharma.krail.core.network

const val NSW_TRANSPORT_BASE_URL = "https://api.transport.nsw.gov.au"

/**
 * Local KRAIL-BFF base URL, sourced from `local.properties` `krail.bffBaseUrl`.
 * Empty when the developer has not opted in to local-BFF testing; services
 * fall back to NSW direct in that case.
 *
 * Intentionally not used in release builds. When release routing lands it
 * will be flagged via Firebase Remote Config, not this constant.
 */
val KRAIL_BFF_BASE_URL: String = NetworkBuildKonfig.KRAIL_BFF_BASE_URL

/** True when the developer has set `krail.bffBaseUrl` in local.properties. */
val IS_BFF_LOCAL_OVERRIDE_SET: Boolean = KRAIL_BFF_BASE_URL.isNotBlank()
