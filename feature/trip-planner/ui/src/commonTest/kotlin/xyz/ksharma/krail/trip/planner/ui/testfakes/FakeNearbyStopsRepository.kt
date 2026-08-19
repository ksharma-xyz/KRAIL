package xyz.ksharma.krail.trip.planner.ui.testfakes

import xyz.ksharma.krail.core.maps.data.model.NearbyStop
import xyz.ksharma.krail.core.maps.data.repository.NearbyStopsRepository

internal class FakeNearbyStopsRepository : NearbyStopsRepository {
    var nearbyStops: List<NearbyStop> = emptyList()

    override suspend fun getStopsNearby(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double,
        productClasses: Set<Int>,
        maxResults: Int,
    ): List<NearbyStop> = nearbyStops.take(maxResults)
}
