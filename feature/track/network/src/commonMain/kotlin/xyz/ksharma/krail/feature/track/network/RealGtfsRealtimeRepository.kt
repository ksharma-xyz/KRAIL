@file:Suppress(
    "TooManyFunctions",
    "LoopWithTooManyJumpStatements",
    "MagicNumber",
)

package xyz.ksharma.krail.feature.track.network

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number // Month.number extension property
import kotlinx.datetime.toLocalDateTime
import xyz.ksharma.krail.core.datetime.DateTimeHelper.utcToLocalDateTimeAEST
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.feature.track.GtfsRealtimeRepository
import xyz.ksharma.krail.feature.track.LegTrackingInfo
import xyz.ksharma.krail.feature.track.LiveTrackingOverlay
import xyz.ksharma.krail.feature.track.LiveVehiclePosition

internal class RealGtfsRealtimeRepository(
    private val service: GtfsRealtimeService,
    private val ioDispatcher: CoroutineDispatcher,
) : GtfsRealtimeRepository {

    private val feedCache = mutableMapOf<String, String>()
    private val tripUpdateLastModified = mutableMapOf<String, String>()
    private val vehiclePosLastModified = mutableMapOf<String, String>()

    private data class LegWork(
        val info: LegTrackingInfo,
        val feedNames: List<String>,
        val transportationId: String,
        val departureAest: String,
        val startDate: String,
    )

    override suspend fun pollLiveTracking(
        legs: List<LegTrackingInfo>,
        originUtcDateTime: String,
    ): LiveTrackingOverlay {
        val departureAest = originUtcDateTime.toAestHhMmSs()
        val startDate = originUtcDateTime.toGtfsDate()
        logPollStart(departureAest, startDate)

        val works = legs.mapNotNull { leg ->
            val feeds = feedNamesForMode(leg.transportMode)
            if (feeds.isEmpty()) {
                log("[REPO] leg[${leg.legIndex}] mode=${leg.transportMode} → no GTFS-RT feeds, skipping")
                return@mapNotNull null
            }
            log(
                "[REPO] leg[${leg.legIndex}] mode=${leg.transportMode} lineName=${leg.lineName} " +
                    "realtimeTripId=${leg.realtimeTripId} feeds=$feeds depAest=$departureAest startDate=$startDate",
            )
            LegWork(
                info = leg,
                feedNames = feeds,
                transportationId = leg.lineName,
                departureAest = departureAest,
                startDate = startDate,
            )
        }

        if (works.isEmpty()) {
            log("[REPO] no workable legs — returning empty overlay")
            return LiveTrackingOverlay(
                vehiclePositions = emptyMap(),
                stopDelays = emptyMap(),
                lastModified = null,
            )
        }

        val vehiclePositions = mutableMapOf<Int, LiveVehiclePosition>()
        val stopDelays = mutableMapOf<String, Int>()
        var lastModified: String? = null

        coroutineScope {
            works.map { work ->
                async(ioDispatcher) {
                    val (pos, modified) = fetchVehiclePositionForLeg(work)
                    if (pos != null) vehiclePositions[work.info.legIndex] = pos
                    modified?.let { lastModified = it }

                    val delays = fetchStopDelaysForLeg(work)
                    if (delays.isNotEmpty()) stopDelays.putAll(delays)
                }
            }.forEach { it.await() }
        }

        log(
            "[REPO] pollLiveTracking done — " +
                "vehiclePositions=${vehiclePositions.size} stopDelays=${stopDelays.size}",
        )
        return LiveTrackingOverlay(
            vehiclePositions = vehiclePositions,
            stopDelays = stopDelays,
            lastModified = lastModified,
        )
    }

    override fun clearCache() {
        feedCache.clear()
        tripUpdateLastModified.clear()
        vehiclePosLastModified.clear()
        log("[REPO] cache cleared")
    }

    private suspend fun fetchVehiclePositionForLeg(work: LegWork): Pair<LiveVehiclePosition?, String?> {
        val vehiclePosFeed = work.feedNames.firstOrNull { it in VEHICLE_POS_FEEDS }
            ?: return null to null
        val vpKey = "vehiclepos/$vehiclePosFeed"
        log(
            "[REPO] leg[${work.info.legIndex}] vehiclepos fetch " +
                "feed=$vehiclePosFeed sinceLastModified=${vehiclePosLastModified[vpKey]}",
        )
        val vpResult = service.fetchFeed(
            feedName = vehiclePosFeed,
            feedType = GtfsFeedType.VEHICLE_POSITIONS,
            sinceLastModified = vehiclePosLastModified[vpKey],
        )
        log("[REPO] leg[${work.info.legIndex}] vehiclepos result=${vpResult::class.simpleName}")
        return when (vpResult) {
            is GtfsRealtimeResult.Fresh -> {
                vpResult.lastModified?.let { vehiclePosLastModified[vpKey] = it }
                val pos = vpResult.findLiveVehiclePosition(
                    realtimeTripId = work.info.realtimeTripId,
                    transportationId = work.transportationId,
                    departureAest = work.departureAest,
                    startDate = work.startDate,
                )
                val matchStr = if (pos != null) {
                    "FOUND lat=${pos.latitude} lon=${pos.longitude} bearing=${pos.bearing}"
                } else {
                    "NOT FOUND"
                }
                log("[REPO] leg[${work.info.legIndex}] vehiclepos match=$matchStr")
                pos to vpResult.lastModified
            }
            is GtfsRealtimeResult.Unchanged -> {
                log("[REPO] leg[${work.info.legIndex}] vehiclepos Unchanged")
                null to null
            }
            is GtfsRealtimeResult.Error -> {
                logError("[REPO] leg[${work.info.legIndex}] vehiclepos error", vpResult.cause)
                null to null
            }
        }
    }

    @Suppress("ReturnCount", "NestedBlockDepth")
    private suspend fun fetchStopDelaysForLeg(work: LegWork): Map<String, Int> {
        val cachedFeed = feedCache[work.transportationId]
        val feedsToSearch = if (cachedFeed != null) listOf(cachedFeed) else work.feedNames
        log(
            "[REPO] leg[${work.info.legIndex}] " +
                "tripUpdates cachedFeed=$cachedFeed feedsToSearch=$feedsToSearch",
        )
        for (feedName in feedsToSearch) {
            log(
                "[REPO] leg[${work.info.legIndex}] tripUpdate fetch " +
                    "feed=$feedName sinceLastModified=${tripUpdateLastModified[feedName]}",
            )
            val result = service.fetchFeed(
                feedName = feedName,
                feedType = GtfsFeedType.TRIP_UPDATES,
                sinceLastModified = tripUpdateLastModified[feedName],
            )
            log("[REPO] leg[${work.info.legIndex}] tripUpdate result=${result::class.simpleName}")
            when (result) {
                is GtfsRealtimeResult.Fresh -> {
                    result.lastModified?.let { tripUpdateLastModified[feedName] = it }
                    val delays = result.findStopDelays(
                        realtimeTripId = work.info.realtimeTripId,
                        transportationId = work.transportationId,
                        departureAest = work.departureAest,
                        startDate = work.startDate,
                    )
                    log("[REPO] leg[${work.info.legIndex}] stop delays found=${delays.size}")
                    if (delays.isNotEmpty()) {
                        if (cachedFeed == null) feedCache[work.transportationId] = feedName
                        return delays
                    } else if (cachedFeed != null) {
                        log(
                            "[REPO] leg[${work.info.legIndex}] " +
                                "trip vanished from cached feed=$cachedFeed, clearing",
                        )
                        feedCache.remove(work.transportationId)
                    }
                }
                is GtfsRealtimeResult.Unchanged -> {
                    log("[REPO] leg[${work.info.legIndex}] tripUpdate Unchanged — skipping")
                    return emptyMap()
                }
                is GtfsRealtimeResult.Error ->
                    logError(
                        "[REPO] leg[${work.info.legIndex}] tripUpdate error for $feedName",
                        result.cause,
                    )
            }
        }
        return emptyMap()
    }

    private fun feedNamesForMode(mode: TransportMode): List<String> = when (mode) {
        TransportMode.Train -> listOf("sydneytrains", "nswtrains")
        TransportMode.Metro -> listOf("metro")
        TransportMode.LightRail -> listOf(
            "lightrail/cbdandsoutheast",
            "lightrail/newcastle",
            "lightrail/innerwest",
        )
        TransportMode.Bus -> listOf("buses")
        TransportMode.SchoolBus -> listOf("buses")
        TransportMode.Ferry -> listOf("ferries/sydneyferries", "ferries/MFF")
        TransportMode.Coach -> emptyList()
    }

    private fun logPollStart(departureAest: String, startDate: String) {
        val nowAest = runCatching {
            val now = Clock.System.now().toLocalDateTime(TimeZone.of("Australia/Sydney"))
            now.hour.toString().padStart(2, '0') + ":" +
                now.minute.toString().padStart(2, '0') + ":" +
                now.second.toString().padStart(2, '0')
        }.getOrDefault("??:??:??")
        log(
            "[REPO] pollLiveTracking — nowAest=$nowAest departureAest=$departureAest " +
                "startDate=$startDate tripStarted=${nowAest >= departureAest}",
        )
    }

    private fun String.toAestHhMmSs(): String = runCatching {
        utcToLocalDateTimeAEST().let { dt ->
            dt.hour.toString().padStart(2, '0') + ":" +
                dt.minute.toString().padStart(2, '0') + ":" +
                dt.second.toString().padStart(2, '0')
        }
    }.getOrDefault(substringAfterLast("T").take(8))

    private fun String.toGtfsDate(): String = runCatching {
        utcToLocalDateTimeAEST().let { dt ->
            dt.year.toString() +
                dt.month.number.toString().padStart(2, '0') +
                dt.dayOfMonth.toString().padStart(2, '0')
        }
    }.getOrDefault(substringBefore("T").replace("-", ""))
}
