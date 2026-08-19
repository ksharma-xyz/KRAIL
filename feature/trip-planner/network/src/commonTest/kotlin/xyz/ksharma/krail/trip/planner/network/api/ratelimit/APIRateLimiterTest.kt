package xyz.ksharma.krail.trip.planner.network.api.ratelimit

import app.cash.turbine.test
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class APIRateLimiterTest {

    private val rateLimiter = NetworkRateLimiter()

    @Test
    fun `Given rate limiter When triggered once Then should emit only once within the time interval`() = runTest {
        // Given
        val apiCall = suspend { "API Response" }

        // When
        rateLimiter.rateLimitFlow(apiCall).test {
            rateLimiter.triggerEvent()
            assertEquals("API Response", awaitItem())
            rateLimiter.triggerEvent()
            expectNoEvents()
            delay(1.seconds)
            rateLimiter.triggerEvent()
            assertEquals("API Response", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given rate limiter When triggered rapidly Then should handle rapid triggers correctly`() = runTest {
        // Given
        val apiCall = suspend { "API Response" }

        // When
        rateLimiter.rateLimitFlow(apiCall).test {
            repeat(10) {
                rateLimiter.triggerEvent()
                delay(10)
            }

            // Then
            assertEquals("API Response", awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given rate limiter When no triggers Then should handle no triggers`() = runTest {
        // Given
        val apiCall = suspend { "API Response" }

        // When
        rateLimiter.rateLimitFlow(apiCall).test {
            // Then
            expectNoEvents()
            delay(2.seconds)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
