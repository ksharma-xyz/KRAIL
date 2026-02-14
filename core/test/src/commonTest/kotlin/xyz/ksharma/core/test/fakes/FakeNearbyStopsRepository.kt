package xyz.ksharma.core.test.fakes

import xyz.ksharma.krail.core.maps.data.model.NearbyStop
import xyz.ksharma.krail.core.maps.data.repository.NearbyStopsRepository

class FakeNearbyStopsRepository : NearbyStopsRepository {

    private val stops = mutableListOf<NearbyStop>()
    var shouldThrowError = false
    var errorMessage = "Test error"

    fun addStop(stop: NearbyStop) {
        stops.add(stop)
    }

    fun clearStops() {
        stops.clear()
    }

    override suspend fun getStopsNearby(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double,
        productClasses: Set<Int>,
        maxResults: Int,
    ): List<NearbyStop> {
        if (shouldThrowError) {
            throw Exception(errorMessage)
        }

        // Simple distance-based filtering for testing
        return stops
            .filter { stop ->
                val distance = calculateDistance(centerLat, centerLon, stop.latitude, stop.longitude)
                distance <= radiusKm
            }
            .filter { stop ->
                if (productClasses.isEmpty()) {
                    true
                } else {
                    stop.transportModes.any { mode ->
                        productClasses.contains(mode.productClass)
                    }
                }
            }
            .sortedBy { stop ->
                calculateDistance(centerLat, centerLon, stop.latitude, stop.longitude)
            }
            .take(maxResults)
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        // Simple Euclidean distance for testing (not accurate for real geo calculations)
        val latDiff = lat2 - lat1
        val lonDiff = lon2 - lon1
        return kotlin.math.sqrt(latDiff * latDiff + lonDiff * lonDiff) * 111.0 // approximate km per degree
    }
}

