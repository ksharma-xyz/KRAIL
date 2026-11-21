package xyz.ksharma.krail.trip.planner.network.disruption

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import xyz.ksharma.krail.core.testing.fakes.FakeClock
import xyz.ksharma.krail.core.testing.fakes.FakeGtfsRealtimeService
import xyz.ksharma.krail.feature.track.network.GtfsFeedType
import xyz.ksharma.krail.feature.track.network.GtfsRealtimeResult
import xyz.ksharma.krail.feature.track.network.GtfsRealtimeService
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionFeed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
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

    @Test
    fun `Given the first request times out with nothing cached Then it retries once and returns the alerts`() = runTest {
        val scripted = ScriptedService(HANGS, goodFeed())

        val result = RealDisruptionRepository(service = scripted, clock = clock).load(DisruptionFeed.METRO)

        assertEquals(2, scripted.calls)
        assertEquals(1, result.getOrThrow().size)
    }

    @Test
    fun `Given the first request errors with nothing cached Then it retries once and returns the alerts`() = runTest {
        val scripted = ScriptedService(GtfsRealtimeResult.Error(RuntimeException("reset")), goodFeed())

        val result = RealDisruptionRepository(service = scripted, clock = clock).load(DisruptionFeed.METRO)

        assertEquals(2, scripted.calls)
        assertEquals(1, result.getOrThrow().size)
    }

    @Test
    fun `Given both requests time out Then it fails after exactly two requests`() = runTest {
        val scripted = ScriptedService(HANGS, HANGS)

        val result = RealDisruptionRepository(service = scripted, clock = clock).load(DisruptionFeed.METRO)

        assertEquals(2, scripted.calls)
        assertFailsWith<IllegalStateException> { result.getOrThrow() }
    }

    @Test
    fun `Given both requests error Then it fails after exactly two requests`() = runTest {
        val scripted = ScriptedService(
            GtfsRealtimeResult.Error(RuntimeException("502")),
            GtfsRealtimeResult.Error(RuntimeException("503")),
        )

        val result = RealDisruptionRepository(service = scripted, clock = clock).load(DisruptionFeed.METRO)

        assertEquals(2, scripted.calls)
        assertEquals("503", assertFailsWith<RuntimeException> { result.getOrThrow() }.message)
    }

    @Test
    fun `Given a good first request Then it is not retried`() = runTest {
        val scripted = ScriptedService(goodFeed())

        RealDisruptionRepository(service = scripted, clock = clock).load(DisruptionFeed.METRO)

        assertEquals(1, scripted.calls)
    }

    @Test
    fun `Given an answer is cached and a refresh times out Then it is not retried and the cached answer is kept`() = runTest {
        val scripted = ScriptedService(goodFeed(), HANGS)
        val repository = RealDisruptionRepository(service = scripted, clock = clock)
        repository.load(DisruptionFeed.METRO)
        clock.advanceBy(4.minutes)

        val result = repository.load(DisruptionFeed.METRO)

        assertEquals(2, scripted.calls)
        assertEquals(1, result.getOrThrow().size)
    }

    /** Plays [outcomes] in order, then repeats the last. A null outcome is a request that never answers. */
    private class ScriptedService(private vararg val outcomes: GtfsRealtimeResult?) : GtfsRealtimeService {
        var calls = 0
            private set

        override suspend fun fetchFeed(
            feedName: String,
            feedType: GtfsFeedType,
            sinceLastModified: String?,
        ): GtfsRealtimeResult {
            val outcome = outcomes[minOf(calls, outcomes.lastIndex)]
            calls++
            // A request that never answers: the repository's budget cancels it on virtual time.
            return outcome ?: delay(1.hours).let { error("budget did not cancel the request") }
        }
    }

    private fun goodFeed() = GtfsRealtimeResult.Fresh(
        AlertFixtures.feed(AlertFixtures.busesReplaceMetro, AlertFixtures.stationUpdate),
        lastModified = null,
    )

    private companion object {
        val HANGS: GtfsRealtimeResult? = null
    }
}
