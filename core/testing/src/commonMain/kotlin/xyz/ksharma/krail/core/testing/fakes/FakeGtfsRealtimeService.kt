package xyz.ksharma.krail.core.testing.fakes

import com.google.transit.realtime.FeedHeader
import com.google.transit.realtime.FeedMessage
import kotlinx.coroutines.yield
import xyz.ksharma.krail.feature.track.network.GtfsFeedType
import xyz.ksharma.krail.feature.track.network.GtfsRealtimeResult
import xyz.ksharma.krail.feature.track.network.GtfsRealtimeService

/**
 * Canonical fake for [GtfsRealtimeService].
 *
 * - [result] is what the next `fetchFeed` returns. Set it to a decoded fixture for the happy
 *   path, or [GtfsRealtimeResult.Error] to drive the failure path.
 * - [requests] records every `(feedName, feedType)` asked for, so a test can prove a cache or
 *   a kill switch stopped a download.
 */
class FakeGtfsRealtimeService(
    var result: GtfsRealtimeResult = GtfsRealtimeResult.Fresh(EMPTY_FEED, lastModified = null),
) : GtfsRealtimeService {

    val requests = mutableListOf<Pair<String, GtfsFeedType>>()

    override suspend fun fetchFeed(
        feedName: String,
        feedType: GtfsFeedType,
        sinceLastModified: String?,
    ): GtfsRealtimeResult {
        requests += feedName to feedType
        // The real service always suspends on the network; so does this.
        yield()
        return result
    }

    companion object {
        val EMPTY_FEED = FeedMessage(header_ = FeedHeader(gtfs_realtime_version = "2.0"))
    }
}
