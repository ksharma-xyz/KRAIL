package xyz.ksharma.krail.feature.track

import xyz.ksharma.krail.feature.crowding.CarriageLevels

data class LiveVehiclePosition(
    val latitude: Double,
    val longitude: Double,
    /** 0–360 degrees clockwise from north. 0 means unknown/not provided. */
    val bearing: Float,
    val status: VehicleStatus,
    val lastUpdatedEpochSec: Long,
    /**
     * Live level per carriage from the consist (TfNSW extension 1007), in feed order. Null when
     * the vehicle reports no carriages, which is most Sydney Trains sets and every bus.
     */
    val carriages: CarriageLevels? = null,
)

enum class VehicleStatus { INCOMING_AT, STOPPED_AT, IN_TRANSIT_TO }

data class LiveTrackingOverlay(
    /** legIndex → live position for that leg's vehicle. */
    val vehiclePositions: Map<Int, LiveVehiclePosition>,
    /** stopId → delay in seconds. Negative = early. */
    val stopDelays: Map<String, Int>,
    val lastModified: String?,
)
