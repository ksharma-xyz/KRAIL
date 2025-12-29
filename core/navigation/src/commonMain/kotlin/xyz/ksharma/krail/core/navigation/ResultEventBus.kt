package xyz.ksharma.krail.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.receiveAsFlow

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
class ResultEventBus private constructor() {
    /**
     * Map from the result key to a channel of results.
     */
    val channelMap: MutableMap<String, Channel<Any?>> = mutableMapOf()

    /**
     * Provides a flow for the given resultKey.
     */
    inline fun <reified T> getResultFlow(resultKey: String = T::class.toString()) =
        channelMap[resultKey]?.receiveAsFlow()

    /**
     * Sends a result into the channel associated with the given resultKey.
     *
     * Creates the channel if it doesn't exist. Uses BUFFERED capacity to avoid
     * blocking senders if receiver is temporarily unavailable.
     */
    inline fun <reified T> sendResult(resultKey: String = T::class.toString(), result: T) {
        if (!channelMap.contains(resultKey)) {
            channelMap[resultKey] = Channel(capacity = BUFFERED, onBufferOverflow = BufferOverflow.SUSPEND)
        }

        channelMap[resultKey]?.trySend(result)
    }

    /**
     * Removes all results associated with the given key from the store.
     */
    inline fun <reified T> removeResult(resultKey: String = T::class.toString()) {
        channelMap.remove(resultKey)
    }

    companion object {
        private var INSTANCE: ResultEventBus? = null

        fun getInstance(): ResultEventBus {
            if (INSTANCE == null) {
                INSTANCE = ResultEventBus()
            }
            return INSTANCE!!
        }
    }
}
