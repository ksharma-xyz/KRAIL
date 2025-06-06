package xyz.ksharma.krail.park.ride.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import xyz.ksharma.krail.core.network.NetworkBuildKonfig.IOS_NSW_TRANSPORT_API_KEY

actual fun parkRideHttpClient(baseClient: HttpClient): HttpClient {
    return baseClient.config {
        defaultRequest {
            headers.append(
                HttpHeaders.Authorization,
                "apikey $IOS_NSW_TRANSPORT_API_KEY"
            )
        }
    }
}
