package xyz.ksharma.krail.core.maps.data.model

import xyz.ksharma.krail.trip.planner.ui.state.TransportMode

/**
 * Nearby stop data model.
 */
data class NearbyStop(
    val stopId: String,
    val stopName: String,
    val latitude: Double,
    val longitude: Double,
    val transportModes: List<TransportMode>,
)
