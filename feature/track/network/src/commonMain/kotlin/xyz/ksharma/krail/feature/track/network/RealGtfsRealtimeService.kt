package xyz.ksharma.krail.feature.track.network

import com.google.transit.realtime.FeedMessage
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.core.network.NSW_TRANSPORT_BASE_URL

internal class RealGtfsRealtimeService(
    private val httpClient: HttpClient,
) : GtfsRealtimeService {

    override suspend fun fetchFeed(
        feedName: String,
        feedType: GtfsFeedType,
        sinceLastModified: String?,
    ): GtfsRealtimeResult = runCatching {
        val url = buildUrl(feedName, feedType)
        log("GtfsRT: ▶ fetching $url (sinceLastModified=$sinceLastModified)")

        // HEAD check — skip parsing if feed hasn't changed since last poll.
        // If HEAD fails for any reason (e.g. 502 gateway error), fall through to GET.
        if (sinceLastModified != null) {
            runCatching {
                val head = httpClient.head(url)
                val serverLastModified = head.headers["Last-Modified"]
                log("GtfsRT: HEAD $url → status=${head.status.value}, Last-Modified=$serverLastModified")
                if (head.status == HttpStatusCode.OK && serverLastModified == sinceLastModified) {
                    log("GtfsRT: $feedName unchanged (Last-Modified=$sinceLastModified)")
                    return GtfsRealtimeResult.Unchanged
                }
            }.onFailure { e ->
                val reason = "${e::class.simpleName}: ${e.message}"
                log("GtfsRT: HEAD $url failed ($reason) — skipping HEAD, proceeding with GET")
            }
        }

        val response = httpClient.get(url) {
            accept(ContentType("application", "x-protobuf"))
        }

        val statusCode = response.status.value
        val lastModified = response.headers["Last-Modified"]
        val contentType = response.headers["Content-Type"]
        log("GtfsRT: GET $url → HTTP $statusCode, Content-Type=$contentType, Last-Modified=$lastModified")

        if (statusCode != HttpStatusCode.OK.value) {
            logError("GtfsRT: ❌ $feedName HTTP $statusCode — aborting parse")
            return GtfsRealtimeResult.Error(Exception("HTTP $statusCode for $feedName"))
        }

        val bytes = response.readRawBytes()
        log("GtfsRT: $feedName response body = ${bytes.size} bytes")

        if (bytes.isEmpty()) {
            logError("GtfsRT: ❌ $feedName empty response body — nothing to parse")
            return GtfsRealtimeResult.Error(Exception("Empty body for $feedName"))
        }

        val message = FeedMessage.ADAPTER.decode(bytes)

        val vehicleCount = message.entity.count { it.vehicle != null }
        val tripUpdateCount = message.entity.count { it.trip_update != null }
        val alertCount = message.entity.count { it.alert != null }
        log(
            "GtfsRT: ✅ $feedName parsed — total=${message.entity.size} entities " +
                "(vehiclePositions=$vehicleCount, tripUpdates=$tripUpdateCount, alerts=$alertCount)",
        )

        GtfsRealtimeResult.Fresh(message, lastModified)
    }.getOrElse { e ->
        logError("GtfsRT: ❌ exception fetching/parsing $feedName — ${e::class.simpleName}: ${e.message}", e)
        GtfsRealtimeResult.Error(e)
    }

    private fun buildUrl(feedName: String, feedType: GtfsFeedType): String {
        return when (feedType) {
            GtfsFeedType.VEHICLE_POSITIONS -> {
                // Vehicle positions are always v2 and at a dedicated /vehiclepos/ path.
                // Documented at: https://api.transport.nsw.gov.au/v2/gtfs/vehiclepos/
                "$NSW_TRANSPORT_BASE_URL/v2/gtfs/vehiclepos/$feedName"
            }
            GtfsFeedType.TRIP_UPDATES -> {
                val version = if (feedName in V2_FEEDS) "v2" else "v1"
                "$NSW_TRANSPORT_BASE_URL/$version/gtfs/realtime/$feedName"
            }
        }
    }

    companion object {
        // These feeds have v2 realtime (trip-updates) endpoints; all others use v1.
        private val V2_FEEDS = setOf("sydneytrains", "metro")
    }
}

/**
 * Feeds that have a dedicated `/v2/gtfs/vehiclepos/` endpoint.
 * Source: https://opendata.transport.nsw.gov.au (v2 vehicle positions API)
 */
internal val VEHICLE_POS_FEEDS = setOf("sydneytrains", "metro", "lightrail/innerwest")
