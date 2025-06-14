package xyz.ksharma.krail.sandook

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow

interface NswParkRideSandook {

    // NSWParkRide Table methods
    fun getAll(): Flow<List<NSWParkRide>>
    fun getByStopIds(stopIds: List<String>): Flow<List<NSWParkRide>>
    suspend fun insertOrReplace(parkRide: NSWParkRide)
    suspend fun insertOrReplaceAll(parkRides: List<NSWParkRide>)
    suspend fun deleteAll()

    // SavedParkRide Table methods
    fun observeSavedParkRides(): Flow<List<SavedParkRide>>
    fun getFacilitiesByStopId(stopId: String): Flow<List<String>>
    suspend fun insertOrReplaceSavedParkRide(stopId: String, facilityId: String)
    suspend fun deleteSavedParkRide(stopId: String, facilityId: String)
    suspend fun clearSavedParkRides()
}

internal class RealNswParkRideSandook(
    private val factory: SandookDriverFactory,
    private val ioDispatcher: CoroutineDispatcher,
) : NswParkRideSandook {

    private val parkRideQueries: NswParkRideQueries by lazy {
        NswParkRideQueries(factory.createDriver())
    }

    // region NSWParkRide Table methods
    override fun getAll(): Flow<List<NSWParkRide>> =
        parkRideQueries.selectAll().asFlow().mapToList(ioDispatcher)

    override fun getByStopIds(stopIds: List<String>): Flow<List<NSWParkRide>> =
        parkRideQueries.selectByStopIds(stopIds)
            .asFlow()
            .mapToList(ioDispatcher)

    override suspend fun insertOrReplace(parkRide: NSWParkRide) {
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
            longitude = parkRide.longitude
        )
    }

    override suspend fun insertOrReplaceAll(parkRides: List<NSWParkRide>) {
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
                    longitude = parkRide.longitude
                )
            }
        }
    }

    override suspend fun deleteAll() {
        parkRideQueries.deleteAll()
    }
    // endregion

    // region SavedParkRide Table methods
    override fun observeSavedParkRides(): Flow<List<SavedParkRide>> =
        parkRideQueries.selectAllSavedParkRides().asFlow().mapToList(ioDispatcher)

    override fun getFacilitiesByStopId(stopId: String): Flow<List<String>> =
        parkRideQueries.selectFacilitiesByStopId(stopId).asFlow().mapToList(ioDispatcher)

    override suspend fun insertOrReplaceSavedParkRide(stopId: String, facilityId: String) {
        parkRideQueries.insertOrReplaceSavedParkRide(stopId, facilityId)
    }

    override suspend fun deleteSavedParkRide(stopId: String, facilityId: String) {
        parkRideQueries.deleteSavedParkRide(stopId, facilityId)
    }

    override suspend fun clearSavedParkRides() {
        parkRideQueries.clearSavedParkRides()
    }

    // endregion
}
