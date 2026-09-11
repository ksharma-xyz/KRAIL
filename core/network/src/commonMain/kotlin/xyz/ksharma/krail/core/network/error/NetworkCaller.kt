package xyz.ksharma.krail.core.network.error

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
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
    private val analytics: Analytics,
) {

    private val endpointsCurrentlyFailing = mutableSetOf<String>()
    private val failingEndpointsLock = Mutex()

    /**
     * @param endpoint path only, never a query string. Query strings carry stop ids, and
     *   those are not sent to analytics. Used to key the failed-state transition.
     * @param upstream `nsw` or `bff`, so our infrastructure's failures can be told apart
     *   from NSW's.
     * @param block the request. Anything it throws is classified; anything it returns is the
     *   success value.
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun <T> call(
        endpoint: String,
        upstream: String,
        block: suspend () -> T,
    ): Result<T> = withContext(ioDispatcher) {
        try {
            val value = block()
            recordSuccess(endpoint = endpoint, upstream = upstream)
            Result.success(value)
        } catch (throwable: Throwable) {
            // Before anything else: a cancellation is not a failure to classify.
            coroutineContext.ensureActive()

            // Read the transport state now rather than passing it in. A caller that captured
            // it before the request would be classifying against a state up to 30 seconds
            // stale, which is exactly long enough to cross a tunnel.
            val transportDown = connectivity.state.value.mayClaimOffline
            val error = throwable.toNetworkError(isTransportDown = transportDown)

            logError("network call failed: ${error.kind} (transportDown=$transportDown)", throwable)
            recordFailure(
                endpoint = endpoint,
                upstream = upstream,
                error = error,
                transportDown = transportDown,
            )
            Result.failure(NetworkException(error = error, cause = throwable))
        }
    }

    /**
     * Records that the retry plugin re-sent a request.
     *
     * Not deduped, unlike failures: the interesting number is how many retries happened,
     * and comparing that against FAILURE rows is what says whether retrying earns its cost
     * or just multiplies load during an incident. The plugin caps at two attempts per
     * request, so this cannot run away.
     */
    fun recordRetry(endpoint: String, upstream: String) {
        analytics.track(
            AnalyticsEvent.NetworkStatusEvent(
                action = AnalyticsEvent.NetworkStatusEvent.Action.RETRY,
                errorKind = "none",
                transportUp = true,
                upstream = upstream,
                endpoint = endpoint,
            ),
        )
    }

    private suspend fun recordFailure(
        endpoint: String,
        upstream: String,
        error: NetworkError,
        transportDown: Boolean,
    ) {
        val isNewFailure = failingEndpointsLock.withLock { endpointsCurrentlyFailing.add(endpoint) }
        if (!isNewFailure) return

        analytics.track(
            AnalyticsEvent.NetworkStatusEvent(
                action = AnalyticsEvent.NetworkStatusEvent.Action.FAILURE,
                errorKind = error.kind,
                transportUp = !transportDown,
                upstream = upstream,
                endpoint = endpoint,
            ),
        )
    }

    private suspend fun recordSuccess(endpoint: String, upstream: String) {
        val wasFailing = failingEndpointsLock.withLock { endpointsCurrentlyFailing.remove(endpoint) }
        if (!wasFailing) return

        analytics.track(
            AnalyticsEvent.NetworkStatusEvent(
                action = AnalyticsEvent.NetworkStatusEvent.Action.RECOVERED,
                // No error to report on the way back up; the pair is joined on endpoint.
                errorKind = "none",
                transportUp = true,
                upstream = upstream,
                endpoint = endpoint,
            ),
        )
    }
}
