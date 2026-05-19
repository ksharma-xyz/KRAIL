package xyz.ksharma.krail.core.testing.coroutines

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class KrailTestKitTest {

    @Test
    fun `ioDispatcher and mainDispatcher share one scheduler`() = krailRunTest {
        // The whole point: production code that injects ioDispatcher AND code that runs
        // on Dispatchers.Main must observe the same virtual clock. Different schedulers
        // would trip kotlinx-coroutines' "Detected use of different schedulers" check.
        assertSame(scheduler, (ioDispatcher as kotlinx.coroutines.test.TestDispatcher).scheduler)
        assertSame(scheduler, (mainDispatcher as kotlinx.coroutines.test.TestDispatcher).scheduler)
    }

    @Test
    fun `pumpOnce with Duration advances exactly the requested virtual time`() = krailRunTest {
        var fired = false
        launch(ioDispatcher) {
            delay(500.milliseconds)
            fired = true
        }
        pumpOnce(499.milliseconds)
        assertEquals(false, fired)
        pumpOnce(1.milliseconds)
        assertTrue(fired)
    }

    @Test
    fun `pumpOnce with millis overload matches Duration overload`() = krailRunTest {
        var count = 0
        launch(ioDispatcher) {
            repeat(3) {
                delay(100)
                count++
            }
        }
        pumpOnce(100L); assertEquals(1, count)
        pumpOnce(100L); assertEquals(2, count)
        pumpOnce(100L); assertEquals(3, count)
    }
}
