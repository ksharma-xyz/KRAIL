package xyz.ksharma.krail.io.gtfs.readiness

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import xyz.ksharma.krail.core.testing.coroutines.KrailTestScope
import xyz.ksharma.krail.core.testing.coroutines.krailRunTest
import xyz.ksharma.krail.core.testing.fakes.FakeSandook
import xyz.ksharma.krail.core.testing.fakes.FakeSandookPreferences
import xyz.ksharma.krail.core.testing.fakes.FakeStopsManager
import xyz.ksharma.krail.io.gtfs.nswstops.StopsManager.Companion.MINIMUM_REQUIRED_NSW_STOPS
import xyz.ksharma.krail.io.gtfs.readiness.RealAppDataReadiness.Companion.MAX_AUTOMATIC_RETRIES
import xyz.ksharma.krail.io.gtfs.readiness.RealAppDataReadiness.Companion.RETRY_BACKOFF_MS
import xyz.ksharma.krail.io.gtfs.seedCurrentStops
import xyz.ksharma.krail.io.gtfs.seedStopsWithoutCanary
import xyz.ksharma.krail.sandook.SandookPreferences.Companion.KEY_NSW_STOPS_VERSION
import xyz.ksharma.krail.sandook.SandookPreferences.Companion.NSW_STOPS_VERSION
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RealAppDataReadinessTest {

    private val stops = FakeStopsManager()
    private val busRoutes = FakeStopsManager()
    private val sandook = FakeSandook()
    private val preferences = FakeSandookPreferences()
    private val failure = Result.failure<Unit>(IllegalStateException("disk full"))

    private fun KrailTestScope.readiness() = RealAppDataReadiness(
        stopsManager = stops,
        busRoutesManager = busRoutes,
        sandook = sandook,
        preferences = preferences,
        ioDispatcher = ioDispatcher,
        scope = CoroutineScope(SupervisorJob() + ioDispatcher),
    )

    private fun currentData() {
        preferences.setLong(KEY_NSW_STOPS_VERSION, NSW_STOPS_VERSION)
        sandook.seedCurrentStops()
    }

    /** Every automatic retry's backoff, so the whole launch run has finished. */
    private fun KrailTestScope.finishLaunchRun() {
        pumpOnce((1..MAX_AUTOMATIC_RETRIES).sumOf { RETRY_BACKOFF_MS * it })
    }

    @Test
    fun `the launch run ends Ready when the database agrees`() = krailRunTest {
        currentData()
        val readiness = readiness()

        readiness.retry()
        runCurrent()

        assertEquals(StopsReadiness.Ready(MINIMUM_REQUIRED_NSW_STOPS), readiness.state.value)
        assertEquals(1, stops.insertStopsCallCount)
        assertEquals(1, busRoutes.insertStopsCallCount)
    }

    @Test
    fun `a manager that reports success is not enough when the table is empty`() = krailRunTest {
        // Both managers say success. The preference flag even matches. The rows are not there.
        preferences.setLong(KEY_NSW_STOPS_VERSION, NSW_STOPS_VERSION)
        val readiness = readiness()

        readiness.retry()
        runCurrent()
        finishLaunchRun()

        assertEquals(StopsReadiness.Failed(attempt = 4, reason = REASON_COUNT_LOW), readiness.state.value)
    }

    @Test
    fun `rows and version are fine but the canary stop is missing gives Failed`() = krailRunTest {
        preferences.setLong(KEY_NSW_STOPS_VERSION, NSW_STOPS_VERSION)
        sandook.seedStopsWithoutCanary()
        val readiness = readiness()

        readiness.retry()
        runCurrent()
        finishLaunchRun()

        assertEquals(StopsReadiness.Failed(attempt = 4, reason = REASON_CANARY_MISSING), readiness.state.value)
    }

    @Test
    fun `rows and canary are fine but the stored version is older gives Failed`() = krailRunTest {
        preferences.setLong(KEY_NSW_STOPS_VERSION, NSW_STOPS_VERSION - 1)
        sandook.seedCurrentStops()
        val readiness = readiness()

        readiness.retry()
        runCurrent()
        finishLaunchRun()

        assertEquals(StopsReadiness.Failed(attempt = 4, reason = REASON_VERSION_OLD), readiness.state.value)
    }

    @Test
    fun `a failed insert retries three times with a backoff then stops`() = krailRunTest {
        stops.result = failure
        val readiness = readiness()

        readiness.retry()
        runCurrent()
        assertEquals(1, stops.insertStopsCallCount, "the first retry must wait for its backoff")
        assertEquals(StopsReadiness.Loading, readiness.state.value, "retries are not shown as Failed")

        finishLaunchRun()
        assertEquals(1 + MAX_AUTOMATIC_RETRIES, stops.insertStopsCallCount)
        assertEquals(StopsReadiness.Failed(attempt = 4, reason = "disk full"), readiness.state.value)

        pumpOnce(RETRY_BACKOFF_MS * 100)
        assertEquals(1 + MAX_AUTOMATIC_RETRIES, stops.insertStopsCallCount, "no further automatic retry")
    }

    @Test
    fun `retry after Failed runs the insert once more and not a whole cycle`() = krailRunTest {
        stops.result = failure
        val readiness = readiness()
        readiness.retry()
        runCurrent()
        finishLaunchRun()

        readiness.retry()
        runCurrent()
        pumpOnce(RETRY_BACKOFF_MS * 100)

        assertEquals(5, stops.insertStopsCallCount)
        assertEquals(StopsReadiness.Failed(attempt = 5, reason = "disk full"), readiness.state.value)
    }

    @Test
    fun `retry after Failed recovers when the insert now works`() = krailRunTest {
        currentData()
        stops.enqueue(List(1 + MAX_AUTOMATIC_RETRIES) { failure })
        val readiness = readiness()
        readiness.retry()
        runCurrent()
        finishLaunchRun()
        assertIs<StopsReadiness.Failed>(readiness.state.value)

        readiness.retry()
        runCurrent()

        assertEquals(StopsReadiness.Ready(MINIMUM_REQUIRED_NSW_STOPS), readiness.state.value)
    }

    @Test
    fun `Ready is published before the bus routes insert finishes`() = krailRunTest {
        currentData()
        val busGate = CompletableDeferred<Unit>()
        busRoutes.gate = busGate
        val readiness = readiness()

        readiness.retry()
        runCurrent()

        assertEquals(StopsReadiness.Ready(MINIMUM_REQUIRED_NSW_STOPS), readiness.state.value)
        assertEquals(1, busRoutes.insertStopsCallCount, "the bus routes insert has started and is held")
        assertEquals(StopsReadiness.Ready(MINIMUM_REQUIRED_NSW_STOPS), readiness.awaitReady(timeoutMs = AWAIT_MS))

        busGate.complete(Unit)
        runCurrent()
        assertEquals(StopsReadiness.Ready(MINIMUM_REQUIRED_NSW_STOPS), readiness.state.value)
    }

    @Test
    fun `a bus routes failure after Ready leaves the state Ready and is not retried`() = krailRunTest {
        currentData()
        busRoutes.result = failure
        val readiness = readiness()

        readiness.retry()
        runCurrent()
        pumpOnce(RETRY_BACKOFF_MS * 100)

        assertEquals(StopsReadiness.Ready(MINIMUM_REQUIRED_NSW_STOPS), readiness.state.value)
        assertEquals(1, busRoutes.insertStopsCallCount)
    }

    @Test
    fun `a stops failure skips the bus routes insert for that attempt`() = krailRunTest {
        stops.result = failure
        val readiness = readiness()

        readiness.retry()
        runCurrent()
        finishLaunchRun()

        assertIs<StopsReadiness.Failed>(readiness.state.value)
        assertEquals(0, busRoutes.insertStopsCallCount)
    }

    @Test
    fun `retry from Ready leaves the state Ready and starts no run`() = krailRunTest {
        currentData()
        val readiness = readiness()
        readiness.retry()
        runCurrent()

        readiness.retry()
        assertEquals(
            StopsReadiness.Ready(MINIMUM_REQUIRED_NSW_STOPS),
            readiness.state.value,
            "not even briefly Loading",
        )
        runCurrent()

        assertEquals(1, stops.insertStopsCallCount)
        assertEquals(1, busRoutes.insertStopsCallCount)
    }

    @Test
    fun `a manager that throws instead of returning a Result ends in Failed, not Loading`() = krailRunTest {
        stops.throwOnInsert = IllegalStateException("broke its contract")
        val readiness = readiness()

        readiness.retry()
        runCurrent()
        finishLaunchRun()

        assertEquals(
            StopsReadiness.Failed(attempt = 4, reason = "broke its contract"),
            readiness.state.value,
        )

        currentData()
        stops.throwOnInsert = null
        readiness.retry()
        runCurrent()
        assertEquals(StopsReadiness.Ready(MINIMUM_REQUIRED_NSW_STOPS), readiness.state.value)
    }

    @Test
    fun `a cancelled run ends in Failed, not Loading, and a later retry works`() = krailRunTest {
        val gate = CompletableDeferred<Unit>()
        stops.gate = gate
        val readiness = readiness()
        readiness.retry()
        runCurrent()
        assertEquals(StopsReadiness.Loading, readiness.state.value)

        gate.completeExceptionally(CancellationException("scope went away"))
        runCurrent()

        assertEquals(StopsReadiness.Failed(attempt = 1, reason = REASON_RUN_INTERRUPTED), readiness.state.value)

        stops.gate = null
        currentData()
        readiness.retry()
        runCurrent()
        assertEquals(StopsReadiness.Ready(MINIMUM_REQUIRED_NSW_STOPS), readiness.state.value)
    }

    @Test
    fun `retry while loading does nothing`() = krailRunTest {
        currentData()
        val gate = CompletableDeferred<Unit>()
        stops.gate = gate
        val readiness = readiness()

        readiness.retry()
        runCurrent()
        readiness.retry()
        readiness.retry()
        runCurrent()

        assertEquals(StopsReadiness.Loading, readiness.state.value)
        assertEquals(1, stops.insertStopsCallCount)

        gate.complete(Unit)
        runCurrent()
        assertEquals(StopsReadiness.Ready(MINIMUM_REQUIRED_NSW_STOPS), readiness.state.value)
        assertEquals(1, stops.insertStopsCallCount)
    }

    @Test
    fun `an unreadable database is reported as Failed and does not leave the state Loading`() = krailRunTest {
        currentData()
        sandook.failStopsCount = true
        val readiness = readiness()

        readiness.retry()
        runCurrent()
        finishLaunchRun()

        assertEquals(
            StopsReadiness.Failed(attempt = 4, reason = "FakeSandook: stopsCount failed"),
            readiness.state.value,
        )
    }

    @Test
    fun `a failure with no message still gives a reason`() = krailRunTest {
        stops.result = Result.failure(IllegalStateException(null as String?))
        val readiness = readiness()

        readiness.retry()
        runCurrent()
        finishLaunchRun()

        assertEquals(StopsReadiness.Failed(attempt = 4, reason = "IllegalStateException"), readiness.state.value)
    }

    @Test
    fun `awaitReady does not start another run once one has finished`() = krailRunTest {
        currentData()
        val readiness = readiness()
        readiness.retry()
        runCurrent()

        val result = readiness.awaitReady(timeoutMs = AWAIT_MS)

        assertEquals(StopsReadiness.Ready(MINIMUM_REQUIRED_NSW_STOPS), result)
        assertEquals(1, stops.insertStopsCallCount)
    }

    @Test
    fun `awaitReady starts the first run and returns Ready`() = krailRunTest {
        currentData()
        val gate = CompletableDeferred<Unit>()
        stops.gate = gate
        val readiness = readiness()

        val result = async { readiness.awaitReady(timeoutMs = AWAIT_MS) }
        runCurrent()
        assertEquals(1, stops.insertStopsCallCount, "nobody called retry, awaitReady started the run")
        assertTrue(result.isActive, "still waiting while the insert is held")

        gate.complete(Unit)
        runCurrent()

        assertEquals(StopsReadiness.Ready(MINIMUM_REQUIRED_NSW_STOPS), result.await())
    }

    @Test
    fun `awaitReady returns Failed as soon as the retries are used up, before its timeout`() = krailRunTest {
        stops.result = failure
        val readiness = readiness()

        val result = async { readiness.awaitReady(timeoutMs = AWAIT_MS) }
        runCurrent()
        finishLaunchRun()

        assertTrue(result.isCompleted, "returned on Failed, not by waiting out the timeout")
        assertEquals(StopsReadiness.Failed(attempt = 4, reason = "disk full"), result.await())
        assertTrue(scheduler.currentTime < AWAIT_MS)
    }

    @Test
    fun `awaitReady returns the current state at its timeout and does not throw`() = krailRunTest {
        val gate = CompletableDeferred<Unit>()
        stops.gate = gate
        val readiness = readiness()

        val result = async { readiness.awaitReady(timeoutMs = SHORT_AWAIT_MS) }
        runCurrent()
        pumpOnce(SHORT_AWAIT_MS)

        assertTrue(result.isCompleted, "must not wait for an insert that never finishes")
        assertEquals(StopsReadiness.Loading, result.await())

        gate.complete(Unit)
        runCurrent()
    }

    private companion object {
        const val AWAIT_MS = 60_000L
        const val SHORT_AWAIT_MS = 1_000L
    }
}
