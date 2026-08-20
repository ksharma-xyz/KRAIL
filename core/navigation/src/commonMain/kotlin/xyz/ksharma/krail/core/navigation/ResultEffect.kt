package xyz.ksharma.krail.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * An Effect to receive results from another screen via [ResultEventBus].
 *
 * This composable sets up a listener that receives results sent via the event bus,
 * enabling communication between screens that don't have a direct parent-child relationship.
 *
 * ## Critical for Two-Pane Layouts
 * This effect is essential for adaptive layouts where screens exist in separate composition
 * scopes (e.g., list and detail panes). It works in conjunction with the singleton
 * [ResultEventBus] to ensure results can be delivered across composition boundaries.
 *
 * ## Usage Example
 * ```kotlin
 * // In SavedTripsScreen (receiver):
 * ResultEffect<StopSelectedResult> { result ->
 *     when (result.fieldType) {
 *         FROM -> viewModel.onEvent(FromStopChanged(result.stopId))
 *         TO -> viewModel.onEvent(ToStopChanged(result.stopId))
 *     }
 * }
 *
 * // In SearchStopScreen (sender):
 * val resultEventBus = LocalResultEventBus.current
 * resultEventBus.sendResult(result = StopSelectedResult(...))
 * navigator.goBack()
 * ```
 * reference - https://github.com/android/nav3-recipes/blob/main/app/src/main/java/com/example/nav3recipes/results/event/ResultEffect.kt
 *
 * ## How It Works
 * 1. Creates a channel for the result type if it doesn't exist
 * 2. Collects from the channel's flow
 * 3. Invokes the callback when a result is received
 * 4. Automatically cleaned up when the composable leaves composition
 *
 * @param resultEventBus the ResultEventBus to retrieve the result from. Defaults to
 * the singleton instance from [LocalResultEventBusObj.current]
 * @param resultKey the key that should be associated with this effect. Defaults to
 * the class name of type T
 * @param onResult the callback to invoke when a result is received
 *
 * @see ResultEventBus for the underlying event bus implementation
 * @see LocalResultEventBusObj for accessing the singleton instance
 */
@Composable
inline fun <reified T> ResultEffect(
    resultEventBus: ResultEventBus = LocalResultEventBusObj.current,
    resultKey: String = T::class.toString(),
    crossinline onResult: suspend (T) -> Unit,
) {
    LaunchedEffect(resultKey) {
        // channelFor creates the channel if this is the first use of the key, atomically.
        // This used to hand-roll the same check-then-act the bus itself used to do, which meant
        // a receiver arriving at the same moment as a sender could install a second channel
        // over the one the result was already sitting in. One accessor, one create.
        resultEventBus.channelFor(resultKey).receiveAsFlow().collect { result ->
            onResult.invoke(result as T)
        }
    }
}
