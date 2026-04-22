package xyz.ksharma.krail.trip.planner.ui.journeymap.business

import xyz.ksharma.krail.core.maps.state.BoundingBox
import xyz.ksharma.krail.core.maps.state.CameraFocus
import xyz.ksharma.krail.core.maps.state.LatLng
import xyz.ksharma.krail.core.maps.ui.utils.MapCameraUtils
import xyz.ksharma.krail.core.transport.nsw.NswTransportLine
import xyz.ksharma.krail.feature.track.TrackedJourneyDisplay
import xyz.ksharma.krail.feature.track.TrackedLeg
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyLegFeature
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyMapDisplay
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyMapUiState
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.JourneyStopFeature
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.RouteSegment
import xyz.ksharma.krail.trip.planner.ui.state.journeymap.StopType

/**
 * Pure mapper — no I/O. Stop coordinates must be pre-fetched by the caller (ViewModel).
 * Missing coordinates for a stop are silently skipped; the map renders with remaining stops.
 */
object TrackedJourneyMapMapper {

    private const val LEG_ID_PREFIX = "tracked_leg_"

    fun TrackedJourneyDisplay.toJourneyMapState(
        stopCoordinates: Map<String, LatLng>,
    ): JourneyMapUiState.Ready {
        val transportLegs = legs.filterIsInstance<TrackedLeg.Transport>()

        data class LegResult(val feature: JourneyLegFeature, val stops: List<JourneyStopFeature>)

        val legResults = transportLegs.mapIndexed { index, leg ->
            val lineColor = NswTransportLine.entries
                .firstOrNull { it.key == leg.lineName }
                ?.hexColor
                ?: leg.lineColorCode

            val stopFeatures = leg.stops.mapIndexedNotNull { stopIndex, stop ->
                val position = stopCoordinates[stop.stopId] ?: return@mapIndexedNotNull null
                val stopType = when {
                    index == 0 && stopIndex == 0 -> StopType.ORIGIN
                    index == transportLegs.lastIndex && stopIndex == leg.stops.lastIndex -> StopType.DESTINATION
                    stopIndex == 0 || stopIndex == leg.stops.lastIndex -> StopType.INTERCHANGE
                    else -> StopType.REGULAR
                }
                JourneyStopFeature(
                    stopId = stop.stopId,
                    stopName = stop.name,
                    position = position,
                    stopType = stopType,
                    platform = null,
                    lineName = leg.lineName,
                    lineColor = lineColor,
                    departureTime = stop.utcTime,
                )
            }

            val routeSegment = if (leg.routePathCoordinates.isNotEmpty()) {
                RouteSegment.PathSegment(points = leg.routePathCoordinates)
            } else {
                RouteSegment.StopConnectorSegment(stops = stopFeatures)
            }

            LegResult(
                feature = JourneyLegFeature(
                    legId = LEG_ID_PREFIX + index,
                    transportMode = leg.transportMode,
                    lineName = leg.lineName,
                    lineColor = lineColor,
                    routeSegment = routeSegment,
                ),
                stops = stopFeatures,
            )
        }

        val legFeatures = legResults.map { it.feature }
        val allStops = legResults.flatMap { it.stops }

        val bounds = calculateTrackedBounds(legFeatures)

        return JourneyMapUiState.Ready(
            mapDisplay = JourneyMapDisplay(
                legs = legFeatures,
                stops = allStops,
            ),
            cameraFocus = bounds?.let { CameraFocus(it) },
        )
    }

    private fun calculateTrackedBounds(legs: List<JourneyLegFeature>): BoundingBox? {
        val allPoints = legs.flatMap { leg ->
            when (val seg = leg.routeSegment) {
                is RouteSegment.PathSegment -> seg.points
                is RouteSegment.StopConnectorSegment -> seg.stops.mapNotNull { it.position }
            }
        }
        return MapCameraUtils.calculateBounds(allPoints)
    }
}
