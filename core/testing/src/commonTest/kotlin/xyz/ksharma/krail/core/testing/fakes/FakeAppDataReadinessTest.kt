package xyz.ksharma.krail.core.testing.fakes

import kotlinx.coroutines.async
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.io.gtfs.readiness.StopsReadiness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeAppDataReadinessTest {

    @Test
    fun `awaitReady returns at once on Ready and on Failed`() = krailRunTest {
        val fake = FakeAppDataReadiness(StopsReadiness.Ready(stopCount = 7))
        assertEquals(StopsReadiness.Ready(7), fake.awaitReady(timeoutMs = 1_000))

        fake.emit(StopsReadiness.Failed(attempt = 1, reason = "x"))
        assertEquals(StopsReadiness.Failed(1, "x"), fake.awaitReady(timeoutMs = 1_000))
    }

    @Test
    fun `awaitReady waits through Loading until the test emits`() = krailRunTest {
        val fake = FakeAppDataReadiness(StopsReadiness.Loading)
        val result = async { fake.awaitReady(timeoutMs = 10_000) }
        runCurrent()
        assertTrue(result.isActive)

        fake.emit(StopsReadiness.Ready(stopCount = 3))
        runCurrent()

        assertEquals(StopsReadiness.Ready(3), result.await())
    }

    @Test
    fun `awaitReady gives the current state at its timeout`() = krailRunTest {
        val fake = FakeAppDataReadiness(StopsReadiness.Loading)
        val result = async { fake.awaitReady(timeoutMs = 500) }

        pumpOnce(500)

        assertEquals(StopsReadiness.Loading, result.await())
    }

    @Test
    fun `retry is counted and does not change the state`() {
        val fake = FakeAppDataReadiness(StopsReadiness.Unknown)

        fake.retry()
        fake.retry()

        assertEquals(2, fake.retryCallCount)
        assertEquals(StopsReadiness.Unknown, fake.state.value)
    }
}
