package xyz.ksharma.krail.trip.planner.network.api.mapper

import app.krail.bff.proto.Coord
import app.krail.bff.proto.JourneyCardInfo
import app.krail.bff.proto.JourneyList
import app.krail.bff.proto.Leg
import app.krail.bff.proto.Stop
import app.krail.bff.proto.TransportLeg
import app.krail.bff.proto.TransportModeLine
import app.krail.bff.proto.WalkInterchange
import app.krail.bff.proto.WalkPosition
import app.krail.bff.proto.WalkingLeg
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse

/**
 * Phase C consumer mapper: converts a Wire-decoded [JourneyList] (from
 * `/api/v1/trip/plan-proto`) into the existing [TripResponse] domain model
 * so all downstream UI mappers work unchanged.
 *
 * Polyline data (the bug fix that motivates Phase C) is propagated:
 *  - [TransportLeg.coords] and [WalkingLeg.coords] populate [TripResponse.Leg.coords].
 *  - [WalkInterchange.coords] populate [TripResponse.Interchange.coords].
 *  - [Stop.coord] populates [TripResponse.StopSequence.coord].
 *
 * Acceptable gaps (proto v0.3.0 does not carry an equivalent; mapper leaves
 * the corresponding [TripResponse] field null/empty):
 *  - Per-leg UTC dep/arr times for intermediate legs. Proto exposes UTC only
 *    at the journey level (origin/destination of the whole journey). The
 *    first leg's origin and the last leg's destination get UTC times from
 *    the journey-level fields; intermediate stops carry only display strings
 *    via [Stop.time].
 *  - [TripResponse.Leg.duration] in seconds. Proto carries display strings
 *    ("5 mins") instead. UI's resolveDurationSeconds() falls back to
 *    computing from dep/arr UTC where available.
 *  - [TripResponse.Leg.infos] (service alerts beyond id/subtitle/content/priority/url).
 *    Proto's [app.krail.bff.proto.ServiceAlert] is a leaner shape; the
 *    mapper translates the available fields and leaves richer NSW-only
 *    fields (timestamps, version, urlText) null.
 *  - [TripResponse.Leg.hints], [TripResponse.Leg.footPathInfo],
 *    [TripResponse.Leg.distance], [TripResponse.Leg.isRealtimeControlled].
 *  - [TripResponse.Journey.error], [TripResponse.systemMessages]. The proto
 *    path returns either a successful [JourneyList] or an HTTP error; there
 *    is no in-band error envelope.
 *
 * The mapper is defensive: every nullable [TripResponse] field that the proto
 * omits stays null. It does not throw on missing optional fields. Required
 * proto fields (per the contract convention) arriving empty are tolerated
 * (treated as missing) rather than throwing, so the polyline path still
 * renders for partially-populated upstream data.
 */
internal fun journeyListToTripResponse(list: JourneyList): TripResponse {
    return TripResponse(
        journeys = list.journeys.map { it.toJourney() },
    )
}

private const val WALKING_LEG_PRODUCT_CLASS = 99L

private fun JourneyCardInfo.toJourney(): TripResponse.Journey {
    val legCount = legs.size
    val mappedLegs = legs.mapIndexed { index, leg ->
        leg.toLeg(
            isFirstLeg = index == 0,
            isLastLeg = index == legCount - 1,
            journeyOriginUtc = origin_utc_date_time.takeIf { it.isNotEmpty() },
            journeyDestinationUtc = destination_utc_date_time.takeIf { it.isNotEmpty() },
        )
    }
    return TripResponse.Journey(legs = mappedLegs)
}

private fun Leg.toLeg(
    isFirstLeg: Boolean,
    isLastLeg: Boolean,
    journeyOriginUtc: String?,
    journeyDestinationUtc: String?,
): TripResponse.Leg {
    val transport = transport_leg
    val walk = walking_leg
    return when {
        transport != null -> transport.toTransportTripLeg(
            isFirstLeg = isFirstLeg,
            isLastLeg = isLastLeg,
            journeyOriginUtc = journeyOriginUtc,
            journeyDestinationUtc = journeyDestinationUtc,
        )
        walk != null -> walk.toWalkingTripLeg()
        else -> TripResponse.Leg()
    }
}

private fun TransportLeg.toTransportTripLeg(
    isFirstLeg: Boolean,
    isLastLeg: Boolean,
    journeyOriginUtc: String?,
    journeyDestinationUtc: String?,
): TripResponse.Leg {
    val protoStops = stops
    val protoFirstStop = protoStops.firstOrNull()
    val protoLastStop = protoStops.lastOrNull()

    val originStop = protoFirstStop?.toStopSequence(
        plannedDeparture = if (isFirstLeg) journeyOriginUtc else null,
        plannedArrival = null,
    )
    val destinationStop = protoLastStop?.toStopSequence(
        plannedDeparture = null,
        plannedArrival = if (isLastLeg) journeyDestinationUtc else null,
    )

    val stopSequence = if (protoStops.size >= 2) {
        buildList {
            originStop?.let { add(it) }
            // Intermediate stops carry only display time strings.
            for (i in 1 until protoStops.size - 1) {
                add(protoStops[i].toStopSequence())
            }
            destinationStop?.let { add(it) }
        }
    } else {
        listOfNotNull(originStop, destinationStop)
    }

    return TripResponse.Leg(
        origin = originStop,
        destination = destinationStop,
        stopSequence = stopSequence,
        transportation = transport_mode_line?.toTransportation(
            displayText = display_text,
            // v0.4.1 split ids: journey identity/dedupe + the live-tracking
            // lock key. Without them every proto journey deduped onto one
            // card ("nullnull" trip codes).
            transportationId = transportation_id,
            realtimeTripId = realtime_trip_id,
        ),
        coords = coords.toLatLngList(),
        interchange = walk_interchange?.toInterchange(),
        // Proto carries the render-ready duration string; without this the
        // UI leg mapper derives duration from per-leg UTC times the proto
        // doesn't have and drops the leg entirely.
        bffDisplayDuration = total_duration.takeIf { it.isNotEmpty() },
        infos = service_alert_list
            .map { alert ->
                TripResponse.Info(
                    id = alert.id,
                    subtitle = alert.subtitle,
                    content = alert.content,
                    priority = alert.priority,
                    url = alert.url,
                )
            }
            .takeIf { it.isNotEmpty() },
    )
}

private fun WalkingLeg.toWalkingTripLeg(): TripResponse.Leg {
    return TripResponse.Leg(
        coords = coords.toLatLngList(),
        // A synthetic Transportation block with productClass 99 keeps NSW-side
        // helpers (e.g. TripResponseExt.isWalkingLeg) working on the proto
        // path. The display name is left null; the UI surfaces walk legs by
        // their duration string, not their line label.
        transportation = TripResponse.Transportation(
            product = TripResponse.Product(productClass = WALKING_LEG_PRODUCT_CLASS),
        ),
        bffDisplayDuration = duration.takeIf { it.isNotEmpty() },
    )
}

private fun TransportModeLine.toTransportation(
    displayText: String?,
    transportationId: String? = null,
    realtimeTripId: String? = null,
): TripResponse.Transportation {
    return TripResponse.Transportation(
        id = transportationId,
        disassembledName = line_name.takeIf { it.isNotEmpty() },
        name = line_name.takeIf { it.isNotEmpty() },
        description = displayText,
        product = TripResponse.Product(
            productClass = transport_mode_type.toLong(),
            name = line_name.takeIf { it.isNotEmpty() },
        ),
        properties = realtimeTripId?.let { TripResponse.TransportationProperties(realtimeTripId = it) },
    )
}

private fun Stop.toStopSequence(
    plannedDeparture: String? = null,
    plannedArrival: String? = null,
): TripResponse.StopSequence {
    return TripResponse.StopSequence(
        name = name.takeIf { it.isNotEmpty() },
        disassembledName = name.takeIf { it.isNotEmpty() },
        coord = coord?.toLatLngArray(),
        departureTimePlanned = plannedDeparture,
        departureTimeEstimated = plannedDeparture,
        arrivalTimePlanned = plannedArrival,
        arrivalTimeEstimated = plannedArrival,
        // Proto stop times are render-ready display strings ("12:05pm");
        // without this, stops lacking UTC times vanish from the timeline.
        bffDisplayTime = time.takeIf { it.isNotEmpty() },
    )
}

private fun WalkInterchange.toInterchange(): TripResponse.Interchange {
    return TripResponse.Interchange(
        type = when (position) {
            WalkPosition.IDEST -> WALKING_LEG_PRODUCT_CLASS
            else -> null
        },
        coords = coords.toLatLngList(),
        desc = duration.takeIf { it.isNotEmpty() },
    )
}

/**
 * Converts a list of proto [Coord] points to the NSW JSON-shape
 * `[[lat, lon], [lat, lon], ...]` array the existing UI expects.
 * Returns null when the proto list is empty so downstream null-checks
 * (e.g. JourneyMapMapper.createLegFromCoordinates) short-circuit cleanly.
 */
private fun List<Coord>.toLatLngList(): List<List<Double>>? {
    if (isEmpty()) return null
    return map { it.toLatLngArray() }
}

private fun Coord.toLatLngArray(): List<Double> = listOf(lat, lon)
