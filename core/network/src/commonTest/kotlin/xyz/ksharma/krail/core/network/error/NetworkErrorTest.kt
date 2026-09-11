package xyz.ksharma.krail.core.network.error

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Pins the behaviour of every [NetworkError] case.
 *
 * The `when` expressions in `NetworkError.kt` are exhaustive, so adding a case
 * already fails the build until someone decides what it means. These tests pin the
 * decisions themselves, which the compiler cannot check: that a 4xx is never
 * retried, that offline is the only case recovery waits for, and that an upstream
 * outage is not presented as the rider's connection problem.
 */
class NetworkErrorTest {

    private val allCases = listOf(
        NetworkError.Offline,
        NetworkError.Unreachable,
        NetworkError.Timeout,
        NetworkError.CaptivePortal,
        NetworkError.Malformed,
        NetworkError.Upstream(code = 503),
        NetworkError.Request(code = 404),
        NetworkError.Unknown(cause = IllegalStateException("boom")),
    )

    @Test
    fun `retryable cases are exactly unreachable timeout and upstream`() {
        assertTrue(NetworkError.Unreachable.isRetryable)
        assertTrue(NetworkError.Timeout.isRetryable)
        assertTrue(NetworkError.Upstream(code = 503).isRetryable)

        assertFalse(NetworkError.Offline.isRetryable)
        assertFalse(NetworkError.CaptivePortal.isRetryable)
        assertFalse(NetworkError.Malformed.isRetryable)
        assertFalse(NetworkError.Unknown(cause = RuntimeException()).isRetryable)
    }

    @Test
    fun `a 4xx is never retried`() {
        // Repeating a rejected key or an unknown stop id produces the same answer.
        // Retrying it only multiplies load during an incident.
        listOf(400, 401, 403, 404, 429).forEach { code ->
            assertFalse(NetworkError.Request(code = code).isRetryable, "HTTP $code")
        }
    }

    @Test
    fun `offline is not retryable but is the one case recovery waits for`() {
        // Retrying immediately while offline just burns the connect timeout again.
        // It comes back through the reconnection path instead.
        assertFalse(NetworkError.Offline.isRetryable)
        assertTrue(NetworkError.Offline.recoversOnReconnect)
    }

    @Test
    fun `no case other than offline recovers on reconnect`() {
        allCases.filter { it != NetworkError.Offline }.forEach { error ->
            assertFalse(error.recoversOnReconnect, "${error.kind} should not wait for reconnect")
        }
    }

    @Test
    fun `an upstream outage is not presented as the rider's connection problem`() {
        // The distinction the whole taxonomy exists for: NSW being down is not the
        // rider's fault and must not read as though it were.
        assertFalse(NetworkError.Upstream(code = 503).isConnectionProblem)
        assertFalse(NetworkError.Unreachable.isConnectionProblem)

        assertTrue(NetworkError.Offline.isConnectionProblem)
        assertTrue(NetworkError.CaptivePortal.isConnectionProblem)
    }

    @Test
    fun `kind is stable and unique across cases`() {
        val kinds = allCases.map { it.kind }
        assertEquals(kinds.size, kinds.toSet().size, "analytics kinds must not collide: $kinds")
        assertEquals("offline", NetworkError.Offline.kind)
        assertEquals("captive_portal", NetworkError.CaptivePortal.kind)
    }

    @Test
    fun `kind ignores the code so upstream failures aggregate`() {
        // One analytics bucket for "NSW is down", not one per status code.
        assertEquals(
            NetworkError.Upstream(code = 500).kind,
            NetworkError.Upstream(code = 503).kind,
        )
    }

    @Test
    fun `asNetworkError reads the error back out of a NetworkException`() {
        val thrown: Throwable = NetworkException(
            error = NetworkError.Timeout,
            cause = IllegalStateException("socket"),
        )

        assertSame(NetworkError.Timeout, thrown.asNetworkError())
    }

    @Test
    fun `asNetworkError wraps a stray throwable rather than throwing`() {
        // A service returning something other than a NetworkException is a bug, but
        // a call site handling a failure is the worst place to raise a second one.
        val stray = IllegalArgumentException("not from a service")

        val error = stray.asNetworkError()

        assertTrue(error is NetworkError.Unknown)
        assertSame(stray, error.cause)
    }

    @Test
    fun `networkErrorOrNull is null for a success`() {
        assertNull(Result.success(value = 1).networkErrorOrNull())
    }

    @Test
    fun `networkErrorOrNull surfaces the error for a failure`() {
        val result: Result<Int> = Result.failure(NetworkException(error = NetworkError.Offline))

        assertSame(NetworkError.Offline, result.networkErrorOrNull())
    }

    @Test
    fun `the exception message is the analytics kind`() {
        // Keeps a crash report and an analytics row describing the same failure in
        // the same words.
        assertEquals("offline", NetworkException(error = NetworkError.Offline).message)
    }
}
