package xyz.ksharma.krail.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

/**
 * OkHttp, which is what `NetworkErrorClassifier.android.kt` classifies the exceptions
 * of: `UnknownHostException` for both airplane mode and a real DNS failure, and
 * `java.net.SocketTimeoutException` for a stalled read. Swapping the engine would
 * silently invalidate that file and the tests written against it.
 */
internal actual fun krailHttpClientEngine(): HttpClientEngine = OkHttp.create()
