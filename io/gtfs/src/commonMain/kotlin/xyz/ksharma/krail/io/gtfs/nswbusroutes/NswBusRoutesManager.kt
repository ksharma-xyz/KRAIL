package xyz.ksharma.krail.io.gtfs.nswbusroutes

import app.krail.kgtfs.proto.NswBusRouteList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import krail.io.gtfs.generated.resources.Res
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.io.gtfs.nswstops.StopsManager
import xyz.ksharma.krail.sandook.NswBusRoutesSandook
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.sandook.SandookPreferences.Companion.KEY_NSW_BUS_ROUTES_VERSION
import xyz.ksharma.krail.sandook.SandookPreferences.Companion.NSW_BUS_ROUTES_VERSION
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.TimeSource

internal class NswBusRoutesManager(
    private val ioDispatcher: CoroutineDispatcher,
    private val nswBusRoutesSandook: NswBusRoutesSandook,
    private val preferences: SandookPreferences,
    // The bundled file is the only thing a test cannot reach, so it is the one seam.
    private val readRoutesBytes: suspend () -> ByteArray = { Res.readBytes("files/NSW_BUSES_ROUTES.pb") },
) : StopsManager {

    // Invoked from both RealAppStart and AppDataReadiness; serialise so concurrent first-launch
    // callers don't both run the full insert (see NswStopsManager for the same guard).
    private val insertMutex = Mutex()

    init {
        log("NswBusRoutesManager Initialized with version: $NSW_BUS_ROUTES_VERSION")
    }

    override suspend fun insertStops(): Result<Unit> = runCatching {
        insertMutex.withLock {
            log("NswBusRoutesManager Inserting NSW Bus Routes data if not already inserted")
            if (shouldInsertBusRoutes()) {
                insertNswBusRoutes()
            } else {
                log("NswBusRoutesManager Bus routes already inserted in the database.")
            }
            log("NswBusRoutesManager insertStops() completed successfully")
        }
    }.onFailure { error ->
        if (error is CancellationException) throw error
        logError("NswBusRoutesManager Error inserting bus routes: ${error.message}")
    }

    private suspend fun insertNswBusRoutes() {
        parseAndInsertBusRoutes()
        log("[NswBusRoutesManager] NSW Bus Routes parsed and inserted successfully.")
        preferences.setLong(
            key = KEY_NSW_BUS_ROUTES_VERSION,
            value = NSW_BUS_ROUTES_VERSION,
        )
        log("NswBusRoutesManager Bus routes inserted, version: $NSW_BUS_ROUTES_VERSION")
    }

    private fun shouldInsertBusRoutes(): Boolean {
        val storedVersion = preferences.getLong(KEY_NSW_BUS_ROUTES_VERSION) ?: 0
        log("NswBusRoutesManager Current version: $NSW_BUS_ROUTES_VERSION, Stored: $storedVersion")

        // Check if we need to insert based on version or route count
        val insertedRoutesCount = nswBusRoutesSandook.busRouteGroupsCount()
        return storedVersion < NSW_BUS_ROUTES_VERSION || insertedRoutesCount < MINIMUM_REQUIRED_ROUTES
    }

    /**
     * Reads and decodes the NSW bus routes from a protobuf file, then replaces the stored rows in
     * one transaction. The file is read and decoded first so the write lock is held only for the
     * insert.
     */
    private suspend fun parseAndInsertBusRoutes() = withContext(ioDispatcher) {
        val totalMark = TimeSource.Monotonic.markNow()
        log("NswBusRoutesManager Starting bus routes insertion...")

        val readMark = TimeSource.Monotonic.markNow()
        val byteArray = readRoutesBytes()
        log("NswBusRoutesManager ⏱️ File read time: ${readMark.elapsedNow().inWholeMilliseconds}ms")

        val decodeMark = TimeSource.Monotonic.markNow()
        val decodedRoutes = NswBusRouteList.ADAPTER.decode(byteArray)
        log(
            "NswBusRoutesManager ⏱️ Proto decode time: ${decodeMark.elapsedNow().inWholeMilliseconds}ms " +
                "(${decodedRoutes.routes.size} routes)",
        )

        val insertMark = TimeSource.Monotonic.markNow()
        insertRoutesInTransaction(decodedRoutes)
        log("NswBusRoutesManager ⏱️ Insert time: ${insertMark.elapsedNow().inWholeMilliseconds}ms")
        log("NswBusRoutesManager ⏱️ TOTAL time: ${totalMark.elapsedNow().inWholeMilliseconds}ms")
        log("NswBusRoutesManager Insertion complete")
    }

    private fun insertRoutesInTransaction(decoded: NswBusRouteList) {
        var totalStops = 0
        var totalVariants = 0
        var totalTrips = 0

        // One transaction: faster, and the clear commits with the insert, so readers keep the old
        // rows until then and a failure leaves them untouched.
        nswBusRoutesSandook.insertTransaction {
            nswBusRoutesSandook.clearNswBusRoutesData()
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

        log(
            "NswBusRoutesManager Inserted: ${decoded.routes.size} routes, " +
                "$totalVariants variants, $totalTrips trips, $totalStops stops",
        )
    }

    companion object {
        private const val MINIMUM_REQUIRED_ROUTES = 10 // Minimum expected routes
    }
}
