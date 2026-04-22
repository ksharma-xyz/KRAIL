package xyz.ksharma.krail.feature.track

import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object TripDeepLinkDecoder {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Decodes a base64url-encoded string (the `?d=` query param value) into [TripDeepLink].
     * Returns null if decoding or parsing fails.
     */
    fun decode(encoded: String): TripDeepLink? = runCatching {
        val jsonString = encoded.decodeBase64Url() ?: return null
        json.decodeFromString<TripDeepLink>(jsonString)
    }.getOrNull()
}

private const val BASE64_BLOCK_SIZE = 4

@OptIn(ExperimentalEncodingApi::class)
internal fun String.decodeBase64Url(): String? = runCatching {
    val padded = this + "=".repeat((BASE64_BLOCK_SIZE - this.length % BASE64_BLOCK_SIZE) % BASE64_BLOCK_SIZE)
    Base64.UrlSafe.decode(padded).decodeToString()
}.getOrNull()
