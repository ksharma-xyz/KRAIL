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
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class NswStopsManager(
    private val ioDispatcher: CoroutineDispatcher,
    private val sandook: Sandook,
    private val preferences: SandookPreferences,
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
    @OptIn(ExperimentalTime::class)
    private suspend fun parseAndInsertStops(): Boolean = withContext(ioDispatcher) {
        val totalStartTime = Clock.System.now().toEpochMilliseconds()

        sandook.clearNswStopsTable()
        sandook.clearNswProductClassTable()

        val readStartTime = Clock.System.now().nanosecondsOfSecond
        val byteArray = Res.readBytes("files/NSW_STOPS.pb")
        val readTime = Clock.System.now().nanosecondsOfSecond - readStartTime
        log("NswStopsManager ⏱️ File read time: ${readTime}ms")

        val decodeStartTime = Clock.System.now().nanosecondsOfSecond
        val decodedStops = NswStopList.ADAPTER.decode(byteArray)
        val decodeTime = Clock.System.now().nanosecondsOfSecond - decodeStartTime
        log("NswStopsManager ⏱️ Proto decode time: ${decodeTime}ms")

        log("Start inserting stops. Currently ${sandook.stopsCount()} stops in the database")

        val insertStartTime = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val result = insertStopsInTransaction(decodedStops)
        val insertTime = kotlin.time.Clock.System.now().toEpochMilliseconds() - insertStartTime

        val totalTime = Clock.System.now().nanosecondsOfSecond - totalStartTime
        log("NswStopsManager ⏱️ Insert time: ${insertTime}ms")
        log("NswStopsManager ⏱️ TOTAL time: ${totalTime}ms (${decodedStops.nswStops.size} stops)")

        result
    }

    private suspend fun insertStopsInTransaction(decoded: NswStopList) = withContext(ioDispatcher) {
        sandook.insertTransaction {
            decoded.nswStops.forEach { nswStop ->
                sandook.insertNswStop(
                    stopId = nswStop.stopId,
                    stopName = nswStop.stopName,
                    stopLat = nswStop.lat,
                    stopLon = nswStop.lon,
                    isParent = nswStop.isParent,
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
