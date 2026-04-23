@file:Suppress("MagicNumber", "ReturnCount", "UnusedParameter")

package xyz.ksharma.krail.feature.track.network

import com.google.transit.realtime.FeedEntity
import com.google.transit.realtime.FeedMessage
import com.google.transit.realtime.Position
import com.google.transit.realtime.VehiclePosition
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.feature.track.LiveVehiclePosition
import xyz.ksharma.krail.feature.track.VehicleStatus

/** Convenience overloads so callers never need [FeedMessage] on their classpath. */
internal fun GtfsRealtimeResult.Fresh.findLiveVehiclePosition(
    realtimeTripId: String?,
    transportationId: String,
    departureAest: String,
    startDate: String,
): LiveVehiclePosition? = with(GtfsRealtimeMatcher) {
    message.findLiveVehiclePosition(realtimeTripId, transportationId, departureAest, startDate)
}

internal fun GtfsRealtimeResult.Fresh.findStopDelays(
    realtimeTripId: String?,
    transportationId: String,
    departureAest: String,
    startDate: String,
): Map<String, Int> = with(GtfsRealtimeMatcher) {
    message.findStopDelays(realtimeTripId, transportationId, departureAest, startDate)
}

/**
 * Pure matching logic for GTFS-RT feeds.
 *
 * Feed selection uses [feedNamesForTransportation] which takes the raw `iconId` from the
 * TfNSW API — this is more granular than [TransportMode] since both Sydney Trains and NSW
 * Trains share productClass=1 but use different GTFS-RT feed endpoints.
 */
object GtfsRealtimeMatcher {

    /**
     * Returns the GTFS-RT feed name(s) to fetch for a given leg.
     *
     * Returns a list because some regions split their feed across multiple numbered files
     * (e.g. northcoast, northcoast2, northcoast3). All returned feeds must be searched.
     * Empty list means no GTFS-RT coverage for this leg (coaches, school buses, etc.).
     */
    fun feedNamesForTransportation(iconId: Long?, productClass: Int?): List<String> =
        when (iconId?.toInt()) {
            1, 19 -> listOf("sydneytrains")
            2, 3 -> listOf("nswtrains")
            24 -> listOf("metro")
            13, 20 -> listOf("lightrail/cbdandsoutheast")
            21 -> listOf("lightrail/newcastle")
            // Parramatta Light Rail iconId TBD — add once confirmed from a real trip response
            4, 5, 6, 9,
            14, 23,
            -> listOf("buses")

            15 -> listOf("regionbuses/newcastlehunter")
            10 -> listOf("ferries/sydneyferries")
            12 -> listOf("ferries/MFF")
            31 -> listOf("regionbuses/centralwestandorana", "regionbuses/centralwestandorana2")
            32 -> listOf("regionbuses/farwest")
            33 -> listOf("regionbuses/newenglandnorthwest")
            34 -> listOf("regionbuses/newcastlehunter")
            35 -> listOf(
                "regionbuses/northcoast",
                "regionbuses/northcoast2",
                "regionbuses/northcoast3",
            )

            36 -> listOf("regionbuses/riverinamurray", "regionbuses/riverinamurray2")
            37 -> listOf("regionbuses/southeasttablelands", "regionbuses/southeasttablelands2")
            38 -> listOf("regionbuses/sydneysurrounds")
            // Coaches (7, 22), school buses (8), private ferries (11, 18), unknown → no GTFS-RT
            else -> emptyList()
        }

    /**
     * Finds the matched vehicle and maps it to [LiveVehiclePosition].
     * Returns null if no vehicle is found or its position data is absent.
     * Keeps proto types confined to this module — callers receive domain types only.
     */
    fun FeedMessage.findLiveVehiclePosition(
        realtimeTripId: String?,
        transportationId: String,
        departureAest: String,
        startDate: String,
    ): LiveVehiclePosition? {
        val vehicle =
            findVehiclePosition(realtimeTripId, transportationId, departureAest, startDate)
                ?: return null
        val pos = vehicle.position ?: return null

        logMatchedVehicle(vehicle, pos)

        return LiveVehiclePosition(
            latitude = pos.latitude.toDouble(),
            longitude = pos.longitude.toDouble(),
            bearing = pos.bearing ?: 0f,
            status = when (vehicle.current_status) {
                VehiclePosition.VehicleStopStatus.INCOMING_AT -> VehicleStatus.INCOMING_AT
                VehiclePosition.VehicleStopStatus.STOPPED_AT -> VehicleStatus.STOPPED_AT
                else -> VehicleStatus.IN_TRANSIT_TO
            },
            lastUpdatedEpochSec = vehicle.timestamp ?: 0L,
        )
    }

    private fun FeedMessage.findVehiclePosition(
        realtimeTripId: String?,
        transportationId: String,
        departureAest: String,
        startDate: String,
    ): VehiclePosition? {
        val vehicleEntities = entity.filter { it.vehicle != null }
        val tripUpdateEntities = entity.filter { it.trip_update != null }
        val lineToken = normalise(transportationId.substringAfterLast(":").ifBlank { transportationId })

        logFeedOverview(vehicleEntities, tripUpdateEntities, realtimeTripId, lineToken, startDate)
        logMatchCandidates(vehicleEntities, tripUpdateEntities, realtimeTripId, lineToken)

        // Primary match — exact trip_id
        if (!realtimeTripId.isNullOrBlank()) {
            entity.firstOrNull { it.vehicle?.trip?.trip_id == realtimeTripId }
                ?.vehicle
                ?.let {
                    log("[LIVETRACK_MATCH] ✅ primary exact match hit")
                    return it
                }

            entity.firstOrNull { it.vehicle?.trip?.trip_id?.endsWith(realtimeTripId) == true }
                ?.vehicle
                ?.let {
                    log("[LIVETRACK_MATCH] ✅ primary endsWith match hit")
                    return it
                }

            log("[LIVETRACK_MATCH] ❌ primary match missed — '$realtimeTripId'")
        }

        // Fallback — route_id exact match (normalised) + optional start_date guard.
        val result = entity.firstOrNull { e ->
            val trip = e.vehicle?.trip ?: return@firstOrNull false
            normalise(trip.route_id ?: return@firstOrNull false) == lineToken &&
                (trip.start_date == null || trip.start_date == startDate)
        }?.vehicle

        if (result != null) {
            log("[LIVETRACK_MATCH] ✅ fallback route_id match hit")
            return result
        }

        log("[LIVETRACK_MATCH] ❌ fallback route_id missed — lineToken=$lineToken, startDate=$startDate")
        logPreTripCandidates(vehicleEntities, lineToken, startDate)
        return null
    }

    /**
     * Returns a map of stopId → delay in seconds (negative = early) for a specific trip.
     *
     * Uses the same matching priority as [findVehiclePosition].
     */
    fun FeedMessage.findStopDelays(
        realtimeTripId: String?,
        transportationId: String,
        departureAest: String,
        startDate: String,
    ): Map<String, Int> {
        val lineToken =
            normalise(transportationId.substringAfterLast(":").ifBlank { transportationId })

        val byTripId = if (!realtimeTripId.isNullOrBlank()) {
            entity.firstOrNull { it.trip_update?.trip?.trip_id == realtimeTripId }?.trip_update
                ?: entity.firstOrNull {
                    it.trip_update?.trip?.trip_id?.endsWith(realtimeTripId) == true
                }?.trip_update
        } else {
            null
        }

        val tripUpdate = byTripId ?: entity.firstOrNull { e ->
            val trip = e.trip_update?.trip ?: return@firstOrNull false
            normalise(trip.route_id ?: return@firstOrNull false) == lineToken &&
                (trip.start_date == null || trip.start_date == startDate)
        }?.trip_update

        return tripUpdate?.stop_time_update
            ?.mapNotNull { update ->
                val stopId = update.stop_id ?: return@mapNotNull null
                val delay =
                    update.arrival?.delay ?: update.departure?.delay ?: return@mapNotNull null
                stopId to delay
            }
            ?.toMap()
            ?: emptyMap()
    }

    // ── Diagnostic logging helpers ────────────────────────────────────────────

    private fun logMatchedVehicle(vehicle: VehiclePosition, pos: Position) {
        log(
            "[LIVETRACK_MATCH] matched vehicle — " +
                "vehicleId=${vehicle.vehicle?.id} label=${vehicle.vehicle?.label} | " +
                "trip_id=${vehicle.trip?.trip_id} route_id=${vehicle.trip?.route_id} " +
                "start_time=${vehicle.trip?.start_time} start_date=${vehicle.trip?.start_date} | " +
                "lat=${pos.latitude} lon=${pos.longitude} bearing=${pos.bearing} " +
                "status=${vehicle.current_status} timestampEpoch=${vehicle.timestamp}",
        )
    }

    private fun logFeedOverview(
        vehicleEntities: List<FeedEntity>,
        tripUpdateEntities: List<FeedEntity>,
        realtimeTripId: String?,
        lineToken: String,
        startDate: String,
    ) {
        log(
            "[LIVETRACK_MATCH] feed vehicles=${vehicleEntities.size} " +
                "tripUpdates=${tripUpdateEntities.size} — " +
                "seeking realtimeTripId=$realtimeTripId lineToken=$lineToken startDate=$startDate",
        )
        val vehicleSample = vehicleEntities.take(5).map { e ->
            "trip_id=${e.vehicle?.trip?.trip_id} route_id=${e.vehicle?.trip?.route_id}"
        }
        log("[LIVETRACK_MATCH] vehicle sample (first 5):\n${vehicleSample.joinToString("\n")}")
    }

    private fun logMatchCandidates(
        vehicleEntities: List<FeedEntity>,
        tripUpdateEntities: List<FeedEntity>,
        realtimeTripId: String?,
        lineToken: String,
    ) {
        if (vehicleEntities.isEmpty() && tripUpdateEntities.isNotEmpty()) {
            val tuSample = tripUpdateEntities.take(5)
                .map { e -> "trip_id=${e.trip_update?.trip?.trip_id}" }
            log("[LIVETRACK_MATCH] ⚠️ no vehicle positions — TripUpdate sample:\n${tuSample.joinToString("\n")}")
        }

        if (!realtimeTripId.isNullOrBlank()) {
            val byTripId = vehicleEntities.filter {
                val tid = it.vehicle?.trip?.trip_id ?: return@filter false
                tid == realtimeTripId || tid.endsWith(realtimeTripId) || tid.contains(realtimeTripId)
            }
            val tripIds = byTripId.map { it.vehicle?.trip?.trip_id }
            log("[LIVETRACK_MATCH] trip_id candidates: ${byTripId.size} — $tripIds")

            val tuByTripId = tripUpdateEntities.filter {
                val tid = it.trip_update?.trip?.trip_id ?: return@filter false
                tid == realtimeTripId || tid.endsWith(realtimeTripId) || tid.contains(realtimeTripId)
            }
            val tuTripIds = tuByTripId.map { it.trip_update?.trip?.trip_id }
            log("[LIVETRACK_MATCH] tripUpdate candidates: ${tuByTripId.size} — $tuTripIds")
        }

        val byRoute = vehicleEntities.filter {
            val rid = it.vehicle?.trip?.route_id ?: return@filter false
            normalise(rid) == lineToken
        }
        log("[LIVETRACK_MATCH] route_id candidates (normalised='$lineToken'): ${byRoute.size}")
    }

    private fun logPreTripCandidates(
        vehicleEntities: List<FeedEntity>,
        lineToken: String,
        startDate: String,
    ) {
        val preTripCandidates = vehicleEntities.filter {
            val rid = it.vehicle?.trip?.route_id ?: return@filter false
            normalise(rid) == lineToken
        }
        if (preTripCandidates.isNotEmpty()) {
            log("[LIVETRACK_PRETIP] 🔍 ${preTripCandidates.size} vehicle(s) on '$lineToken' (our date=$startDate):")
            preTripCandidates.forEach { e ->
                val v = e.vehicle ?: return@forEach
                log(
                    "[LIVETRACK_PRETIP]   vehicleId=${v.vehicle?.id} " +
                        "trip_id=${v.trip?.trip_id} start_date=${v.trip?.start_date} " +
                        "status=${v.current_status}",
                )
            }
        } else {
            log("[LIVETRACK_PRETIP] 🔍 no vehicles found for '$lineToken' in feed")
        }
    }

    private fun normalise(value: String): String =
        value.lowercase().replace(Regex("[^a-z0-9]"), "")
}
