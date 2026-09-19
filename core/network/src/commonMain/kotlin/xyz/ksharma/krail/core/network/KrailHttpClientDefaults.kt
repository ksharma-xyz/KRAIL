package xyz.ksharma.krail.core.network

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.connectivity.ConnectivityObserver
import xyz.ksharma.krail.core.network.error.installCaptivePortalValidator
import xyz.ksharma.krail.core.log.log as krailLog

/**
 * Every plugin the KRAIL client runs, in the order it runs them.
 *
 * This block used to exist twice, once per platform, as a byte-for-byte copy in
 * `androidMain` and `iosMain` whose only real difference was the engine constructor.
 * Two copies of a pipeline is two pipelines: an edit to one of them is invisible to
 * the other, no test compares them, and the first symptom would be a behaviour that
 * only reproduces on one platform. The engine is now the only thing either platform
 * declares — see [krailHttpClientEngine].
 *
 * Order matters and is deliberate:
 * 1. [defaultRequest] stamps the app version on every call, including retries.
 * 2. [ContentNegotiation] decodes JSON. It never overwrites an `Accept` header a
 *    caller set explicitly, which the five protobuf endpoints depend on;
 *    `ContentNegotiationAcceptHeaderTest` pins that.
 * 3. [Logging] on debug builds only, method + URL + status, never bodies.
 * 4. [installKrailRetry] before the validator, so a retried request is validated too.
 * 5. [installCaptivePortalValidator] while response headers still exist.
 * 6. [HttpTimeout] last, so its budget covers the whole pipeline above it.
 */
internal fun HttpClientConfig<*>.installKrailDefaults(
    appInfoProvider: AppInfoProvider,
    connectivity: ConnectivityObserver,
    onRetry: (endpoint: String, upstream: String) -> Unit,
) {
    expectSuccess = true

    defaultRequest {
        header(KRAIL_VERSION_HEADER, appInfoProvider.getAppInfo().appVersion)
    }

    installJsonNegotiation()

    installKrailLogging(appInfoProvider = appInfoProvider)

    // Retry policy lives in one place so a future upstream inherits it rather than
    // opting in. It declines entirely while transport is down.
    installKrailRetry(connectivity = connectivity, onRetry = onRetry)

    // Turns a Wi-Fi login page into NetworkError.CaptivePortal while the response
    // headers still exist. Without it a portal is indistinguishable from a schema
    // change by the time deserialisation fails.
    installCaptivePortalValidator()

    installKrailTimeouts()
}

private fun HttpClientConfig<*>.installJsonNegotiation() {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            },
        )
    }
}

private fun HttpClientConfig<*>.installKrailLogging(appInfoProvider: AppInfoProvider) {
    install(Logging) {
        if (appInfoProvider.getAppInfo().isDebug) {
            // Method + URL + status only. Bodies contain stop ids and times that,
            // in aggregate, may reveal user patterns — so we never log them.
            // (See KRAIL_INTEGRATION_MASTER_PLAN.md §13 on logging.)
            level = LogLevel.INFO
            logger = object : Logger {
                override fun log(message: String) {
                    krailLog("$KRAIL_NETWORK_LOG_TAG $message")
                }
            }
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        } else {
            level = LogLevel.NONE
        }
    }
}

private fun HttpClientConfig<*>.installKrailTimeouts() {
    install(HttpTimeout) {
        requestTimeoutMillis = DEFAULT_TIMEOUTS.requestTimeoutMillis
        connectTimeoutMillis = DEFAULT_TIMEOUTS.connectTimeoutMillis
        socketTimeoutMillis = DEFAULT_TIMEOUTS.socketTimeoutMillis
    }
}

// Sent on every request so a server-side log line can be read against an app version
// without the rider being identifiable from it.
private const val KRAIL_VERSION_HEADER = "X-Krail-Version"
