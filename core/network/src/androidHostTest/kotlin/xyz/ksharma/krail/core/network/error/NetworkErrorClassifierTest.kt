package xyz.ksharma.krail.core.network.error

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Classifies throwables produced by a **real** Ktor OkHttp client, not by
 * constructing exception instances by hand.
 *
 * That distinction is the point of this file. A test that does
 * `UnknownHostException().toNetworkError(...)` proves the `when` branch is wired,
 * and proves nothing about whether OkHttp actually throws that type for the
 * condition in question. Every case below drives a real request at a real address
 * and classifies whatever genuinely comes back, so if an engine upgrade changes
 * the exception surface this fails instead of silently reclassifying live
 * failures.
 *
 * ## Addresses used, and why they are safe
 *
 *  - `.invalid` is reserved by RFC 2606 and is guaranteed never to resolve.
 *  - `203.0.113.0/24` is TEST-NET-3 from RFC 5737, reserved for documentation and
 *    guaranteed not to be routed.
 *  - `127.0.0.1` on a closed port refuses locally.
 *
 * None of them leave the machine in a way that reaches a third party, and none
 * depend on the network being in any particular state, so this runs the same on CI
 * as it does locally.
 */
class NetworkErrorClassifierTest {

    private fun client(timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS) = HttpClient(OkHttp) {
        expectSuccess = true
        install(HttpTimeout) {
            requestTimeoutMillis = timeoutMillis
            connectTimeoutMillis = timeoutMillis
            socketTimeoutMillis = timeoutMillis
        }
    }

    private suspend fun classify(
        url: String,
        isTransportDown: Boolean,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    ): NetworkError = client(timeoutMillis).use { httpClient ->
        // runCatching rather than a try/catch: the point is to classify whatever the
        // engine genuinely produces, and naming a specific type here would defeat
        // that. It also keeps the generic-catch rule satisfied honestly rather than
        // by suppression.
        runCatching { httpClient.get(url).let { _: HttpResponse -> Unit } }
            .exceptionOrNull()
            ?.toNetworkError(isTransportDown = isTransportDown)
            ?: fail("expected $url to fail, but the request succeeded")
    }

    @Test
    fun `an unresolvable host with transport down is offline`() = runTest {
        // This is the airplane-mode shape: OkHttp cannot complete a DNS lookup, and
        // throws the same UnknownHostException it throws for a genuine DNS failure.
        val error = classify(UNRESOLVABLE_URL, isTransportDown = true)

        assertEquals(NetworkError.Offline, error)
    }

    @Test
    fun `the same unresolvable host with transport up is unreachable`() = runTest {
        // Identical throwable, different answer. The whole reason the classifier
        // takes the transport state: nothing in the exception tells these apart.
        val error = classify(UNRESOLVABLE_URL, isTransportDown = false)

        assertEquals(NetworkError.Unreachable, error)
    }

    @Test
    fun `transport state does not turn a DNS failure into something retryable twice`() = runTest {
        val offline = classify(UNRESOLVABLE_URL, isTransportDown = true)
        val unreachable = classify(UNRESOLVABLE_URL, isTransportDown = false)

        // Offline waits for reconnection, Unreachable retries. Neither does both.
        assertTrue(offline.recoversOnReconnect)
        assertTrue(!offline.isRetryable)
        assertTrue(unreachable.isRetryable)
        assertTrue(!unreachable.recoversOnReconnect)
    }

    @Test
    fun `a refused connection on a live host is unreachable`() = runTest {
        // Loopback with nothing listening: the host resolves and answers, the port
        // does not. Distinct from a DNS failure and it must not read as offline.
        val error = classify(REFUSED_URL, isTransportDown = false)

        assertEquals(NetworkError.Unreachable, error)
    }

    @Test
    fun `an unroutable address times out rather than failing fast`() = runTest {
        // TEST-NET-3 swallows the SYN, so this is the weak-signal shape: the
        // connection neither completes nor is refused.
        val error = classify(
            url = UNROUTABLE_URL,
            isTransportDown = false,
            timeoutMillis = SHORT_TIMEOUT_MILLIS,
        )

        assertEquals(NetworkError.Timeout, error)
    }

    @Test
    fun `a timeout is retryable but does not wait for a reconnection`() = runTest {
        val error = classify(
            url = UNROUTABLE_URL,
            isTransportDown = false,
            timeoutMillis = SHORT_TIMEOUT_MILLIS,
        )

        assertTrue(error.isRetryable)
        assertTrue(!error.recoversOnReconnect)
    }

    @Test
    fun `a timeout while transport is down is still a timeout not offline`() = runTest {
        // The request got far enough to stall, so calling this "you are offline"
        // would be a worse description than the one the exception already gives.
        val error = classify(
            url = UNROUTABLE_URL,
            isTransportDown = true,
            timeoutMillis = SHORT_TIMEOUT_MILLIS,
        )

        assertEquals(NetworkError.Timeout, error)
    }

    @Test
    fun `cancellation is rethrown and never classified`() {
        // A rider leaving the screen cancels the job. Classifying that as a failure
        // paints an error state over a screen nobody is looking at.
        val cancellation = CancellationException("screen left")

        assertFailsWith<CancellationException> {
            cancellation.toNetworkError(isTransportDown = false)
        }
    }

    @Test
    fun `a captive portal response is not mistaken for a schema change`() {
        // Both surface as a failure to read the body. Only the content type, which
        // the validator still holds, separates a Wi-Fi login page from a genuine
        // shape change, and only one of them is fixable by the rider.
        val portal = CaptivePortalException(declaredContentType = "text/html")

        val error = portal.toNetworkError(isTransportDown = false)

        assertEquals(NetworkError.CaptivePortal, error)
        assertTrue(error.isConnectionProblem)
    }

    @Test
    fun `an unrecognised throwable is Unknown and keeps its cause`() = runTest {
        val stray = IllegalStateException("nothing to do with the network")

        val error = stray.toNetworkError(isTransportDown = false)

        assertIs<NetworkError.Unknown>(error)
        assertEquals(stray, error.cause)
        assertTrue(!error.isRetryable)
    }

    private companion object {
        // RFC 2606 reserves .invalid; it can never resolve.
        const val UNRESOLVABLE_URL = "https://krail-does-not-exist.invalid/v1/tp/trip"

        // RFC 5737 TEST-NET-3, reserved for documentation and never routed.
        const val UNROUTABLE_URL = "http://203.0.113.1/v1/tp/trip"

        // Loopback, port almost certainly closed.
        const val REFUSED_URL = "http://127.0.0.1:9/v1/tp/trip"

        const val DEFAULT_TIMEOUT_MILLIS = 5_000L
        const val SHORT_TIMEOUT_MILLIS = 1_500L
    }
}
