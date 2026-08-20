package xyz.ksharma.krail.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Local for receiving results in a [ResultEventBus]
 *
 * **IMPORTANT**: This always provides the singleton instance to ensure consistent behavior
 * across list and detail panes in adaptive layouts.
 *
 * ## Why Singleton?
 * In two-pane layouts (tablets/foldables), the list and detail screens exist in separate
 * composition scopes. Without a singleton:
 * - SearchStop (detail pane) and SavedTrips (list pane) would have different ResultEventBus instances
 * - Results sent from SearchStop wouldn't reach SavedTrips
 * - Stop selection wouldn't update the UI
 *
 * The singleton pattern ensures both panes share the same event bus, enabling proper
 * communication across composition boundaries.
 */
object LocalResultEventBusObj {
    private val LocalResultEventBus: ProvidableCompositionLocal<ResultEventBus> =
        compositionLocalOf { ResultEventBus.getInstance() }

    /**
     * The current [ResultEventBus] - returns the singleton instance
     */
    val current: ResultEventBus
        @Composable
        get() = LocalResultEventBus.current

    /**
     * Provides a [ResultEventBus] to the composition
     *
     * **Note**: This is kept for API compatibility, but always returns the singleton instance.
     */
    infix fun provides(
        bus: ResultEventBus,
    ): ProvidedValue<ResultEventBus> {
        return LocalResultEventBus.provides(bus)
    }
}

/**
 * An EventBus for passing results between multiple sets of screens.
 *
 * **SINGLETON PATTERN**: Shared across all navigation scopes (list/detail panes) to ensure
 * results are delivered correctly in two-pane layouts where list and detail have
 * separate composition scopes.
 *
 * ## Architecture
 * This implements a channel-based event bus pattern where:
 * - Each result type has its own buffered channel
 * - Senders use `sendResult()` to emit results
 * - Receivers use `ResultEffect` composable to listen for results
 * - Channels are created lazily when first accessed
 *
 * ## Usage
 * ```kotlin
 * // In sender screen (e.g., SearchStop):
 * val resultEventBus = LocalResultEventBus.current
 * resultEventBus.sendResult(result = MyResult(...))
 *
 * // In receiver screen (e.g., SavedTrips):
 * ResultEffect<MyResult> { result ->
 *     // Handle result
 * }
 * ```
 *
 * ## Why Singleton?
 * Critical for two-pane layouts where list and detail screens exist in separate
 * composition scopes. Without singleton, each scope would have its own bus instance,
 * breaking communication between panes.
 *
 * @see ResultEffect for the composable consumer API
 * @see LocalResultEventBusObj for accessing the singleton instance
 */
@OptIn(ExperimentalAtomicApi::class)
class ResultEventBus private constructor() {

    private val channels = AtomicReference<Map<String, Channel<Any?>>>(emptyMap())

    /**
     * Map from the result key to a channel of results. Read-only: create through
     * [channelFor] so the create stays atomic.
     */
    val channelMap: Map<String, Channel<Any?>> get() = channels.load()

    /**
     * The channel for [resultKey], creating it if this is the first anyone has asked.
     *
     * BUFFERED capacity, so a result sent while no one is collecting waits for the next
     * collector instead of being dropped. That is what makes the pane handoff work: the sender
     * navigates back before the receiver's effect has restarted.
     *
     * The atomic snapshot behind it is the point. This used to be a plain `mutableMapOf` with a
     * check-then-act create inside [sendResult]: two senders racing on one key could each see it
     * absent, each create a channel, and the second would replace the first in the map, taking
     * the first result with it — silently, with the receiver simply never hearing about a stop
     * the rider had picked. Reproduced in `ResultEventBusTest.concurrentSendsBothArrive` before
     * this changed, as a hang rather than an assertion failure, because a lost result is a
     * receiver that waits forever.
     *
     * A snapshot plus CAS rather than a lock: the map is tiny, writes are rare (once per result
     * key for the life of the process) and there is no common-code `synchronized`. A losing CAS
     * discards a channel nothing has ever sent to.
     */
    fun channelFor(resultKey: String): Channel<Any?> {
        while (true) {
            val current = channels.load()
            current[resultKey]?.let { return it }

            val created = Channel<Any?>(
                capacity = BUFFERED,
                onBufferOverflow = BufferOverflow.SUSPEND,
            )
            if (channels.compareAndSet(current, current + (resultKey to created))) return created
        }
    }

    /** Drops the channel for [resultKey], with the same CAS discipline as [channelFor]. */
    fun removeChannel(resultKey: String) {
        while (true) {
            val current = channels.load()
            if (!current.containsKey(resultKey)) return
            if (channels.compareAndSet(current, current - resultKey)) return
        }
    }

    /**
     * Provides a flow for the given resultKey, or null when nothing has ever used that key.
     */
    inline fun <reified T> getResultFlow(resultKey: String = T::class.toString()) =
        channelMap[resultKey]?.receiveAsFlow()

    /**
     * Sends a result into the channel associated with the given resultKey.
     */
    inline fun <reified T> sendResult(resultKey: String = T::class.toString(), result: T) {
        channelFor(resultKey).trySend(result)
    }

    /**
     * Removes all results associated with the given key from the store.
     */
    inline fun <reified T> removeResult(resultKey: String = T::class.toString()) {
        removeChannel(resultKey)
    }

    companion object {

        private val instance = AtomicReference<ResultEventBus?>(null)

        /**
         * The one bus, created on first use.
         *
         * CAS rather than a null check and assign, for the same reason as [channelFor]: two
         * callers arriving at once could each build a bus, and one of them would then be
         * sending results into an object nobody else can hear. A singleton that is only
         * usually a singleton loses exactly the cross-pane delivery it exists to provide.
         */
        fun getInstance(): ResultEventBus {
            instance.load()?.let { return it }

            val created = ResultEventBus()
            return if (instance.compareAndSet(null, created)) created else instance.load()!!
        }
    }
}
