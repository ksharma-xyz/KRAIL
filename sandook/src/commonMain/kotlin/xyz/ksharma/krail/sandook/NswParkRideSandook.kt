package xyz.ksharma.krail.sandook

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow

interface NswParkRideSandook {

    // NSWParkRideFacilityDetail Table methods
    fun getAllParkRideFacilityDetail(): Flow<List<NSWParkRideFacilityDetail>>
    fun getByStopIds(stopIds: List<String>): List<NSWParkRideFacilityDetail>

    suspend fun insertOrReplace(parkRide: NSWParkRideFacilityDetail)
    suspend fun insertOrReplaceAll(parkRides: List<NSWParkRideFacilityDetail>)
    suspend fun deleteAll()

    // SavedParkRide Table methods

    fun observeSavedParkRides(): Flow<List<SavedParkRide>>

    suspend fun deleteSavedParkRide(stopId: String, facilityId: String)

    fun getFacilitiesByStopIdAndSource(
        stopId: String,
        source: SavedParkRideSource = SavedParkRideSource.SavedTrips,
    ): Flow<List<String>>

    /**
     * Inserts or replaces saved park rides with the given pairs of stopId and facilityId.
     * If a pair already exists, it will be replaced with the new value.
     */
    suspend fun insertOrReplaceSavedParkRides(
        parkRideInfoList: Set<SavedParkRide>,
        source: SavedParkRideSource,
    )

    /**
     * Clears all saved park rides where source is matching.
     */
    suspend fun clearAllSavedParkRidesBySource(source: SavedParkRideSource)

    companion object {
        /**
         * Represents the source of the saved park ride.
         * This is used to differentiate between saved trips and user-added Park and Ride facility.
         */
        sealed class SavedParkRideSource(val value: String) {
            data object SavedTrips : SavedParkRideSource("saved_trip")
            data object UserAdded : SavedParkRideSource("user")
        }
    }

    suspend fun getLastApiCallTimestamp(facilityId: String): Long?
    suspend fun updateApiCallTimestamp(facilityId: String, timestamp: Long)
    fun getSavedParkRideByFacilityId(facilityId: String): SavedParkRide?
}

internal class RealNswParkRideSandook(
    private val parkRideQueries: NswParkRideQueries,
    private val ioDispatcher: CoroutineDispatcher,
) : NswParkRideSandook {

    // region NSWParkRideFacilityDetail Table methods
    override fun getAllParkRideFacilityDetail(): Flow<List<NSWParkRideFacilityDetail>> =
        parkRideQueries.selectAll().asFlow().mapToList(ioDispatcher)

    override fun getByStopIds(stopIds: List<String>): List<NSWParkRideFacilityDetail> =
        parkRideQueries.selectByStopIds(stopIds).executeAsList()

    override suspend fun insertOrReplace(parkRide: NSWParkRideFacilityDetail) {
        parkRideQueries.insertOrReplace(
            facilityId = parkRide.facilityId,
            spotsAvailable = parkRide.spotsAvailable,
            totalSpots = parkRide.totalSpots,
            facilityName = parkRide.facilityName,
            percentageFull = parkRide.percentageFull,
            stopId = parkRide.stopId,
            timeText = parkRide.timeText,
            suburb = parkRide.suburb,
            address = parkRide.address,
            latitude = parkRide.latitude,
            longitude = parkRide.longitude,
            stopName = parkRide.stopName,
            timestamp = parkRide.timestamp,
        )
    }

    override suspend fun insertOrReplaceAll(parkRides: List<NSWParkRideFacilityDetail>) {
        parkRideQueries.transaction {
            parkRides.forEach { parkRide ->
                parkRideQueries.insertOrReplace(
                    facilityId = parkRide.facilityId,
                    spotsAvailable = parkRide.spotsAvailable,
                    totalSpots = parkRide.totalSpots,
                    facilityName = parkRide.facilityName,
                    percentageFull = parkRide.percentageFull,
                    stopId = parkRide.stopId,
                    timeText = parkRide.timeText,
                    suburb = parkRide.suburb,
                    address = parkRide.address,
                    latitude = parkRide.latitude,
                    longitude = parkRide.longitude,
                    stopName = parkRide.stopName,
                    timestamp = parkRide.timestamp,
                )
            }
        }
    }

    override suspend fun getLastApiCallTimestamp(facilityId: String): Long? {
        return parkRideQueries.getTimestampByFacilityId(facilityId).executeAsOneOrNull()
    }

    override suspend fun updateApiCallTimestamp(facilityId: String, timestamp: Long) {
        parkRideQueries.updateTimestampByFacilityId(timestamp, facilityId)
    }

    override suspend fun deleteAll() {
        parkRideQueries.deleteAll()
    }
    // endregion

    // region SavedParkRide Table methods

    override fun observeSavedParkRides(): Flow<List<SavedParkRide>> =
        parkRideQueries.selectAllSavedParkRides().asFlow().mapToList(ioDispatcher)

    override fun getFacilitiesByStopIdAndSource(
        stopId: String,
        source: NswParkRideSandook.Companion.SavedParkRideSource,
    ): Flow<List<String>> =
        parkRideQueries.selectFacilitiesByStopIdAndSource(stopId, source.value).asFlow()
            .mapToList(ioDispatcher)

    override suspend fun insertOrReplaceSavedParkRides(
        parkRideInfoList: Set<SavedParkRide>,
        source: NswParkRideSandook.Companion.SavedParkRideSource,
    ) {
        parkRideQueries.transaction {
            parkRideInfoList.forEach { info ->
                parkRideQueries.insertOrReplaceSavedParkRide(
                    stopId = info.stopId,
                    facilityId = info.facilityId,
                    stopName = info.stopName,
                    facilityName = info.facilityName,
                    source = source.value,
                )
            }
        }
    }

    override suspend fun deleteSavedParkRide(stopId: String, facilityId: String) {
        parkRideQueries.deleteSavedParkRide(stopId, facilityId)
    }

    override suspend fun clearAllSavedParkRidesBySource(source: NswParkRideSandook.Companion.SavedParkRideSource) {
        parkRideQueries.clearSavedParkRidesBySource(source.value)
    }

    // Add to NswParkRideSandook
    override fun getSavedParkRideByFacilityId(facilityId: String): SavedParkRide? {
        return parkRideQueries.getSavedParkRideByFacilityId(facilityId).executeAsOneOrNull()
    }

    // endregion
}
