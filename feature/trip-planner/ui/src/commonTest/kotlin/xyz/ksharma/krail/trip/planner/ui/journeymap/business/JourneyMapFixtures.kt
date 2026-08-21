package xyz.ksharma.krail.trip.planner.ui.journeymap.business

import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse

/**
 * Plain NSW-shaped `TripResponse` builders for the journey-map mapper suites.
 *
 * These are data-class constructors, not boundary fakes — the mappers under test are pure
 * functions, so the only "input edge" is the API shape itself.
 */

internal fun leg(
    coords: List<List<Double>>? = null,
    interchange: TripResponse.Interchange? = null,
    transportation: TripResponse.Transportation? = null,
    stopSequence: List<TripResponse.StopSequence>? = null,
    origin: TripResponse.StopSequence? = null,
    destination: TripResponse.StopSequence? = null,
): TripResponse.Leg = TripResponse.Leg(
    coords = coords,
    interchange = interchange,
    transportation = transportation,
    stopSequence = stopSequence,
    origin = origin,
    destination = destination,
)

internal fun transportation(
    productClass: Long?,
    disassembledName: String? = null,
): TripResponse.Transportation = TripResponse.Transportation(
    disassembledName = disassembledName,
    product = productClass?.let { TripResponse.Product(productClass = it) },
)

// Mirrors the API shape: every field is independently optional, and the mapper's fallbacks
// are exactly what the tests exercise.
@Suppress("LongParameterList")
internal fun stopSequence(
    id: String? = "stop_id",
    name: String? = "Long Stop Name",
    disassembledName: String? = "Stop",
    lat: Double? = null,
    lon: Double? = null,
    parentLat: Double? = null,
    parentLon: Double? = null,
    platform: String? = null,
    platformName: String? = null,
    arrivalTimePlanned: String? = null,
    arrivalTimeEstimated: String? = null,
    departureTimePlanned: String? = null,
    departureTimeEstimated: String? = null,
): TripResponse.StopSequence = TripResponse.StopSequence(
    id = id,
    name = name,
    disassembledName = disassembledName,
    coord = if (lat != null && lon != null) listOf(lat, lon) else null,
    parent = if (parentLat != null && parentLon != null) {
        TripResponse.ParentLocation(coord = listOf(parentLat, parentLon))
    } else {
        null
    },
    properties = if (platform != null || platformName != null) {
        TripResponse.DestinationProperties(platform = platform, platformName = platformName)
    } else {
        null
    },
    arrivalTimePlanned = arrivalTimePlanned,
    arrivalTimeEstimated = arrivalTimeEstimated,
    departureTimePlanned = departureTimePlanned,
    departureTimeEstimated = departureTimeEstimated,
)
