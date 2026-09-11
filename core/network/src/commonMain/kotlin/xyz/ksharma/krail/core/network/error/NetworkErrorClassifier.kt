package xyz.ksharma.krail.core.network.error

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.serialization.JsonConvertException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Turns a thrown exception plus the transport state observed at the moment it was
 * thrown into a [NetworkError].
 *
 * ## Why two inputs
 *
 * Neither source is sufficient alone, and this is the crux of the whole design.
 *
 * The OS transport state is a hint, not a fact: a device behind a captive portal,
 * on a dropped VPN, or on a carrier blackholing packets all report a usable
 * network. And the exception alone cannot classify either, because on Android both
 * airplane mode and a genuine DNS failure on a working network surface as
 * `UnknownHostException`. Those are different sentences to the rider and one of
 * them is not their fault.
 *
 * So: transport state is an input, the request outcome is the authority. The same
 * throwable classifies differently depending on what the OS was reporting when it
 * landed.
 *
 * The full mapping, per platform, is the table in `docs/NETWORK_RELIABILITY.md`.
 * That table is the specification for this function and the fixture its tests are
 * written from.
 *
 * @param isTransportDown whether the OS reported no usable transport. Note the
 *   polarity: this is `TransportState.mayClaimOffline`, which is false while the
 *   state is still `Unknown`. An app that has not yet heard from the OS must not
 *   tell a rider they are offline.
 */
expect fun Throwable.toNetworkError(isTransportDown: Boolean): NetworkError

/**
 * The cases both platforms share, which is most of them: everything Ktor itself
 * raises above the engine.
 *
 * Each `actual` handles the platform-specific connection failures first and
 * delegates here for the rest, so the shared behaviour cannot drift between
 * Android and iOS. That drift is a known failure mode in this codebase; see
 * `docs/learning/2026-08-22-two-platforms-two-vocabularies.md`.
 *
 * @return the shared classification, or null when this throwable is one only the
 *   platform can identify.
 */
internal fun Throwable.toSharedNetworkError(): NetworkError? = when (this) {
    // Cancellation is not a failure. A rider leaving the screen cancels the job,
    // and classifying that as an error paints an error state over a screen nobody
    // is looking at. suspendSafeResult already calls ensureActive() so this should
    // never arrive, and it is rethrown rather than classified if it does.
    is CancellationException -> throw this

    is HttpRequestTimeoutException,
    is ConnectTimeoutException,
    is SocketTimeoutException,
    -> NetworkError.Timeout

    // Checked before the ResponseException branch below: a captive portal answers
    // 200 with a login page, so it is not an HTTP error at all and only the
    // content type gives it away. CaptivePortalException is raised by the response
    // validator, which still has the headers.
    is CaptivePortalException -> NetworkError.CaptivePortal

    is ClientRequestException -> NetworkError.Request(code = response.status.value)
    is ServerResponseException -> NetworkError.Upstream(code = response.status.value)

    // Any other non-2xx that expectSuccess turned into an exception.
    is ResponseException -> response.status.value.let { code ->
        if (code in SERVER_ERROR_RANGE) NetworkError.Upstream(code) else NetworkError.Request(code)
    }

    is JsonConvertException -> NetworkError.Malformed

    else -> null
}

private val SERVER_ERROR_RANGE = 500..599

/**
 * Raised by the response validator when an endpoint that serves JSON or protobuf
 * answers with something else, which in practice means a Wi-Fi login page.
 *
 * Exists because the distinction between a captive portal and a genuine schema
 * change has to be drawn while the response headers are still in hand. By the time
 * deserialisation throws, both look identical.
 */
class CaptivePortalException(
    val declaredContentType: String?,
) : Exception("unexpected content type: $declaredContentType")
