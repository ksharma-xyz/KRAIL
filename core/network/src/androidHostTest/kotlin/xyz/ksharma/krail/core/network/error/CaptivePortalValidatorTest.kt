package xyz.ksharma.krail.core.network.error

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Drives the validator through a real Ktor client rather than calling it directly, because
 * the thing worth checking is that it is installed in the request pipeline at all and that
 * what it throws survives classification.
 *
 * The failure mode this guards is specific: a captive portal answers `200` with a login page,
 * so `expectSuccess` is satisfied and nothing goes wrong until the body fails to parse. By
 * then the headers are gone and the portal is indistinguishable from a schema change. Only
 * one of those two is fixable by the rider.
 */
class CaptivePortalValidatorTest {

    private fun client(
        status: HttpStatusCode,
        contentType: ContentType,
        body: String,
    ) = HttpClient(MockEngine) {
        expectSuccess = true
        installCaptivePortalValidator()
        engine {
            addHandler {
                respond(
                    content = body,
                    status = status,
                    headers = headersOf(HttpHeaders.ContentType, contentType.toString()),
                )
            }
        }
    }

    private suspend fun classify(client: HttpClient): NetworkError =
        runCatching { client.get("https://api.transport.nsw.gov.au/v1/tp/trip").bodyAsText() }
            .exceptionOrNull()
            ?.toNetworkError(isTransportDown = false)
            ?: NetworkError.Unknown(cause = IllegalStateException("request unexpectedly succeeded"))

    @Test
    fun `an HTML login page answering 200 is a captive portal`() = runTest {
        // The shape that matters: status says success, so nothing else in the stack objects.
        val error = classify(
            client(
                status = HttpStatusCode.OK,
                contentType = ContentType.Text.Html,
                body = "<html><body>Sign in to WiFi</body></html>",
            ),
        )

        assertEquals(NetworkError.CaptivePortal, error)
    }

    @Test
    fun `HTML is a portal regardless of status code`() = runTest {
        // Some portals answer 511 Network Authentication Required, some redirect. All three
        // arrive as HTML, which is why the validator ignores the status.
        val error = classify(
            client(
                status = HttpStatusCode(PORTAL_STATUS, "Network Authentication Required"),
                contentType = ContentType.Text.Html,
                body = "<html>portal</html>",
            ),
        )

        assertEquals(NetworkError.CaptivePortal, error)
    }

    @Test
    fun `a normal JSON response passes through untouched`() = runTest {
        val response = client(
            status = HttpStatusCode.OK,
            contentType = ContentType.Application.Json,
            body = """{"journeys":[]}""",
        ).get("https://api.transport.nsw.gov.au/v1/tp/trip").bodyAsText()

        assertEquals("""{"journeys":[]}""", response)
    }

    @Test
    fun `protobuf passes through untouched`() = runTest {
        // The BFF proto endpoints must not be mistaken for portals.
        val response = client(
            status = HttpStatusCode.OK,
            contentType = ContentType("application", "x-protobuf"),
            body = "binary-ish",
        ).get("https://bff.example/api/v1/trip/plan-proto").bodyAsText()

        assertEquals("binary-ish", response)
    }

    @Test
    fun `a genuine 5xx stays an upstream failure and is not called a portal`() = runTest {
        // NSW returning an HTML error page would be ambiguous, but NSW returns JSON, and
        // calling an outage "sign in to this Wi-Fi" would be a confident wrong answer.
        val error = classify(
            client(
                status = HttpStatusCode.ServiceUnavailable,
                contentType = ContentType.Application.Json,
                body = """{"error":"unavailable"}""",
            ),
        )

        assertIs<NetworkError.Upstream>(error)
        assertEquals(HttpStatusCode.ServiceUnavailable.value, error.code)
    }

    @Test
    fun `a captive portal is a connection problem and is never retried`() = runTest {
        val error = classify(
            client(
                status = HttpStatusCode.OK,
                contentType = ContentType.Text.Html,
                body = "<html>portal</html>",
            ),
        )

        // Retrying cannot help: only the rider accepting the portal can.
        assertEquals(false, error.isRetryable)
        assertEquals(true, error.isConnectionProblem)
        assertEquals(false, error.recoversOnReconnect)
    }

    private companion object {
        /** 511 Network Authentication Required, which some portals answer with. */
        const val PORTAL_STATUS = 511
    }
}
