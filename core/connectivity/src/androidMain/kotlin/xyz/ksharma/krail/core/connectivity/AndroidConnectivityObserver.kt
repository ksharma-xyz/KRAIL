package xyz.ksharma.krail.core.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import xyz.ksharma.krail.core.log.log

/**
 * [ConnectivityObserver] over [ConnectivityManager.registerDefaultNetworkCallback].
 *
 * Requires `android.permission.ACCESS_NETWORK_STATE`, which is an install-time
 * permission with no runtime prompt.
 *
 * ## Why VALIDATED and not INTERNET
 *
 * [NetworkCapabilities.NET_CAPABILITY_INTERNET] means "this network claims to
 * offer internet access". It is true the moment a phone joins a cafe Wi-Fi, before
 * the captive portal has been accepted and while nothing can actually be reached.
 * Gating on it turns this class into a confident source of wrong answers, which is
 * worse than having no source at all.
 *
 * [NetworkCapabilities.NET_CAPABILITY_VALIDATED] means the platform actually
 * probed and got through. That is the closest thing the OS offers to the truth,
 * and it is still only a probe from a moment ago — see [TransportState].
 */
internal class AndroidConnectivityObserver(
    context: Context,
    scope: CoroutineScope,
) : ConnectivityObserver {

    private val connectivityManager: ConnectivityManager? =
        context.getSystemService(ConnectivityManager::class.java)

    override val state: StateFlow<TransportState> = callbackFlow {
        val manager = connectivityManager
        if (manager == null) {
            // No ConnectivityManager is not a "we are offline" signal, it is an
            // absence of information. Stay Unknown so requests are still attempted
            // and failures are never attributed to the rider's connection.
            log("KrailConnectivity: ConnectivityManager unavailable, staying Unknown")
            awaitClose { }
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities,
            ) {
                trySend(capabilities.toTransportState())
            }

            override fun onLost(network: Network) {
                trySend(TransportState.Down)
            }

            override fun onUnavailable() {
                trySend(TransportState.Down)
            }
        }

        // Seed from the current network before the first callback arrives. Without
        // this the flow sits at Unknown until something about the network changes,
        // which on a device that has been connected for hours may be never.
        trySend(manager.currentTransportState())

        manager.registerDefaultNetworkCallback(callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }
        .distinctUntilChanged()
        .logTransitions()
        .stateIn(
            scope = scope,
            // Eagerly, not WhileSubscribed: this is an app-scoped singleton whose
            // whole job is to already know the answer when a request fails. A
            // subscriber-gated registration would report Unknown at exactly the
            // moment the classifier needs a real value.
            started = SharingStarted.Eagerly,
            initialValue = TransportState.Unknown,
        )

    private fun ConnectivityManager.currentTransportState(): TransportState {
        val capabilities = activeNetwork?.let(::getNetworkCapabilities)
        return capabilities?.toTransportState() ?: TransportState.Down
    }
}

private fun NetworkCapabilities.toTransportState(): TransportState {
    val usable = hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    return if (usable) TransportState.Up else TransportState.Down
}
