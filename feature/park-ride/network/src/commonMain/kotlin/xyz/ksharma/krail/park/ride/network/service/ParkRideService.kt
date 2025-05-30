package xyz.ksharma.krail.park.ride.network.service

import xyz.ksharma.krail.park.ride.network.model.CarParkFacilityDetailResponse

/**
 * Swagger: https://opendata.transport.nsw.gov.au/data/dataset/car-park-api/resource/b880cae7-ed81-4d3e-9aba-b948d6626b20
 */
interface ParkRideService {

    /**
     * Returns the details of a car park facility by its ID.
     */
    suspend fun getCarParkFacilities(facilityId: String): CarParkFacilityDetailResponse

    /**
     * Returns a map of facility ID to facility name for all car parks.
     * Since facility ID is not specified, a list of facility names with their ID will be returned.
     */
    suspend fun getCarParkFacilities(): Map<String, String>
}