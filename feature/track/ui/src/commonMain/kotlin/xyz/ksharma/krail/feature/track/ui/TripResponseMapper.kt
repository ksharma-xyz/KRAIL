package xyz.ksharma.krail.feature.track.ui

import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.datetime.DateTimeHelper.calculateTimeDifference
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toFormattedDurationTimeString
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toHHMM
import xyz.ksharma.krail.core.datetime.DateTimeHelper.utcToLocalDateTimeAEST
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.core.transport.nsw.NswTransportLine
import xyz.ksharma.krail.feature.track.DepartureDeviation
import xyz.ksharma.krail.feature.track.TrackedJourneyDisplay
import xyz.ksharma.krail.feature.track.TrackedLeg
import xyz.ksharma.krail.feature.track.TripDeepLink
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.network.api.model.departureDeviationMinutes
import xyz.ksharma.krail.trip.planner.network.api.model.isWalkingLeg
import kotlin.math.absoluteValue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

internal object TripResponseMapper {

    private const val SECONDS_PER_MINUTE = 60L

    /**
     * Finds the journey in [TripResponse] that matches the deep link.
     *
     * Matches by checking that all deep-link transportation IDs appear in the journey legs
     * AND that the first leg's planned departure time matches the deep link's departure time.
     * Planned departure is used (not estimated) because it's stable — it never shifts with
     * real-time delays — making it a reliable identity discriminator between service runs.
     *
     * Note: When a disruption restructures a service mid-route (e.g. trains replaced by buses at a
     * station), the API may return the originally direct journey as a multi-leg journey requiring
     * an interchange — even though the deep link was encoded for a single-leg trip. This is
     * accurate live data; the tracker displays it as-is. The leg count can therefore legitimately
     * increase (or decrease) between polls. If the disruption clears, the API reverts to the
     * original single-leg form on the next poll.
     *
     * **NotFound on disruption:**
     * If the disruption is severe enough that TfNSW drops the journey from the response entirely
     * (rather than restructuring it), this function returns null and the state transitions
     * to [xyz.ksharma.krail.feature.track.TrackTripState.NotFound].
     */
    @OptIn(ExperimentalTime::class)
    fun TripResponse.findMatchingJourney(deepLink: TripDeepLink): TripResponse.Journey? {
        val deepLinkInstant = runCatching { Instant.parse(deepLink.departureUtcDateTime) }.getOrNull()
        val count = journeys?.size ?: 0
        log(
            "TrackTrip: findMatchingJourney — $count journeys, " +
                "seeking legs=${deepLink.legs.map { it.transportationId }}, dep=${deepLink.departureUtcDateTime}",
        )

        val match = journeys?.firstOrNull { journey ->
            journey.matchesDeepLinkStrict(deepLink, deepLinkInstant)
        }

        if (match != null) {
            log("TrackTrip: findMatchingJourney — matched journey (strict leg IDs)")
            return match
        }

        // Fallback: TfNSW sometimes restructures a service mid-disruption — e.g. a 2-leg journey
        // (bus + T8) is replaced by a direct 1-leg service (T1). The original leg IDs no longer
        // appear in the response, so strict matching fails. Fall back to matching only by:
        //   1. Departure time from the same origin (±60 s)
        //   2. Same destination stop parent ID
        // This is looser but safe: two services departing at the same minute from the same origin
        // and terminating at the same destination are the same trip, just restructured.
        log("TrackTrip: findMatchingJourney — strict match failed, trying fallback (time + destination)")
        val fallback = journeys?.firstOrNull { journey ->
            journey.matchesDeepLinkFallback(deepLink, deepLinkInstant)
        }

        if (fallback == null) {
            log("TrackTrip: findMatchingJourney — no match found among $count journeys (strict + fallback)")
        } else {
            log("TrackTrip: findMatchingJourney — matched via fallback (service restructured by TfNSW)")
        }
        return fallback
    }

    @Suppress("ReturnCount")
    fun TripResponse.Journey.toTrackedJourneyDisplay(deepLink: TripDeepLink): TrackedJourneyDisplay? {
        val publicLegs = legs?.filter { !it.isWalkingLeg() && it.transportation != null }
        val firstLeg = publicLegs?.firstOrNull() ?: return null
        val lastLeg = publicLegs.lastOrNull() ?: return null

        val originEstimated = firstLeg.origin?.departureTimeEstimated
        val originPlanned = firstLeg.origin?.departureTimePlanned
        val originUtc = originEstimated ?: originPlanned ?: return null
        val destinationUtc = lastLeg.destination?.arrivalTimeEstimated
            ?: lastLeg.destination?.arrivalTimePlanned ?: return null

        val travelDuration = calculateTimeDifference(originUtc, destinationUtc)
        val scheduledOriginTime = if (originEstimated != null && originPlanned != null &&
            originEstimated != originPlanned
        ) {
            originPlanned.utcToDisplayTime()
        } else {
            null
        }

        val trackedLegs = legs?.mapNotNull { leg ->
            if (leg.isWalkingLeg()) {
                val durationSecs = leg.duration ?: return@mapNotNull null
                TrackedLeg.Walk(durationText = durationSecs.toFormattedSecondsString())
            } else {
                leg.toTrackedTransportLeg()
            }
        }?.toImmutableList() ?: return null

        return TrackedJourneyDisplay(
            fromStopId = deepLink.fromStopId,
            toStopId = deepLink.toStopId,
            fromStopName = deepLink.fromStopName,
            toStopName = deepLink.toStopName,
            originTime = originUtc.utcToDisplayTime(),
            scheduledOriginTime = scheduledOriginTime,
            destinationTime = destinationUtc.utcToDisplayTime(),
            originUtcDateTime = originUtc,
            destinationUtcDateTime = destinationUtc,
            travelTime = travelDuration.toFormattedDurationTimeString(),
            legs = trackedLegs,
            departureDeviation = firstLeg.toDepartureDeviation(),
        )
    }

    fun TripResponse.Leg.toTrackedTransportLeg(): TrackedLeg.Transport? {
        val productClass = transportation?.product?.productClass?.toInt() ?: return null
        val mode = NswTransportConfig.modeFromProductClass(productClass) ?: return null
        val lineName = transportation?.disassembledName ?: return null
        val lineColor = NswTransportLine.entries.firstOrNull { it.key == lineName }?.hexColor
            ?: mode.colorCode

        val stops = stopSequence?.mapNotNull { stop ->
            stop.toTrackedStop()
        }?.toImmutableList() ?: return null

        val routePathCoordinates = coords
            ?.mapNotNull { pair ->
                val lat = pair.getOrNull(0) ?: return@mapNotNull null
                val lon = pair.getOrNull(1) ?: return@mapNotNull null
                LatLng(latitude = lat, longitude = lon)
            }
            ?.toImmutableList()
            ?: kotlinx.collections.immutable.persistentListOf()

        return TrackedLeg.Transport(
            transportMode = mode,
            lineName = lineName,
            lineColorCode = lineColor,
            headsign = transportation?.destination?.name,
            stops = stops,
            realtimeTripId = transportation?.properties?.realtimeTripId,
            routePathCoordinates = routePathCoordinates,
        )
    }

    fun TripResponse.Leg.toDepartureDeviation(): DepartureDeviation? {
        val mins = departureDeviationMinutes() ?: return null
        return when {
            mins == 0L -> DepartureDeviation.OnTime
            mins > 0L -> DepartureDeviation.Late(
                "${mins.absoluteValue} ${if (mins.absoluteValue == 1L) "min" else "mins"} late",
            )
            else -> DepartureDeviation.Early(
                "${mins.absoluteValue} ${if (mins.absoluteValue == 1L) "min" else "mins"} early",
            )
        }
    }

    fun String.utcToDisplayTime(): String = utcToLocalDateTimeAEST().toHHMM()

    fun Long.toFormattedSecondsString(): String {
        val mins = this / SECONDS_PER_MINUTE
        return "$mins ${if (mins == 1L) "min" else "mins"}"
    }
}
