package xyz.ksharma.krail.trip.planner.ui.state.searchstop

import kotlinx.collections.immutable.toImmutableList

// Example mapper from domain models to MapUiState.Ready.
// Replace DomainRoute/DomainStop with your actual domain models.
object MapUiMapper {
    fun map(
        domainRoutes: List<DomainRoute>,
        domainStops: List<DomainStop>,
        selectedStopId: String?,
    ): MapUiState.Ready {
        val routeFeatures = domainRoutes.map { r ->
            RouteFeature(
                id = r.id,
                colorHex = r.colorHex,
                points = r.path.map { LatLng(it.lat, it.lng) },
            )
        }

        val stopFeatures = domainStops.map { s ->
            StopFeature(
                stopId = s.id,
                stopName = s.name,
                lineId = s.lineId,
                position = LatLng(s.lat, s.lng),
            )
        }

        val selected = stopFeatures.find { it.stopId == selectedStopId }?.let {
            SelectedStopUi(it.stopId, it.stopName, it.lineId)
        }

        return MapUiState.Ready(
            mapDisplay = MapDisplay(
                routes = routeFeatures.toImmutableList(),
                stops = stopFeatures.toImmutableList(),
                selectedStop = selected,
            ),
        )
    }
}

// Placeholder domain types for the example mapper (remove if you have real ones).
data class DomainRoute(val id: String, val colorHex: String, val path: List<Point>)
data class DomainStop(val id: String, val name: String, val lineId: String?, val lat: Double, val lng: Double)
data class Point(val lat: Double, val lng: Double)
