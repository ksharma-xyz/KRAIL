package xyz.ksharma.krail.feature.track

import xyz.ksharma.krail.core.transport.TransportMode

/**
 * Fetches live vehicle positions and stop delays from GTFS-RT feeds.
 * Implementations handle feed selection, HTTP fetching, proto parsing, and caching.
 * No proto types leak beyond this interface.
 */
interface GtfsRealtimeRepository {

    /**
     * Polls live vehicle positions and stop delays for all given legs in parallel.
     *
     * @param legs one entry per transport leg in the journey (walk legs are ignored by impl)
     * @param originUtcDateTime ISO-8601 UTC departure time of the journey's first leg
     * @return overlay with found positions and delays; empty maps if nothing matched
     */
    suspend fun pollLiveTracking(
        legs: List<LegTrackingInfo>,
        originUtcDateTime: String,
    ): LiveTrackingOverlay

    /** Clears feed discovery cache — call when a new trip starts so stale entries don't persist. */
    fun clearCache()
}

/**
 * Minimal per-leg data needed by [GtfsRealtimeRepository] to find a vehicle in the GTFS-RT feed.
 * Contains only plain Kotlin types — no proto or network dependencies.
 */
data class LegTrackingInfo(
    /** Zero-based position of this leg in [TrackedJourneyDisplay.legs]. */
    val legIndex: Int,
    val transportMode: TransportMode,
    /** Short line identifier, e.g. "T1", "M1", "700". Used as fallback route_id token. */
    val lineName: String,
    /** GTFS-RT RealtimeTripId from the TfNSW API. Enables exact trip_id matching. */
    val realtimeTripId: String?,
    /**
     * Boarding / alighting stop ids and the leg's planned departure
     * (ISO-8601 UTC). Used by the BFF tracking path (`TrackRequest.TrackLeg`)
     * to slice the trip to the user's journey segment and detect expiry.
     * Defaults keep the legacy GTFS-RT direct path source-compatible.
     */
    val originStopId: String = "",
    val destinationStopId: String = "",
    val plannedDepartureUtc: String = "",
)
