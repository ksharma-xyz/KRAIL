package xyz.ksharma.krail.feature.track

import kotlinx.collections.immutable.ImmutableList
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.transport.TransportMode

data class TrackedJourney(
    val deepLink: TripDeepLink,
    val display: TrackedJourneyDisplay? = null,
    val isArrived: Boolean = false,
)

data class TrackedJourneyDisplay(
    val fromStopId: String,
    val toStopId: String,
    val fromStopName: String,
    val toStopName: String,
    val originTime: String,
    val scheduledOriginTime: String?,
    val destinationTime: String,
    val originUtcDateTime: String,
    val destinationUtcDateTime: String,
    val travelTime: String,
    val legs: ImmutableList<TrackedLeg>,
    val departureDeviation: DepartureDeviation? = null,
)

sealed class TrackedLeg {
    data class Transport(
        val transportMode: TransportMode,
        val lineName: String,
        val lineColorCode: String,
        val headsign: String?,
        val stops: ImmutableList<TrackedStop>,
        /** GTFS-RT RealtimeTripId from the TfNSW API. Used for primary exact trip_id matching. */
        val realtimeTripId: String? = null,
        /** Actual route path from the API's coords field — used for curved polylines on the map. */
        val routePathCoordinates: ImmutableList<LatLng> = kotlinx.collections.immutable.persistentListOf(),
    ) : TrackedLeg()

    data class Walk(val durationText: String) : TrackedLeg()
}

data class TrackedStop(
    val stopId: String,
    val name: String,
    val scheduledTime: String,
    val estimatedTime: String?,
    /** UTC ISO-8601 time — estimated from the trip API if available, else scheduled. Used for time comparisons. */
    val utcTime: String,
    /** Raw scheduled UTC ISO-8601 time (never estimated). Base for applying GTFS-RT delay offsets. */
    val scheduledUtcTime: String,
    /** Latitude from the API response coord field. Used to avoid DB lookup for map display. */
    val lat: Double? = null,
    /** Longitude from the API response coord field. Used to avoid DB lookup for map display. */
    val lon: Double? = null,
)

sealed class DepartureDeviation {
    data class Late(val text: String) : DepartureDeviation()
    data class Early(val text: String) : DepartureDeviation()
    data object OnTime : DepartureDeviation()
}
