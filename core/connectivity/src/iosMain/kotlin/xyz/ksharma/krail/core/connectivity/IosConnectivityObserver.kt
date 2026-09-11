package xyz.ksharma.krail.core.connectivity

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.Network.nw_path_t
import platform.darwin.dispatch_queue_create

/**
 * [ConnectivityObserver] over Apple's `nw_path_monitor` from the Network
 * framework.
 *
 * Hand-rolled rather than taking a KMP connectivity dependency: this is roughly
 * sixty lines over an API Kotlin/Native already exposes, and a dependency that has
 * to be read anyway the first time it misbehaves in a tunnel is a bad trade. See
 * the decisions table in `docs/NETWORK_RELIABILITY.md`.
 *
 * ## What this cannot tell us
 *
 * `nw_path_monitor` has no equivalent of Android's `NET_CAPABILITY_VALIDATED`.
 * `nw_path_status_satisfied` means a route exists and is usable in principle, not
 * that anything was reached over it. A captive portal reports satisfied. That is
 * not a gap to be closed here: the classifier's content-type check catches the
 * portal case at the moment it matters, which is when a request actually fails.
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosConnectivityObserver(
    scope: CoroutineScope,
) : ConnectivityObserver {

    override val state: StateFlow<TransportState> = callbackFlow {
        val monitor = nw_path_monitor_create()

        // The monitor delivers on whatever queue it is given. A dedicated serial
        // queue keeps updates ordered and off the main thread; trySend is safe to
        // call from it.
        val queue = dispatch_queue_create(MONITOR_QUEUE_LABEL, null)
        nw_path_monitor_set_queue(monitor, queue)

        nw_path_monitor_set_update_handler(monitor) { path: nw_path_t? ->
            trySend(path.toTransportState())
        }

        nw_path_monitor_start(monitor)

        // No seed send here: unlike Android's ConnectivityManager there is nothing
        // to read synchronously, and nw_path_monitor_start fires the update handler
        // with the current path immediately. Until it does, Unknown is the honest
        // answer.
        awaitClose { nw_path_monitor_cancel(monitor) }
    }
        .distinctUntilChanged()
        .logTransitions()
        .stateIn(
            scope = scope,
            // Eagerly for the same reason as Android: the classifier needs a real
            // value at the moment a request fails, not at the moment somebody
            // happens to be collecting.
            started = SharingStarted.Eagerly,
            initialValue = TransportState.Unknown,
        )

    private companion object {
        const val MONITOR_QUEUE_LABEL = "xyz.ksharma.krail.connectivity.monitor"
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun nw_path_t?.toTransportState(): TransportState = when {
    this == null -> TransportState.Unknown
    nw_path_get_status(this) == nw_path_status_satisfied -> TransportState.Up
    else -> TransportState.Down
}
