package xyz.ksharma.krail.trip.planner.network.api.service

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
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
    return HttpClient(Darwin) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            })
        }
        install(Logging) {
            coroutineScope.launch {
                if (appInfoProvider.getAppInfo().isDebug) {
                    level = LogLevel.BODY
                    logger = object : Logger {
                        override fun log(message: String) {
                            // Package name is used to avoid collision with the overriden Log method
                            xyz.ksharma.krail.core.log.log(message)
                        }
                    }
                    sanitizeHeader { header -> header == HttpHeaders.Authorization }
                } else {
                    level = LogLevel.NONE
                }
            }
        }

        defaultRequest {
            headers.append(
                HttpHeaders.Authorization,
                "apikey ${BuildKonfig.IOS_NSW_TRANSPORT_API_KEY}"
            )
        }
    }
}
