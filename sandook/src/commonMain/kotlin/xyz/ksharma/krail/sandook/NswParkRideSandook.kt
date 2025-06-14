package xyz.ksharma.krail.sandook

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow

interface NswParkRideSandook {
    fun getAll(): Flow<List<NSWParkRide>>
    fun getByStopIds(stopIds: List<String>): Flow<List<NSWParkRide>>
    suspend fun insertOrReplace(parkRide: NSWParkRide)
    suspend fun insertOrReplaceAll(parkRides: List<NSWParkRide>)
    suspend fun deleteAll()
}

internal class RealNswParkRideSandook(
    private val factory: SandookDriverFactory,
    private val ioDispatcher: CoroutineDispatcher,
) : NswParkRideSandook {

    private val queries: NswParkRideQueries by lazy {
        NswParkRideQueries(factory.createDriver())
    }

    override fun getAll(): Flow<List<NSWParkRide>> =
        queries.selectAll().asFlow().mapToList(ioDispatcher)

    override fun getByStopIds(stopIds: List<String>): Flow<List<NSWParkRide>> =
        queries.selectByStopIds(stopIds)
            .asFlow()
            .mapToList(ioDispatcher)

    override suspend fun insertOrReplace(parkRide: NSWParkRide) {
        queries.insertOrReplace(
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
        queries.transaction {
            parkRides.forEach { parkRide ->
                queries.insertOrReplace(
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
        queries.deleteAll()
    }
}
