package xyz.ksharma.krail.trip.planner.network.api.ratelimit

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Implements rate limiting for API calls using Kotlin Flows.
 * This class ensures that API calls are made at a controlled rate to avoid hitting rate limits.
 *
 * ## One instance per consumer
 *
 * [triggerEvent] is a broadcast to every flow built from this instance. Two consumers sharing
 * one limiter therefore both fetch whenever either of them refreshes. The DI binding is a
 * `factory` for exactly that reason — see `tripPlannerNetworkModule`.
 *
 * ## Trigger semantics: at most one pending trigger, delivered exactly once
 *
 * A trigger fired while nothing is collecting is **held** and delivered to the next collector.
 * The screen relies on this: `onLoadTimeTable` triggers before `fetchTrip`'s collector has
 * attached, and losing that trigger would mean the timetable never loads.
 *
 * `replay = 1` on the trigger channel is what holds that trigger. Once delivered, the pending
 * trigger is **cleared** by [rateLimitFlow]. Without the clear, the same trigger re-ran for
 * every later collection — so returning to the screen, or any code path that rebuilds the
 * flow, fired a fetch nobody asked for on top of the one it was starting.
 */
internal class NetworkRateLimiter : RateLimiter {

    private val triggerFlow = MutableSharedFlow<Unit>(replay = 1)
    private val isFirstTime = MutableStateFlow(false)

    /**
     * Returns a Flow that emits the result of the provided API call, ensuring that the calls are rate-limited.
     *
     * @param block A suspend function representing the API call to be rate-limited.
     * @return A Flow that emits the result of the API call.
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    override fun <T> rateLimitFlow(block: suspend () -> T): Flow<T> {
        return triggerFlow
            .debounce {
                // First time the block should be executed immediately and subsequent must be rate limited.
                val interval = if (isFirstTime.value) rateLimitInterval else 0.milliseconds
                // log(("state: ${isFirstTime.value} and interval: $interval")
                interval
            }
            .flatMapLatest {
                // log(("flatmapLatest: Triggered")
                isFirstTime.update { true } // Mark the first trigger
                // The trigger has been served — drop it so a later collection does not
                // replay it as a second, unrequested fetch. Only affects collectors that
                // attach after this point; this one has already received it.
                triggerFlow.resetReplayCache()
                flow {
                    //   log(("Inside flow -emitting block")
                    emit(block())
                }
            }
    }

    /**
     * Triggers the API call by emitting a new item to the triggerFlow.
     * This method should be called whenever a button is tapped and API call is to be made.
     *
     * @return true if the event was triggered successfully, false otherwise.
     */
    override fun triggerEvent() = triggerFlow.tryEmit(Unit)

    companion object {
        private val rateLimitInterval = 1.seconds
    }
}
