package xyz.ksharma.krail.core.network.error

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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

    private fun caller(observer: ConnectivityObserver) =
        NetworkCaller(connectivity = observer, ioDispatcher = UnconfinedTestDispatcher())

    @Test
    fun `a successful call returns the value`() = runTest {
        val result = caller(TestObserver(TransportState.Up)).call { 42 }

        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `a failure is always wrapped in a NetworkException`() = runTest {
        val result = caller(TestObserver(TransportState.Up)).call<Int> {
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
                caller(TestObserver(TransportState.Up)).call<Int> {
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

        val result = caller(observer).call<Int> {
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
        val result = caller(TestObserver(TransportState.Up)).call<Int> {
            throw java.net.UnknownHostException("api.transport.nsw.gov.au")
        }

        assertEquals(NetworkError.Unreachable, result.networkErrorOrNull())
    }

    @Test
    fun `transport Unknown does not produce an offline claim`() = runTest {
        // Nothing has been heard from the OS yet. That is not evidence the rider is offline,
        // and telling them so would be a confident guess.
        val result = caller(TestObserver(TransportState.Unknown)).call<Int> {
            throw java.net.UnknownHostException("api.transport.nsw.gov.au")
        }

        assertEquals(NetworkError.Unreachable, result.networkErrorOrNull())
    }
}
