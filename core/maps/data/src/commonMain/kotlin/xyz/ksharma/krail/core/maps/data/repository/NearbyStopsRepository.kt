package xyz.ksharma.krail.core.maps.data.repository

import xyz.ksharma.krail.core.maps.data.model.NearbyStop

interface NearbyStopsRepository {
    /**
     * Get stops near a center point within a radius.
     *
     * @param centerLat Center latitude
     * @param centerLon Center longitude
     * @param radiusKm Search radius in kilometers
     * @param productClasses Filter by transport modes (empty = all modes)
     * @param maxResults Maximum number of results (default 50)
     * @return List of nearby stops sorted by distance
     */
    suspend fun getStopsNearby(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double,
        productClasses: Set<Int> = emptySet(),
        maxResults: Int = 50,
    ): List<NearbyStop>
}
