package xyz.ksharma.krail.core.network.error

import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Android classification, over the OkHttp engine.
 *
 * The interesting line is the first one. `UnknownHostException` is what OkHttp
 * throws both when the device is in airplane mode and when DNS genuinely fails on
 * a working network, because in both cases the lookup does not complete. Nothing
 * in the exception distinguishes them, so the transport state decides, and it is
 * the reason this function takes a second argument at all.
 *
 * Verified against a real Ktor OkHttp client in `NetworkErrorClassifierTest`,
 * which exercises unresolvable hosts, refused connections and timeouts rather than
 * constructing the exceptions by hand.
 */
actual fun Throwable.toNetworkError(isTransportDown: Boolean): NetworkError {
    toSharedNetworkError()?.let { return it }

    return when (this) {
        // Both airplane mode and a real DNS failure. Only the transport state
        // separates them, and while it is Unknown we decline to blame the rider.
        is UnknownHostException -> if (isTransportDown) {
            NetworkError.Offline
        } else {
            NetworkError.Unreachable
        }

        // java.net's socket timeout, distinct from Ktor's own. OkHttp surfaces
        // this one for a read that stalls past the socket timeout.
        is SocketTimeoutException -> NetworkError.Timeout

        is ConnectException,
        is NoRouteToHostException,
        is SocketException,
        -> if (isTransportDown) NetworkError.Offline else NetworkError.Unreachable

        // A TLS failure on a network that is up is not a connectivity problem and
        // must not read as one. Retrying will not help either.
        is SSLException -> NetworkError.Unknown(cause = this)

        // Catch-all for the rest of java.io: broken pipes, resets and the engine's
        // own wrappers. Transport being down is the stronger signal here.
        is IOException -> if (isTransportDown) NetworkError.Offline else NetworkError.Unreachable

        else -> NetworkError.Unknown(cause = this)
    }
}
