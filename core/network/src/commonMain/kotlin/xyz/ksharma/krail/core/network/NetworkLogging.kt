package xyz.ksharma.krail.core.network

import xyz.ksharma.krail.core.log.log as krailLog

/**
 * Logcat / iOS console tag for KRAIL networking. Use this prefix in all
 * shared-client log lines so a developer can filter the firehose with
 * `adb logcat -s KrailNetwork:*` (Android) or the Xcode console search
 * field (iOS).
 *
 * This is intentionally a plain string prefix rather than a logcat tag.
 * KRAIL's [krailLog] derives its own caller-class tag, so we embed
 * `KrailNetwork` in the message text and let the caller-class tag
 * indicate which file emitted the line.
 */
internal const val KRAIL_NETWORK_LOG_TAG: String = "KrailNetwork:"

/**
 * Identifies which upstream a request is targeting. Logged as the
 * "target" segment of a network log line so a developer can tell at a
 * glance whether a call took the BFF path or the NSW direct path.
 *
 * Distinct from `xyz.ksharma.krail.feature.debug.settings.state.NetworkTarget`
 * which selects between BFF deployments (`BFF_LOCAL` vs `BFF_PROD`); this
 * enum is purely a pre-call log label.
 */
enum class NetworkUpstream(val label: String) {
    /** KRAIL-BFF (local override or future production deploy). */
    BFF(label = "BFF"),

    /** NSW Open Data Transport API (direct). */
    NSW(label = "NSW"),
}

/**
 * One-line "we're about to call X" pre-flight log. The Ktor [io.ktor.client.plugins.logging.Logging]
 * plugin already logs the response status and timing once the call completes;
 * this helper exists purely so the BFF/NSW branch decision is visible in
 * logcat before the request fires.
 *
 * Example output (with the [KRAIL_NETWORK_LOG_TAG] prefix prepended by [krailLog]):
 *
 * ```
 * KrailNetwork: BFF GET /v1/tp/trip [override=on]
 * KrailNetwork: NSW GET /v1/tp/trip [override=off]
 * ```
 *
 * No bodies, no query strings, just the path. Per
 * `KRAIL_INTEGRATION_MASTER_PLAN.md` §13, we deliberately do not log
 * request/response bodies.
 *
 * @param target Which upstream is being hit on this branch (BFF or NSW).
 * @param method HTTP method, e.g. `GET`, `HEAD`.
 * @param path The path component only, no scheme, host, or query.
 * @param overrideOn `true` when the caller resolved to the BFF path.
 *                   Logged so the developer can reason about which branch ran.
 */
fun logNetworkCall(
    target: NetworkUpstream,
    method: String,
    path: String,
    overrideOn: Boolean = IS_BFF_LOCAL_OVERRIDE_SET,
) {
    val overrideLabel = if (overrideOn) "on" else "off"
    krailLog("$KRAIL_NETWORK_LOG_TAG ${target.label} $method $path [override=$overrideLabel]")
}

/**
 * Maps a resolved base URL to the [NetworkUpstream] enum used in pre-call
 * log lines. Anything pointing at NSW's `api.transport.nsw.gov.au` is `NSW`;
 * everything else is `BFF` (local override or eventual prod URL).
 */
fun String.toNetworkUpstream(): NetworkUpstream =
    if (startsWith(NSW_TRANSPORT_BASE_URL)) NetworkUpstream.NSW else NetworkUpstream.BFF
