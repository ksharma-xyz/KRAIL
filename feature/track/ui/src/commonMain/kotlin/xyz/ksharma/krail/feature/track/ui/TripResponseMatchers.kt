package xyz.ksharma.krail.feature.track.ui

import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.feature.track.TrackedStop
import xyz.ksharma.krail.feature.track.TripDeepLink
import xyz.ksharma.krail.feature.track.ui.TripResponseMapper.utcToDisplayTime
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.network.api.model.isWalkingLeg
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Strict deep-link match: all deep-link transportation IDs must appear in the journey legs
 * AND the first leg's planned departure must match the deep link's departure time (±60 s).
 */
@OptIn(ExperimentalTime::class)
internal fun TripResponse.Journey.matchesDeepLinkStrict(
    deepLink: TripDeepLink,
    deepLinkInstant: Instant?,
): Boolean {
    val publicLegs = legs
        ?.filter { it.transportation != null && !it.isWalkingLeg() }
        ?: return false

    val legIds = publicLegs.mapNotNull { it.transportation?.id }
    val firstDep = publicLegs.firstOrNull()?.origin?.departureTimePlanned

    // All deep-link transportation IDs must appear in the journey legs.
    val idsMatch = deepLink.legs.all { dl -> legIds.contains(dl.transportationId) }
    if (!idsMatch) return false

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
    return timeMatch
}

/**
 * Fallback deep-link match when TfNSW restructures a service mid-disruption and the original
 * leg IDs no longer appear. Matches only by departure time (±60 s) from the same origin and
 * the same destination stop parent ID.
 */
@OptIn(ExperimentalTime::class)
internal fun TripResponse.Journey.matchesDeepLinkFallback(
    deepLink: TripDeepLink,
    deepLinkInstant: Instant?,
): Boolean {
    val publicLegs = legs
        ?.filter { it.transportation != null && !it.isWalkingLeg() }
        ?: return false

    val firstDep = publicLegs.firstOrNull()?.origin?.departureTimePlanned
    val lastDestId = publicLegs.lastOrNull()?.destination?.parent?.id
        ?: publicLegs.lastOrNull()?.destination?.id

    val timeMatch = if (deepLinkInstant != null && firstDep != null) {
        val legInstant = runCatching { Instant.parse(firstDep) }.getOrNull()
        legInstant != null && (legInstant - deepLinkInstant).absoluteValue < 60.seconds
    } else {
        false
    }

    val destMatch = lastDestId == deepLink.toStopId

    log(
        "TrackTrip:   fallback journey firstDep=$firstDep " +
            "lastDestId=$lastDestId → time=$timeMatch dest=$destMatch",
    )
    return timeMatch && destMatch
}

/**
 * Maps a single API stop in a leg's stop sequence into a [TrackedStop], or null when the stop
 * lacks a usable name or time.
 */
internal fun TripResponse.StopSequence.toTrackedStop(): TrackedStop? {
    val name = disassembledName ?: name ?: return null
    val scheduledUtc = departureTimePlanned ?: arrivalTimePlanned
    val estimatedUtc = departureTimeEstimated ?: arrivalTimeEstimated
    val timeUtc = estimatedUtc ?: scheduledUtc ?: return null
    return TrackedStop(
        stopId = id.orEmpty(),
        name = name,
        scheduledTime = (scheduledUtc ?: timeUtc).utcToDisplayTime(),
        estimatedTime = estimatedUtc
            ?.takeIf { it != scheduledUtc }
            ?.utcToDisplayTime(),
        utcTime = timeUtc,
        scheduledUtcTime = scheduledUtc ?: timeUtc,
        // API coord is [latitude, longitude]
        lat = coord?.getOrNull(0),
        lon = coord?.getOrNull(1),
    )
}
