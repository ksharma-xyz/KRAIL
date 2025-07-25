package xyz.ksharma.core.test.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import xyz.ksharma.krail.sandook.NSWParkRideFacilityDetail
import xyz.ksharma.krail.sandook.NswParkRideSandook
import xyz.ksharma.krail.sandook.SavedParkRide

class FakeNswParkRideSandook : NswParkRideSandook {
    private val data = MutableStateFlow<List<NSWParkRideFacilityDetail>>(emptyList())
    private val savedParkRides = MutableStateFlow<List<SavedParkRide>>(emptyList())

    override fun observeSavedParkRides(): Flow<List<SavedParkRide>> = savedParkRides.asStateFlow()

    override fun getAllParkRideFacilityDetail(): Flow<List<NSWParkRideFacilityDetail>> = data.asStateFlow()

    override fun getByStopIds(stopIds: List<String>): List<NSWParkRideFacilityDetail> =
        data.value.filter { it.stopId in stopIds }

    override suspend fun insertOrReplace(parkRide: NSWParkRideFacilityDetail) {
        data.value = data.value.filterNot { it.facilityId == parkRide.facilityId } + parkRide
    }

    override suspend fun insertOrReplaceAll(parkRides: List<NSWParkRideFacilityDetail>) {
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
        savedParkRides.asStateFlow().map { list ->
            list.filter { it.stopId == stopId && it.source == source.value }
                .map { it.facilityId }
        }

    override suspend fun insertOrReplaceSavedParkRides(
        parkRideInfoList: Set<SavedParkRide>,
        source: NswParkRideSandook.Companion.SavedParkRideSource
    ) {
        val newRides = parkRideInfoList.map { info ->
            SavedParkRide(
                stopId = info.stopId,
                facilityId = info.facilityId,
                stopName = info.stopName,
                facilityName = info.facilityName,
                source = source.value
            )
        }

        val filtered = savedParkRides.value.filterNot { ride ->
            newRides.any {
                it.stopId == ride.stopId &&
                        it.facilityId == ride.facilityId &&
                        it.source == ride.source
            }
        }

        savedParkRides.value = filtered + newRides
    }

    override suspend fun clearAllSavedParkRidesBySource(source: NswParkRideSandook.Companion.SavedParkRideSource) {
        savedParkRides.value = savedParkRides.value.filterNot { it.source == source.value }
    }

    override suspend fun getLastApiCallTimestamp(facilityId: String): Long? {
        return data.value.firstOrNull { it.facilityId == facilityId }?.timestamp
    }

    override suspend fun updateApiCallTimestamp(facilityId: String, timestamp: Long) {
        data.value = data.value.map { detail ->
            if (detail.facilityId == facilityId) detail.copy(timestamp = timestamp) else detail
        }
    }

    override fun getSavedParkRideByFacilityId(facilityId: String): SavedParkRide? {
        return savedParkRides.value.firstOrNull { it.facilityId == facilityId }
    }

    override suspend fun deleteSavedParkRide(stopId: String, facilityId: String) {
        savedParkRides.value = savedParkRides.value.filterNot {
            it.stopId == stopId && it.facilityId == facilityId
        }
    }
}
