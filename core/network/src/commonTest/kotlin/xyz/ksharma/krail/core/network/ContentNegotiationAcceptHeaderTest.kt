package xyz.ksharma.krail.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins one line of behaviour that five services depend on and none of them state.
 *
 * `RealTripPlanningService`, `RealDeparturesService`, `RealGtfsRealtimeService`,
 * `BffGtfsRealtimeRepository` and `RealParkRideService` all ask for
 * `application/x-protobuf` and then read the response with `readRawBytes()`. The
 * shared client installs `ContentNegotiation` with a JSON converter. If that plugin
 * appended `application/json` to the `Accept` header the way a content-negotiating
 * server would expect, an upstream would be free to answer JSON to a caller that is
 * about to hand the bytes to a protobuf decoder, and the failure would surface as a
 * decode error a long way from its cause.
 *
 * It does not append. This test is the evidence, and it is here so a Ktor upgrade
 * that changes the merge strategy fails the build instead of a rider's trip. Ktor
 * 3.6 added `ContentTypeMergeStrategy.SkipIfPresent` (KTOR-5009) to make this
 * configurable; KRAIL does not set it, because the default already behaves this way
 * and an explicit setting would be a second place to keep the two facts agreeing.
 */
class ContentNegotiationAcceptHeaderTest {

    private fun clientRecording(header: MutableList<String?>) = HttpClient(MockEngine) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = true
                },
            )
        }
        engine {
            addHandler { request ->
                header += request.headers[HttpHeaders.Accept]
                respond("")
            }
        }
    }

    @Test
    fun `an explicitly set Accept header is sent unchanged`() = runTest {
        val sent = mutableListOf<String?>()

        clientRecording(sent).get("https://api.transport.nsw.gov.au/v1/gtfs/realtime/buses") {
            accept(ContentType("application", "x-protobuf"))
        }

        assertEquals("application/x-protobuf", sent.single())
    }

    @Test
    fun `a request that asks for nothing still advertises JSON`() = runTest {
        val sent = mutableListOf<String?>()

        clientRecording(sent).get("https://api.transport.nsw.gov.au/v1/tp/trip")

        // The other half of the contract: ContentNegotiation is doing its job for the
        // JSON endpoints, so the first assertion is about precedence, not about the
        // plugin being inert.
        assertEquals("application/json", sent.single())
    }
}
