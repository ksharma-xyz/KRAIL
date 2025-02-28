package xyz.ksharma.krail.trip.planner.network.api.service

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.trip.planner.network.BuildKonfig

actual fun httpClient(
    appInfoProvider: AppInfoProvider,
    coroutineScope: CoroutineScope,
): HttpClient {
    return HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            })
        }
        install(Logging) {
            level = LogLevel.BODY
            logger = object : Logger {
                override fun log(message: String) {
                    println(message)
                }
            }
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }

        defaultRequest {
            headers.append(
                HttpHeaders.Authorization,
                "apikey ${BuildKonfig.ANDROID_NSW_TRANSPORT_API_KEY}"
            )
        }
    }
}
