package xyz.ksharma.krail.park.ride.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CarParkFacilitiesResponse(

    @SerialName("facilities")
    val facilities: Map<String, String>
)

@Serializable
data class CarParkFacilityDetailResponse(

    @SerialName("tsn")
    val tsn: String,

    @SerialName("time")
    val time: String,

    @SerialName("spots")
    val spots: String,

    @SerialName("zones")
    val zones: List<Zone>,

    @SerialName("ParkID")
    val parkID: Int,

    @SerialName("location")
    val location: Location,

    @SerialName("occupancy")
    val occupancy: Occupancy,

    @SerialName("MessageDate")
    val messageDate: String,

    @SerialName("facility_id")
    val facilityId: String,

    @SerialName("facility_name")
    val facilityName: String,

    @SerialName("tfnsw_facility_id")
    val tfnswFacilityId: String
)

@Serializable
data class Zone(

    @SerialName("spots")
    val spots: String,

    @SerialName("zone_id")
    val zoneId: String,

    @SerialName("occupancy")
    val occupancy: Occupancy,

    @SerialName("zone_name")
    val zoneName: String,

    @SerialName("parent_zone_id")
    val parentZoneId: String
)

@Serializable
data class Occupancy(

    @SerialName("loop")
    val loop: String?,

    @SerialName("total")
    val total: String?,

    @SerialName("monthlies")
    val monthlies: String?,

    @SerialName("open_gate")
    val openGate: String?,

    @SerialName("transients")
    val transients: String?
)

@Serializable
data class Location(

    @SerialName("suburb")
    val suburb: String,

    @SerialName("address")
    val address: String,

    @SerialName("latitude")
    val latitude: String,

    @SerialName("longitude")
    val longitude: String
)
