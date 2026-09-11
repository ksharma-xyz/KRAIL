package xyz.ksharma.krail.core.connectivity

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * Reports what the operating system knows about the device's network transport.
 *
 * One app-scoped singleton. The underlying OS registration is made once at app
 * start and deliberately never torn down: every consumer shares the same
 * [StateFlow], so there is nothing to reference-count and no teardown to get
 * wrong.
 *
 * Read [TransportState] before using this. The short version: this answers "does
 * the OS think there is a network", which is not the same question as "will this
 * request work", and the app must never present the former as the latter.
 */
interface ConnectivityObserver {

    /**
     * Hot, conflated, app-scoped. Starts at [TransportState.Unknown] and never
     * completes.
     *
     * Safe to read synchronously via [StateFlow.value] from any thread, which is
     * what the failure classifier does at the moment a request throws.
     */
    val state: StateFlow<TransportState>
}

/**
 * Emits once each time transport comes back after being away.
 *
 * The intended use is recovery: a ViewModel that recorded an offline failure
 * collects this and refetches, so a rider coming out of a tunnel gets their board
 * back without tapping anything.
 *
 * Three operators, each load-bearing:
 *
 *  - [distinctUntilChanged] collapses the repeated callbacks Android fires while
 *    a network is settling, so one reconnection is one emission.
 *  - [drop] discards the current value. Without it, a collector that starts while
 *    already [TransportState.Up] would immediately see a "reconnection" that never
 *    happened and fire a spurious refetch on every screen entry.
 *  - [filter] keeps only the transitions INTO connected.
 *
 * [TransportState.Unknown] is mapped to "not up", so the first real callback after
 * app start counts as a reconnection only if the app actually started offline.
 */
fun ConnectivityObserver.reconnections(): Flow<Unit> =
    state.map { it == TransportState.Up }
        .distinctUntilChanged()
        .drop(1)
        .filter { it }
        .map { }
