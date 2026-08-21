package xyz.ksharma.krail.feature.track.network

import com.google.transit.realtime.FeedEntity
import com.google.transit.realtime.FeedHeader
import com.google.transit.realtime.FeedMessage
import com.google.transit.realtime.Position
import com.google.transit.realtime.TripDescriptor
import com.google.transit.realtime.TripUpdate
import com.google.transit.realtime.VehiclePosition

/** Wire-generated GTFS-RT builders for the matcher suite. Proto types, not fakes. */

internal fun feed(vararg entities: FeedEntity) = FeedMessage(
    header_ = FeedHeader(gtfs_realtime_version = "2.0"),
    entity = entities.toList(),
)

internal fun vehicleEntity(
    id: String = "entity",
    tripId: String? = null,
    routeId: String? = null,
    startDate: String? = null,
    position: Position? = Position(latitude = -33.87f, longitude = 151.21f),
    status: VehiclePosition.VehicleStopStatus? = null,
    timestamp: Long? = null,
) = FeedEntity(
    id = id,
    vehicle = VehiclePosition(
        trip = TripDescriptor(trip_id = tripId, route_id = routeId, start_date = startDate),
        position = position,
        current_status = status,
        timestamp = timestamp,
    ),
)

internal fun tripUpdateEntity(
    id: String = "entity",
    tripId: String? = null,
    routeId: String? = null,
    startDate: String? = null,
    stopTimeUpdates: List<TripUpdate.StopTimeUpdate> = emptyList(),
) = FeedEntity(
    id = id,
    trip_update = TripUpdate(
        trip = TripDescriptor(trip_id = tripId, route_id = routeId, start_date = startDate),
        stop_time_update = stopTimeUpdates,
    ),
)

internal fun stopTimeUpdate(
    stopId: String?,
    arrivalDelay: Int? = null,
    departureDelay: Int? = null,
) = TripUpdate.StopTimeUpdate(
    stop_id = stopId,
    arrival = arrivalDelay?.let { TripUpdate.StopTimeEvent(delay = it) },
    departure = departureDelay?.let { TripUpdate.StopTimeEvent(delay = it) },
)
