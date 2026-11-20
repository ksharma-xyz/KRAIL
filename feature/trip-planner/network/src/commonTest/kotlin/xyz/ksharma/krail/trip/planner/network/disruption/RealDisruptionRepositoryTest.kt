package xyz.ksharma.krail.trip.planner.network.disruption

import kotlinx.coroutines.test.runTest
import xyz.ksharma.krail.core.testing.fakes.FakeClock
import xyz.ksharma.krail.core.testing.fakes.FakeGtfsRealtimeService
import xyz.ksharma.krail.feature.track.network.GtfsFeedType
import xyz.ksharma.krail.feature.track.network.GtfsRealtimeResult
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionFeed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class RealDisruptionRepositoryTest {

    private val clock = FakeClock()
    private val service = FakeGtfsRealtimeService(
        GtfsRealtimeResult.Fresh(
            AlertFixtures.feed(AlertFixtures.busesReplaceMetro, AlertFixtures.stationUpdate),
            lastModified = null,
        ),
    )
    private val repository = RealDisruptionRepository(service = service, clock = clock)

    @Test
    fun `Given a first load Then it asks the alerts feed for metro and classifies it`() = runTest {
        val result = repository.load(DisruptionFeed.METRO)

        assertEquals(listOf("metro" to GtfsFeedType.ALERTS), service.requests)
        assertEquals(1, result.getOrThrow().size)
    }

    @Test
    fun `Given a second load inside the ttl Then no second request is made`() = runTest {
        repository.load(DisruptionFeed.METRO)
        clock.advanceBy(2.minutes)
        repository.load(DisruptionFeed.METRO)

        assertEquals(1, service.requests.size)
    }

    @Test
    fun `Given a load after the ttl Then it asks again`() = runTest {
        repository.load(DisruptionFeed.METRO)
        clock.advanceBy(3.minutes + 1.seconds)
        repository.load(DisruptionFeed.METRO)

        assertEquals(2, service.requests.size)
    }

    @Test
    fun `Given a failure after a good load Then the last good answer is kept`() = runTest {
        repository.load(DisruptionFeed.METRO)
        clock.advanceBy(4.minutes)
        service.result = GtfsRealtimeResult.Error(RuntimeException("502"))

        val result = repository.load(DisruptionFeed.METRO)

        assertEquals(1, result.getOrThrow().size)
    }

    @Test
    fun `Given a failure with nothing cached Then it returns the failure`() = runTest {
        service.result = GtfsRealtimeResult.Error(RuntimeException("502"))

        assertTrue(repository.load(DisruptionFeed.METRO).isFailure)
    }
}
