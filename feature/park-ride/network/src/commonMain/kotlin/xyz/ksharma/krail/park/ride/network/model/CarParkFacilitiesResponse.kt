package xyz.ksharma.krail.park.ride.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CarParkFacilityDetailResponse(

    /**
     * This respresents the  GTFS Stop ID.
     */
    @SerialName("tsn")
    val tsn: String,

    /**
     * e.g. 803068391
     */
    @SerialName("time")
    val time: String,

    /**
     * The total number of parking spots available in the facility.
     * E.g. "spots": "100",
     */
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

    /**
     * This represents the unique identifier for the facility.
     * Use this value as parameter for the API endpoint
     * E.g.   "facility_id": "488",
     */
    @SerialName("facility_id")
    val facilityId: String,

    /**
     * This represents the name of the facility.
     * E.g.  "facility_name": "Park&Ride - Seven Hills",
     */
    @SerialName("facility_name")
    val facilityName: String,

    /**
     * "tfnsw_facility_id": "214710TPR001"
     */
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

/**
 * Represents occupancy details for a parking facility or zone.
 */
@Serializable
data class Occupancy(
    /** Number of vehicles detected by loop sensors (real-time count).
     * Mostly the data here is null, as sensors are not accurate.  */
    @SerialName("loop")
    val loop: String?,

    /** Total number of parking spots available. */
    @SerialName("total")
    val total: String?,

    /** Number of spots reserved for monthly permit holders. */
    @SerialName("monthlies")
    val monthlies: String?,

    /** Number of spots accessible via open gate (unrestricted access). */
    @SerialName("open_gate")
    val openGate: String?,

    /** Number of spots available for short-term or transient parkers. */
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
