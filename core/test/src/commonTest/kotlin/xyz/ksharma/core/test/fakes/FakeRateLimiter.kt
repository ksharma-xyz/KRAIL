package xyz.ksharma.core.test.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import xyz.ksharma.krail.trip.planner.network.api.ratelimit.RateLimiter

class FakeRateLimiter : RateLimiter {

    var triggerCount: Int = 0
        private set

    override fun <T> rateLimitFlow(block: suspend () -> T): Flow<T> {
        return flow { emit(block()) }
    }

    override fun triggerEvent(): Boolean {
        triggerCount++
        return true
    }

    fun reset() {
        triggerCount = 0
    }
}
