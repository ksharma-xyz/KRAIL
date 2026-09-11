package xyz.ksharma.krail.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import xyz.ksharma.krail.core.connectivity.ConnectivityObserver
import xyz.ksharma.krail.core.connectivity.TransportState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Drives the retry policy through a real Ktor pipeline over a MockEngine.
 *
 * The two properties worth holding are both about *not* retrying: a 4xx never is, and
 * nothing is while transport is down. The second is what keeps an offline failure costing
 * one connect timeout instead of three, and it is invisible to any test that does not
 * actually run the plugin.
 */
class KrailRetryTest {

    private class TestObserver(initial: TransportState) : ConnectivityObserver {
        private val _state = MutableStateFlow(initial)
        override val state: StateFlow<TransportState> = _state
    }

    private class Scenario(
        transport: TransportState,
        private val responses: List<HttpStatusCode>,
    ) {
        var attempts = 0
            private set

        val retries = mutableListOf<Pair<String, String>>()

        val client: HttpClient = HttpClient(MockEngine) {
            expectSuccess = true
            installKrailRetry(
                connectivity = TestObserver(transport),
                onRetry = { endpoint, upstream -> retries += endpoint to upstream },
            )
            engine {
                addHandler {
                    val status = responses.getOrElse(attempts) { responses.last() }
                    attempts++
                    respond(content = "body", status = status)
                }
            }
        }
    }

    @Test
    fun `a 5xx is retried while transport is up`() = runTest {
        val scenario = Scenario(
            transport = TransportState.Up,
            responses = listOf(HttpStatusCode.ServiceUnavailable, HttpStatusCode.OK),
        )

        val body = scenario.client.get(NSW_URL).bodyAsText()

        assertEquals("body", body)
        assertEquals(2, scenario.attempts, "should have retried once")
    }

    @Test
    fun `retrying fires the observation hook with a path and an upstream`() = runTest {
        // Without this hook the plugin retries silently and Action.RETRY can never fire,
        // so there is no way to tell whether retrying earns its cost.
        val scenario = Scenario(
            transport = TransportState.Up,
            responses = listOf(HttpStatusCode.ServiceUnavailable, HttpStatusCode.OK),
        )

        scenario.client.get(NSW_URL).bodyAsText()

        assertEquals(1, scenario.retries.size)
        val (endpoint, upstream) = scenario.retries.single()
        assertEquals("NSW", upstream)
        assertTrue(endpoint.contains("trip"), "endpoint was $endpoint")
        assertTrue(!endpoint.contains("?"), "query string must never be reported: $endpoint")
    }

    @Test
    fun `nothing is retried while transport is down`() = runTest {
        // The whole point of the gate. Three attempts at a 15 second connect timeout is 45
        // seconds of spinner to reach a conclusion the OS already had.
        val scenario = Scenario(
            transport = TransportState.Down,
            responses = listOf(HttpStatusCode.ServiceUnavailable),
        )

        assertFailsWith<ServerResponseException> { scenario.client.get(NSW_URL).bodyAsText() }

        assertEquals(1, scenario.attempts, "should not have retried while offline")
        assertTrue(scenario.retries.isEmpty())
    }

    @Test
    fun `a 4xx is never retried`() = runTest {
        // A rejected key or an unknown stop id produces the same answer every time.
        val scenario = Scenario(
            transport = TransportState.Up,
            responses = listOf(HttpStatusCode.NotFound),
        )

        assertFailsWith<Exception> { scenario.client.get(NSW_URL).bodyAsText() }

        assertEquals(1, scenario.attempts)
        assertTrue(scenario.retries.isEmpty())
    }

    @Test
    fun `retries are capped so a polling screen cannot multiply load`() = runTest {
        val scenario = Scenario(
            transport = TransportState.Up,
            responses = listOf(HttpStatusCode.ServiceUnavailable),
        )

        assertFailsWith<ServerResponseException> { scenario.client.get(NSW_URL).bodyAsText() }

        // One original plus at most two retries.
        assertEquals(3, scenario.attempts)
    }

    private companion object {
        const val NSW_URL = "https://api.transport.nsw.gov.au/v1/tp/trip?name_origin=200060"
    }
}
