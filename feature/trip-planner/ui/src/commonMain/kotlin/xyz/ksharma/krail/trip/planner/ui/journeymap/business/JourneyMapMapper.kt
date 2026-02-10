package xyz.ksharma.krail.trip.planner.ui.journeymap.business

import xyz.ksharma.krail.core.maps.state.BoundingBox
import xyz.ksharma.krail.core.maps.state.CameraFocus
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.maps.ui.utils.MapCameraUtils
import xyz.ksharma.krail.trip.planner.network.api.model.TripResponse
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode
import xyz.ksharma.krail.trip.planner.ui.state.TransportModeLine
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyLegFeature
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyMapDisplay
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyMapUiState
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.RouteSegment
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.StopType

/**
 * Mapper to convert TripResponse.Journey to JourneyMapUiState.
 */
object JourneyMapMapper {

    private const val WALKING_PATH_COLOR = "#757575"
    private const val LEG_ID_PREFIX = "leg_"

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
        val transportMode = transportation?.toTransportMode()
        val lineName = transportation?.disassembledName
        val lineColor = calculateLineColor(transportMode, lineName)

        return createLegFromCoordinates(index, transportMode, lineName, lineColor)
            ?: createLegFromInterchange(index, lineColor)
            ?: createLegFromStops(index, transportMode, lineName, lineColor)
    }

    private fun calculateLineColor(transportMode: TransportMode?, lineName: String?): String {
        return when {
            transportMode == null -> WALKING_PATH_COLOR
            lineName != null ->
                TransportModeLine.TransportLine.entries
                    .firstOrNull { it.key == lineName }
                    ?.hexColor
                    ?: transportMode.colorCode
            else -> transportMode.colorCode
        }
    }

    private fun TripResponse.Leg.createLegFromCoordinates(
        index: Int,
        transportMode: TransportMode?,
        lineName: String?,
        lineColor: String,
    ): JourneyLegFeature? {
        if (coords.isNullOrEmpty()) return null

        val coordinates = coords!!.mapNotNull { coord ->
            if (coord.size >= 2) LatLng(latitude = coord[0], longitude = coord[1]) else null
        }
        if (coordinates.isEmpty()) return null

        return JourneyLegFeature(
            legId = LEG_ID_PREFIX + index,
            transportMode = transportMode,
            lineName = lineName,
            lineColor = lineColor,
            routeSegment = RouteSegment.PathSegment(points = coordinates),
        )
    }

    private fun TripResponse.Leg.createLegFromInterchange(
        index: Int,
        lineColor: String,
    ): JourneyLegFeature? {
        val coordsList = interchange?.coords ?: return null

        val coordinates = coordsList.mapNotNull { coord ->
            if (coord.size >= 2) LatLng(latitude = coord[0], longitude = coord[1]) else null
        }
        if (coordinates.isEmpty()) return null

        return JourneyLegFeature(
            legId = LEG_ID_PREFIX + index,
            transportMode = null,
            lineName = null,
            lineColor = lineColor,
            routeSegment = RouteSegment.PathSegment(points = coordinates),
        )
    }

    private fun TripResponse.Leg.createLegFromStops(
        index: Int,
        transportMode: TransportMode?,
        lineName: String?,
        lineColor: String,
    ): JourneyLegFeature? {
        if (transportation == null) return null

        val stops = stopSequence?.mapNotNull { it.toJourneyStopFeature() } ?: emptyList()
        if (stops.isEmpty()) return null

        return JourneyLegFeature(
            legId = LEG_ID_PREFIX + index,
            transportMode = transportMode,
            lineName = lineName,
            lineColor = lineColor,
            routeSegment = RouteSegment.StopConnectorSegment(stops = stops),
        )
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
            val lineName = leg.transportation?.disassembledName
            val transportMode = leg.transportation?.toTransportMode()
            val lineColor = calculateLineColor(transportMode, lineName)

            // Add origin of first leg with line name and color
            if (legIndex == 0) {
                leg.origin?.toJourneyStopFeature()?.let { stop ->
                    allStops.add(
                        stop.copy(
                            stopType = StopType.ORIGIN,
                            lineName = lineName,
                            lineColor = lineColor,
                        ),
                    )
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
                // Mark as interchange for middle legs - add line name and color for next leg
                val nextLeg = legs.getOrNull(legIndex + 1)
                val nextLineName = nextLeg?.transportation?.disassembledName
                val nextTransportMode = nextLeg?.transportation?.toTransportMode()
                val nextLineColor = calculateLineColor(nextTransportMode, nextLineName)

                leg.destination?.toJourneyStopFeature()?.let { stop ->
                    allStops.add(
                        stop.copy(
                            stopType = StopType.INTERCHANGE,
                            lineName = nextLineName,
                            lineColor = nextLineColor,
                        ),
                    )
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

        return MapCameraUtils.calculateBounds(allCoordinates)
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
