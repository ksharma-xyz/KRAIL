package xyz.ksharma.krail.trip.planner.network.disruption

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.feature.track.network.GtfsFeedType
import xyz.ksharma.krail.feature.track.network.GtfsRealtimeResult
import xyz.ksharma.krail.feature.track.network.GtfsRealtimeService
import xyz.ksharma.krail.trip.planner.ui.state.disruption.Disruption
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionFeed
import xyz.ksharma.krail.trip.planner.ui.state.disruption.DisruptionRepository
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Reads one alerts feed per mode and keeps it in memory.
 *
 * - **Cache:** [ttl] defaults to three minutes. NSW sends `max-age=90`, so nothing fresher exists,
 *   and the results screen asks every 30 seconds, so most asks cost no request.
 * - **Dedupe:** one request at a time per repository. A second caller waits on the lock and then
 *   reads the cache the first one filled.
 * - **Budget:** [requestBudget] caps the wait. There is no retry: this is optional information and
 *   the results never wait for it.
 * - **Failure:** the last good answer is returned when there is one, so a dropped request never
 *   removes a banner that was already showing. With nothing cached it returns the failure.
 */
@OptIn(ExperimentalTime::class)
internal class RealDisruptionRepository(
    private val service: GtfsRealtimeService,
    private val clock: Clock = Clock.System,
    private val ttl: Duration = 3.minutes,
    private val requestBudget: Duration = 3.seconds,
) : DisruptionRepository {

    private class Cached(val at: Instant, val items: List<Disruption>)

    private val cache = mutableMapOf<DisruptionFeed, Cached>()
    private val lock = Mutex()

    override suspend fun load(feed: DisruptionFeed): Result<List<Disruption>> {
        freshFor(feed)?.let { return Result.success(it) }
        return lock.withLock {
            freshFor(feed)?.let { return@withLock Result.success(it) }
            fetch(feed)
        }
    }

    private fun freshFor(feed: DisruptionFeed): List<Disruption>? =
        cache[feed]?.takeIf { clock.now() - it.at < ttl }?.items

    private suspend fun fetch(feed: DisruptionFeed): Result<List<Disruption>> {
        val result = withTimeoutOrNull(requestBudget) {
            service.fetchFeed(feedName = feed.feedName, feedType = GtfsFeedType.ALERTS)
        }
        val stale = cache[feed]?.items
        return when (result) {
            is GtfsRealtimeResult.Fresh -> {
                val items = classifyAlerts(feed, result.message.entity.mapNotNull { it.alert })
                cache[feed] = Cached(clock.now(), items)
                log("[DISRUPTION] ${feed.feedName}: ${items.size} service alerts")
                Result.success(items)
            }
            is GtfsRealtimeResult.Error -> {
                log("[DISRUPTION] ${feed.feedName} failed: ${result.cause.message}")
                stale?.let { Result.success(it) } ?: Result.failure(result.cause)
            }
            GtfsRealtimeResult.Unchanged, null -> {
                log("[DISRUPTION] ${feed.feedName} unavailable within $requestBudget")
                stale?.let { Result.success(it) } ?: Result.failure(IllegalStateException("alerts timed out"))
            }
        }
    }
}
