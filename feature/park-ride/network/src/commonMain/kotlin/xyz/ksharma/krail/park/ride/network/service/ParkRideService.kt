package xyz.ksharma.krail.park.ride.network.service

import xyz.ksharma.krail.park.ride.network.model.CarParkFacilityDetailResponse
import xyz.ksharma.krail.park.ride.network.model.ParkingStopBatchResponse

/**
 * Swagger: https://opendata.transport.nsw.gov.au/data/dataset/car-park-api/resource/b880cae7-ed81-4d3e-9aba-b948d6626b20
 * API documentation: https://opendata.transport.nsw.gov.au/data/dataset/car-park-api/resource/1cd0b16d-d9f8-4973-93cd-3c6f66a3e99c
 */
interface ParkRideService {

    /**
     * Returns the details of a car park facility by its ID.
     */
    suspend fun fetchCarParkFacilities(facilityId: String): Result<CarParkFacilityDetailResponse>

    /**
     * Returns a map of facility ID to facility name for all car parks.
     * Since facility ID is not specified, a list of facility names with their ID will be returned.
     */
    suspend fun fetchCarParkFacilities(): Result<Map<String, String>>

    /**
     * Batch fetch availability for multiple saved-trip stop IDs in one HTTP call
     * (BFF override path only). The BFF resolves each stop to its NSW
     * park-and-ride facilities server-side and fans out the per-facility
     * NSW calls concurrently, so the app collapses N HTTP round trips
     * into 1 (see `KRAIL-BFF/docs/handover/PARK_RIDE_BATCH_HANDOVER.md`
     * §3.2 for the rationale).
     *
     * Falls back to per-facility NSW direct calls when the BFF override
     * is off, since there is no equivalent batch endpoint on NSW. In
     * that case this method returns `null` and the caller is expected
     * to orchestrate the existing per-facility fan-out.
     *
     * @param stopIds Saved-trip stop IDs (NSW namespace prefix optional;
     *   the BFF strips it). 1 to 20 IDs per call (BFF cap). An empty
     *   list short-circuits to an empty response without firing a
     *   request. More than 20 IDs are silently truncated to the first
     *   20 to mirror the BFF's cap behaviour.
     * @return A [ParkingStopBatchResponse] when the BFF override is on,
     *   or `null` when the override is off (caller must use the NSW
     *   per-facility path).
     */
    suspend fun fetchAvailabilityForStops(
        stopIds: List<String>,
    ): ParkingStopBatchResponse?
}
