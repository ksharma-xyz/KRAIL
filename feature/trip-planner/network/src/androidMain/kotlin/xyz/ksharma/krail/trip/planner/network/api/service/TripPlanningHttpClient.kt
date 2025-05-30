package xyz.ksharma.krail.trip.planner.network.api.service

import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import xyz.ksharma.krail.core.network.NetworkBuildKonfig.ANDROID_NSW_TRANSPORT_API_KEY

actual fun tripPlannerHttpClient(baseClient: HttpClient): HttpClient {
    return baseClient.config {
        defaultRequest {
            headers.append(
                HttpHeaders.Authorization,
                "apikey $ANDROID_NSW_TRANSPORT_API_KEY"
            )
        }
    }
}
