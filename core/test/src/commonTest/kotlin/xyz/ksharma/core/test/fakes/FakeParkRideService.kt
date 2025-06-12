package xyz.ksharma.core.test.fakes

import xyz.ksharma.krail.park.ride.network.model.CarParkFacilityDetailResponse
import xyz.ksharma.krail.park.ride.network.model.Location
import xyz.ksharma.krail.park.ride.network.model.Occupancy
import xyz.ksharma.krail.park.ride.network.model.Zone
import xyz.ksharma.krail.park.ride.network.service.ParkRideService

class FakeParkRideService(
    private val facilityResponses: Map<String, CarParkFacilityDetailResponse> = Companion.facilityResponses,
) : ParkRideService {

    override suspend fun getCarParkFacilities(facilityId: String): CarParkFacilityDetailResponse {
        return this@FakeParkRideService.facilityResponses[facilityId]
            ?: error("No fake response for facilityId: $facilityId")
    }

    override suspend fun getCarParkFacilities(): Map<String, String> {
        return this@FakeParkRideService.facilityResponses.mapValues { it.value.facilityName }
    }

    companion object {
        val facilityResponses = mapOf(
            "31" to CarParkFacilityDetailResponse(
                tsn = "2153478",
                time = "803037917",
                spots = "774",
                zones = listOf(
                    Zone(
                        spots = "774",
                        zoneId = "1",
                        occupancy = Occupancy(
                            loop = "32707",
                            total = "200",
                            monthlies = null,
                            openGate = null,
                            transients = "100"
                        ),
                        zoneName = "SYD392 Bella Vista Car Park",
                        parentZoneId = "0"
                    )
                ),
                parkID = 1,
                location = Location(
                    suburb = "Bella Vista",
                    address = "Byles Place",
                    latitude = "-33.727438",
                    longitude = "150.941761"
                ),
                occupancy = Occupancy(
                    loop = "32707",
                    total = "200",
                    monthlies = null,
                    openGate = null,
                    transients = "100"
                ),
                messageDate = "2025-06-12T20:05:17",
                facilityId = "31",
                facilityName = "Park&Ride - Bella Vista",
                tfnswFacilityId = "2153478TPR001"
            ),
            "42" to CarParkFacilityDetailResponse(
                tsn = "9999999",
                time = "803037999",
                spots = "500",
                zones = listOf(
                    Zone(
                        spots = "500",
                        zoneId = "2",
                        occupancy = Occupancy(
                            loop = "12345",
                            total = "50",
                            monthlies = "10",
                            openGate = null,
                            transients = "40"
                        ),
                        zoneName = "SYD400 Norwest Car Park",
                        parentZoneId = "0"
                    )
                ),
                parkID = 2,
                location = Location(
                    suburb = "Norwest",
                    address = "Norwest Blvd",
                    latitude = "-33.733333",
                    longitude = "150.950000"
                ),
                occupancy = Occupancy(
                    loop = "12345",
                    total = "50",
                    monthlies = "10",
                    openGate = null,
                    transients = "40"
                ),
                messageDate = "2025-06-13T10:00:00",
                facilityId = "42",
                facilityName = "Park&Ride - Norwest",
                tfnswFacilityId = "9999999TPR002"
            )
        )
    }
}
