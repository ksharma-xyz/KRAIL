package xyz.ksharma.krail.io.gtfs.nswbusroutes

import app.krail.kgtfs.proto.NswBusRouteList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import krail.io.gtfs.generated.resources.Res
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.io.gtfs.nswstops.StopsManager
import xyz.ksharma.krail.sandook.NswBusRoutesSandook
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.sandook.SandookPreferences.Companion.KEY_NSW_BUS_ROUTES_VERSION
import xyz.ksharma.krail.sandook.SandookPreferences.Companion.NSW_BUS_ROUTES_VERSION

class NswBusRoutesManager(
    private val ioDispatcher: CoroutineDispatcher,
    private val nswBusRoutesSandook: NswBusRoutesSandook,
    private val preferences: SandookPreferences,
) : StopsManager {

    init {
        log("NswBusRoutesManager Initialized with version: $NSW_BUS_ROUTES_VERSION")
    }

    override suspend fun insertStops() = runCatching {
        log("NswBusRoutesManager Inserting NSW Bus Routes data if not already inserted")
        if (shouldInsertBusRoutes()) {
            insertNswBusRoutes()
        } else {
            log("NswBusRoutesManager Bus routes already inserted in the database.")
        }
        log("NswBusRoutesManager insertStops() completed successfully")
    }.getOrElse { error ->
        logError("NswBusRoutesManager Error inserting bus routes: ${error.message}")
        error.printStackTrace()
    }

    private suspend fun insertNswBusRoutes() {
        val insertResult = parseAndInsertBusRoutes()
        if (insertResult) {
            log("[NswBusRoutesManager] NSW Bus Routes parsed and inserted successfully.")
            preferences.setLong(
                key = KEY_NSW_BUS_ROUTES_VERSION,
                value = NSW_BUS_ROUTES_VERSION,
            )
            log("NswBusRoutesManager Bus routes inserted, version: $NSW_BUS_ROUTES_VERSION")
        } else {
            logError("NswBusRoutesManager Failed to insert NSW Bus Routes into the database.")
        }
    }

    private fun shouldInsertBusRoutes(): Boolean {
        val storedVersion = preferences.getLong(KEY_NSW_BUS_ROUTES_VERSION) ?: 0
        log("NswBusRoutesManager Current version: $NSW_BUS_ROUTES_VERSION, Stored: $storedVersion")

        // Check if we need to insert based on version or route count
        val insertedRoutesCount = nswBusRoutesSandook.busRouteGroupsCount()
        return storedVersion < NSW_BUS_ROUTES_VERSION || insertedRoutesCount < MINIMUM_REQUIRED_ROUTES
    }

    /**
     * Reads and decodes the NSW bus routes from a protobuf file, then inserts into the database.
     */
    private suspend fun parseAndInsertBusRoutes(): Boolean = withContext(ioDispatcher) {
        try {
            log("NswBusRoutesManager Starting bus routes insertion...")

            // Clear existing data
            nswBusRoutesSandook.clearNswBusRoutesData()

            // Read and parse proto file
            val byteArray = Res.readBytes("files/NSW_BUSES_ROUTES.pb")
            val decodedRoutes = NswBusRouteList.ADAPTER.decode(byteArray)

            // Insert all data in a transaction
            val result = insertRoutesInTransaction(decodedRoutes)

            log("NswBusRoutesManager Insertion complete")
            result
        } catch (e: Exception) {
            logError("NswBusRoutesManager Exception: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private suspend fun insertRoutesInTransaction(decoded: NswBusRouteList) = withContext(ioDispatcher) {
        try {
            var totalStops = 0
            var totalVariants = 0
            var totalTrips = 0

            // Wrap all insertions in a single transaction for much better performance
            nswBusRoutesSandook.insertTransaction {
                decoded.routes.forEach { routeGroup ->
                    // Insert route group (e.g., "702")
                    nswBusRoutesSandook.insertBusRouteGroup(routeGroup.routeShortName)

                    routeGroup.variants.forEach { variant ->
                        totalVariants++
                        // Insert route variant (e.g., "2504_702", "Blacktown to Seven Hills")
                        nswBusRoutesSandook.insertBusRouteVariant(
                            routeId = variant.routeId,
                            routeShortName = routeGroup.routeShortName,
                            routeName = variant.routeName,
                        )

                        variant.trips.forEach { trip ->
                            totalTrips++
                            // Insert trip option
                            nswBusRoutesSandook.insertBusTripOption(
                                tripId = trip.tripId,
                                routeId = variant.routeId,
                                headsign = trip.headsign,
                            )

                            // Insert ordered stops for this trip
                            // stopSequence preserves the order from the proto file (index 0, 1, 2, ...)
                            // This allows the database to efficiently return stops in the correct order
                            trip.stopIds.forEachIndexed { index, stopId ->
                                nswBusRoutesSandook.insertBusTripStop(
                                    tripId = trip.tripId,
                                    stopId = stopId,
                                    stopSequence = index, // Order from proto file
                                )
                                totalStops++
                            }
                        }
                    }
                }
            }

            log("NswBusRoutesManager Inserted: ${decoded.routes.size} routes, $totalVariants variants, $totalTrips trips, $totalStops stops")
            true
        } catch (e: Exception) {
            logError("NswBusRoutesManager Exception during insertion: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    companion object {
        private const val MINIMUM_REQUIRED_ROUTES = 10 // Minimum expected routes
    }
}
