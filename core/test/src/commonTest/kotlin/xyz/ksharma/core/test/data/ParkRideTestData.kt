package xyz.ksharma.core.test.data

import xyz.ksharma.krail.park.ride.network.model.CarParkFacilityDetailResponse
import xyz.ksharma.krail.park.ride.network.model.Location
import xyz.ksharma.krail.park.ride.network.model.Occupancy
import xyz.ksharma.krail.park.ride.network.model.Zone

fun buildOccupancy(
    loop: String? = "32707",
    total: String? = "200",
    monthlies: String? = null,
    openGate: String? = null,
    transients: String? = "100"
) = Occupancy(loop, total, monthlies, openGate, transients)

fun buildZone(
    spots: String = "774",
    zoneId: String = "1",
    occupancy: Occupancy = buildOccupancy(),
    zoneName: String = "SYD392 Bella Vista Car Park",
    parentZoneId: String = "0"
) = Zone(spots, zoneId, occupancy, zoneName, parentZoneId)

fun buildLocation(
    suburb: String = "Bella Vista",
    address: String = "Byles Place",
    latitude: String = "-33.727438",
    longitude: String = "150.941761"
) = Location(suburb, address, latitude, longitude)

fun buildCarParkFacilityDetailResponse(
    tsn: String = "2153478",
    time: String = "803037917",
    spots: String = "774",
    zones: List<Zone> = listOf(buildZone()),
    parkID: Int = 1,
    location: Location = buildLocation(),
    occupancy: Occupancy = buildOccupancy(),
    messageDate: String = "2025-06-12T20:05:17",
    facilityId: String = "31",
    facilityName: String = "Park&Ride - Bella Vista",
    tfnswFacilityId: String = "2153478TPR001"
) = CarParkFacilityDetailResponse(
    tsn,
    time,
    spots,
    zones,
    parkID,
    location,
    occupancy,
    messageDate,
    facilityId,
    facilityName,
    tfnswFacilityId
)

val facilityResponse = buildCarParkFacilityDetailResponse(
    facilityName = "Test Facility",
    spots = "1000",
    occupancy = buildOccupancy(transients = "200")
)
