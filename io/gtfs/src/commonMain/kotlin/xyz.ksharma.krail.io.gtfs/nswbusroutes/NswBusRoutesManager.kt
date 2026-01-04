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
            log("NswBusRoutesManager Starting to read proto file: NSW_BUSES_ROUTES.pb")

            // Clear existing data
            nswBusRoutesSandook.clearNswBusRoutesData()
            log("NswBusRoutesManager Cleared existing bus routes data")

            // Read and parse proto file
            val byteArray = Res.readBytes("files/NSW_BUSES_ROUTES.pb")
            log("NswBusRoutesManager Proto file read successfully, size: ${byteArray.size} bytes")

            val decodedRoutes = NswBusRouteList.ADAPTER.decode(byteArray)
            log("NswBusRoutesManager Proto decoded successfully, routes count: ${decodedRoutes.routes.size}")

            log("Start inserting bus routes. Currently ${nswBusRoutesSandook.busRouteGroupsCount()} routes in DB")
            val result = insertRoutesInTransaction(decodedRoutes)

            log("NswBusRoutesManager Insertion completed. Result: $result")
            result
        } catch (e: Exception) {
            logError("NswBusRoutesManager Exception during parseAndInsertBusRoutes: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private suspend fun insertRoutesInTransaction(decoded: NswBusRouteList) = withContext(ioDispatcher) {
        try {
            log("NswBusRoutesManager Starting transaction to insert ${decoded.routes.size} route groups")
            var totalStops = 0
            var totalVariants = 0
            var totalTrips = 0

            decoded.routes.forEachIndexed { routeIndex, routeGroup ->
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
                        trip.stopIds.forEachIndexed { index, stopId ->
                            nswBusRoutesSandook.insertBusTripStop(
                                tripId = trip.tripId,
                                stopId = stopId,
                                stopSequence = index,
                            )
                            totalStops++
                        }
                    }
                }

                // Log progress every 50 routes
                if ((routeIndex + 1) % 50 == 0) {
                    log("NswBusRoutesManager Progress: ${routeIndex + 1}/${decoded.routes.size} route groups processed")
                }
            }

            log("NswBusRoutesManager Successfully inserted ${decoded.routes.size} route groups, $totalVariants variants, $totalTrips trips, $totalStops trip stops.")
            true
        } catch (e: Exception) {
            logError("NswBusRoutesManager Exception during insertRoutesInTransaction: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    companion object {
        private const val MINIMUM_REQUIRED_ROUTES = 10 // Minimum expected routes
    }
}
