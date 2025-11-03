package xyz.ksharma.krail.io.gtfs.nswstops

import app.krail.kgtfs.proto.NswStopList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import krail.io.gtfs.generated.resources.Res
import xyz.ksharma.krail.core.log.log
import xyz.ksharma.krail.core.log.logError
import xyz.ksharma.krail.io.gtfs.nswstops.StopsManager.Companion.MINIMUM_REQUIRED_NSW_STOPS
import xyz.ksharma.krail.sandook.Sandook
import xyz.ksharma.krail.sandook.SandookPreferences
import xyz.ksharma.krail.sandook.SandookPreferences.Companion.NSW_STOPS_VERSION
import xyz.ksharma.krail.sandook.SavedTripValidator

class NswStopsManager(
    private val ioDispatcher: CoroutineDispatcher,
    private val sandook: Sandook,
    private val preferences: SandookPreferences,
    private val savedTripValidator: SavedTripValidator,
) : StopsManager {

    init {
        log("NswStopsManager Initialized with NSW Stops version: $NSW_STOPS_VERSION: $this")
    }

    override suspend fun insertStops() = runCatching {
        log("NswStopsManager Inserting NSW Stops data if not already inserted: $this")
        if (shouldInsertNswStops()) {
            insertNswStops()
        } else {
            log("NswStopsManager Stops already inserted in the database.")
        }
    }.getOrElse { error ->
        log("NswStopsManager Error inserting stops: $error")
    }

    private suspend fun insertNswStops() {
        val insertResult = parseAndInsertStops()
        if (insertResult) {
            log("[NswStopsManager] NswStops parsed and inserted successfully.")
            preferences.setLong(
                key = SandookPreferences.KEY_NSW_STOPS_VERSION,
                value = NSW_STOPS_VERSION,
            )
            log("NswStopsManager NswStops inserted in the database, new version: $NSW_STOPS_VERSION.")

            // Validate saved trips after inserting new stops
            log("NswStopsManager Validating saved trips against new stops...")
            savedTripValidator.validateAllSavedTrips()
        } else {
            logError(
                message = "NswStopsManager Failed to insert NSW Stops into the database.",
            )
        }
    }

    private fun shouldInsertNswStops(): Boolean {
        val storedVersion = preferences.getLong(SandookPreferences.KEY_NSW_STOPS_VERSION) ?: 0
        log("RealAppStart Current NSW Stops data version: $NSW_STOPS_VERSION, Stored version: $storedVersion")
        val insertedStopsCount = sandook.stopsCount()
        return storedVersion < NSW_STOPS_VERSION || insertedStopsCount < MINIMUM_REQUIRED_NSW_STOPS
    }

    /**
     * Reads and decodes the NSW stops from a protobuf file, then inserts the stops into the database.
     */
    private suspend fun parseAndInsertStops(): Boolean = withContext(ioDispatcher) {
        sandook.clearNswStopsTable()
        sandook.clearNswProductClassTable()

        val byteArray = Res.readBytes("files/NSW_STOPS.pb")
        val decodedStops = NswStopList.ADAPTER.decode(byteArray)

        log("Start inserting stops. Currently ${sandook.stopsCount()} stops in the database")
        insertStopsInTransaction(decodedStops)
    }

    private suspend fun insertStopsInTransaction(decoded: NswStopList) = withContext(ioDispatcher) {
        sandook.insertTransaction {
            decoded.nswStops.forEach { nswStop ->
                sandook.insertNswStop(
                    stopId = nswStop.stopId,
                    stopName = nswStop.stopName,
                    stopLat = nswStop.lat,
                    stopLon = nswStop.lon,
                )
                nswStop.productClass.forEach { productClass ->
                    sandook.insertNswStopProductClass(
                        stopId = nswStop.stopId,
                        productClass = productClass,
                    )
                }
            }
            log("Inserted ${decoded.nswStops.size} stops into the database.")
            true
        }
    }
}
