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

    override fun getAll(): Flow<List<NSWParkRide>> = data

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


    override fun getFacilitiesByStopId(stopId: String): Flow<List<String>> =
        savedParkRides.map { list ->
            list.filter { it.stopId == stopId }.map { it.facilityId }
        }

    override suspend fun insertOrReplaceSavedParkRide(stopId: String, facilityId: String) {
        val current = savedParkRides.value.toMutableList()
        current.removeAll { it.stopId == stopId && it.facilityId == facilityId }
        current.add(SavedParkRide(stopId, facilityId))
        savedParkRides.value = current
    }

    override suspend fun deleteSavedParkRide(stopId: String, facilityId: String) {
        savedParkRides.value = savedParkRides.value.filterNot {
            it.stopId == stopId && it.facilityId == facilityId
        }
    }

    override suspend fun clearSavedParkRides() {
        savedParkRides.value = emptyList()
    }
}
