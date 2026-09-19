package xyz.ksharma.krail.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

/**
 * Darwin, i.e. `NSURLSession`. `NetworkErrorClassifier.ios.kt` reads
 * `DarwinHttpRequestException.origin.code` against Apple's `NSURLErrorDomain` codes,
 * which no other engine raises, so the engine choice and that file are one decision.
 */
internal actual fun krailHttpClientEngine(): HttpClientEngine = Darwin.create()
