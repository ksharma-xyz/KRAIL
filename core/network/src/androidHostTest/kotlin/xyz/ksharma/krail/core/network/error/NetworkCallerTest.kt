package xyz.ksharma.krail.core.network.error

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import xyz.ksharma.krail.core.analytics.Analytics
import xyz.ksharma.krail.core.analytics.event.AnalyticsEvent
import xyz.ksharma.krail.core.connectivity.ConnectivityObserver
import xyz.ksharma.krail.core.connectivity.TransportState
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Holds the contract every `Real*Service` depends on: a failure is always a
 * [NetworkException], cancellation is never one, and the transport state is read at the moment
 * of failure rather than captured earlier.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NetworkCallerTest {

    private class TestObserver(initial: TransportState) : ConnectivityObserver {
        private val _state = MutableStateFlow(initial)
        override val state: StateFlow<TransportState> = _state
        fun set(value: TransportState) {
            _state.value = value
        }
    }

    // Local rather than :core:testing's FakeAnalytics: that module depends on
    // :core:network, so consuming it here would be a project cycle.
    private class RecordingAnalytics : Analytics {
        val events = mutableListOf<AnalyticsEvent>()
        override fun track(event: AnalyticsEvent) { events += event }
        override fun setUserId(userId: String) = Unit
        override fun setUserProperty(name: String, value: String) = Unit
    }

    private val analytics = RecordingAnalytics()

    private fun caller(observer: ConnectivityObserver) = NetworkCaller(
        connectivity = observer,
        ioDispatcher = UnconfinedTestDispatcher(),
        analytics = analytics,
    )

    private suspend fun <T> NetworkCaller.callTest(block: suspend () -> T) =
        call(endpoint = ENDPOINT, upstream = UPSTREAM, block = block)

    @Test
    fun `a successful call returns the value`() = runTest {
        val result = caller(TestObserver(TransportState.Up)).callTest { 42 }

        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `a failure is always wrapped in a NetworkException`() = runTest {
        val result = caller(TestObserver(TransportState.Up)).callTest<Int> {
            throw IllegalStateException("boom")
        }

        val thrown = result.exceptionOrNull()
        assertIs<NetworkException>(thrown)
        assertIs<NetworkError.Unknown>(thrown.error)
    }

    @Test
    fun `cancellation propagates instead of becoming a failed Result`() = runTest {
        // A rider leaving a screen cancels its jobs. Recording that as a network failure
        // paints an error state over a screen nobody is looking at.
        var observed: Throwable? = null

        val job = launch {
            try {
                caller(TestObserver(TransportState.Up)).callTest<Int> {
                    throw CancellationException("screen left")
                }
            } catch (expected: CancellationException) {
                observed = expected
            }
        }
        job.join()

        assertIs<CancellationException>(observed)
    }

    @Test
    fun `transport state is read when the call fails, not when it starts`() = runTest {
        // The difference matters: a request that starts connected and fails after the radio
        // drops is Offline, and a caller that captured the state up front would call it
        // Unreachable. Thirty seconds is exactly long enough to cross a tunnel.
        val observer = TestObserver(TransportState.Up)
        val started = CompletableDeferred<Unit>()

        val result = caller(observer).callTest<Int> {
            started.complete(Unit)
            // Transport drops mid-flight.
            observer.set(TransportState.Down)
            throw java.net.UnknownHostException("api.transport.nsw.gov.au")
        }

        assertTrue(started.isCompleted)
        assertEquals(NetworkError.Offline, result.networkErrorOrNull())
    }

    @Test
    fun `the same failure with transport up is unreachable, not offline`() = runTest {
        val result = caller(TestObserver(TransportState.Up)).callTest<Int> {
            throw java.net.UnknownHostException("api.transport.nsw.gov.au")
        }

        assertEquals(NetworkError.Unreachable, result.networkErrorOrNull())
    }

    @Test
    fun `transport Unknown does not produce an offline claim`() = runTest {
        // Nothing has been heard from the OS yet. That is not evidence the rider is offline,
        // and telling them so would be a confident guess.
        val result = caller(TestObserver(TransportState.Unknown)).callTest<Int> {
            throw java.net.UnknownHostException("api.transport.nsw.gov.au")
        }

        assertEquals(NetworkError.Unreachable, result.networkErrorOrNull())
    }

    @Test
    fun `repeated failures on one endpoint fire the event once`() = runTest {
        // An offline device on a polling screen calls the same endpoint every 30 seconds.
        // Without the failed-state set this would fire twice a minute for as long as the
        // rider stays in the tunnel.
        val networkCaller = caller(TestObserver(TransportState.Down))

        repeat(3) {
            networkCaller.callTest<Int> { throw java.net.UnknownHostException("nsw") }
        }

        val failures = analytics.events
            .filterIsInstance<AnalyticsEvent.NetworkStatusEvent>()
            .filter { it.action == AnalyticsEvent.NetworkStatusEvent.Action.FAILURE }
        assertEquals(1, failures.size)
        assertEquals("offline", failures.single().errorKind)
    }

    @Test
    fun `a success after a failure fires recovered exactly once`() = runTest {
        val observer = TestObserver(TransportState.Down)
        val networkCaller = caller(observer)

        networkCaller.callTest<Int> { throw java.net.UnknownHostException("nsw") }
        observer.set(TransportState.Up)
        networkCaller.callTest { 1 }
        // A second success is not a second recovery.
        networkCaller.callTest { 2 }

        val recovered = analytics.events
            .filterIsInstance<AnalyticsEvent.NetworkStatusEvent>()
            .filter { it.action == AnalyticsEvent.NetworkStatusEvent.Action.RECOVERED }
        assertEquals(1, recovered.size)
    }

    @Test
    fun `a success with no prior failure fires nothing`() = runTest {
        caller(TestObserver(TransportState.Up)).callTest { 1 }

        assertTrue(analytics.events.isEmpty())
    }

    private companion object {
        const val ENDPOINT = "/v1/tp/trip"
        const val UPSTREAM = "NSW"
    }
}
