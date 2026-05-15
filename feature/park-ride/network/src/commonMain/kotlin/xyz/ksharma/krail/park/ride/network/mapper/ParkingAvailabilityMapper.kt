package xyz.ksharma.krail.park.ride.network.mapper

import app.krail.bff.proto.ApiError
import app.krail.bff.proto.FacilityAvailability
import app.krail.bff.proto.ParkingAvailabilityResponse
import app.krail.bff.proto.StopParkingBlock
import xyz.ksharma.krail.park.ride.network.model.BatchError
import xyz.ksharma.krail.park.ride.network.model.CarParkFacilityDetailResponse
import xyz.ksharma.krail.park.ride.network.model.Location
import xyz.ksharma.krail.park.ride.network.model.Occupancy
import xyz.ksharma.krail.park.ride.network.model.ParkingStopBatchResponse
import xyz.ksharma.krail.park.ride.network.model.StopFacilities

/**
 * Phase C consumer mapper: converts a Wire-decoded
 * [ParkingAvailabilityResponse] (from `/api/v1/parking/availability-proto`)
 * into the existing [ParkingStopBatchResponse] domain model so all
 * downstream consumers (notably SavedTripsViewModel) work unchanged.
 *
 * Only the `?stopIds=` mode is consumed: KRAIL hits the BFF with
 * `stopIds=...` so the proto's top-level `facilities` map and
 * `unknown_stops` list for `?ids=` mode are not relevant here. The
 * proto's top-level `facilities` map (populated in `?ids=` mode only)
 * is intentionally ignored.
 *
 * Field mapping summary:
 *  - [ParkingAvailabilityResponse.stops] (`Map<stopId, StopParkingBlock>`)
 *    becomes [ParkingStopBatchResponse.stops].
 *  - [StopParkingBlock.facilities] becomes [StopFacilities.facilities].
 *  - [FacilityAvailability.facility_id] / [FacilityAvailability.facility_name]
 *    populate [CarParkFacilityDetailResponse.facilityId] /
 *    [CarParkFacilityDetailResponse.facilityName].
 *  - [FacilityAvailability.total_spots] (Int) is stringified into
 *    [CarParkFacilityDetailResponse.spots]. The existing model uses
 *    Strings because NSW returns numbers as strings; preserve that
 *    contract on the proto path.
 *  - [FacilityAvailability.occupied_spots] (Int) is stringified into
 *    [Occupancy.total].
 *  - [FacilityAvailability.location] (proto `Coord{lat, lon}`) populates
 *    [Location.latitude] / [Location.longitude] (as Strings).
 *  - [FacilityAvailability.suburb] / [FacilityAvailability.address]
 *    populate [Location.suburb] / [Location.address].
 *  - [FacilityAvailability.updated_at] populates
 *    [CarParkFacilityDetailResponse.messageDate].
 *  - [StopParkingBlock.errors] (`Map<String, ApiError>`) becomes
 *    [StopFacilities.errors] (`Map<String, BatchError>`).
 *  - [ParkingAvailabilityResponse.unknown_stops] becomes
 *    [ParkingStopBatchResponse.unknownStops].
 *  - [ParkingAvailabilityResponse.correlation_id] becomes
 *    [ParkingStopBatchResponse.correlationId].
 *
 * Acceptable gaps (proto v0.3.0 does not carry an equivalent; mapper
 * supplies safe defaults so the existing JSON model still constructs):
 *  - NSW-internal IDs (`tsn`, `time`, `parkID`, `tfnswFacilityId`) have
 *    no proto carrier. They are not used by any KRAIL screen, so the
 *    mapper sets `tsn` and `time` to empty strings, `parkID` to 0, and
 *    `tfnswFacilityId` to an empty string.
 *  - Per-zone breakdown (`zones[]`) is not in proto - proto carries
 *    only aggregate totals. No app screen renders per-zone today, so
 *    the mapper emits an empty list.
 *  - Detailed occupancy sub-fields (`loop`, `monthlies`, `transients`,
 *    `openGate`) are not in proto - only aggregate `occupied_spots`
 *    maps to [Occupancy.total]. The other sub-fields stay null.
 */
internal fun ParkingAvailabilityResponse.toStopBatchResponse(): ParkingStopBatchResponse {
    return ParkingStopBatchResponse(
        stops = stops.mapValues { (_, block) -> block.toStopFacilities() },
        unknownStops = unknown_stops,
        correlationId = correlation_id.takeIf { it.isNotEmpty() },
    )
}

private fun StopParkingBlock.toStopFacilities(): StopFacilities {
    return StopFacilities(
        facilities = facilities.mapValues { (_, fa) -> fa.toCarParkFacilityDetailResponse() },
        errors = errors.mapValues { (_, err) -> err.toBatchError() },
    )
}

private fun FacilityAvailability.toCarParkFacilityDetailResponse(): CarParkFacilityDetailResponse {
    return CarParkFacilityDetailResponse(
        // NSW-internal IDs without a proto carrier. Empty defaults keep
        // the data class constructible while documenting the gap.
        tsn = "",
        time = "",
        spots = total_spots.toString(),
        zones = emptyList(),
        parkID = 0,
        location = Location(
            suburb = suburb,
            address = address,
            latitude = location?.lat?.toString(),
            longitude = location?.lon?.toString(),
        ),
        occupancy = Occupancy(
            loop = null,
            total = occupied_spots.toString(),
            monthlies = null,
            openGate = null,
            transients = null,
        ),
        messageDate = updated_at.orEmpty(),
        facilityId = facility_id,
        facilityName = facility_name,
        tfnswFacilityId = "",
    )
}

private fun ApiError.toBatchError(): BatchError {
    return BatchError(
        code = code,
        message = message,
    )
}
