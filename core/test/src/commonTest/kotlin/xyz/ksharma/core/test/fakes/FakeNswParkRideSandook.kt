package xyz.ksharma.core.test.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import xyz.ksharma.krail.sandook.NSWParkRide
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.sandook.SavedParkRide

class FakeNswParkRideSandook : NswParkRideSandook {
    private val data = MutableStateFlow<List<NSWParkRide>>(emptyList())
    private val savedParkRides = MutableStateFlow<List<SavedParkRide>>(emptyList())

    override fun observeSavedParkRides(): Flow<List<SavedParkRide>> = savedParkRides.asStateFlow()

    override fun getAll(): Flow<List<NSWParkRide>> = data.asStateFlow()

    override fun getByStopIds(stopIds: List<String>): Flow<List<NSWParkRide>> =
        data.map { list -> list.filter { it.stopId in stopIds } }

    override suspend fun insertOrReplace(parkRide: NSWParkRide) {
        data.value = data.value.filterNot { it.facilityId == parkRide.facilityId } + parkRide
    }

    override suspend fun insertOrReplaceAll(parkRides: List<NSWParkRide>) {
        val ids = parkRides.map { it.facilityId }.toSet()
        data.value = data.value.filterNot { it.facilityId in ids } + parkRides
    }

    override suspend fun deleteAll() {
        data.value = emptyList()
    }

    override fun getFacilitiesByStopIdAndSource(
        stopId: String,
        source: NswParkRideSandook.Companion.SavedParkRideSource
    ): Flow<List<String>> =
        savedParkRides.map { list ->
            list.filter { it.stopId == stopId && it.source == source.value }
                .map { it.facilityId }
        }

    override suspend fun insertOrReplaceSavedParkRides(
        pairs: Set<Pair<String, String>>,
        source: NswParkRideSandook.Companion.SavedParkRideSource
    ) {
        val newRides = pairs.map { (stopId, facilityId) ->
            SavedParkRide(stopId, facilityId, source.value)
        }
        val filtered = savedParkRides.value.filterNot { ride ->
            newRides.any { it.stopId == ride.stopId && it.facilityId == ride.facilityId && it.source == ride.source }
        }
        savedParkRides.value = filtered + newRides
    }

    override suspend fun clearAllSavedParkRidesBySource(source: NswParkRideSandook.Companion.SavedParkRideSource) {
        savedParkRides.value = savedParkRides.value.filterNot { it.source == source.value }
    }

    override suspend fun deleteSavedParkRide(stopId: String, facilityId: String) {
        savedParkRides.value = savedParkRides.value.filterNot {
            it.stopId == stopId && it.facilityId == facilityId
        }
    }
}
