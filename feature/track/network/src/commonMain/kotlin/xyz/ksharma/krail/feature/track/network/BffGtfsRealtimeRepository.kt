package xyz.ksharma.krail.feature.track.network

import app.krail.bff.proto.TrackRequest
import app.krail.bff.proto.TrackResponse
import app.krail.bff.proto.VehicleLive
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.number
import xyz.ksharma.krail.core.datetime.DateTimeHelper.utcToLocalDateTimeAEST
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.core.network.BffEndpointResolver
import xyz.ksharma.krail.core.network.NetworkUpstream
import xyz.ksharma.krail.core.network.logNetworkCall
import xyz.ksharma.krail.core.network.toNetworkUpstream
import xyz.ksharma.krail.core.remoteconfig.flag.Flag
import xyz.ksharma.krail.core.remoteconfig.flag.FlagKeys
import xyz.ksharma.krail.core.remoteconfig.flag.asBoolean
import xyz.ksharma.krail.feature.track.GtfsRealtimeRepository
import xyz.ksharma.krail.feature.track.LegTrackingInfo
import xyz.ksharma.krail.feature.track.LiveTrackingOverlay
import xyz.ksharma.krail.feature.track.LiveVehiclePosition
import xyz.ksharma.krail.feature.track.VehicleStatus

/**
 * BFF-backed [GtfsRealtimeRepository]: one `POST /api/v1/track/snapshot`
 * replaces the per-feed GTFS-RT fetch + client-side matching — the BFF
 * polls NSW once for all users and returns a render-ready [TrackResponse]
 * (contract: track.proto, krail-api-proto v0.4.0).
 *
 * Routing: used only when [BffEndpointResolver] resolves to the BFF AND the
 * [FlagKeys.BFF_USE_FOR_TRACK] kill switch is not off. Any BFF failure
 * falls back to [direct] (NSW GTFS-RT polling) for that poll — tracking
 * never breaks because the BFF is unreachable.
 */
internal class BffGtfsRealtimeRepository(
    private val httpClient: HttpClient,
    private val resolver: BffEndpointResolver,
    private val flag: Flag,
    private val direct: GtfsRealtimeRepository,
    private val ioDispatcher: CoroutineDispatcher,
) : GtfsRealtimeRepository {

    override suspend fun pollLiveTracking(
        legs: List<LegTrackingInfo>,
        originUtcDateTime: String,
    ): LiveTrackingOverlay {
        val baseUrl = resolver.resolveBaseUrl()
        val useBff = baseUrl.toNetworkUpstream() == NetworkUpstream.BFF &&
            flag.getFlagValue(FlagKeys.BFF_USE_FOR_TRACK.key).asBoolean(fallback = true)
        if (!useBff) {
            return direct.pollLiveTracking(legs, originUtcDateTime)
        }
        return runCatching { snapshotOverlay(baseUrl, legs, originUtcDateTime) }
            .getOrElse { error ->
                logError("[BFFTRACK] snapshot failed, falling back to direct GTFS-RT", error)
                direct.pollLiveTracking(legs, originUtcDateTime)
            }
    }

    override fun clearCache() = direct.clearCache()

    private suspend fun snapshotOverlay(
        baseUrl: String,
        legs: List<LegTrackingInfo>,
        originUtcDateTime: String,
    ): LiveTrackingOverlay = withContext(ioDispatcher) {
        val request = TrackRequest(
            legs = legs.mapNotNull { leg -> leg.toTrackLeg(originUtcDateTime) },
        )
        if (request.legs.isEmpty()) {
            log("[BFFTRACK] no legs with a realtimeTripId — nothing to track via BFF")
            return@withContext LiveTrackingOverlay(emptyMap(), emptyMap(), lastModified = null)
        }

        logNetworkCall(
            target = NetworkUpstream.BFF,
            method = "POST",
            path = "/api/v1/track/snapshot",
        )
        val bytes: ByteArray = httpClient.post("$baseUrl/api/v1/track/snapshot") {
            contentType(ContentType("application", "x-protobuf"))
            accept(ContentType("application", "x-protobuf"))
            setBody(TrackRequest.ADAPTER.encode(request))
        }.body()

        val response = TrackResponse.ADAPTER.decode(bytes)
        log(
            "[BFFTRACK] snapshot — ${response.legs.size} legs, statuses=" +
                response.legs.joinToString { it.status.name },
        )
        response.toOverlay()
    }

    private fun LegTrackingInfo.toTrackLeg(originUtcDateTime: String): TrackRequest.TrackLeg? {
        val tripId = realtimeTripId?.takeIf { it.isNotBlank() } ?: return null
        val departureUtc = plannedDepartureUtc.ifBlank { originUtcDateTime }
        return TrackRequest.TrackLeg(
            leg_ref = legIndex.toString(),
            realtime_trip_id = tripId,
            product_class = transportMode.productClass,
            origin_stop_id = originStopId,
            destination_stop_id = destinationStopId,
            service_date = departureUtc.toServiceDate(),
            planned_departure_utc = departureUtc,
        )
    }
}

/**
 * Maps the BFF snapshot onto the overlay shape the track screen already
 * renders. Legs keep their request order; `leg_ref` round-trips the
 * original `legIndex`.
 */
internal fun TrackResponse.toOverlay(): LiveTrackingOverlay {
    val positions = buildMap {
        legs.forEach { leg ->
            val legIndex = leg.leg_ref.toIntOrNull() ?: return@forEach
            val vehicle = leg.vehicle ?: return@forEach
            put(legIndex, vehicle.toLivePosition())
        }
    }
    val delays = buildMap {
        legs.forEach { leg ->
            if (!leg.has_delay) return@forEach
            leg.stops.forEach { stop -> put(stop.stop_id, leg.delay_seconds) }
        }
    }
    return LiveTrackingOverlay(
        vehiclePositions = positions,
        stopDelays = delays,
        lastModified = fetched_at_epoch_sec.takeIf { it > 0 }?.toString(),
    )
}

private fun VehicleLive.toLivePosition(): LiveVehiclePosition = LiveVehiclePosition(
    latitude = latitude,
    longitude = longitude,
    bearing = if (has_bearing) bearing_degrees else 0f,
    status = when (stop_relation) {
        VehicleLive.StopRelation.INCOMING_AT -> VehicleStatus.INCOMING_AT
        VehicleLive.StopRelation.STOPPED_AT -> VehicleStatus.STOPPED_AT
        else -> VehicleStatus.IN_TRANSIT_TO
    },
    lastUpdatedEpochSec = measured_at_epoch_sec,
)

/** ISO-8601 UTC → YYYYMMDD Sydney service date (the BFF expiry key). */
internal fun String.toServiceDate(): String = runCatching {
    utcToLocalDateTimeAEST().let { dt ->
        dt.year.toString() +
            dt.month.number.toString().padStart(2, '0') +
            dt.dayOfMonth.toString().padStart(2, '0')
    }
}.getOrDefault(substringBefore("T").replace("-", ""))
