package xyz.ksharma.krail.feature.track.network.crowding

import com.google.transit.realtime.FeedMessage
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import xyz.ksharma.krail.core.network.error.NetworkCaller
import xyz.ksharma.krail.core.network.error.NetworkError
import xyz.ksharma.krail.core.network.error.networkErrorOrNull
import xyz.ksharma.krail.core.testing.fakes.FakeAnalytics
import xyz.ksharma.krail.core.testing.fakes.FakeClock
import xyz.ksharma.krail.core.testing.fakes.FakeConnectivityObserver
import xyz.ksharma.krail.core.testing.fakes.FakeGtfsRealtimeService
import xyz.ksharma.krail.feature.crowding.CarriageIndex
import xyz.ksharma.krail.feature.track.network.GtfsFeedType
import xyz.ksharma.krail.feature.track.network.GtfsRealtimeResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class RealCarriageForecastRepositoryTest {

    private val service = FakeGtfsRealtimeService(
        result = GtfsRealtimeResult.Fresh(
            FeedMessage.ADAPTER.decode(SydneyTrainsTripUpdateFixture.bytes()),
            lastModified = null,
        ),
    )
    private val clock = FakeClock()
    private val connectivity = FakeConnectivityObserver()
    private var enabled = true

    private fun repository(dispatcher: TestDispatcher) = RealCarriageForecastRepository(
        service = service,
        networkCaller = NetworkCaller(connectivity, dispatcher, FakeAnalytics()),
        isEnabled = { enabled },
        clock = clock,
    )

    @Test
    fun `loads the Sydney Trains trip updates feed`() = runTest {
        val index = repository(StandardTestDispatcher(testScheduler)).load().getOrThrow()
        assertEquals(1, index.tripCount)
        assertEquals(listOf("sydneytrains" to GtfsFeedType.TRIP_UPDATES), service.requests)
    }

    @Test
    fun `second caller within a minute shares the first download`() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))
        repository.load()
        clock.advanceBy(30.seconds)
        repository.load()
        assertEquals(1, service.requests.size)
    }

    @Test
    fun `a stale copy is fetched again`() = runTest {
        val repository = repository(StandardTestDispatcher(testScheduler))
        repository.load()
        clock.advanceBy(61.seconds)
        repository.load()
        assertEquals(2, service.requests.size)
    }

    @Test
    fun `kill switch off makes no request and returns an empty index`() = runTest {
        enabled = false
        val result = repository(StandardTestDispatcher(testScheduler)).load()
        assertEquals(CarriageIndex.EMPTY, result.getOrThrow())
        assertTrue(service.requests.isEmpty())
    }

    @Test
    fun `a failed download is a typed network error and is not cached`() = runTest {
        service.result = GtfsRealtimeResult.Error(RuntimeException("HTTP 503 for sydneytrains"))
        val repository = repository(StandardTestDispatcher(testScheduler))
        val failure = repository.load()
        assertIs<NetworkError>(failure.networkErrorOrNull())
        repository.load()
        assertEquals(2, service.requests.size)
    }
}
