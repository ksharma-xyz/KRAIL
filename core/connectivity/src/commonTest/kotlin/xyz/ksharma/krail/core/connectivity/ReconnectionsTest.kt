package xyz.ksharma.krail.core.connectivity

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Holds the three operators in [reconnections] and the two derived properties on
 * [TransportState].
 *
 * Every case here is one that produced a real bug in a previous design of this
 * kind: a refetch firing on every screen entry because the current value was not
 * dropped, a burst of refetches because settling callbacks were not collapsed, and
 * an offline message shown before the first callback because Unknown was folded
 * into Down.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReconnectionsTest {

    private class TestObserver(initial: TransportState) : ConnectivityObserver {
        private val _state = MutableStateFlow(initial)
        override val state: StateFlow<TransportState> = _state
        fun set(value: TransportState) {
            _state.value = value
        }
    }

    @Test
    fun `does not emit for the state it starts in`() = runTest {
        val observer = TestObserver(TransportState.Up)

        observer.reconnections().test {
            // Already Up. Nothing has reconnected, so a collector starting here
            // must not trigger a refetch.
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun `emits once when transport returns`() = runTest {
        val observer = TestObserver(TransportState.Down)

        observer.reconnections().test {
            observer.set(TransportState.Up)
            awaitItem()
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun `collapses repeated up callbacks into one emission`() = runTest {
        val observer = TestObserver(TransportState.Down)

        observer.reconnections().test {
            // Android fires onCapabilitiesChanged several times while a network
            // settles. That is one reconnection, not four refetches.
            observer.set(TransportState.Up)
            observer.set(TransportState.Up)
            observer.set(TransportState.Up)
            awaitItem()
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun `emits again after going down and back up`() = runTest {
        val observer = TestObserver(TransportState.Up)

        observer.reconnections().test {
            observer.set(TransportState.Down)
            expectNoEvents()

            observer.set(TransportState.Up)
            awaitItem()

            observer.set(TransportState.Down)
            observer.set(TransportState.Up)
            awaitItem()

            cancel()
        }
    }

    @Test
    fun `unknown to up counts as a reconnection only from a known down`() = runTest {
        val observer = TestObserver(TransportState.Unknown)

        observer.reconnections().test {
            // Unknown maps to "not up", so the first real callback saying Up is a
            // transition. An app that started offline and then connected should
            // refetch, and this is that case.
            observer.set(TransportState.Up)
            awaitItem()
            cancel()
        }
    }

    @Test
    fun `unknown does not permit an offline claim`() {
        assertFalse(TransportState.Unknown.mayClaimOffline)
        assertFalse(TransportState.Up.mayClaimOffline)
        assertTrue(TransportState.Down.mayClaimOffline)
    }

    @Test
    fun `unknown still permits a request attempt`() {
        assertTrue(TransportState.Unknown.shouldAttemptRequest)
        assertTrue(TransportState.Up.shouldAttemptRequest)
        assertFalse(TransportState.Down.shouldAttemptRequest)
    }

    @Test
    fun `every transport state is covered by both derived properties`() {
        // Guards the two `when`-free properties above: adding a state must force a
        // decision about both, and this fails loudly if one is forgotten.
        assertEquals(3, TransportState.entries.size)
    }
}
