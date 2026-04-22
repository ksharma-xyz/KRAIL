@file:Suppress("MagicNumber")

package xyz.ksharma.krail.feature.track.network

import com.google.transit.realtime.FeedMessage
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
            14, 23 -> listOf("buses")

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
                "regionbuses/northcoast3"
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

        // Log full vehicle details so we can confirm the match is for the right vehicle
        // and understand what data is available before the trip starts.
        log(
            "[LIVETRACK_MATCH] matched vehicle — " +
                "vehicleId=${vehicle.vehicle?.id} label=${vehicle.vehicle?.label} licensePlate=${vehicle.vehicle?.license_plate} | " +
                "trip_id=${vehicle.trip?.trip_id} route_id=${vehicle.trip?.route_id} " +
                "start_time=${vehicle.trip?.start_time} start_date=${vehicle.trip?.start_date} " +
                "scheduleRelationship=${vehicle.trip?.schedule_relationship} | " +
                "lat=${pos.latitude} lon=${pos.longitude} bearing=${pos.bearing} speed=${pos.speed} | " +
                "currentStopSeq=${vehicle.current_stop_sequence} currentStopId=${vehicle.stop_id} " +
                "status=${vehicle.current_status} timestampEpoch=${vehicle.timestamp}"
        )

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

        log(
            "[LIVETRACK_MATCH] feed total=${entity.size} " +
                "(vehicles=${vehicleEntities.size}, tripUpdates=${tripUpdateEntities.size}) — " +
                "seeking realtimeTripId=$realtimeTripId, lineToken=$lineToken, startDate=$startDate"
        )

        // Sample first 5 vehicle entries.
        val vehicleSample = vehicleEntities.take(5).map { e ->
            "trip_id=${e.vehicle?.trip?.trip_id} | route_id=${e.vehicle?.trip?.route_id} | start_date=${e.vehicle?.trip?.start_date} | start_time=${e.vehicle?.trip?.start_time}"
        }
        log("[LIVETRACK_MATCH] vehicle sample (first 5):\n${vehicleSample.joinToString("\n")}")

        // If no vehicle entities, dump trip_update sample so we know what the feed actually contains.
        if (vehicleEntities.isEmpty() && tripUpdateEntities.isNotEmpty()) {
            val tuSample = tripUpdateEntities.take(5).map { e ->
                "trip_id=${e.trip_update?.trip?.trip_id} | route_id=${e.trip_update?.trip?.route_id} | start_date=${e.trip_update?.trip?.start_date} | start_time=${e.trip_update?.trip?.start_time}"
            }
            log("[LIVETRACK_MATCH] ⚠️ feed has NO vehicle positions — TripUpdate sample (first 5):\n${tuSample.joinToString("\n")}")
        }

        // Show any entry whose trip_id contains our realtimeTripId (partial match scan).
        if (!realtimeTripId.isNullOrBlank()) {
            val candidatesByTripId = vehicleEntities.filter {
                val tid = it.vehicle?.trip?.trip_id ?: return@filter false
                tid == realtimeTripId || tid.endsWith(realtimeTripId) || tid.contains(realtimeTripId)
            }
            log("[LIVETRACK_MATCH] vehicle trip_id candidates (exact/endsWith/contains '$realtimeTripId'): ${candidatesByTripId.size} — ${candidatesByTripId.map { it.vehicle?.trip?.trip_id }}")

            // Also scan trip_updates so we can confirm whether the trip exists in the feed at all.
            val tuCandidates = tripUpdateEntities.filter {
                val tid = it.trip_update?.trip?.trip_id ?: return@filter false
                tid == realtimeTripId || tid.endsWith(realtimeTripId) || tid.contains(realtimeTripId)
            }
            log("[LIVETRACK_MATCH] tripUpdate trip_id candidates (exact/endsWith/contains '$realtimeTripId'): ${tuCandidates.size} — ${tuCandidates.map { it.trip_update?.trip?.trip_id }}")
        }

        // Show any entry whose route_id normalises to our lineToken.
        val candidatesByRoute = vehicleEntities.filter {
            val rid = it.vehicle?.trip?.route_id ?: return@filter false
            normalise(rid) == lineToken
        }
        log("[LIVETRACK_MATCH] route_id candidates (normalised == '$lineToken'): ${candidatesByRoute.size} — ${candidatesByRoute.take(5).map { "trip_id=${it.vehicle?.trip?.trip_id} route_id=${it.vehicle?.trip?.route_id} start_date=${it.vehicle?.trip?.start_date}" }}")

        // Primary match — exact trip_id
        if (!realtimeTripId.isNullOrBlank()) {
            entity.firstOrNull { it.vehicle?.trip?.trip_id == realtimeTripId }
                ?.vehicle?.let {
                    log("[LIVETRACK_MATCH] ✅ primary exact match hit")
                    return it
                }

            entity.firstOrNull { it.vehicle?.trip?.trip_id?.endsWith(realtimeTripId) == true }
                ?.vehicle?.let {
                    log("[LIVETRACK_MATCH] ✅ primary endsWith match hit")
                    return it
                }

            log("[LIVETRACK_MATCH] ❌ primary match missed — no trip_id exact/endsWith match for '$realtimeTripId'")
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

        log("[LIVETRACK_MATCH] ❌ fallback route_id match missed — lineToken=$lineToken, startDate=$startDate")

        // Pre-trip probe: search any vehicle on the same route ignoring start_date entirely.
        // A transit agency may pre-assign the vehicle to this trip_id in the feed even before
        // departure, OR the vehicle may still be running a prior service on the same route.
        // Logging these helps us determine if pre-trip tracking is feasible.
        val preTripCandidates = vehicleEntities.filter {
            val rid = it.vehicle?.trip?.route_id ?: return@filter false
            normalise(rid) == lineToken
        }
        if (preTripCandidates.isNotEmpty()) {
            log(
                "[LIVETRACK_PRETIP] 🔍 found ${preTripCandidates.size} vehicle(s) on route '$lineToken' " +
                    "regardless of start_date (our date=$startDate) — this is the pre-trip probe:"
            )
            preTripCandidates.forEach { e ->
                val v = e.vehicle ?: return@forEach
                log(
                    "[LIVETRACK_PRETIP]   vehicleId=${v.vehicle?.id} label=${v.vehicle?.label} | " +
                        "trip_id=${v.trip?.trip_id} start_date=${v.trip?.start_date} start_time=${v.trip?.start_time} " +
                        "scheduleRelationship=${v.trip?.schedule_relationship} | " +
                        "lat=${v.position?.latitude} lon=${v.position?.longitude} bearing=${v.position?.bearing} " +
                        "currentStopId=${v.stop_id} status=${v.current_status} timestampEpoch=${v.timestamp}"
                )
            }
        } else {
            log("[LIVETRACK_PRETIP] 🔍 no vehicles found for route '$lineToken' in feed at all — vehicle not yet published or wrong feed")
        }

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

        val tripUpdate = (if (!realtimeTripId.isNullOrBlank()) {
            entity.firstOrNull { it.trip_update?.trip?.trip_id == realtimeTripId }?.trip_update
                ?: entity.firstOrNull { it.trip_update?.trip?.trip_id?.endsWith(realtimeTripId) == true }?.trip_update
        } else null) ?: entity.firstOrNull { e ->
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

    /**
     * Normalises a route/line token for comparison:
     * lowercase, strips whitespace and non-alphanumeric characters.
     * "T1 " → "t1", "T1_EXT" → "t1ext", "B1" → "b1"
     */
    private fun normalise(value: String): String =
        value.lowercase().replace(Regex("[^a-z0-9]"), "")
}
