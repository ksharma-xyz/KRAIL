package xyz.ksharma.krail.core.testing.fakes

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import xyz.ksharma.krail.core.connectivity.ConnectivityObserver
import xyz.ksharma.krail.core.connectivity.TransportState

/**
 * Drives [TransportState] transitions directly so a test can exercise going
 * offline and coming back without a device.
 *
 * Starts at [TransportState.Unknown], matching the real observers: a test that
 * wants a known starting state has to say so, which is the point.
 */
class FakeConnectivityObserver(
    initial: TransportState = TransportState.Unknown,
) : ConnectivityObserver {

    private val _state = MutableStateFlow(initial)

    override val state: StateFlow<TransportState> = _state

    fun set(newState: TransportState) {
        _state.value = newState
    }

    fun goOffline() = set(TransportState.Down)

    fun goOnline() = set(TransportState.Up)
}
