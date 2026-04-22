package xyz.ksharma.krail.feature.track.ui

import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.datetime.DateTimeHelper.calculateTimeDifference
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toFormattedDurationTimeString
import xyz.ksharma.krail.core.datetime.DateTimeHelper.toHHMM
import xyz.ksharma.krail.core.datetime.DateTimeHelper.utcToLocalDateTimeAEST
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.core.transport.nsw.NswTransportLine
import xyz.ksharma.krail.feature.track.DepartureDeviation
import xyz.ksharma.krail.feature.track.TrackedJourneyDisplay
import xyz.ksharma.krail.feature.track.TrackedLeg
import xyz.ksharma.krail.feature.track.TrackedStop
import xyz.ksharma.krail.feature.track.TripDeepLink
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.network.api.model.departureDeviationMinutes
import xyz.ksharma.krail.trip.planner.network.api.model.isWalkingLeg
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import xyz.ksharma.krail.core.log.log

internal object TripResponseMapper {

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
                "seeking legs=${deepLink.legs.map { it.transportationId }}, dep=${deepLink.departureUtcDateTime}"
        )

        val match = journeys?.firstOrNull { journey ->
            val publicLegs = journey.legs
                ?.filter { it.transportation != null && !it.isWalkingLeg() }
                ?: return@firstOrNull false

            val legIds = publicLegs.mapNotNull { it.transportation?.id }
            val firstDep = publicLegs.firstOrNull()?.origin?.departureTimePlanned

            // All deep-link transportation IDs must appear in the journey legs.
            val idsMatch = deepLink.legs.all { dl -> legIds.contains(dl.transportationId) }
            if (!idsMatch) return@firstOrNull false

            // Departure time: parse both as Instant so string-format differences don't break the
            // match (e.g. "2026-04-22T22:35:00Z" vs "2026-04-22T22:35:00.000Z").
            // Allow 60 s tolerance for any minor rounding in the API response.
            val timeMatch = if (deepLinkInstant != null && firstDep != null) {
                val legInstant = runCatching { Instant.parse(firstDep) }.getOrNull()
                legInstant != null && (legInstant - deepLinkInstant).absoluteValue < 60.seconds
            } else {
                firstDep == deepLink.departureUtcDateTime
            }

            log("TrackTrip:   journey ids=$legIds firstDep=$firstDep → idsMatch=$idsMatch timeMatch=$timeMatch")
            timeMatch
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
            val publicLegs = journey.legs
                ?.filter { it.transportation != null && !it.isWalkingLeg() }
                ?: return@firstOrNull false

            val firstDep = publicLegs.firstOrNull()?.origin?.departureTimePlanned
            val lastDestId = publicLegs.lastOrNull()?.destination?.parent?.id
                ?: publicLegs.lastOrNull()?.destination?.id

            val timeMatch = if (deepLinkInstant != null && firstDep != null) {
                val legInstant = runCatching { Instant.parse(firstDep) }.getOrNull()
                legInstant != null && (legInstant - deepLinkInstant).absoluteValue < 60.seconds
            } else false

            val destMatch = lastDestId == deepLink.toStopId

            log("TrackTrip:   fallback journey firstDep=$firstDep lastDestId=$lastDestId → time=$timeMatch dest=$destMatch")
            timeMatch && destMatch
        }

        if (fallback == null) {
            log("TrackTrip: findMatchingJourney — no match found among $count journeys (strict + fallback)")
        } else {
            log("TrackTrip: findMatchingJourney — matched via fallback (service restructured by TfNSW)")
        }
        return fallback
    }

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
        } else null

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
            val name = stop.disassembledName ?: stop.name ?: return@mapNotNull null
            val scheduledUtc = stop.departureTimePlanned ?: stop.arrivalTimePlanned
            val estimatedUtc = stop.departureTimeEstimated ?: stop.arrivalTimeEstimated
            val timeUtc = estimatedUtc ?: scheduledUtc ?: return@mapNotNull null
            TrackedStop(
                stopId = stop.id.orEmpty(),
                name = name,
                scheduledTime = (scheduledUtc ?: timeUtc).utcToDisplayTime(),
                estimatedTime = estimatedUtc
                    ?.takeIf { it != scheduledUtc }
                    ?.utcToDisplayTime(),
                utcTime = timeUtc,
                scheduledUtcTime = scheduledUtc ?: timeUtc,
                // API coord is [latitude, longitude]
                lat = stop.coord?.getOrNull(0),
                lon = stop.coord?.getOrNull(1),
            )
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
        val mins = this / 60
        return "$mins ${if (mins == 1L) "min" else "mins"}"
    }
}
