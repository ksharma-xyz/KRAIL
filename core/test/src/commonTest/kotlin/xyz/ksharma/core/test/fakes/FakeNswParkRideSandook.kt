package xyz.ksharma.core.test.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.sandook.NSWParkRide

class FakeNswParkRideSandook : NswParkRideSandook {
    private val data = MutableStateFlow<List<NSWParkRide>>(emptyList())

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
}