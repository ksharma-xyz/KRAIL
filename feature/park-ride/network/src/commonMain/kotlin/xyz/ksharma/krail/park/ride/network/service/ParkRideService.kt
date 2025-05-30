package xyz.ksharma.krail.park.ride.network.service

import xyz.ksharma.krail.park.ride.network.model.CarParkFacilitiesResponse
import xyz.ksharma.krail.park.ride.network.model.CarParkFacilityDetailResponse

/**
 * Swagger: https://opendata.transport.nsw.gov.au/data/dataset/car-park-api/resource/b880cae7-ed81-4d3e-9aba-b948d6626b20
 */
interface ParkRideService {

    suspend fun getCarParkFacilities(facilityId: String): CarParkFacilityDetailResponse

    suspend fun getCarParkFacilities(): CarParkFacilitiesResponse
}