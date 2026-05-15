package xyz.ksharma.krail.departures.network.api.mapper

import app.krail.bff.proto.DepartureBoardResponse
import app.krail.bff.proto.DepartureRow
import app.krail.bff.proto.StopRef
import app.krail.bff.proto.TransitLine
import xyz.ksharma.krail.departures.network.api.model.DepartureMonitorResponse

/**
 * Phase C consumer mapper: converts a Wire-decoded [DepartureBoardResponse]
 * (from `/api/v1/stops/{stopId}/departures-proto`) into the existing
 * [DepartureMonitorResponse] domain model so all downstream UI mappers and
 * UI render unchanged.
 *
 * Field mapping summary:
 *  - [DepartureRow.line.display_name] populates [DepartureMonitorResponse.Transportation.disassembledName]
 *    and [DepartureMonitorResponse.Transportation.name] (used as the line label, e.g. "T1").
 *  - [DepartureRow.line.transport_mode_type] populates
 *    [DepartureMonitorResponse.Product.cls] (productClass, e.g. 1 = Train).
 *  - [DepartureRow.destination] populates
 *    [DepartureMonitorResponse.Destination.name].
 *  - [DepartureRow.time.planned_utc] populates [DepartureMonitorResponse.StopEvent.departureTimePlanned].
 *  - [DepartureRow.time.estimated_utc] (when present) populates [DepartureMonitorResponse.StopEvent.departureTimeEstimated].
 *  - [DepartureRow.platform_text] populates [DepartureMonitorResponse.Location.disassembledName]
 *    (the existing UI uses this for the platform label, e.g. "Platform 1", "Stand A").
 *  - [StopRef.id] / [StopRef.name] populate the per-event
 *    [DepartureMonitorResponse.Location.id] / [DepartureMonitorResponse.Location.name]
 *    (proto carries the stop once at the response root; the JSON model
 *    repeats it per stop event).
 *
 * Acceptable gaps (proto v0.3.0 does not carry an equivalent; mapper leaves
 * the corresponding [DepartureMonitorResponse] field null/empty):
 *  - [DepartureRow.is_realtime] has no direct sink in [DepartureMonitorResponse]
 *    (no isRealtimeControlled or realtimeStatus field); the existing JSON
 *    model also omits these. KRAIL infers realtime from the presence of
 *    [DepartureMonitorResponse.StopEvent.departureTimeEstimated] today, so
 *    behaviour matches.
 *  - [DepartureRow.trip_id] has no sink in the existing model.
 *  - [DepartureRow.date_label] is a presentation hint (e.g. "today" /
 *    "tomorrow"); KRAIL computes its own date labels client-side.
 *  - [TransitLine.color_hex] is unmapped; KRAIL renders mode color from
 *    productClass anyway.
 *  - [DepartureMonitorResponse.error] / [DepartureMonitorResponse.version]
 *    stay null; the proto path returns either a successful response or an
 *    HTTP error, with no in-band error envelope.
 */
internal fun DepartureBoardResponse.toDepartureMonitorResponse(): DepartureMonitorResponse {
    val protoStop = stop
    return DepartureMonitorResponse(
        stopEvents = departures.map { it.toStopEvent(protoStop) },
    )
}

private fun DepartureRow.toStopEvent(
    stopRef: StopRef?,
): DepartureMonitorResponse.StopEvent {
    return DepartureMonitorResponse.StopEvent(
        departureTimePlanned = time?.planned_utc?.takeIf { it.isNotEmpty() },
        departureTimeEstimated = time?.estimated_utc?.takeIf { it.isNotEmpty() },
        location = stopRef?.toLocation(platformText = platform_text),
        transportation = line?.toTransportation(destinationName = destination),
    )
}

private fun StopRef.toLocation(
    platformText: String?,
): DepartureMonitorResponse.Location {
    return DepartureMonitorResponse.Location(
        id = id.takeIf { it.isNotEmpty() },
        name = name.takeIf { it.isNotEmpty() },
        // The existing UI reads platform from disassembledName first; keep
        // that contract on the proto path.
        disassembledName = platformText,
    )
}

private fun TransitLine.toTransportation(
    destinationName: String,
): DepartureMonitorResponse.Transportation {
    return DepartureMonitorResponse.Transportation(
        disassembledName = display_name.takeIf { it.isNotEmpty() },
        name = display_name.takeIf { it.isNotEmpty() },
        destination = DepartureMonitorResponse.Destination(
            name = destinationName.takeIf { it.isNotEmpty() },
        ),
        product = DepartureMonitorResponse.Product(
            cls = transport_mode_type,
            name = display_name.takeIf { it.isNotEmpty() },
        ),
    )
}
