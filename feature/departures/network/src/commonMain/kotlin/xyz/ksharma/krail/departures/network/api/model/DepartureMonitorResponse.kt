package xyz.ksharma.krail.departures.network.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response model for the NSW Transport departure monitor API.
 * Swagger: https://opendata.transport.nsw.gov.au/dataset/trip-planner-apis
 * Endpoint: GET /v1/tp/departure_mon
 *
 * Each [StopEvent] represents a single vehicle departure from a stop.
 * Only the fields needed for the departures UI are modelled here — the
 * full API response contains additional location / infos data that we
 * intentionally omit to keep the model lean.
 */
@Serializable
data class DepartureMonitorResponse(
    @SerialName("stopEvents") val stopEvents: List<StopEvent>? = null,
    @SerialName("error") val error: Error? = null,
    @SerialName("version") val version: String? = null,
) {

    @Serializable
    data class Error(
        @SerialName("message") val message: String? = null,
        @SerialName("versions") val versions: Versions? = null,
    )

    @Serializable
    data class Versions(
        @SerialName("controller") val controller: String? = null,
        @SerialName("interfaceMax") val interfaceMax: String? = null,
        @SerialName("interfaceMin") val interfaceMin: String? = null,
    )

    /**
     * A single departure event at a stop.
     *
     * [departureTimePlanned] is the scheduled departure time in ISO-8601 format.
     * [departureTimeEstimated] is the real-time estimate when available; prefer
     * this over [departureTimePlanned] for display.
     * [location] describes the platform / stop the service departs from.
     * [transportation] describes the service (line, destination, mode).
     */
    @Serializable
    data class StopEvent(
        @SerialName("departureTimePlanned") val departureTimePlanned: String? = null,
        @SerialName("departureTimeEstimated") val departureTimeEstimated: String? = null,
        @SerialName("location") val location: Location? = null,
        @SerialName("transportation") val transportation: Transportation? = null,
    )

    /**
     * The stop / platform this departure departs from.
     *
     * [disassembledName] is the human-readable platform label (e.g. "Platform 1",
     * "Stand A"). Fall back to [name] if [disassembledName] is absent.
     */
    @Serializable
    data class Location(
        @SerialName("id") val id: String? = null,
        @SerialName("name") val name: String? = null,
        @SerialName("disassembledName") val disassembledName: String? = null,
        @SerialName("parent") val parent: Parent? = null,
    )

    @Serializable
    data class Parent(
        @SerialName("id") val id: String? = null,
        @SerialName("name") val name: String? = null,
        @SerialName("disassembledName") val disassembledName: String? = null,
    )

    /**
     * Describes the public transport service departing from a stop.
     *
     * [disassembledName] is the short line identifier (e.g. "T1", "333", "F1").
     * [product] carries the product class used for mode-based colour coding.
     * [destination] is the final stop / terminus of the service.
     */
    @Serializable
    data class Transportation(
        @SerialName("id") val id: String? = null,
        @SerialName("name") val name: String? = null,
        @SerialName("disassembledName") val disassembledName: String? = null,
        @SerialName("number") val number: String? = null,
        @SerialName("description") val description: String? = null,
        @SerialName("destination") val destination: Destination? = null,
        @SerialName("product") val product: Product? = null,
        @SerialName("operator") val operator: Operator? = null,
    )

    @Serializable
    data class Destination(
        @SerialName("id") val id: String? = null,
        @SerialName("name") val name: String? = null,
    )

    /**
     * [cls] maps to the NSW Transport product class:
     * 1 = Train, 2 = Metro, 4 = Light Rail, 5 = Bus, 7 = Coach, 9 = Ferry, 11 = School Bus
     */
    @Serializable
    data class Product(
        @SerialName("class") val cls: Int? = null,
        @SerialName("iconId") val iconId: Int? = null,
        @SerialName("name") val name: String? = null,
    )

    @Serializable
    data class Operator(
        @SerialName("id") val id: String? = null,
        @SerialName("name") val name: String? = null,
    )
}
