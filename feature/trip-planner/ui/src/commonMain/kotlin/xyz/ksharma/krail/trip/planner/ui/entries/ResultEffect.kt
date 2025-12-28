/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.ksharma.krail.trip.planner.ui.entries

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

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
 *
 * ## How It Works
 * 1. Creates a channel for the result type if it doesn't exist
 * 2. Collects from the channel's flow
 * 3. Invokes the callback when a result is received
 * 4. Automatically cleaned up when the composable leaves composition
 *
 * @param resultEventBus the ResultEventBus to retrieve the result from. Defaults to
 * the singleton instance from [LocalResultEventBus.current]
 * @param resultKey the key that should be associated with this effect. Defaults to
 * the class name of type T
 * @param onResult the callback to invoke when a result is received
 *
 * @see ResultEventBus for the underlying event bus implementation
 * @see LocalResultEventBus for accessing the singleton instance
 */
@Composable
inline fun <reified T> ResultEffect(
    resultEventBus: ResultEventBus = LocalResultEventBus.current,
    resultKey: String = T::class.toString(),
    crossinline onResult: suspend (T) -> Unit
) {
    LaunchedEffect(resultKey) {
        // Ensure the channel exists by calling getResultFlow (which creates it if needed)
        val flow = resultEventBus.getResultFlow<T>(resultKey)

        if (flow == null) {
            // Create the channel if it doesn't exist
            if (!resultEventBus.channelMap.contains(resultKey)) {
                resultEventBus.channelMap[resultKey] = kotlinx.coroutines.channels.Channel(
                    capacity = kotlinx.coroutines.channels.Channel.BUFFERED,
                    onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.SUSPEND
                )
            }
            // Get the flow again
            val newFlow = resultEventBus.getResultFlow<T>(resultKey)
            newFlow?.collect { result ->
                onResult.invoke(result as T)
            }
        } else {
            flow.collect { result ->
                onResult.invoke(result as T)
            }
        }
    }
}
