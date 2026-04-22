package xyz.ksharma.krail.feature.track.network

import com.google.transit.realtime.FeedMessage

/**
 * Which TfNSW GTFS-RT API path to hit.
 *
 * - [TRIP_UPDATES]       → `/v1|v2/gtfs/realtime/{feedName}`
 * - [VEHICLE_POSITIONS]  → `/v2/gtfs/vehiclepos/{feedName}`
 */
internal enum class GtfsFeedType { TRIP_UPDATES, VEHICLE_POSITIONS }

internal interface GtfsRealtimeService {
    /**
     * Fetches a GTFS-RT feed by name.
     *
     * @param feedName path segment after the base, e.g. `"sydneytrains"`, `"buses"`
     * @param feedType [GtfsFeedType.TRIP_UPDATES] hits `/gtfs/realtime/`, [GtfsFeedType.VEHICLE_POSITIONS] hits `/gtfs/vehiclepos/`
     * @param sinceLastModified value of `Last-Modified` header from the previous response.
     */
    suspend fun fetchFeed(
        feedName: String,
        feedType: GtfsFeedType = GtfsFeedType.TRIP_UPDATES,
        sinceLastModified: String? = null,
    ): GtfsRealtimeResult
}

internal sealed class GtfsRealtimeResult {
    data class Fresh(
        val message: FeedMessage,
        val lastModified: String?,
    ) : GtfsRealtimeResult()

    data object Unchanged : GtfsRealtimeResult()

    data class Error(val cause: Throwable) : GtfsRealtimeResult()
}
