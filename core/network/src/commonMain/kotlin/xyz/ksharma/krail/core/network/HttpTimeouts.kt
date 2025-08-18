package xyz.ksharma.krail.core.network

import io.ktor.client.plugins.HttpTimeoutConfig
import kotlin.time.Duration.Companion.seconds

val REQUEST_TIMEOUT = 30.seconds.inWholeMilliseconds
val CONNECT_TIMEOUT = 15.seconds.inWholeMilliseconds
val SOCKET_TIMEOUT = 30.seconds.inWholeMilliseconds

val DEFAULT_TIMEOUTS = HttpTimeoutConfig(
    requestTimeoutMillis = REQUEST_TIMEOUT,
    connectTimeoutMillis = CONNECT_TIMEOUT,
    socketTimeoutMillis = SOCKET_TIMEOUT
)