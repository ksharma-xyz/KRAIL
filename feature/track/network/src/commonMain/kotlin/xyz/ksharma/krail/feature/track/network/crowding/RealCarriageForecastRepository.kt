package xyz.ksharma.krail.feature.track.network.crowding

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.ksharma.krail.core.network.error.NetworkCaller
import xyz.ksharma.krail.feature.crowding.CarriageForecastRepository
import xyz.ksharma.krail.feature.crowding.CarriageIndex
import xyz.ksharma.krail.feature.track.network.GtfsFeedType
import xyz.ksharma.krail.feature.track.network.GtfsRealtimeResult
import xyz.ksharma.krail.feature.track.network.GtfsRealtimeService
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Lives beside the live trip screen's GTFS-R client so there is one client for these feeds,
 * and reaches trip results and the departure board through [CarriageForecastRepository] in
 * `:feature:crowding:state`, which neither UI module has to depend on this one for.
 *
 * One cached download is shared by every caller for [maxAge] (one budget, one gate). The
 * mutex makes two screens that ask at once share a single request.
 */
@OptIn(ExperimentalTime::class)
internal class RealCarriageForecastRepository(
    private val service: GtfsRealtimeService,
    private val networkCaller: NetworkCaller,
    private val isEnabled: () -> Boolean,
    private val clock: Clock = Clock.System,
    private val maxAge: Duration = DEFAULT_MAX_AGE,
) : CarriageForecastRepository {

    private val lock = Mutex()
    private var cached: CarriageIndex? = null
    private var cachedAt: Instant? = null

    override suspend fun load(): Result<CarriageIndex> {
        if (!isEnabled()) return Result.success(CarriageIndex.EMPTY)
        return lock.withLock {
            val fresh = cached?.takeIf { isFresh() }
            if (fresh != null) {
                Result.success(fresh)
            } else {
                fetch().onSuccess {
                    cached = it
                    cachedAt = clock.now()
                }
            }
        }
    }

    private fun isFresh(): Boolean = cachedAt?.let { clock.now() - it < maxAge } == true

    private suspend fun fetch(): Result<CarriageIndex> =
        networkCaller.call(endpoint = ENDPOINT, upstream = UPSTREAM) {
            // No If-Modified-Since: the cache above already decides when to ask again, and an
            // Unchanged answer would carry no index to return.
            when (val result = service.fetchFeed(FEED, GtfsFeedType.TRIP_UPDATES)) {
                is GtfsRealtimeResult.Fresh -> result.message.toCarriageIndex()
                is GtfsRealtimeResult.Unchanged -> cached ?: CarriageIndex.EMPTY
                is GtfsRealtimeResult.Error -> throw result.cause
            }
        }

    private companion object {
        const val FEED = "sydneytrains"
        const val ENDPOINT = "/v2/gtfs/realtime/sydneytrains"
        const val UPSTREAM = "nsw"
        val DEFAULT_MAX_AGE = 60.seconds
    }
}
