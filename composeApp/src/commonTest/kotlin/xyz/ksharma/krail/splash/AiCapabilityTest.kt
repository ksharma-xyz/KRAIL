package xyz.ksharma.krail.splash

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import xyz.ksharma.krail.core.aitext.AiAvailability
import xyz.ksharma.krail.core.aitext.AiUnavailableReasons
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `aiCapability` runs on every launch, so the property that matters most is that every path
 * produces a value and none of them throws. The mapping is secondary to that.
 */
class AiCapabilityTest {

    @Test
    fun `an available model reports available`() = runTest {
        assertEquals(AiCapability.AVAILABLE, capabilityOf { AiAvailability.Available })
    }

    @Test
    fun `each known reason is reported as itself`() = runTest {
        listOf(
            AiUnavailableReasons.MODEL_DOWNLOADING,
            AiUnavailableReasons.DEVICE_UNSUPPORTED,
            AiUnavailableReasons.NEEDS_DEVICE_SETTING,
            AiUnavailableReasons.CHECK_FAILED,
        ).forEach { reason ->
            assertEquals(reason, capabilityOf { AiAvailability.Unavailable(reason) })
        }
    }

    @Test
    fun `an exception message on a failed check never reaches the value`() = runTest {
        // Android reports "check_failed: <throwable.message>". SDK text is not a dimension
        // value and can carry anything.
        val value = capabilityOf {
            AiAvailability.Unavailable("${AiUnavailableReasons.CHECK_FAILED}: binder died at 0x7f")
        }

        assertEquals(AiUnavailableReasons.CHECK_FAILED, value)
    }

    @Test
    fun `a reason this code does not know is sent as unknown`() = runTest {
        assertEquals(AiCapability.UNKNOWN, capabilityOf { AiAvailability.Unavailable("sdk_new_state") })
    }

    @Test
    fun `a check that throws reports check_failed instead of crashing`() = runTest {
        val value = capabilityOf { error("platform exploded") }

        assertEquals(AiUnavailableReasons.CHECK_FAILED, value)
    }

    @Test
    fun `a check that throws an Error still reports check_failed`() = runTest {
        // A missing class on an old OS surfaces as an Error, not an Exception.
        val value = capabilityOf { throw NotImplementedError("no GenAI on this device") }

        assertEquals(AiUnavailableReasons.CHECK_FAILED, value)
    }

    @Test
    fun `a check that never answers reports timeout`() = runTest {
        val value = AiCapability.of(
            peek = { awaitCancellation() },
            dispatcher = StandardTestDispatcher(testScheduler),
            timeoutMs = 50,
        )

        assertEquals(AiCapability.TIMEOUT, value)
    }

    private suspend fun TestScope.capabilityOf(
        peek: suspend () -> AiAvailability,
    ): String = AiCapability.of(peek, StandardTestDispatcher(testScheduler))
}
