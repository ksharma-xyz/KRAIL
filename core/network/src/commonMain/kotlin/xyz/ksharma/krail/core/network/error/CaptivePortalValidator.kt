package xyz.ksharma.krail.core.network.error

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Detects a network answering on the upstream's behalf, which in practice means a Wi-Fi
 * login page.
 *
 * ## Why this cannot be done in the classifier
 *
 * A captive portal and a genuine schema change both surface as a failure to read the body,
 * and by the time that exception exists the response headers are gone. The only thing that
 * separates them is what the response *said* it was: an endpoint that promised JSON or
 * protobuf and answered `text/html` was intercepted. So the distinction has to be drawn
 * here, while the headers are still in hand, and [CaptivePortalException] carries it forward
 * to `toNetworkError`.
 *
 * The difference matters because only one of the two is fixable, and only by the rider.
 *
 * ## Why it checks for HTML rather than for "not JSON"
 *
 * A stricter rule, "fail anything that is not the content type we expected", would also fire
 * on an upstream that legitimately changed its type, on a `text/plain` error body, and on any
 * endpoint added later with a different shape. That would turn a schema change into a
 * confident "you need to sign in to this Wi-Fi", which is a worse answer than the generic one
 * it replaced.
 *
 * Portals are HTML. Nothing KRAIL calls serves HTML. That is the whole signal, and keeping it
 * narrow is what stops this from producing false accusations.
 */
internal fun HttpClientConfig<*>.installCaptivePortalValidator() {
    HttpResponseValidator {
        validateResponse { response ->
            if (response.looksLikeCaptivePortal()) {
                throw CaptivePortalException(
                    declaredContentType = response.contentType()?.toString(),
                )
            }
        }
    }
}

/**
 * True when a response claims to be a web page.
 *
 * Deliberately ignores the status code. A portal usually answers `200` with a login page,
 * but some answer `511 Network Authentication Required` and some redirect, and all three
 * arrive here as HTML. The content type is the reliable part.
 */
private fun HttpResponse.looksLikeCaptivePortal(): Boolean {
    val type = contentType() ?: return false
    return type.match(ContentType.Text.Html)
}
