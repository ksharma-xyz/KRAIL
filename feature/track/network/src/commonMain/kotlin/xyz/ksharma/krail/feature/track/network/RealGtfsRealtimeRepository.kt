@file:Suppress("LongMethod", "TooManyFunctions")

package xyz.ksharma.krail.feature.track.network

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number  // Month.number extension property
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

    /** Caches the confirmed feed name per lineName so later polls skip the search. */
    private val feedCache = mutableMapOf<String, String>()

    /** Last-Modified header per trip-updates feed for HEAD optimisation. */
    private val tripUpdateLastModified = mutableMapOf<String, String>()

    /** Last-Modified header per vehicle-positions feed — separate cache from trip updates. */
    private val vehiclePosLastModified = mutableMapOf<String, String>()

    override suspend fun pollLiveTracking(
        legs: List<LegTrackingInfo>,
        originUtcDateTime: String,
    ): LiveTrackingOverlay {
        val departureAest = originUtcDateTime.toAestHhMmSs()
        val startDate = originUtcDateTime.toGtfsDate()

        // Log current time vs departure so we can confirm whether we're polling before trip starts.
        val nowAest = runCatching {
            val now = Clock.System.now().toLocalDateTime(TimeZone.of("Australia/Sydney"))
            now.hour.toString().padStart(2, '0') + ":" +
                now.minute.toString().padStart(2, '0') + ":" +
                now.second.toString().padStart(2, '0')
        }.getOrDefault("??:??:??")
        log("[REPO] pollLiveTracking — nowAest=$nowAest departureAest=$departureAest startDate=$startDate tripStarted=${nowAest >= departureAest}")

        data class LegWork(
            val info: LegTrackingInfo,
            val feedNames: List<String>,
            val transportationId: String,
            val departureAest: String,
            val startDate: String,
        )

        val works = legs.mapNotNull { leg ->
            val feeds = feedNamesForMode(leg.transportMode)
            if (feeds.isEmpty()) {
                log("[REPO] leg[${leg.legIndex}] mode=${leg.transportMode} → no GTFS-RT feeds, skipping")
                return@mapNotNull null
            }
            log("[REPO] leg[${leg.legIndex}] mode=${leg.transportMode} lineName=${leg.lineName} realtimeTripId=${leg.realtimeTripId} feeds=$feeds depAest=$departureAest startDate=$startDate")
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
            return LiveTrackingOverlay(vehiclePositions = emptyMap(), stopDelays = emptyMap(), lastModified = null)
        }

        val vehiclePositions = mutableMapOf<Int, LiveVehiclePosition>()
        val stopDelays = mutableMapOf<String, Int>()
        var lastModified: String? = null

        coroutineScope {
            works.map { work ->
                async(ioDispatcher) {
                    // ── Pass 1: Vehicle Positions (/vehiclepos/) ────────────────────────
                    val vehiclePosFeed = work.feedNames.firstOrNull { it in VEHICLE_POS_FEEDS }
                    if (vehiclePosFeed != null) {
                        val vpKey = "vehiclepos/$vehiclePosFeed"
                        log("[REPO] leg[${work.info.legIndex}] vehiclepos fetch feed=$vehiclePosFeed sinceLastModified=${vehiclePosLastModified[vpKey]}")
                        val vpResult = service.fetchFeed(
                            feedName = vehiclePosFeed,
                            feedType = GtfsFeedType.VEHICLE_POSITIONS,
                            sinceLastModified = vehiclePosLastModified[vpKey],
                        )
                        log("[REPO] leg[${work.info.legIndex}] vehiclepos result=${vpResult::class.simpleName}")
                        when (vpResult) {
                            is GtfsRealtimeResult.Fresh -> {
                                vpResult.lastModified?.let {
                                    vehiclePosLastModified[vpKey] = it
                                    lastModified = it
                                }
                                val pos = vpResult.findLiveVehiclePosition(
                                    realtimeTripId = work.info.realtimeTripId,
                                    transportationId = work.transportationId,
                                    departureAest = work.departureAest,
                                    startDate = work.startDate,
                                )
                                log("[REPO] leg[${work.info.legIndex}] vehiclepos match=${if (pos != null) "FOUND lat=${pos.latitude} lon=${pos.longitude} bearing=${pos.bearing}" else "NOT FOUND"}")
                                if (pos != null) vehiclePositions[work.info.legIndex] = pos
                            }
                            is GtfsRealtimeResult.Unchanged ->
                                log("[REPO] leg[${work.info.legIndex}] vehiclepos Unchanged")
                            is GtfsRealtimeResult.Error ->
                                logError("[REPO] leg[${work.info.legIndex}] vehiclepos error", vpResult.cause)
                        }
                    }

                    // ── Pass 2: Trip Updates (/realtime/) — stop delays ─────────────────
                    val cachedFeed = feedCache[work.transportationId]
                    val feedsToSearch = if (cachedFeed != null) listOf(cachedFeed) else work.feedNames
                    log("[REPO] leg[${work.info.legIndex}] tripUpdates cachedFeed=$cachedFeed feedsToSearch=$feedsToSearch")

                    for (feedName in feedsToSearch) {
                        log("[REPO] leg[${work.info.legIndex}] tripUpdate fetch feed=$feedName sinceLastModified=${tripUpdateLastModified[feedName]}")
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
                                    stopDelays.putAll(delays)
                                    break
                                } else if (cachedFeed != null) {
                                    log("[REPO] leg[${work.info.legIndex}] trip vanished from cached feed=$cachedFeed, clearing")
                                    feedCache.remove(work.transportationId)
                                }
                            }
                            is GtfsRealtimeResult.Unchanged -> {
                                log("[REPO] leg[${work.info.legIndex}] tripUpdate Unchanged — skipping")
                                break
                            }
                            is GtfsRealtimeResult.Error ->
                                logError("[REPO] leg[${work.info.legIndex}] tripUpdate error for $feedName", result.cause)
                        }
                    }
                }
            }.forEach { it.await() }
        }

        log("[REPO] pollLiveTracking done — vehiclePositions=${vehiclePositions.size} stopDelays=${stopDelays.size}")
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

    private fun feedNamesForMode(mode: TransportMode): List<String> = when (mode) {
        TransportMode.Train     -> listOf("sydneytrains", "nswtrains")
        TransportMode.Metro     -> listOf("metro")
        TransportMode.LightRail -> listOf("lightrail/cbdandsoutheast", "lightrail/newcastle", "lightrail/innerwest")
        TransportMode.Bus       -> listOf("buses")
        TransportMode.Ferry     -> listOf("ferries/sydneyferries", "ferries/MFF")
        TransportMode.Coach     -> emptyList()
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
