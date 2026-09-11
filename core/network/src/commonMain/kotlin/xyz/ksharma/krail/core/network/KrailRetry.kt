package xyz.ksharma.krail.core.network

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.http.HttpStatusCode
import xyz.ksharma.krail.core.connectivity.ConnectivityObserver
import xyz.ksharma.krail.core.connectivity.shouldAttemptRequest
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Retry policy for every KRAIL request.
 *
 * Installed once on the base client so a future upstream inherits it rather than opting in.
 * Three constraints matter more than the plugin's defaults, and each is a decision recorded
 * in `docs/NETWORK_RELIABILITY.md`.
 *
 * **Never retry while transport is down.** Three attempts at a 15 second connect timeout is
 * 45 seconds of spinner to reach a conclusion the OS already had. An offline failure comes
 * back through the reconnect path instead, which is why
 * [xyz.ksharma.krail.core.network.error.NetworkError.Offline] is not in the retryable set.
 *
 * **Never retry a 4xx.** A rejected key or an unknown stop id produces the same answer every
 * time; retrying only multiplies load during an incident.
 *
 * **Never retry more than twice.** Polling screens already refetch every 30 seconds. A
 * retrying poll doubles load on NSW during exactly the outage that caused the retry, so the
 * ceiling is deliberately low.
 */
internal fun HttpClientConfig<*>.installKrailRetry(connectivity: ConnectivityObserver) {
    install(HttpRequestRetry) {
        maxRetries = MAX_RETRIES

        retryIf { _, response ->
            // 5xx only. 4xx is never retried, and 2xx obviously is not either.
            connectivity.state.value.shouldAttemptRequest &&
                response.status.value in SERVER_ERROR_RANGE
        }

        retryOnExceptionIf { _, cause ->
            // Transport being down is the one case where retrying is pure waste: the OS has
            // already told us the next attempt fails the same way.
            connectivity.state.value.shouldAttemptRequest && cause.isTransient()
        }

        exponentialDelay(
            base = BACKOFF_BASE,
            maxDelayMs = MAX_BACKOFF.inWholeMilliseconds,
            // Jitter, so a stop that many riders are watching does not produce a
            // synchronised retry burst when NSW comes back.
            randomizationMs = JITTER.inWholeMilliseconds,
        )

        // Honour the upstream when it says how long to wait. Nothing KRAIL guesses beats
        // being told.
        modifyRequest { it.headers.remove(RETRY_ATTEMPT_HEADER) }
    }
}

/**
 * Whether an exception is worth a second attempt.
 *
 * Deliberately narrow: a timeout or a connection that failed to establish may succeed on a
 * retry, while a deserialisation failure or a TLS error will not. Classification proper
 * happens in `toNetworkError`; this is only the retry predicate, which runs before a failure
 * has a transport state to be classified against.
 */
private fun Throwable.isTransient(): Boolean =
    this is io.ktor.client.network.sockets.ConnectTimeoutException ||
        this is io.ktor.client.network.sockets.SocketTimeoutException ||
        this is io.ktor.client.plugins.HttpRequestTimeoutException

private const val MAX_RETRIES = 2
private const val BACKOFF_BASE = 2.0
private val MAX_BACKOFF = 4.seconds
private val JITTER = 250.milliseconds
private val SERVER_ERROR_RANGE = HttpStatusCode.InternalServerError.value..599
private const val RETRY_ATTEMPT_HEADER = "x-krail-retry-attempt"
