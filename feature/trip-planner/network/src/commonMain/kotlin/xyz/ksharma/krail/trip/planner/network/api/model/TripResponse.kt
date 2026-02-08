package xyz.ksharma.krail.trip.planner.network.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Swagger: https://opendata.transport.nsw.gov.au/dataset/trip-planner-apis/resource/917c66c3-8123-4a0f-b1b1-b4220f32585d
 */
@Serializable
data class TripResponse(
    @SerialName("systemMessages") val systemMessages: List<SystemMessages>? = null,
    @SerialName("journeys") val journeys: List<Journey>? = null,
    @SerialName("error") val error: Error? = null,
    @SerialName("version") val version: String? = null,
) {
    @Serializable
    data class Error(
        @SerialName("versions") val versions: Versions? = null,
        @SerialName("message") val message: String? = null,
    )

    @Serializable
    data class Versions(
        @SerialName("controller") val controller: String? = null,
        @SerialName("interfaceMax") val interfaceMax: String? = null,
        @SerialName("interfaceMin") val interfaceMin: String? = null,
    )

    @Serializable
    data class Journey(
        @SerialName("legs") val legs: List<Leg>? = null,
    )

    @Serializable
    data class Leg(
        /**
         * The approximate distance in metres travelled to complete this journey leg.
         */
        @SerialName("distance") val distance: Long? = null,

        /**
         * Contains a number of additional informational messages that may be useful for travellers.
         */
        @SerialName("hints") val hints: List<Hint>? = null,

        @SerialName("origin") val origin: StopSequence? = null,
        @SerialName("destination") val destination: StopSequence? = null,
        /**
         * This is a list of all stops that are made for this leg. It is sorted in order of its
         * stopping sequence. If the leg is a walking leg between two stops, then this will contain
         * these two stops in order.
         *
         * Contains information about a single stop in the journey leg.
         * Typically, the first stop in a journey leg will only include departure time information,
         * while the last stop in a journey leg will only include arrival time information.
         * Stops in between will include both arrival and departure time information.
         */
        @SerialName("stopSequence") val stopSequence: List<StopSequence>? = null,

        /**
         * This indicates whether or not real-time data has been used to calculate the
         * departure/arrival timestamps.
         */
        @SerialName("isRealtimeControlled") val isRealtimeControlled: Boolean? = null,

        /**
         * This element describes a route, including information about its route number, usual destination,
         * route type and operator.
         */
        @SerialName("transportation") val transportation: Transportation? = null,

        /**
         * The approximate amount of time in "seconds" required to complete this journey leg.
         */
        @SerialName("duration") val duration: Long? = null,

        /**
         * If the leg corresponds to a walking leg, this element contains walking directions.
         */
        @SerialName("footPathInfo") val footPathInfo: List<FootPathInfo>? = null,

        /**
         * This element indicates whether the walking directions are redundant in [footPathInfo].
         */
        @SerialName("footPathInfoRedundant") val footPathInfoRedundant: Boolean? = null,

        /**
         * This element describes a single information message that may be associated with a journey leg.
         * The data in this is similar to that from add_info endpoint, but is presented differently.
         */
        @SerialName("infos") val infos: List<Info>? = null,

        /**
         * This object describes walking directions for interchanging between two consecutive legs.
         * This occurs in the case when there's only a small distance between two transit legs,
         * not enough to constitute a standalone walking leg.
         */
        @SerialName("interchange") val interchange: Interchange? = null,

        /**
         * Array of coordinate pairs [latitude, longitude] representing the route path.
         * Available for both transit and walking legs.
         * Each element is a 2-element array: [latitude, longitude]
         */
        @SerialName("coords") val coords: List<List<Double>>? = null,
    )

    @Serializable
    data class StopSequence(
        /**
         * A timestamp in YYYY-MM-DDTHH:MM:SSZ format that indicates the estimated arrival time.
         * If real-time information is available then this timestamp is the real-time estimate,
         * otherwise it is the same as the value in [arrivalTimePlanned]
         */
        @SerialName("arrivalTimeEstimated") val arrivalTimeEstimated: String? = null,

        /**
         * A timestamp in YYYY-MM-DDTHH:MM:SSZ format that indicates the planned arrival time.
         * This is the original scheduled time.
         */
        @SerialName("arrivalTimePlanned") val arrivalTimePlanned: String? = null,

        /**
         * A timestamp in YYYY-MM-DDTHH:MM:SSZ format that indicates the estimated departure time.
         * If real-time information is available then this timestamp is the real-time estimate,
         * otherwise it is the same as the value in [departureTimePlanned].
         */
        @SerialName("departureTimeEstimated") val departureTimeEstimated: String? = null,

        /**
         * A timestamp in YYYY-MM-DDTHH:MM:SSZ format that indicates the planned departure time.
         * This is the original scheduled time.
         */
        @SerialName("departureTimePlanned") val departureTimePlanned: String? = null,

        /**
         * This is the long version of the location name, which may include the suburb or other information.
         */
        @SerialName("name") val name: String? = null,

        /**
         * This is the short version of the location name, which does not include the suburb or
         * other information.
         */
        @SerialName("disassembledName") val disassembledName: String? = null,

        /**
         * This is a unique ID for the returned location.
         * Certain types of ID can be used for subsequent searches performed with stop_finder,
         * or can be used as the origin or destination in an trip request.
         * The format of a location ID differs greatly, depending on the type of location it is.
         */
        @SerialName("id") val id: String? = null,

        /**
         * This is the type of location being returned. It will typically represent a specific stop
         * or platform.
         * [ poi, singlehouse, stop, platform, street, locality, suburb, unknown ]
         */
        @SerialName("type") val type: String? = null,

        /**
         * Contains additional information about this stop, such as wheelchair accessibility information.
         */
        @SerialName("properties") val properties: DestinationProperties? = null,

        /**
         * Coordinates of the stop/platform location.
         * Array format: [latitude, longitude]
         * Example: [-33.774221, 150.935976]
         */
        @SerialName("coord") val coord: List<Double>? = null,

        /**
         * Parent location information (usually the station when this is a platform)
         */
        @SerialName("parent") val parent: ParentLocation? = null,

        /**
         * Indicates if the id is a global stop ID
         */
        @SerialName("isGlobalId") val isGlobalId: Boolean? = null,

        /**
         * Level/floor of the platform
         * 0 = ground level, negative = underground, positive = elevated
         * Example: -2 for Town Hall underground platform
         */
        @SerialName("niveau") val niveau: Int? = null,

        /**
         * List of transport mode IDs available at this stop
         * Values: 1=Train, 2=Metro, 4=Light Rail, 5=Bus, 7=Coach, 9=Ferry, 11=School Bus
         */
        @SerialName("modes") val modes: List<Int>? = null,

        /**
         * Planned departure time from the base timetable (non-real-time)
         */
        @SerialName("departureTimeBaseTimetable") val departureTimeBaseTimetable: String? = null,

        /**
         * Planned arrival time from the base timetable (non-real-time)
         */
        @SerialName("arrivalTimeBaseTimetable") val arrivalTimeBaseTimetable: String? = null,
    )

    @Serializable
    data class DestinationProperties(
        /**
         * true if the stop is wheelchair accessible, false otherwise.
         */
        @SerialName("WheelchairAccess") val wheelchairAccess: String? = null,

        @SerialName("downloads") val downloads: List<Download>? = null,

        /**
         * No official documentation on this. Adding values as I see in the response.
         * Enum - STANDING_ONLY, MANY_SEATS,
         */
        @SerialName("occupancy") val occupancy: String? = null,

        /**
         * No official documentation on this.
         */
        @SerialName("platform") val platform: String? = null,

        /**
         * Platform name (e.g., "Platform 1", "Stand A")
         */
        @SerialName("platformName") val platformName: String? = null,

        /**
         * Planned platform name from timetable
         */
        @SerialName("plannedPlatformName") val plannedPlatformName: String? = null,

        /**
         * Stopping point planned designation
         */
        @SerialName("stoppingPointPlanned") val stoppingPointPlanned: String? = null,

        /**
         * Platform area number
         */
        @SerialName("area") val area: String? = null,

        /**
         * Number of cars in the train
         */
        @SerialName("NumberOfCars") val numberOfCars: String? = null,
    )

    /**
     * Parent location information for a stop.
     * Usually represents the station when the stop is a specific platform.
     */
    @Serializable
    data class ParentLocation(
        /**
         * ID of the parent location
         */
        @SerialName("id") val id: String? = null,

        /**
         * Full name of the parent location
         */
        @SerialName("name") val name: String? = null,

        /**
         * Short name without suburb
         */
        @SerialName("disassembledName") val disassembledName: String? = null,

        /**
         * Type of location: "stop", "platform", "locality", etc.
         */
        @SerialName("type") val type: String? = null,

        /**
         * Coordinates of the parent location
         * Array format: [latitude, longitude]
         */
        @SerialName("coord") val coord: List<Double>? = null,

        /**
         * Grandparent location (usually locality/suburb)
         */
        @SerialName("parent") val parent: GrandParentLocation? = null,

        /**
         * Additional properties of the parent location
         */
        @SerialName("properties") val properties: ParentProperties? = null,

        /**
         * Level/floor of the parent location
         */
        @SerialName("niveau") val niveau: Int? = null,

        /**
         * Indicates if this is a global ID
         */
        @SerialName("isGlobalId") val isGlobalId: Boolean? = null,
    )

    /**
     * Grandparent location (typically the suburb/locality)
     */
    @Serializable
    data class GrandParentLocation(
        /**
         * ID of the locality
         * Format: "placeID:95308035:1"
         */
        @SerialName("id") val id: String? = null,

        /**
         * Name of the locality/suburb
         * Example: "Seven Hills", "Sydney"
         */
        @SerialName("name") val name: String? = null,

        /**
         * Type is typically "locality"
         */
        @SerialName("type") val type: String? = null,
    )

    /**
     * Additional properties for parent locations
     */
    @Serializable
    data class ParentProperties(
        /**
         * The legacy stop ID
         * Example: "10101234"
         */
        @SerialName("stopId") val stopId: String? = null,
    )

    @Serializable
    data class Download(
        @SerialName("type") val type: String? = null,
        @SerialName("url") val url: String? = null,
    )

    @Serializable
    data class FootPathInfo(
        /**
         * This is approximately how long in seconds the walking instructions contained in this
         * element take to perform.
         */
        @SerialName("duration") val duration: Long? = null,

        // Not required @SerialName("footPathElem") val footPathElem: List<FootPathElem>? = null,

        /**
         * This indicates where in the leg the walking part of this legs occurs, since for some legs this
         * is included with transportation on a vehicle.
         *
         * Enum - BEFORE, AFTER, IDEST
         *
         * IDEST -This indicates that the walking portion of the leg is the entire leg itself.
         * In other words, the leg involves walking only, with no vehicle transportation involved.
         * For example, if you're planning a trip from one location to another that involves walking
         * the entire distance, the "position" would be "IDEST".
         */
        @SerialName("position") val position: String? = null,
    )

    @Serializable
    data class Hint(
        @SerialName("infoText") val infoText: String? = null,
    )

    @Serializable
    data class Info(
        @SerialName("timestamps") val timestamps: Timestamps? = null,

        /**
         * This is short summary that can be used as a heading for the alert content. It may contain
         * HTML tags and/or HTML entities.
         */
        @SerialName("subtitle") val subtitle: String? = null,

        /**
         * This field contains a title that can be used when displaying the url URL.
         */
        @SerialName("urlText") val urlText: String? = null,

        @SerialName("id") val id: String? = null,
        /**
         * 	string
         * This value indicates how important the service alert is. A value of high or veryHigh
         * likely indicates that the alert will correspond to an event that impacts the ability
         * to travel for relevant users, while low or veryLow
         * might be more of an informational message.
         *
         * Enum - veryLow, low, normal, high, veryHigh
         */
        @SerialName("priority") val priority: String? = null,
        @SerialName("version") val version: Long? = null,

        /**
         * This is the descriptive alert content. It may contain HTML tags and/or HTML entities.
         */
        @SerialName("content") val content: String? = null,

        /**
         * This field contains a URL that contains additional information about the alert.
         */
        @SerialName("url") val url: String? = null,
    )

    @Serializable
    data class Timestamps(
        @SerialName("lastModification") val lastModification: String? = null,
        @SerialName("availability") val availability: Ity? = null,
        @SerialName("validity") val validity: List<Ity>? = null,
        @SerialName("creation") val creation: String? = null,
    )

    @Serializable
    data class Ity(
        @SerialName("from") val from: String? = null,
        @SerialName("to") val to: String? = null,
    )

    @Serializable
    data class Interchange(
        /**
         * This indicates the mode of travel for the interchange. Both 99 and 100 indicate walking.
         */
        @SerialName("type") val type: Long? = null,

        /**
         * This is a list of coordinates that makes up the path of the interchange.
         * Contains exactly two values: the first value is the latitude, the second value is the longitude.
         */
        @SerialName("coords") val coords: List<List<Double>>? = null,

        /**
         * This is a description of the interchange.
         */
        @SerialName("desc") val desc: String? = null,
    )

    @Serializable
    data class Transportation(
        /**
         * Contains an ID for the icon that can be used for this route. Different values here are
         * used to differentiate different types of the same route type. For example, private ferries
         * have a different way finding icon to ferries operated by Sydney Ferries.
         *
         * 1: Sydney Trains (product class 1)
         * 2: Intercity Trains (product class 1)
         * 3: Regional Trains (product class 1)
         * 19: Temporary Trains (product class 1)
         *
         * 24: Sydney Metro (product class 2)
         *
         * 13: Sydney Light Rail (product class 4)
         * 20: Temporary Light Rail (product class 4)
         * 21: Newcastle Light Rail (product class 4)
         *
         * 4: Blue Mountains Buses (product class 5)
         * 5: Sydney Buses (product class 5)
         * 6: Central Coast Buses (product class 5)
         * 14: Temporary Buses (product class 5)
         * 15: Hunter Buses (product class 5)
         * 23: On Demand (product class 5)
         * 31: Central West and Orana (product class 5)
         * 32: Far West (product class 5)
         * 33: New England North West (product class 5)
         * 34: Newcastle and Hunter (product class 5)
         * 35: North Coast (product class 5)
         * 36: Riverina Murray (product class 5)
         * 37: South East and Tablelands (product class 5)
         *
         * 38: Sydney and Surrounds (product class 5)
         * 9: Private Buses (product class 5)
         * 17: Private Coaches (product class 5)
         *
         * 7: Regional Coaches (product class 7)
         * 22: Temporary Coaches (product class 7)
         *
         * 10: Sydney Ferries (product class 9)
         * 11: Newcastle Ferries (product class 9)
         * 12: Private Ferries (product class 9)
         * 18: Temporary Ferries (product class 9)
         *
         * 8: School Buses (product class 11)
         */
        @SerialName("iconId") val iconId: Long? = null,

        /**
         * Contains a short name for the route.
         */
        @SerialName("number") val number: String? = null,

        /**
         * This element contains additional properties about the route.
         * product class, iconId, name etc.
         */
        @SerialName("product") val product: Product? = null,

        /**
         * Contains a very short name for the route.
         */
        @SerialName("disassembledName") val disassembledName: String? = null,

        /**
         * This element contains information about where vehicles on this route terminate.
         */
        @SerialName("destination") val destination: OperatorClass? = null,

        @SerialName("name") val name: String? = null,

        @SerialName("description") val description: String? = null,

        @SerialName("id") val id: String? = null,

        @SerialName("operator") val operator: OperatorClass? = null,

        @SerialName("properties") val properties: TransportationProperties? = null,
    )

    @Serializable
    data class TransportationProperties(
        @SerialName("tripCode") val tripCode: String? = null,

        @SerialName("RealtimeTripId") val realtimeTripId: String? = null,
    )

    @Serializable
    data class OperatorClass(
        /**
         * This is the name of the destination for this route.
         */
        @SerialName("name") val name: String? = null,

        /**
         * Contains a unique identifier (if available) of the destination for this route.
         */
        @SerialName("id") val id: String? = null,
    )

    @Serializable
    data class Product(
        /**
         * This field is used by to determine which icon to use when displaying
         * this affected route. It will typically match up with the class value.
         */
        @SerialName("iconID") val iconID: Long? = null,

        /**
         *
         */
        @SerialName("name") val name: String? = null,

        /**
         * This field indicates the type of the route, using the same values
         * as elsewhere in this API.
         *
         * 1: Train
         * 2: Metro
         * 4: Light Rail
         * 5: Bus
         * 7: Coach
         * 9: Ferry
         * 11: School Bus
         * 99: Walking
         * 100: Walking (Footpath)
         * 101: Bicycle
         * 102: Take Bicycle On Public Transport
         * 103: Kiss & Ride
         * 104: Park & Ride
         * 105: Taxi
         * 106: Car
         */
        @SerialName("class") val productClass: Long? = null,
    )

    @Serializable
    data class SystemMessages(
        @SerialName("responseMessages") val responseMessages: List<ResponseMessage>? = null,
    )

    @Serializable
    data class ResponseMessage(
        @SerialName("code") val code: Long? = null,
        @SerialName("module") val module: String? = null,
        @SerialName("error") val error: String? = null,
        @SerialName("type") val type: String? = null,
    )
}
