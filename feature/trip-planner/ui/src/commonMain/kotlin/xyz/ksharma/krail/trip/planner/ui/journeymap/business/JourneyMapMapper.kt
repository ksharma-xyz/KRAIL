package xyz.ksharma.krail.trip.planner.ui.journeymap.business

import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.BoundingBox
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.CameraFocus
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyLegFeature
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyMapDisplay
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyMapUiState
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.RouteSegment
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.StopType
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.LatLng

/**
 * Mapper to convert TripResponse.Journey to JourneyMapUiState.
 */
object JourneyMapMapper {

    /**
     * Converts a TripResponse.Journey to a JourneyMapUiState.Ready.
     */
    fun TripResponse.Journey.toJourneyMapState(): JourneyMapUiState.Ready {
        val legs = this.legs ?: emptyList()

        // Extract all leg features
        val legFeatures = legs.mapIndexedNotNull { index, leg ->
            leg.toJourneyLegFeature(index)
        }

        // Extract all stops and mark origin/destination
        val stopFeatures = extractStopFeatures(legs)

        // Calculate bounding box for camera
        val bounds = calculateBounds(legFeatures)

        return JourneyMapUiState.Ready(
            mapDisplay = JourneyMapDisplay(
                legs = legFeatures,
                stops = stopFeatures,
            ),
            cameraFocus = bounds?.let { CameraFocus(it) },
        )
    }

    /**
     * Converts a TripResponse.Leg to a JourneyLegFeature.
     */
    private fun TripResponse.Leg.toJourneyLegFeature(index: Int): JourneyLegFeature? {
        return when {
            // Walking leg with interchange coordinates
            interchange?.coords != null -> {
                val interchangeData = interchange!!
                val coordsList = interchangeData.coords ?: return null

                val coords = coordsList.mapNotNull { coord ->
                    if (coord.size >= 2) {
                        LatLng(latitude = coord[0], longitude = coord[1])
                    } else {
                        null
                    }
                }
                if (coords.isEmpty()) return null

                JourneyLegFeature(
                    legId = "leg_$index",
                    transportMode = null, // Walking doesn't have a specific TransportMode in the sealed class
                    routeSegment = RouteSegment.PathSegment(points = coords),
                )
            }
            // Transit leg - connect stops
            transportation != null -> {
                val transportData = transportation!!
                val stops = stopSequence?.mapNotNull { it.toJourneyStopFeature() } ?: emptyList()
                if (stops.isEmpty()) return null

                JourneyLegFeature(
                    legId = "leg_$index",
                    transportMode = transportData.toTransportMode(),
                    routeSegment = RouteSegment.StopConnectorSegment(stops = stops),
                )
            }
            else -> null
        }
    }

    /**
     * Converts a TripResponse.StopSequence to a JourneyStopFeature.
     */
    private fun TripResponse.StopSequence.toJourneyStopFeature(): JourneyStopFeature? {
        // Get coordinates from stop or parent
        val coordinates = coord?.takeIf { it.size >= 2 }?.let {
            LatLng(latitude = it[0], longitude = it[1])
        } ?: parent?.coord?.takeIf { it.size >= 2 }?.let {
            LatLng(latitude = it[0], longitude = it[1])
        }

        // Skip stops without coordinates
        if (coordinates == null) return null

        return JourneyStopFeature(
            stopId = id ?: "",
            stopName = disassembledName ?: name ?: "Unknown Stop",
            position = coordinates,
            stopType = StopType.REGULAR, // Will be updated later for origin/destination
            time = departureTimeEstimated ?: departureTimePlanned
                ?: arrivalTimeEstimated ?: arrivalTimePlanned,
            platform = properties?.platform ?: properties?.platformName,
        )
    }

    /**
     * Extract all stops from the journey and mark origin/destination.
     */
    private fun extractStopFeatures(legs: List<TripResponse.Leg>): List<JourneyStopFeature> {
        if (legs.isEmpty()) return emptyList()

        val allStops = mutableListOf<JourneyStopFeature>()

        legs.forEachIndexed { legIndex, leg ->
            // Add origin of first leg
            if (legIndex == 0) {
                leg.origin?.toJourneyStopFeature()?.let { stop ->
                    allStops.add(stop.copy(stopType = StopType.ORIGIN))
                }
            }

            // Add intermediate stops from stop sequence
            leg.stopSequence?.drop(1)?.dropLast(1)?.forEach { stopSeq ->
                stopSeq.toJourneyStopFeature()?.let { allStops.add(it) }
            }

            // Add destination of last leg
            if (legIndex == legs.lastIndex) {
                leg.destination?.toJourneyStopFeature()?.let { stop ->
                    allStops.add(stop.copy(stopType = StopType.DESTINATION))
                }
            } else {
                // Mark as interchange for middle legs
                leg.destination?.toJourneyStopFeature()?.let { stop ->
                    allStops.add(stop.copy(stopType = StopType.INTERCHANGE))
                }
            }
        }

        return allStops
    }

    /**
     * Calculate bounding box from all leg features.
     */
    private fun calculateBounds(legs: List<JourneyLegFeature>): BoundingBox? {
        val allCoordinates = legs.flatMap { leg ->
            when (val segment = leg.routeSegment) {
                is RouteSegment.PathSegment -> segment.points
                is RouteSegment.StopConnectorSegment -> segment.stops.mapNotNull { it.position }
            }
        }

        if (allCoordinates.isEmpty()) return null

        val minLat = allCoordinates.minOf { it.latitude }
        val maxLat = allCoordinates.maxOf { it.latitude }
        val minLng = allCoordinates.minOf { it.longitude }
        val maxLng = allCoordinates.maxOf { it.longitude }

        return BoundingBox(
            southwest = LatLng(minLat, minLng),
            northeast = LatLng(maxLat, maxLng),
        )
    }

    /**
     * Convert TripResponse.Transportation to TransportMode sealed class.
     * Returns null for unknown transport modes.
     */
    private fun TripResponse.Transportation.toTransportMode(): TransportMode? {
        val productClass = product?.productClass?.toInt() ?: return null
        return TransportMode.toTransportModeType(productClass)
    }
}
