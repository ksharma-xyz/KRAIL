package xyz.ksharma.krail.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders

/**
 * Which credential an upstream needs.
 *
 * ## Why this exists
 *
 * Four feature modules each carried an `expect fun <feature>HttpClient(baseClient)` with two
 * `actual` implementations, twelve files in total. Every one of them was byte-for-byte
 * identical apart from the function name and which BuildKonfig constant it read. The only
 * real difference between a "trip planner client" and a "departures client" was nothing.
 *
 * A new upstream should cost a line here, not three files. See the checklist in
 * `docs/NETWORK_RELIABILITY.md`.
 */
enum class ApiCredential {

    /**
     * The NSW Open Data key, sent as `Authorization: apikey <key>`.
     *
     * Also correct for BFF-routed calls: the BFF proxies NSW and expects the same header, so
     * a service does not have to know which upstream [BffEndpointResolver] picked.
     */
    NswApiKey,

    /** No credential. For endpoints that are open, or authenticated some other way. */
    None,
}

/**
 * Returns a client configured for one upstream's credential.
 *
 * Ktor's [HttpClient.config] returns a new client sharing the same engine, so this is cheap
 * and every caller keeps the timeouts, logging and content negotiation installed on the base
 * client. Anything that should apply to every KRAIL request belongs on the base client in
 * `baseHttpClient`, not here, so a future upstream inherits it without opting in.
 */
fun HttpClient.forApi(credential: ApiCredential): HttpClient = when (credential) {
    ApiCredential.NswApiKey -> config {
        defaultRequest {
            headers.append(HttpHeaders.Authorization, "apikey $nswTransportApiKey")
        }
    }

    ApiCredential.None -> this
}

/**
 * The NSW Open Data API key for the platform being built.
 *
 * The two platforms are issued separate keys, which is the entire reason the twelve deleted
 * files existed as `expect`/`actual` pairs. Reduced to one declaration: the branch is over a
 * string, not over a client factory.
 */
expect val nswTransportApiKey: String
