package xyz.ksharma.krail.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import xyz.ksharma.krail.core.appinfo.AppInfoProvider
import xyz.ksharma.krail.core.log.log as krailLog

actual fun baseHttpClient(
    appInfoProvider: AppInfoProvider,
    coroutineScope: CoroutineScope,
): HttpClient {
    return HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = true
                }
            )
        }
        install(Logging) {
            coroutineScope.launch {
                if (appInfoProvider.getAppInfo().isDebug) {
                    level = LogLevel.BODY
                    logger = object : Logger {
                        override fun log(message: String) {
                            krailLog(message)
                        }
                    }
                    sanitizeHeader { header -> header == HttpHeaders.Authorization }
                } else {
                    level = LogLevel.NONE
                }
            }
        }
    }
}
