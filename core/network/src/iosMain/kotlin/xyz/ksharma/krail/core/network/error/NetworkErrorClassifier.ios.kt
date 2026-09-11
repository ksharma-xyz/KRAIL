package xyz.ksharma.krail.core.network.error

import io.ktor.client.engine.darwin.DarwinHttpRequestException

/**
 * iOS classification, over the Darwin engine.
 *
 * ## Unverified on a device
 *
 * Every mapping below is written from Apple's documented `NSURLErrorDomain` codes,
 * **not from observed throwables**. It has not been exercised against a real
 * network change, because an iOS simulator shares the host machine's network stack
 * and has no airplane mode, so the no-transport case cannot be produced there at
 * all.
 *
 * Treat the codes as a starting hypothesis. The first person to run this on a real
 * iPhone should log the actual `origin.code` for each condition in the taxonomy
 * table and correct whatever is wrong here. See the "Verified" column in
 * `docs/NETWORK_RELIABILITY.md`.
 *
 * The shared cases are not affected by this: anything Ktor raises above the engine
 * is classified by [toSharedNetworkError] and is identical on both platforms.
 *
 * ## Why the transport state still matters here
 *
 * Darwin does report `NSURLErrorNotConnectedToInternet` separately from a host
 * lookup failure, so iOS has more information in the exception than Android does.
 * The transport state is still consulted, for two reasons: it keeps the two
 * platforms agreeing about what Offline means, and it covers the case where Darwin
 * reports a generic failure while the OS already knows the radio is off.
 */
actual fun Throwable.toNetworkError(isTransportDown: Boolean): NetworkError {
    toSharedNetworkError()?.let { return it }

    return when ((this as? DarwinHttpRequestException)?.origin?.code) {
        NSURL_ERROR_NOT_CONNECTED_TO_INTERNET -> NetworkError.Offline

        NSURL_ERROR_CANNOT_FIND_HOST,
        NSURL_ERROR_CANNOT_CONNECT_TO_HOST,
        NSURL_ERROR_DNS_LOOKUP_FAILED,
        NSURL_ERROR_NETWORK_CONNECTION_LOST,
        -> if (isTransportDown) NetworkError.Offline else NetworkError.Unreachable

        NSURL_ERROR_TIMED_OUT -> NetworkError.Timeout

        // A TLS failure on a working network is not a connectivity problem and
        // retrying will not help, matching the Android side.
        NSURL_ERROR_SECURE_CONNECTION_FAILED -> NetworkError.Unknown(cause = this)

        // Not a Darwin engine exception at all, so there is no NSError to read.
        null -> NetworkError.Unknown(cause = this)

        else -> if (isTransportDown) NetworkError.Offline else NetworkError.Unknown(cause = this)
    }
}

// NSURLErrorDomain codes. Written as constants rather than pulled from the
// platform bindings so the values are readable next to the classification and can
// be checked against Apple's documentation without leaving the file.
private const val NSURL_ERROR_TIMED_OUT = -1001L
private const val NSURL_ERROR_CANNOT_FIND_HOST = -1003L
private const val NSURL_ERROR_CANNOT_CONNECT_TO_HOST = -1004L
private const val NSURL_ERROR_NETWORK_CONNECTION_LOST = -1005L
private const val NSURL_ERROR_DNS_LOOKUP_FAILED = -1006L
private const val NSURL_ERROR_NOT_CONNECTED_TO_INTERNET = -1009L
private const val NSURL_ERROR_SECURE_CONNECTION_FAILED = -1200L
