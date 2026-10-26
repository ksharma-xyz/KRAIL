package xyz.ksharma.krail.feature.track.network.crowding

import com.google.transit.realtime.FeedMessage
import xyz.ksharma.krail.feature.crowding.CarriageIndex
import xyz.ksharma.krail.feature.crowding.CarriageLevels
import xyz.ksharma.krail.feature.crowding.SeatLevel

/**
 * Indexes every stop of every trip that carries per-carriage forecasts (TfNSW extension 1007
 * on StopTimeUpdate). Stops without carriage data, and trips with none at all, are left out
 * so a lookup miss and "no data" are the same thing.
 */
internal fun FeedMessage.toCarriageIndex(): CarriageIndex {
    val byTrip = entity.mapNotNull { entity ->
        val update = entity.trip_update ?: return@mapNotNull null
        val tripId = update.trip.trip_id?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val byStop = update.stop_time_update.mapNotNull { stop ->
            val stopId = stop.stop_id ?: return@mapNotNull null
            val cars = CarriageLevels.fromPositions(
                stop.carriage_seq_predictive_occupancy.associate { car ->
                    (car.position_in_consist ?: return@mapNotNull null) to
                        SeatLevel.fromGtfs(car.departure_occupancy_status?.value)
                },
            ) ?: return@mapNotNull null
            stopId to cars
        }.toMap()
        byStop.takeIf { it.isNotEmpty() }?.let { tripId to it }
    }.toMap()
    return CarriageIndex(byTrip)
}
