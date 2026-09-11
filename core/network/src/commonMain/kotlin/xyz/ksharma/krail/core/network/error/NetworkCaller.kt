package xyz.ksharma.krail.core.network.error

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.core.connectivity.ConnectivityObserver
import xyz.ksharma.krail.core.connectivity.mayClaimOffline
import xyz.ksharma.krail.core.log.logError
import kotlin.coroutines.coroutineContext

/**
 * Runs a network call and returns a [Result] whose failure is always a [NetworkException].
 *
 * Every `Real*Service` goes through this. That is the point: classification needs the
 * transport state observed **at the moment the call failed**, and a service that reached for
 * the observer itself would be four services each deciding separately how to read it, which is
 * how the app ended up with two incompatible error contracts in the first place.
 *
 * Deliberately not a replacement for
 * [xyz.ksharma.krail.coroutines.ext.suspendSafeResult]. That stays the right tool for
 * non-network work. This one exists only where a throwable needs classifying against
 * connectivity.
 *
 * ## Cancellation
 *
 * [ensureActive] runs before classification, so a cancelled job propagates as a
 * [kotlin.coroutines.cancellation.CancellationException] rather than being turned into a
 * failed [Result]. A rider leaving a screen cancels its jobs, and recording that as a network
 * failure paints an error state over a screen nobody is looking at.
 */
class NetworkCaller(
    private val connectivity: ConnectivityObserver,
    private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * @param block the request. Anything it throws is classified; anything it returns is the
     *   success value.
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun <T> call(block: suspend () -> T): Result<T> = withContext(ioDispatcher) {
        try {
            Result.success(block())
        } catch (throwable: Throwable) {
            // Before anything else: a cancellation is not a failure to classify.
            coroutineContext.ensureActive()

            // Read the transport state now rather than passing it in. A caller that captured
            // it before the request would be classifying against a state up to 30 seconds
            // stale, which is exactly long enough to cross a tunnel.
            val transportDown = connectivity.state.value.mayClaimOffline
            val error = throwable.toNetworkError(isTransportDown = transportDown)

            logError("network call failed: ${error.kind} (transportDown=$transportDown)", throwable)
            Result.failure(NetworkException(error = error, cause = throwable))
        }
    }
}
